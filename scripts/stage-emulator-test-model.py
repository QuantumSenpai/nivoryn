"""Stage the pinned model as test inputs in a debug emulator, not as a public installer.

Real ModelImporter verification/activation remains exercised by RealLlamaDeviceTest.
Physical phones should use the ordinary documented content-picker import instead.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

p = argparse.ArgumentParser()
p.add_argument('--adb', required=True)
p.add_argument('--serial', required=True)
p.add_argument('--resources', type=Path, default=Path('/Volumes/ORBIT/Nivoryn/models/llama-3.2-1b-executorch-public'))
a = p.parse_args()
base = [a.adb, '-s', a.serial]
def adb(*args):
    return subprocess.check_output(base + list(args), text=True, timeout=30).strip()
assert adb('shell', 'getprop', 'ro.kernel.qemu') == '1', 'This helper is restricted to dedicated emulators'
package = 'dev.kyzenn.nivoryn'
adb('shell', 'run-as', package, 'mkdir', '-p', 'files/runtime-smoke')
expected = {
    'tokenizer.bin': (2183982, '82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55'),
    'model.pte': (1135951488, '8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b')}
for name, (size, digest) in expected.items():
    path = a.resources / name
    assert path.stat().st_size == size
    with path.open('rb') as stream:
        assert hashlib.file_digest(stream, 'sha256').hexdigest() == digest, 'Unexpected test resource'
    with path.open('rb') as stream:
        subprocess.run(base + ['exec-in', 'run-as', package, 'dd', 'of=files/runtime-smoke/' + name, 'bs=65536'],
                       stdin=stream, check=True, timeout=180)
    assert adb('shell', 'run-as', package, 'sha256sum', 'files/runtime-smoke/' + name).split()[0] == digest
    print(json.dumps({'testInput': name, 'bytes': size, 'sha256': digest, 'verifiedOnEmulator': True}))
