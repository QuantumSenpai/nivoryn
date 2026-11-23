"""Download the build-pinned Llama export linked by the official ExecuTorch README.

This is a published conversion, not a local export of the Meta source checkpoint.
Run only after accepting the Llama 3.2 licence/AUP. No credentials are printed.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path('/Volumes/ORBIT/Nivoryn/models/llama-3.2-1b-executorch-public'))
    args = parser.parse_args()
    assets = Path(__file__).resolve().parents[2] / 'app/src/main/assets'
    profile = json.loads((assets / 'trusted-model.json').read_text())
    if profile['source'] != 'executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET' or profile['revision'] != '911221e0f49d3354cbd4697a9c09feee25cadec7':
        raise SystemExit('Catalogue source differs from the reviewed published conversion')
    root = args.root.resolve()
    root.mkdir(parents=True, exist_ok=True)
    old_home = Path(os.environ.get('HF_HOME', str(Path.home() / '.cache/huggingface')))
    if 'HF_TOKEN_PATH' not in os.environ and (old_home / 'token').is_file():
        os.environ['HF_TOKEN_PATH'] = str(old_home / 'token')
    os.environ['HF_HOME'] = str(root / 'hf-cache')
    os.environ['HF_ENDPOINT'] = 'https://huggingface.co'
    os.environ['HF_HUB_DISABLE_TELEMETRY'] = '1'
    pairs = [('Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte', 'model.pte', 'model'),
             ('tokenizer.model', 'tokenizer.bin', 'tokenizer')]
    for remote, local, key in pairs:
        target = root / local
        expected_size = profile[key + 'Bytes']
        expected_hash = profile[key + 'Sha256']
        if target.is_file() and target.stat().st_size == expected_size and digest(target) == expected_hash:
            print(local + ': verified existing pinned bytes')
            continue
        if shutil.disk_usage(root).free < expected_size + 128_000_000:
            raise SystemExit('Insufficient resource-volume storage; existing files were preserved')
        from huggingface_hub import hf_hub_download
        downloaded = Path(hf_hub_download(profile['source'], remote, revision=profile['revision'], local_dir=str(root)))
        if downloaded.stat().st_size != expected_size or digest(downloaded) != expected_hash:
            raise SystemExit('Publisher bytes do not match the reviewed build pin; do not import')
        os.replace(downloaded, target)
        print(local + ': downloaded and verified')
    for name in ['LLAMA32_LICENSE.txt', 'LLAMA_NOTICE.txt']:
        shutil.copyfile(assets / name, root / name)
    (root / 'supported-profile.json').write_text(json.dumps(profile, indent=2) + '\n')
    print('Resources ready for user-selected Android import. No phone-performance claim is made.')


if __name__ == '__main__':
    main()
