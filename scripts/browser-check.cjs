const {chromium}=require('C:/Users/Dad/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('node:fs');
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'msedge'});const page=await browser.newPage();const errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://crafatar.com/**',route=>route.abort());
 const routes=['/','/beta-testers/','/modpack/','/server/','/getting-started/','/support/','/rules/','/changelog/','/privacy/','/terms/','/refunds/','/contact/','/links/','/checkout/','/purchase-help/'];
 for(const width of [320,390,768,1024,1440]){
  await page.setViewportSize({width,height:900});
  for(const route of routes){
   const response=await page.goto('http://localhost:8080'+route);if(response.status()!==200)throw Error(route+' HTTP '+response.status());
   if(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth))throw Error('Overflow '+width+' '+route);
   if(await page.locator('body').innerText().then(t=>t.includes(String.fromCharCode(8212))))throw Error('Em dash '+route);
  }
 }
 await page.goto('http://localhost:8080/beta-testers/');
 await page.waitForTimeout(500);
 if(await page.locator('.tester-list li').count()!==8)throw Error('Roster size');
 if((await page.locator('.tester-list').innerText()).toLowerCase().includes('procyonine'))throw Error('Excluded player');
 if(await page.locator('[data-player-head]:not([hidden])').count())throw Error('Head fallback');
 await page.evaluate(()=>{const p=document.createElement('p');p.textContent='Imported'+String.fromCharCode(8212)+'text';p.setAttribute('aria-label',p.textContent);p.id='import-test';document.body.append(p)});
 await page.waitForTimeout(100);if(await page.locator('#import-test').getAttribute('aria-label')!=='Imported-text')throw Error('Imported dash label');
 await page.setViewportSize({width:390,height:844});await page.goto('http://localhost:8080/');await page.locator('.menu-toggle').click();if(await page.locator('.menu-toggle').getAttribute('aria-expanded')!=='true')throw Error('Menu');await page.keyboard.press('Escape');
 await page.setViewportSize({width:1440,height:1000});await page.screenshot({path:'website-source/docs/home-desktop.png',fullPage:true});
 await page.goto('http://localhost:8080/beta-testers/');await page.screenshot({path:'website-source/docs/testers-desktop.png',fullPage:true});
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:'website-source/docs/testers-mobile.png',fullPage:true});
 if(errors.length)throw Error(errors.join('\n'));
 console.log('PASS: 75 page/viewport combinations, no overflow or JavaScript errors; menu, imported text/labels, exact roster, and failed head fallback.');
 await page.unroute('https://crafatar.com/**');
 await page.goto('http://localhost:8080/beta-testers/');await page.waitForTimeout(5000);
 console.log('Live heads loaded: '+await page.locator('[data-player-head]').evaluateAll(imgs=>imgs.filter(i=>i.naturalWidth>0).length)+'/8');
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});

