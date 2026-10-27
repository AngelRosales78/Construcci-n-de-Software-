"""T67: proveedor portátil local, con archivos oficiales verificados fuera de Git."""
from pathlib import Path
import hashlib
import json
import re
import time
import urllib.request
import zipfile
import shutil
from concurrent.futures import ThreadPoolExecutor

ROOT = Path(__file__).resolve().parents[2] / 'local_ai'
ROOT.mkdir(exist_ok=True)


def get(url, extra_headers=None):
    request = urllib.request.Request(url, headers={'User-Agent': 'SP500-academic-local-setup'} | (extra_headers or {}))
    return urllib.request.urlopen(request, timeout=45)


def download(url, path, digest, expected_size=0):
    if path.exists() and expected_size and path.stat().st_size != expected_size:
        quarantine = path.with_suffix(path.suffix + '.unverified')
        if quarantine.exists():
            raise RuntimeError('Ya existe una descarga incompleta en cuarentena; revisar antes de continuar.')
        path.rename(quarantine)
        print('Descarga incompleta apartada; no se ejecuta.', flush=True)
    if not path.exists():
        print('Descargando', path.name, flush=True)
        partial = path.with_suffix(path.suffix + '.partial')
        if expected_size > 500_000_000:
            offset = partial.stat().st_size if partial.exists() else 0
            spans = [(start, min(start + 32*1024*1024, expected_size) - 1)
                     for start in range(offset, expected_size, 32*1024*1024)]
            def part(span):
                start, end = span
                target = path.with_suffix(path.suffix + f'.chunk.{start}')
                if target.exists() and target.stat().st_size == end-start+1:
                    return target
                for attempt in range(4):
                    try:
                        with get(url + f'?download=true&part={start}&attempt={attempt}', {'Range': f'bytes={start}-{end}'}) as response:
                            if response.status != 206 or not response.headers.get('Content-Range', '').startswith(f'bytes {start}-'):
                                raise RuntimeError('La fuente no devolvió el rango solicitado')
                            with target.open('wb') as output:
                                shutil.copyfileobj(response, output, length=1024*1024)
                        if target.stat().st_size != end-start+1:
                            raise OSError('Rango incompleto')
                        return target
                    except (OSError, TimeoutError):
                        if attempt == 3: raise
                raise RuntimeError('No se descargó el rango')
            with ThreadPoolExecutor(max_workers=6) as workers:
                pieces = list(workers.map(part, spans))
            joined = path.with_suffix(path.suffix + '.joining')
            with joined.open('wb') as output:
                if offset:
                    with partial.open('rb') as prefix: shutil.copyfileobj(prefix, output)
                for piece in pieces:
                    with piece.open('rb') as incoming: shutil.copyfileobj(incoming, output)
            if joined.stat().st_size != expected_size:
                raise RuntimeError('El tamaño final no coincide')
            joined.replace(partial)
            for piece in pieces: piece.unlink()
        else:
            _download_single(url, partial)
        partial.replace(path)
    actual = hashlib.file_digest(path.open('rb'), 'sha256').hexdigest()
    if actual != digest:
        raise RuntimeError(f'Hash inválido para {path.name}; no se ejecuta el archivo.')
    print('SHA-256 comprobado:', path.name, flush=True)


def _download_single(url, partial):
        for attempt in range(6):
            offset = partial.stat().st_size if partial.exists() else 0
            try:
                with get(url + ('&' if '?' in url else '?') + f'resume={offset}', {'Range': f'bytes={offset}-'} if offset else {}) as response:
                    mode = 'ab' if offset and response.status == 206 else 'wb'
                    with partial.open(mode) as output:
                        while chunk := response.read(1024 * 1024):
                            output.write(chunk)
                break
            except (OSError, TimeoutError):
                if attempt == 5:
                    raise
                print('Reanudando descarga verificada...', flush=True)
                time.sleep(1)


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
binary = ROOT / 'llama' / release['tag_name']
binary.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(archive) as z:
    for member in z.infolist():
        target = (binary / member.filename).resolve()
        if not target.is_relative_to(binary.resolve()):
            raise RuntimeError('Ruta de archivo fuera del destino')
    if not (binary / 'llama-server.exe').exists():
        z.extractall(binary)
repo = 'Qwen/Qwen3-1.7B-GGUF'
files = json.load(get('https://huggingface.co/api/models/' + repo + '/tree/main'))
model = next(f for f in files if f['path'] == 'Qwen3-1.7B-Q8_0.gguf')
download('https://huggingface.co/' + repo + '/resolve/main/' + model['path'], ROOT / model['path'], model['lfs']['oid'], model['size'])
(ROOT / 'provenance.json').write_text(json.dumps({'engine': release['tag_name'], 'binary': asset['browser_download_url'],
    'binary_sha256': asset['digest'], 'model': repo, 'model_sha256': model['lfs']['oid']}, indent=2), encoding='utf-8')
print('Proveedor local preparado en', ROOT, flush=True)
