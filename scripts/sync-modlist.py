"""Import the published client ZIP's mod list; no API key or local server required."""
import argparse
import io
import json
import pathlib
import re
import tomllib
import urllib.parse
import urllib.request
import zipfile
from html.parser import HTMLParser

ROOT = pathlib.Path(__file__).resolve().parents[1]
USER_AGENT = 'Mythforge-website-modlist/1.0 (+https://mythforge.win)'


def fetch(url, limit):
    request = urllib.request.Request(url, headers={'User-Agent': USER_AGENT})
    with urllib.request.urlopen(request, timeout=120) as response:
        data = response.read(limit + 1)
    if len(data) > limit:
        raise ValueError('Source exceeds supported size')
    return data


def project_url(value):
    parsed = urllib.parse.urlsplit(value)
    if parsed.scheme != 'https' or parsed.netloc != 'www.curseforge.com':
        raise ValueError('Unexpected mod project host')
    if not re.fullmatch(r'/minecraft/(?:mc-mods|texture-packs|data-packs|customization)/[a-z0-9-]+/?', parsed.path):
        raise ValueError('Unexpected mod project path')
    return value.rstrip('/')


class ModLinks(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.entries = []
        self.href = None
        self.parts = []

    def handle_starttag(self, tag, attrs):
        if tag == 'a':
            self.href = project_url(dict(attrs).get('href', ''))
            self.parts = []

    def handle_data(self, data):
        if self.href:
            self.parts.append(data)

    def handle_endtag(self, tag):
        if tag == 'a' and self.href:
            label = ''.join(self.parts).strip().replace('\u2014', '-')
            match = re.fullmatch(r'(.+) \(by (.+)\)', label)
            self.entries.append({'name': match[1] if match else label,
                                 'author': match[2] if match else '',
                                 'url': self.href, 'bundled': False})
            self.href = None


def import_zip(data):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        def small(name, limit=2_000_000):
            if archive.getinfo(name).file_size > limit:
                raise ValueError('Oversized release metadata')
            return archive.read(name)
        manifest = json.loads(small('manifest.json'))
        parser = ModLinks()
        parser.feed(small('modlist.html').decode('utf-8-sig'))
        entries = parser.entries
        if not entries or len(entries) != len(manifest['files']):
            raise ValueError('Mod-list count does not match the release manifest')
        if len({entry['url'] for entry in entries}) != len(entries):
            raise ValueError('Duplicate project links in release')
        for name in archive.namelist():
            if not name.startswith('overrides/mods/') or not name.endswith('.jar'):
                continue
            entry = {'name': pathlib.PurePosixPath(name).name, 'author': '',
                     'url': None, 'bundled': True}
            with zipfile.ZipFile(io.BytesIO(small(name, 50_000_000))) as jar:
                for metadata in ['META-INF/neoforge.mods.toml', 'META-INF/mods.toml']:
                    if metadata not in jar.namelist():
                        continue
                    if jar.getinfo(metadata).file_size > 1_000_000:
                        raise ValueError('Oversized bundled mod metadata')
                    descriptor = tomllib.loads(jar.read(metadata).decode('utf-8-sig'))
                    mod = descriptor.get('mods', [{}])[0]
                    entry['name'] = mod.get('displayName') or entry['name']
                    entry['author'] = str(mod.get('authors') or '')
                    break
            entries.append(entry)
        entries.sort(key=lambda entry: (entry['name'].casefold(), entry['url'] or ''))
        return manifest, entries


def main():
    arguments = argparse.ArgumentParser()
    arguments.add_argument('--archive', type=pathlib.Path, help='Seed from the matching published ZIP already downloaded')
    args = arguments.parse_args()
    config_path = ROOT / 'data/site.json'
    config = json.loads(config_path.read_text(encoding='utf-8'))
    project_id = config['curseforge']['projectId']
    feed = json.loads(fetch(f'https://api.cfwidget.com/{project_id}', 5_000_000))
    if feed.get('id') != project_id:
        raise ValueError('Unexpected pack identity')
    releases = [file for file in feed['files'] if file.get('type') == 'release'
                and config['minecraftVersion'] in file.get('versions', [])
                and 'NeoForge' in file.get('versions', [])
                and 'server' not in file.get('name', '').lower()]
    releases.sort(key=lambda file: file['uploaded_at'], reverse=True)
    if not releases:
        raise ValueError('No compatible stable client release found; existing list preserved')
    release = releases[0]
    output = ROOT / 'data/mods.json'
    previous = json.loads(output.read_text(encoding='utf-8')) if output.exists() else {}
    if previous.get('fileId', 0) > release['id']:
        raise ValueError('Release discovery is stale; existing list preserved')
    if previous.get('fileId') == release['id'] and not args.archive:
        print('Published release unchanged; existing mod list retained.')
        return
    file_id = release['id']
    if not isinstance(file_id, int) or not release['name'].endswith('.zip') or '/' in release['name']:
        raise ValueError('Invalid release file')
    download = f"https://edge.forgecdn.net/files/{file_id // 1000}/{file_id % 1000:03d}/{urllib.parse.quote(release['name'], safe='')}"
    data = args.archive.read_bytes() if args.archive else fetch(download, 300_000_000)
    if len(data) != release['filesize']:
        raise ValueError('Archive size does not match published file')
    manifest, entries = import_zip(data)
    version = manifest['version']
    if manifest['minecraft']['version'] != config['minecraftVersion']:
        raise ValueError('Release Minecraft version mismatch')
    if not re.fullmatch(r'\d+(?:\.\d+)+', version):
        raise ValueError('Invalid release version')
    result = {'version': version, 'fileId': file_id, 'publishedAt': release['uploaded_at'],
              'sourceUrl': config['curseforge']['source'] + f'/files/{file_id}', 'mods': entries}
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'Imported {len(entries)} included mods from published release {version}.')


if __name__ == '__main__':
    main()
