"""Package the current native source, APKs and actual recording on the resource SSD.

No weights, account caches, signing keys or local.properties enter the source archive.
The promotional website stays in the original repository and is not altered/packaged here.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

p = argparse.ArgumentParser()
p.add_argument('--output', type=Path, required=True)
p.add_argument('--video', type=Path, required=True)
a = p.parse_args()
root = Path(__file__).resolve().parents[1]
out = a.output.resolve()
out.mkdir(parents=True, exist_ok=True)
def digest(path):
    with path.open('rb') as f:
        return hashlib.file_digest(f, 'sha256').hexdigest()

files = []
allowed = ['app/src', 'gradle/wrapper', 'scripts', 'tools', 'evaluation', 'assets/readme']
for directory in allowed:
    for path in (root / directory).rglob('*'):
        if not path.is_file() or '__pycache__' in path.parts or path.suffix == '.pyc' or any(part.startswith('._') or part == '.DS_Store' for part in path.parts):
            continue
        if path.is_symlink() or not path.resolve().is_relative_to(root):
            raise SystemExit('Refusing a source symlink outside the reviewed snapshot')
        if path.suffix in ['.pte', '.pth', '.safetensors', '.gguf', '.keystore', '.jks', '.key', '.pem', '.p12', '.pfx'] or path.name.startswith('.env') or path.stat().st_size > 20_000_000:
            raise SystemExit('Unexpected heavy/secret file in source snapshot: ' + str(path.relative_to(root)))
        files.append(path)
for name in ['build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat', 'app/build.gradle.kts', 'app/proguard-rules.pro', 'app/gradle.lockfile', 'LICENSE', 'README.md', 'NOTICE.md', 'THIRD_PARTY_NOTICES.md']:
    files.append(root / name)
files += list((root / 'docs').glob('*.md'))
files += [p for p in (root / 'docs/reports/llama-runtime-2026-11-23').glob('*') if p.is_file()]
files += [p for p in (root / 'docs/reports/ps3-increment-2026-11-23').glob('*') if p.is_file()]
with zipfile.ZipFile(out / 'Nivoryn-native-source.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(set(files)):
        archive.write(path, 'nivoryn/' + str(path.relative_to(root)))
for source, name in [(root / 'app/build/outputs/apk/preview/app-preview.apk', 'Nivoryn-Guard-v0.1.2.apk'),
                     (root / 'app/build/outputs/apk/debug/app-debug.apk', 'Nivoryn-debug.apk'),
                     (root / 'app/build/outputs/apk/release/app-release-unsigned.apk', 'Nivoryn-release-unsigned.apk'),
                     (a.video.resolve(), 'Nivoryn-offline-demo.mp4')]:
    if not source.is_file():
        raise SystemExit('Build/recording is missing: ' + str(source))
    shutil.copyfile(source, out / name)
for name in ['PS3_VALIDATION.md', 'PS3_COVERAGE.md', 'MODEL_SETUP.md', 'PHONE_ACCEPTANCE.md', 'THREAT_MODEL.md', 'DEPENDENCIES_PS3.md', 'API_COMPATIBILITY.md', 'NATIVE_MOTION_REFERENCE.md']:
    shutil.copyfile(root / 'docs' / name, out / name)
shutil.copyfile(root / 'app/src/main/assets/trusted-model.json', out / 'trusted-model.json')
for name in ['validation-summary.json', 'offline-demo.json']:
    shutil.copyfile(root / 'docs/reports/llama-runtime-2026-11-23' / name, out / name)
for name in ['LLAMA32_LICENSE.txt', 'LLAMA_NOTICE.txt', 'SMS_CLASSIFIER_NOTICE.txt']:
    shutil.copyfile(root / 'app/src/main/assets' / name, out / name)
fixtures = out / 'safe-fixtures'
fixtures.mkdir(exist_ok=True)
subprocess.run([sys.executable, str(root / 'tools/network/make-fixtures.py'), str(fixtures)], check=True)
shutil.copyfile(root / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk', fixtures / 'benign-instrumentation.apk')
(fixtures / 'README.txt').write_text('Synthetic PCAP metadata fixtures: import only, never transmit. Checksums intentionally omitted.\n'
    'benign.pcap: four SYN/port records; port-sweep.pcap: forty distinct destination ports. Neither is a representative intrusion dataset.\n'
    'benign-instrumentation.apk: existing signed test harness built from this source. Import into APK indicators; do not install it as a product or malware sample.\n')
(out / 'INSTALL.txt').write_text('Install Nivoryn-Guard-v0.1.2.apk: non-debuggable Arm64 preview signed with the existing preview identity. The separate production release APK is unsigned and needs the owner signing key.\n'
    'Weights are separate: follow MODEL_SETUP.md and use the pinned model.pte + tokenizer.bin from ORBIT. Import both via Settings > Llama resources.\n'
    'Read PS3_VALIDATION.md for actual verified scope, experimental behavior and unavailable features. The video is an Arm64 emulator recording, not a physical-phone benchmark.\n'
    'The source ZIP contains current native working-tree changes, tests, tools and reports; no credentials/signing keys/model weights. The existing website is unchanged in the repository.\n')
manifest = {'generatedUtc': datetime.now(timezone.utc).isoformat(),
            'baseRevision': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip(),
            'workingTreeSourceIncluded': True, 'modelWeightsBundled': False,
            'files': {str(path.relative_to(out)): {'bytes': path.stat().st_size, 'sha256': digest(path)} for path in sorted(out.rglob('*')) if path.is_file() and path.name != 'artifacts.json' and not any(part.startswith('._') or part == '.DS_Store' for part in path.relative_to(out).parts)}}
(out / 'artifacts.json').write_text(json.dumps(manifest, indent=2) + '\n')
print(str(out))
