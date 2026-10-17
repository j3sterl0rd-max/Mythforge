"""Create a minimally changed CurseForge client export and verify preservation."""
from pathlib import Path
import copy, hashlib, json, re, zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = Path(r'C:\Users\Dad\Downloads\Mythforge - Beyond the Horizon-1.2.4.zip')
OUT = ROOT / 'client-release'
OUT.mkdir(exist_ok=True)

with zipfile.ZipFile(SOURCE) as old:
    manifest = json.loads(old.read('manifest.json'))
    links = re.findall(r'<li><a href="([^"]+)">([^<]+)', old.read('modlist.html').decode())
    assert len(links) == len(manifest['files'])
    assert manifest['version'] == '1.2.4'
    assert manifest['minecraft']['modLoaders'][0]['id'] == 'neoforge-21.1.253'
    original_files = {x['projectID']: x['fileID'] for x in manifest['files']}
    assert original_files[238222] == 8792638
    additions = [(1120180, 8846327, 'AStages (by Alessandro / _Kessy_)', 'astages'),
                 (889915, 8909889, 'FTB XMod Compat (by FTB)', 'ftb-xmod-compat')]
    for project, file_id, title, slug in additions:
        assert project not in original_files
        manifest['files'].append(dict(projectID=project, fileID=file_id, required=True))
    for item in manifest['files']:
        if item['projectID'] == 238222:
            item['fileID'] = 8815666  # JEI minimum required by XMod Compat
    manifest['version'] = '1.2.5'
    modlist = old.read('modlist.html').decode()
    extra = ''.join(f'<li><a href="https://www.curseforge.com/minecraft/mc-mods/{slug}">{title}</a></li>\n' for _, _, title, slug in additions)
    assert '</ul>' in modlist
    modlist = modlist.replace('</ul>', extra + '</ul>', 1)
    attribution = "Mythforge 1.2.5 client update\n\nMinecraft 1.21.1 / NeoForge 21.1.253 / Java 21.\nAdds AStages 2.5.3-1.21.1 and FTB XMod Compat 21.1.12.\nUpdates JEI from 19.51.0.418 to 19.53.0.425 to meet XMod Compat's dependency.\nNo client anticheat or mod-verification companion is included or required.\nOther original files and mod pins are preserved. No server credentials are included.\n\nAStages: https://www.curseforge.com/minecraft/mc-mods/astages/files/8846327\nFTB XMod Compat: https://www.curseforge.com/minecraft/mc-mods/ftb-xmod-compat/files/8909889\nJEI: https://www.curseforge.com/minecraft/mc-mods/jei/files/8815666\n\nImport this ZIP as a new CurseForge profile, allow publisher downloads, then launch.\nPackaging validation passed. Human client launch/login testing is still required.\nDo not copy server-only mods into this client.\n"
    changed = {'manifest.json': (json.dumps(manifest, indent=2)+'\n').encode(),
               'modlist.html': modlist.encode()}
    added = {'overrides/MYTHFORGE-CLIENT-UPDATE.txt': attribution.encode()}
    target = OUT/'Mythforge-Beyond-the-Horizon-1.2.5.zip'
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as new:
        for entry in old.infolist():
            new.writestr(copy.copy(entry), changed.get(entry.filename, old.read(entry.filename)))
        for name, data in added.items():
            assert name not in old.namelist()
            new.writestr(name, data)
    with zipfile.ZipFile(target) as new:
        assert new.testzip() is None
        preserved = [n for n in old.namelist() if n not in changed]
        assert all(old.read(n) == new.read(n) for n in preserved)
        assert set(new.namelist()) == set(old.namelist()) | set(added)
        current = {x['projectID']:x['fileID'] for x in manifest['files']}
        assert len(current) == len(manifest['files'])
        assert all(current[p] == f for p,f in original_files.items() if p != 238222)
    evidence = dict(source=str(SOURCE), output=str(target), version='1.2.5',
                    sha256=hashlib.sha256(target.read_bytes()).hexdigest(),
                    originalEntriesPreserved=len(preserved), zipCRCVerified=True,
                    originalModPinsPreservedExceptJEI=True,
                    addedPublisherReferences=additions, clientVerificationCompanionRequired=False,
                    jei=dict(projectID=238222,oldFileID=8792638,newFileID=8815666,version='19.53.0.425'),
                    humanClientLaunchVerified=False, humanLoginVerified=False)
    (ROOT/'ops/evidence/client-release.json').write_text(json.dumps(evidence,indent=2)+'\n',encoding='utf-8')
    (OUT/'SHA256SUMS.txt').write_text(evidence['sha256']+'  '+target.name+'\n',encoding='utf-8')
    (OUT/'CHANGELOG.txt').write_text(attribution,encoding='utf-8')
    print(json.dumps(evidence,indent=2))
