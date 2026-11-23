"""Record the synthetic instrumentation flow, holding result only in the test harness."""
import argparse
import json
import pathlib
import subprocess
import time
p = argparse.ArgumentParser()
p.add_argument('--adb', required=True)
p.add_argument('--serial', required=True)
p.add_argument('--output', type=pathlib.Path, required=True)
p.add_argument('--require-llama', action='store_true', help='Require installed real Llama mode and save/reopen/delete flow')
a = p.parse_args()
b = [a.adb, '-s', a.serial]
airplane = subprocess.check_output(b + ['shell', 'settings', 'get', 'global', 'airplane_mode_on'], text=True).strip()
if airplane != '1':
    raise SystemExit('Enable airplane mode on the selected test device before recording')
remote = '/sdcard/nivoryn-readable-offline-demo.mp4'
recorder = subprocess.Popen(b + ['shell', 'screenrecord', '--time-limit', '45', remote])
try:
    time.sleep(1)
    run = subprocess.run(b + ['shell', 'am', 'instrument', '-w', '-e', 'recording', 'true', '-e', 'llamaRequired', str(a.require_llama).lower(), '-e', 'class',
        'dev.kyzenn.nivoryn.OfflineDeviceTest#localEngineFlowWorksInTheActualAndroidProcess',
        'dev.kyzenn.nivoryn.test/androidx.test.runner.AndroidJUnitRunner'], capture_output=True, text=True, timeout=80)
    print(run.stdout)
    if run.returncode or 'OK (1 test)' not in run.stdout:
        raise RuntimeError('Demo test did not pass')
    recorder.wait(timeout=55)
    if recorder.returncode:
        raise RuntimeError('Screen recording failed')
    a.output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(b + ['pull', remote, str(a.output)], check=True, timeout=30)
    subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration,size', '-of', 'json', str(a.output)], check=True)
    a.output.with_suffix('.json').write_text(json.dumps({'airplaneMode': airplane, 'requireLlama': a.require_llama,
        'instrumentationPassed': True, 'serial': a.serial, 'input': 'Synthetic OTP request; no private content',
        'scope': 'Android UI recording; emulator is not phone performance evidence'}, indent=2) + '\n')
finally:
    if recorder.poll() is None:
        recorder.terminate()
