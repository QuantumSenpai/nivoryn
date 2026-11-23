"""Sample actual app memory/frame summaries during the resource-required offline UI test."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time

p = argparse.ArgumentParser()
p.add_argument('--adb', required=True)
p.add_argument('--serial', required=True)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
base = [a.adb, '-s', a.serial]
def adb(*args):
    return subprocess.check_output(base + list(args), text=True, timeout=15).strip()

if adb('shell', 'settings', 'get', 'global', 'airplane_mode_on') != '1':
    raise SystemExit('Enable airplane mode before this acceptance profile')
device = {key: adb('shell', 'getprop', key) for key in ['ro.product.model', 'ro.product.cpu.abi', 'ro.build.version.release', 'ro.kernel.qemu']}
package = 'dev.kyzenn.nivoryn'
adb('shell', 'am', 'force-stop', package)
start = time.monotonic()
run = subprocess.Popen(base + ['shell', 'am', 'instrument', '-w', '-r', '-e', 'llamaRequired', 'true', '-e', 'class',
    package + '.OfflineDeviceTest#localEngineFlowWorksInTheActualAndroidProcess', package + '.test/androidx.test.runner.AndroidJUnitRunner'],
    stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
samples = []
try:
    while run.poll() is None:
        if time.monotonic() - start > 90:
            raise TimeoutError('UI acceptance exceeded 90 seconds')
        memory = adb('shell', 'dumpsys', 'meminfo', package)
        frames = adb('shell', 'dumpsys', 'gfxinfo', package)
        row = {'elapsedMs': round((time.monotonic() - start) * 1000)}
        for name, pattern in {'pssKiB': r'TOTAL PSS:\s*(\d+)', 'rssKiB': r'TOTAL RSS:\s*(\d+)',
                              'renderedFrames': r'Total frames rendered:\s*(\d+)', 'jankyFrames': r'Janky frames:\s*(\d+)'}.items():
            match = re.search(pattern, memory if name in ['pssKiB', 'rssKiB'] else frames)
            if match:
                row[name] = int(match.group(1))
        for percentile in [50, 90, 95, 99]:
            match = re.search(str(percentile) + r'th percentile:\s*(\d+)ms', frames)
            if match:
                row['frameP' + str(percentile) + 'Ms'] = int(match.group(1))
        samples.append(row)
        time.sleep(0.25)
    stdout, stderr = run.communicate(timeout=5)
finally:
    if run.poll() is None:
        run.terminate()
report = {'device': device, 'emulator': device['ro.kernel.qemu'] == '1', 'airplaneMode': True,
          'scope': 'One debug process-cold Compose UI test with real Llama, save/reopen/delete; OS file cache may be warm. Automated scroll/click, software-rendered emulator where identified. Polling adds overhead. Not battery, phone or steady-state FPS evidence.',
          'sampleIntervalMinimumMs': 250, 'peakSampledRssKiB': max((s.get('rssKiB', 0) for s in samples), default=0),
          'peakSampledPssKiB': max((s.get('pssKiB', 0) for s in samples), default=0),
          'wallMs': round((time.monotonic() - start) * 1000), 'samples': samples,
          'testPassed': run.returncode == 0 and 'OK (1 test)' in stdout, 'instrumentation': stdout, 'stderr': stderr}
a.output.parent.mkdir(parents=True, exist_ok=True)
a.output.write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps({k: v for k, v in report.items() if k not in ['samples', 'instrumentation', 'stderr']}))
if not report['testPassed']:
    raise SystemExit('Profiled acceptance did not pass; inspect the report')
