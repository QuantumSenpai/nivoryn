"""Collect synthetic instrumentation reports, never user history, prompts or credentials."""
import argparse
import json
from pathlib import Path
import subprocess

p = argparse.ArgumentParser()
p.add_argument('--adb', required=True)
p.add_argument('--serial', required=True)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
a.output.mkdir(parents=True, exist_ok=True)
base = [a.adb, '-s', a.serial]
for name in ['llama-native-smoke.json', 'llama-pipeline-smoke.json', 'llama-varied-smoke.json', 'extraction-metrics.json', 'network-replay-smoke.json']:
    raw = subprocess.check_output(base + ['exec-out', 'run-as', 'dev.kyzenn.nivoryn', 'cat', 'files/' + name], timeout=15)
    value = json.loads(raw)
    (a.output / name).write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')
    print(name + ': collected')
device = {}
for key in ['ro.product.model', 'ro.product.cpu.abi', 'ro.build.version.release', 'ro.kernel.qemu']:
    device[key] = subprocess.check_output(base + ['shell', 'getprop', key], text=True, timeout=10).strip()
device['airplaneMode'] = subprocess.check_output(base + ['shell', 'settings', 'get', 'global', 'airplane_mode_on'], text=True, timeout=10).strip()
device['scope'] = 'Synthetic fixtures on this identified Android runtime; not real-world accuracy or physical-phone performance'
(a.output / 'device.json').write_text(json.dumps(device, indent=2) + '\n')
