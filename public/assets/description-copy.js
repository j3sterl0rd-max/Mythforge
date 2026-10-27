(() => {
 const content=document.getElementById('description-copy-content');
 const status=document.getElementById('copy-description-status');
 const copy=document.getElementById('copy-description');
 function select(){const range=document.createRange();range.selectNodeContents(content);const selection=window.getSelection();selection.removeAllRanges();selection.addRange(range);content.focus();}
 document.getElementById('select-description').addEventListener('click',()=>{select();status.textContent='Description selected. Press Ctrl+C (or Command+C), then paste into the visual editor.';});
 copy.addEventListener('click',async()=>{
  // Include only the description, never the preview controls or website navigation.
  const html=content.innerHTML;
  const plain=content.innerText;
  select();
  // A user-gesture selection copy preserves images in visual editors without
  // requiring an additional asynchronous clipboard permission prompt.
  if(document.execCommand('copy')){status.textContent='Copied with banners and formatting. Paste into CurseForge\'s visual editor.';return;}
  try {
   if(!navigator.clipboard?.write||typeof ClipboardItem==='undefined')throw new Error('Rich clipboard unavailable');
   await navigator.clipboard.write([new ClipboardItem({'text/html':new Blob([html],{type:'text/html'}),'text/plain':new Blob([plain],{type:'text/plain'})})]);
   status.textContent='Copied with banners and formatting. Paste into CurseForge\'s visual editor, not its Markdown or source view.';
  } catch {
   select();
   const copied=document.execCommand('copy');
   status.textContent=copied?'Copied. Paste into CurseForge\'s visual editor.':'Description selected. Press Ctrl+C (or Command+C), then paste into the visual editor.';
  }
 });
})();
