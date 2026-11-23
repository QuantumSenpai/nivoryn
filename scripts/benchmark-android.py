"""Collect real adb startup/memory/frame evidence. Never represents emulator data as phone data."""
import argparse
import json
import pathlib
import subprocess
import time

p = argparse.ArgumentParser()
p.add_argument('--adb', default='adb')
p.add_argument('--serial', required=True)
p.add_argument('--output', type=pathlib.Path, required=True)
a = p.parse_args()
base = [a.adb, '-s', a.serial]

def adb(*args):
    return subprocess.run(base + list(args), check=True, capture_output=True, text=True, timeout=45).stdout.strip()

package = 'dev.kyzenn.nivoryn'
properties = {key: adb('shell', 'getprop', key) for key in ['ro.product.model', 'ro.product.cpu.abi', 'ro.build.version.release', 'ro.kernel.qemu']}
adb('shell', 'am', 'force-stop', package)
adb('shell', 'dumpsys', 'gfxinfo', package, 'reset')
started = time.monotonic()
startup = adb('shell', 'am', 'start', '-W', '-n', package + '/.MainActivity')
report = {'device': properties, 'emulator': properties['ro.kernel.qemu'] == '1',
          'scope': 'One process cold launch; not a model/OCR/warm-check/battery benchmark',
          'launchCommandWallMs': round((time.monotonic() - started) * 1000), 'amStart': startup,
          'airplaneSetting': adb('shell', 'settings', 'get', 'global', 'airplane_mode_on'),
          'memorySnapshot': adb('shell', 'dumpsys', 'meminfo', package),
          'framesSnapshot': adb('shell', 'dumpsys', 'gfxinfo', package, 'framestats')}
a.output.parent.mkdir(parents=True, exist_ok=True)
a.output.write_text(json.dumps(report, indent=2) + '\n')
