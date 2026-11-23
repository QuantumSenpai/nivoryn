"""Black-box smoke of the exact minified APK. Use a dedicated emulator/test device.

Only the fixed synthetic test message is entered/saved/deleted; no private data is exported.
No run-as, debug hooks, cloud inference or instrumentation in the shipped package.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as E

p = argparse.ArgumentParser()
p.add_argument('--adb', required=True)
p.add_argument('--serial', required=True)
p.add_argument('--output', type=Path, required=True)
p.add_argument('--require-llama', action='store_true')
a = p.parse_args()
base = [a.adb, '-s', a.serial]

def adb(*args):
    return subprocess.check_output(base + list(args), text=True, timeout=30).strip()

def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/nivoryn-distribution-ui.xml')
    return list(E.fromstring(adb('shell', 'cat', '/sdcard/nivoryn-distribution-ui.xml')).iter('node'))

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1 + x2)//2), str((y1 + y2)//2))

width, height = map(int, re.search(r'(\d+)x(\d+)', adb('shell', 'wm', 'size')).groups())
def find(label, scroll=False, timeout=15):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        for n in nodes():
            if label in [n.get('text'), n.get('content-desc')]:
                return n
        if scroll:
            adb('shell', 'input', 'swipe', str(width//2), str(height*3//4), str(width//2), str(height//3), '200')
        time.sleep(.25)
    raise AssertionError('Expected visible UI: ' + label)

assert adb('shell', 'settings', 'get', 'global', 'airplane_mode_on') == '1', 'Enable airplane mode on the test runtime'
package = 'dev.kyzenn.nivoryn'
package_info = adb('shell', 'dumpsys', 'package', package)
assert 'versionName=0.1.2' in package_info and 'versionCode=3 ' in package_info
assert 'DEBUGGABLE' not in package_info, 'Test the actual non-debuggable distribution variant'
apk_path = adb('shell', 'pm', 'path', package).removeprefix('package:')
installed_sha256 = adb('shell', 'sha256sum', apk_path).split()[0]
adb('shell', 'am', 'force-stop', package)
adb('shell', 'am', 'start', '-W', '-n', package + '/.MainActivity')
first = nodes()
start = next((n for n in first if n.get('text') == 'Start checking'), None)
if start is not None:
    tap(start)
field = None
for _ in range(10):
    field = next((n for n in nodes() if n.get('class') == 'android.widget.EditText'), None)
    if field is not None:
        break
    adb('shell', 'input', 'swipe', str(width//2), str(height*3//4), str(width//2), str(height//3), '200')
assert field is not None, 'Missing real input field'
tap(field)
adb('shell', 'input', 'keycombination', 'KEYCODE_CTRL_LEFT', 'KEYCODE_A')
adb('shell', 'input', 'text', 'Please%ssend%syour%sOTP%sto%sfinish%sverification.')
adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
tap(find('Review input', scroll=True))
tap(find('Run local check', scroll=True))
find('!  High risk', timeout=45)
llama_mode = 'Llama generated explanation · validated references'
if a.require_llama:
    find(llama_mode, scroll=True)
tap(find('View evidence', scroll=True))
find('A secret is requested', scroll=True)
tap(find('Save check locally', scroll=True))
tap(find('History'))
original = 'Please send your OTP to finish verification.'
tap(find(original, scroll=True))
if a.require_llama:
    find(llama_mode, scroll=True)
tap(find('History'))
tap(find('Delete saved check', scroll=True))
find('Nothing saved yet. Save a result when you want to keep its original text.', timeout=15)
result = {'device': {k: adb('shell', 'getprop', k) for k in ['ro.product.model', 'ro.build.version.release', 'ro.build.version.sdk', 'ro.product.cpu.abi', 'ro.kernel.qemu']},
    'pageSize': adb('shell', 'getconf', 'PAGE_SIZE'), 'airplaneMode': True,
    'tested': ['custom text', 'deterministic evidence', 'save', 'reopen', 'delete'],
    'realLlamaRequired': a.require_llama, 'passed': True,
    'installedApkSha256': installed_sha256, 'version': '0.1.2', 'versionCode': 3, 'nonDebuggable': True,
    'scope': 'Synthetic black-box UI on the installed distribution APK; not Oppo hardware or Play Protect approval'}
a.output.parent.mkdir(parents=True, exist_ok=True)
a.output.write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result))
