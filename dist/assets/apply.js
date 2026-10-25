(() => {
  const form = document.querySelector('#whitelist-form');
  if (!form) return;
  const suggestedUsername = new URLSearchParams(window.location.search).get('username');
  if (suggestedUsername && /^[A-Za-z0-9_]{3,16}$/.test(suggestedUsername)) {
    form.elements.username.value = suggestedUsername;
  }
  const button = form.querySelector('button[type="submit"]');
  const status = document.querySelector('#application-status');
  const result = document.querySelector('#application-result');
  let proof = '', widget, busy = false;
  const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
  const request = async (path, options = {}) => {
    const response = await fetch(path, {...options, cache: 'no-store', signal: AbortSignal.timeout(12000)});
    if (!response.ok) throw Error('Unavailable');
    return response.json();
  };
  function show(approved) {
    status.hidden = true; result.hidden = false;
    result.querySelector('h2').textContent = approved ? 'Your Whitelist Application Has Been Approved!' : 'Your Whitelist Application Was Not Approved';
    result.querySelector('p').textContent = approved
      ? 'Congratulations! Your Minecraft account has been approved to join Mythforge: Beyond the Horizon. You can now connect to the server using the same Minecraft account you applied with.'
      : 'We were unable to approve your whitelist application. Please contact Mythforge staff to continue the whitelist process.';
    result.querySelector('[data-approved]').hidden = !approved;
    result.querySelector('[data-support]').hidden = approved;
    result.focus();
  }
  window.mythforgeTurnstileReady = () => {
    widget = window.turnstile.render('#application-check', {
      sitekey: form.dataset.sitekey, action: 'whitelist', theme: 'dark',
      callback: token => {proof = token; if (!busy) button.disabled = false;},
      'expired-callback': () => {proof = ''; button.disabled = true;},
      'error-callback': () => {proof = ''; button.disabled = true;}
    });
  };
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (busy || !proof || !form.reportValidity()) return;
    busy = true; button.disabled = true;
    form.querySelector('input').readOnly = true;
    status.hidden = false; result.hidden = true;
    const started = performance.now(); let approved = false;
    try {
      const job = await request('/api/applications', {method: 'POST', headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({username: form.elements.username.value, token: proof})});
      if (job.reference) {
        while (performance.now() - started < 90000) {
          await pause(2000);
          const state = await request('/api/applications/status', {headers: {Authorization: `Bearer ${job.reference}`}});
          if (state.state === 'approved') {approved = true; break;}
          if (state.state !== 'processing') break;
        }
      }
    } catch { /* Every unresolved application uses the same support outcome. */ }
    await pause(Math.max(0, 5000 - (performance.now() - started)));
    show(approved);
    if (widget !== undefined) window.turnstile.remove(widget);
  });
  request('/api/applications/config').then(config => {
    if (!config.available || !config.sitekey) return;
    form.dataset.sitekey = config.sitekey;
    form.hidden = false; document.querySelector('#application-unavailable').hidden = true;
    const script = document.createElement('script');
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?onload=mythforgeTurnstileReady&render=explicit';
    script.async = true; document.head.append(script);
  }).catch(() => {});
})();
