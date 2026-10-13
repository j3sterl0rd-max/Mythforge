from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root = Path(__file__).resolve().parent.parent
output = root.parent / 'Mythforge-Direct-Upload.zip'
with ZipFile(output, 'w', ZIP_DEFLATED) as archive:
    for file in sorted((root / 'dist').rglob('*')):
        if file.is_file():
            archive.write(file, file.relative_to(root / 'dist'))
print(output)