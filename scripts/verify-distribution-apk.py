"""Fail closed before publishing a standalone Arm64 preview APK; no device claims."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import zipfile

release = json.loads((Path(__file__).resolve().parents[1] / 'web/release.json').read_text())
p = argparse.ArgumentParser()
p.add_argument('apk', type=Path)
p.add_argument('--sdk', type=Path, default=Path(os.environ.get('ANDROID_HOME', '/Volumes/ORBIT/Nivoryn/android-sdk')))
p.add_argument('--version', default=release['version'])
p.add_argument('--version-code', type=int, default=release['versionCode'])
a = p.parse_args()
build = a.sdk / 'build-tools/35.0.0'
env = os.environ.copy()
jdk = '/Volumes/ORBIT/Nivoryn/tooling/jdk/jdk-17.0.17+10/Contents/Home'
if 'JAVA_HOME' not in env and Path(jdk).is_dir():
    env['JAVA_HOME'] = jdk

def run(tool, *args):
    return subprocess.check_output([str(build / tool), *map(str, args)], text=True, env=env, timeout=60)

badging = run('aapt', 'dump', 'badging', a.apk)
manifest = run('aapt', 'dump', 'xmltree', a.apk, 'AndroidManifest.xml')
assert "name='dev.kyzenn.nivoryn'" in badging, 'Unexpected package identity'
assert f"versionName='{a.version}'" in badging, 'Unexpected app version'
assert f"versionCode='{a.version_code}'" in badging, 'Unexpected upgrade versionCode'
assert 'application-debuggable' not in badging, 'Never distribute the developer debug variant'
assert not re.search(r'android:testOnly.*(?:0xffffffff|true)', manifest), 'Test-only APK is not a public download'
assert "native-code: 'arm64-v8a'" in badging, 'Public build must declare the supported Arm64 ABI'
assert 'android.permission.INTERNET' not in badging, 'Offline permission boundary changed'
certificates = run('apksigner', 'verify', '--verbose', '--print-certs', a.apk)
assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in certificates
assert '84a0da97e2b069e89cbc70e9259609e7a6f2de3e87e90886eee6d9c823385b76' in certificates, 'Existing preview signing identity must be preserved'
run('zipalign', '-c', '-P', '16', '4', a.apk)
libraries = []
classes = set()
with zipfile.ZipFile(a.apk) as z:
    assert z.testzip() is None, 'Corrupt archive'
    for name in z.namelist():
        if re.fullmatch(r'classes\d*\.dex', name):
            data = z.read(name)
            assert data.startswith(b'dex\n'), 'Expected standard DEX'
            strings = struct.unpack_from('<I', data, 60)[0]
            types = struct.unpack_from('<I', data, 68)[0]
            count, definitions = struct.unpack_from('<II', data, 96)
            for i in range(count):
                type_index = struct.unpack_from('<I', data, definitions + 32*i)[0]
                string_index = struct.unpack_from('<I', data, types + 4*type_index)[0]
                offset = struct.unpack_from('<I', data, strings + 4*string_index)[0]
                while data[offset] & 128:
                    offset += 1
                offset += 1  # Skip the UTF-16 length ULEB; class descriptors are ASCII.
                classes.add(data[offset:data.index(0, offset)].decode('utf8'))
        if not name.endswith('.so'):
            continue
        assert name.startswith('lib/arm64-v8a/'), 'Unsupported public ABI'
        data = z.read(name)
        assert data[:5] == b'\x7fELF\x02', 'Expected 64-bit ELF'
        endian = '<' if data[5] == 1 else '>'
        phoff = struct.unpack_from(endian + 'Q', data, 32)[0]
        size, count = struct.unpack_from(endian + 'HH', data, 54)
        loads = []
        for i in range(count):
            offset = phoff + i * size
            if struct.unpack_from(endian + 'I', data, offset)[0] == 1:
                alignment = struct.unpack_from(endian + 'Q', data, offset + 48)[0]
                assert alignment >= 16384 and alignment & (alignment - 1) == 0, name + ' is not 16 KB ELF aligned'
                loads.append(alignment)
        assert loads, 'Missing ELF load segments'
        libraries.append(name)
assert 'lib/arm64-v8a/libexecutorch.so' in libraries, 'Mandatory runtime missing'
assert 'lib/arm64-v8a/libmlkit_google_ocr_pipeline.so' in libraries, 'Offline OCR missing'
required = {'Lorg/pytorch/executorch/' + c + ';' for c in [
    'Module', 'EValue', 'Tensor', 'ExecuTorchRuntime', 'ExecutorchRuntimeException',
    'extension/llm/LlmModule', 'extension/llm/LlmCallback', 'extension/asr/AsrCallback',
    'training/TrainingModule', 'training/SGD']}
assert required <= classes, 'JNI entry classes stripped/renamed: ' + ', '.join(sorted(required - classes))
with a.apk.open('rb') as f:
    digest = hashlib.file_digest(f, 'sha256').hexdigest()
print(json.dumps({'version': a.version, 'bytes': a.apk.stat().st_size, 'sha256': digest,
    'nonDebuggable': True, 'standalone': True, 'samePreviewCertificate': True,
    'zip16K': True, 'elf16K': True, 'jniClassesPreserved': True, 'libraries': libraries,
    'scope': 'Static distribution checks; not phone installation or Play Protect approval'}, indent=2))
