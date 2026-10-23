"""Archive published release records; fetch full official notes when an API key is configured."""
import argparse, datetime, html, json, os, pathlib, re, urllib.request
ROOT = pathlib.Path(__file__).resolve().parents[1]
UA = 'Mythforge-release-journal/1.0 (+https://mythforge.win)'
def fetch(url, key=None):
    headers={'User-Agent':UA,'Accept':'application/json'}
    if key: headers['x-api-key']=key
    with urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=45) as response:
        data=response.read(8_000_001)
    if len(data)>8_000_000: raise ValueError('Release metadata too large')
    return json.loads(data)
def plain(value):
    value=re.sub(r'<(script|style)\b[^>]*>[\s\S]*?</\1>','',value,flags=re.I)
    value=re.sub(r'</(?:p|li|h[1-6])>|<br\s*/?>','\n',value,flags=re.I)
    return re.sub(r'\n{3,}','\n\n',html.unescape(re.sub(r'<[^>]+>','',value))).replace('\u2014','-').strip()
def merge(config,files,notes):
    old={r.get('sourceUrl'):r for r in config['changelog'] if r.get('sourceUrl')}
    records={r['version']:r for r in config['changelog']}
    for f in files:
        if f.get('type')!='release' or 'server' in f['name'].lower() or config['minecraftVersion'] not in f.get('versions',[]) or 'NeoForge' not in f.get('versions',[]): continue
        match=re.search(r'(\d+(?:\.\d+)+)\.zip$',f['name'],re.I)
        if not match:continue
        version=match[1];url=config['curseforge']['source']+'/files/'+str(f['id'])
        previous=old.get(url,records.get(version,{}))
        record={**previous,'version':version,'date':f['uploaded_at'][:10],'title':'Mythforge '+version,'sourceUrl':url}
        if f['id'] in notes and notes[f['id']]:record['notes']=plain(notes[f['id']])
        # Missing upstream notes never erase archived notes or invent patch contents.
        records[version]=record
    config['changelog']=sorted(records.values(),key=lambda r:(r['date'],tuple(int(n) for n in re.findall(r'\d+',r['version']))),reverse=True)
    return config

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--feed',type=pathlib.Path);args=parser.parse_args()
    path=ROOT/'data/site.json';config=json.loads(path.read_text(encoding='utf-8'));pid=config['curseforge']['projectId']
    feed=json.loads(args.feed.read_text(encoding='utf-8')) if args.feed else fetch('https://api.cfwidget.com/'+str(pid))
    if feed.get('id')!=pid:raise ValueError('Project identity mismatch')
    key=os.environ.get('CURSEFORGE_API_KEY');notes={}
    if key:
        for f in feed['files']:
            if f.get('type')=='release' and 'server' not in f['name'].lower():
                notes[f['id']]=fetch(f'https://api.curseforge.com/v1/mods/{pid}/files/{f["id"]}/changelog',key)['data']
    config=merge(config,feed['files'],notes)
    path.write_text(json.dumps(config,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Archived '+str(len(config['changelog']))+' release records; '+('official notes synchronized.' if key else 'official notes links retained; API key needed to import new note text.'))
if __name__=='__main__':main()
