"""Official Meta-only, version-pinned ExecuTorch provisioning. No model redistribution or mirrors."""
import argparse
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import sys

REPO = 'meta-llama/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8'
REVISION = 'b8c08c95d0476ccb6fc86e9410818d602ecca19d'
RUNTIME = '1.2.0'
FILES = ['consolidated.00.pth', 'params.json', 'tokenizer.model']

def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('action', choices=['download', 'export', 'catalogue'])
    p.add_argument('--root', type=pathlib.Path, default=pathlib.Path('/Volumes/ORBIT/Nivoryn/models/llama-3.2-1b-spinquant'))
    p.add_argument('--executorch-source', type=pathlib.Path)
    p.add_argument('--catalogue-output', type=pathlib.Path)
    a = p.parse_args()
    root = a.root.resolve(); source = root / 'source'; exported = root / 'exported'
    # Keep model caches scoped to this process/project; never relocate global caches.
    old_home = pathlib.Path(os.environ.get('HF_HOME', str(pathlib.Path(os.environ.get('XDG_CACHE_HOME', str(pathlib.Path.home() / '.cache'))) / 'huggingface')))
    if 'HF_TOKEN_PATH' not in os.environ and (old_home / 'token').is_file():
        os.environ['HF_TOKEN_PATH'] = str(old_home / 'token')
    os.environ['HF_HOME'] = str(root / 'hf-cache')
    os.environ['HF_ENDPOINT'] = 'https://huggingface.co'
    os.environ['HF_HUB_DISABLE_TELEMETRY'] = '1'
    os.environ['TORCH_HOME'] = str(root / 'torch-cache')
    if a.action == 'download':
        try:
            from huggingface_hub import snapshot_download
        except ImportError:
            raise SystemExit('Install huggingface_hub in the ORBIT model-tooling venv. See tools/model/README.md.')
        source.mkdir(parents=True, exist_ok=True)
        try:
            snapshot_download(repo_id=REPO, revision=REVISION, allow_patterns=FILES + ['README.md', 'config.json'], local_dir=str(source))
        except Exception:
            raise SystemExit('Official download failed. Check network and authorised Meta/Hugging Face account access. Accept the Llama licence in your own account; do not share credentials. Partial downloads remain resumable in the scoped cache.')
        manifest = {'repository': REPO, 'revision': REVISION, 'files': {n: digest(source / n) for n in FILES}}
        (root / 'source-provenance.json').write_text(json.dumps(manifest, indent=2) + '\n')
    elif a.action == 'export':
        if not a.executorch_source:
            p.error('--executorch-source is required (official clean v1.2.0 checkout)')
        checkout = a.executorch_source.resolve()
        tag = subprocess.check_output(['git', '-C', str(checkout), 'describe', '--tags', '--exact-match', 'HEAD'], text=True).strip()
        remote = subprocess.check_output(['git', '-C', str(checkout), 'remote', 'get-url', 'origin'], text=True).strip().removesuffix('.git')
        if tag != 'v' + RUNTIME or remote not in ['https://github.com/pytorch/executorch', 'git@github.com:pytorch/executorch']:
            raise SystemExit('Exporter must be the official ExecuTorch v1.2.0 checkout.')
        tracked = subprocess.check_output(['git', '-C', str(checkout), 'diff', '--name-only', 'HEAD'], text=True).strip()
        if tracked:
            raise SystemExit('Refusing a modified exporter checkout.')
        provenance = json.loads((root / 'source-provenance.json').read_text())
        if provenance['repository'] != REPO or provenance['revision'] != REVISION or any(digest(source / n) != provenance['files'][n] for n in FILES):
            raise SystemExit('Source provenance/integrity mismatch; re-download from the official pinned source.')
        exported.mkdir(parents=True, exist_ok=True)
        command = [sys.executable, '-m', 'extension.llm.export.export_llm', '--config', 'examples/models/llama/config/llama_xnnpack_spinquant.yaml',
                   '+base.model_class=llama3_2', '+base.checkpoint=' + str(source / FILES[0]), '+base.params=' + str(source / 'params.json'),
                   'export.output_dir=' + str(exported), 'export.output_name=model.pte', 'export.max_seq_length=1024', 'export.max_context_length=1024']
        subprocess.run(command, cwd=checkout, check=True)
        # Native 1.2.0 loader probes HF/TikToken/SentencePiece formats; Llama 2 conversion is inappropriate here.
        shutil.copyfile(source / 'tokenizer.model', exported / 'tokenizer.bin')
        provenance.update({'runtime': RUNTIME, 'recipe': 'llama_xnnpack_spinquant.yaml', 'context': 1024,
                           'modelHash': digest(exported / 'model.pte'), 'tokenizerHash': digest(exported / 'tokenizer.bin')})
        (root / 'export-provenance.json').write_text(json.dumps(provenance, indent=2) + '\n')
    else:
        if not a.catalogue_output:
            p.error('--catalogue-output is required; review provenance before adding it to the signed app')
        manifest = json.loads((root / 'export-provenance.json').read_text())
        model = exported / 'model.pte'; token = exported / 'tokenizer.bin'
        if manifest['repository'] != REPO or manifest['revision'] != REVISION or manifest['runtime'] != RUNTIME:
            raise SystemExit('Unsupported model/export provenance')
        if digest(model) != manifest['modelHash'] or digest(token) != manifest['tokenizerHash']:
            raise SystemExit('Export was modified')
        if not (0 < model.stat().st_size <= 2_000_000_000 and 0 < token.stat().st_size <= 32_000_000):
            raise SystemExit('Export exceeds Android import budget')
        profile = {'family': 'Llama', 'runtime': 'executorch-' + RUNTIME, 'version': 'llama32-1b-spinquant-' + REVISION[:12],
                   'modelSha256': digest(model), 'tokenizerSha256': digest(token), 'modelBytes': model.stat().st_size, 'tokenizerBytes': token.stat().st_size,
                   'source': REPO, 'revision': REVISION, 'context': 1024, 'quantisation': 'SpinQuant INT4 group32 / dynamic INT8 / EO8'}
        a.catalogue_output.parent.mkdir(parents=True, exist_ok=True)
        a.catalogue_output.write_text(json.dumps(profile, indent=2) + '\n')
        print('Build-pinned candidate profile written. Real Android inference and licence/redistribution review are still required; no verification claim is created by this command.')

if __name__ == '__main__':
    main()
