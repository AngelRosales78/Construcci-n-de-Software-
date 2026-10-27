"""T67: proveedor portátil local, con archivos oficiales verificados fuera de Git."""
from pathlib import Path
import hashlib
import json
import re
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2] / 'local_ai'
ROOT.mkdir(exist_ok=True)


def get(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'SP500-academic-local-setup'})
    return urllib.request.urlopen(request, timeout=120)


def download(url, path, digest):
    if not path.exists():
        print('Descargando', path.name, flush=True)
        partial = path.with_suffix(path.suffix + '.partial')
        with get(url) as response, partial.open('wb') as output:
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
        partial.replace(path)
    actual = hashlib.file_digest(path.open('rb'), 'sha256').hexdigest()
    if actual != digest:
        raise RuntimeError(f'Hash inválido para {path.name}; no se ejecuta el archivo.')
    print('SHA-256 comprobado:', path.name, flush=True)


release = json.load(get('https://api.github.com/repos/ggml-org/llama.cpp/releases/latest'))
if not any(a['name'].endswith('bin-win-cpu-x64.zip') for a in release['assets']):
    marker = next(a for a in release['assets'] if a['name'] == 'nightly-tag.txt')
    tag = get(marker['browser_download_url']).read().decode().strip()
    if not re.fullmatch(r'b[0-9]+', tag):
        raise RuntimeError('Versión oficial no reconocida')
    release = json.load(get('https://api.github.com/repos/ggml-org/llama.cpp/releases/tags/' + tag))
asset = next(a for a in release['assets'] if a['name'].endswith('bin-win-cpu-x64.zip'))
if not asset.get('digest', '').startswith('sha256:'):
    raise RuntimeError('El paquete no publica SHA-256; se requiere verificación manual.')
archive = ROOT / asset['name']
download(asset['browser_download_url'], archive, asset['digest'].split(':')[1])
binary = ROOT / 'llama'
binary.mkdir(exist_ok=True)
with zipfile.ZipFile(archive) as z:
    for member in z.infolist():
        target = (binary / member.filename).resolve()
        if not target.is_relative_to(binary.resolve()):
            raise RuntimeError('Ruta de archivo fuera del destino')
    z.extractall(binary)
repo = 'Qwen/Qwen2.5-1.5B-Instruct-GGUF'
files = json.load(get('https://huggingface.co/api/models/' + repo + '/tree/main'))
model = next(f for f in files if f['path'] == 'qwen2.5-1.5b-instruct-q4_k_m.gguf')
download('https://huggingface.co/' + repo + '/resolve/main/' + model['path'], ROOT / model['path'], model['lfs']['oid'])
(ROOT / 'provenance.json').write_text(json.dumps({'engine': release['tag_name'], 'binary': asset['browser_download_url'],
    'binary_sha256': asset['digest'], 'model': repo, 'model_sha256': model['lfs']['oid']}, indent=2), encoding='utf-8')
print('Proveedor local preparado en', ROOT, flush=True)
