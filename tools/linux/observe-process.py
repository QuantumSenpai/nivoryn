"""Read-only bounded Linux process observations. Never launches/installs a submitted program."""
import argparse
import hashlib
import json
import os
import pathlib
import platform
import time

MAX_READ = 65536

def classify(snapshot):
    findings = []
    if snapshot['temporaryExecutable']:
        findings.append('Executable resides in a temporary directory. Build tools may do this legitimately; verify provenance.')
    if snapshot['writableExecutableMappings']:
        findings.append('Writable and executable memory mappings were observed. JIT runtimes can explain this; it is not proof of malware.')
    if snapshot['openSocketDescriptors'] >= 32:
        findings.append('Many open socket descriptors were observed. Review expected workload; this does not identify remote content or prove intrusion.')
    return findings

def bounded(path):
    with path.open('rb') as f:
        content = f.read(MAX_READ + 1)
    if len(content) > MAX_READ:
        raise ValueError('Observation exceeds bound')
    return content.decode('utf-8', errors='replace')

def sample(pid):
    process = pathlib.Path('/proc') / str(pid)
    status = bounded(process / 'status')
    uid_line = next(line for line in status.splitlines() if line.startswith('Uid:'))
    if any(int(uid) != os.getuid() for uid in uid_line.split()[1:]):
        raise PermissionError('Only processes owned by the invoking user are supported')
    executable = os.readlink(process / 'exe')
    maps = bounded(process / 'maps').splitlines()[:512]
    sockets = 0
    with os.scandir(process / 'fd') as entries:
        for index, entry in enumerate(entries):
            if index >= 512:
                raise ValueError('Too many descriptors for supported observation')
            try:
                sockets += os.readlink(entry.path).startswith('socket:[')
            except FileNotFoundError:
                pass
    return {'timestamp': time.time(), 'pid': pid, 'temporaryExecutable': executable.startswith(('/tmp/', '/var/tmp/', '/dev/shm/')),
            'writableExecutableMappings': sum(len(line.split()) > 1 and 'w' in line.split()[1] and 'x' in line.split()[1] for line in maps),
            'openSocketDescriptors': sockets}

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--pid', type=int, required=True)
    p.add_argument('--samples', type=int, default=3)
    p.add_argument('--output', type=pathlib.Path, required=True)
    a = p.parse_args()
    if platform.system() != 'Linux' or a.pid <= 0 or not 1 <= a.samples <= 10:
        p.error('Requires Linux, positive PID and 1..10 samples')
    observations = []
    for i in range(a.samples):
        row = sample(a.pid); row['findings'] = classify(row); observations.append(row)
        if i + 1 < a.samples:
            time.sleep(1)
    report = {'platform': platform.platform(), 'architecture': platform.machine(), 'scope': 'Read-only /proc snapshots of one already-running same-UID process; no Android cross-app monitoring, no execution or malware verdict', 'observations': observations}
    # Exclusive creation, owner-only mode; no accidental overwrite or public telemetry service.
    fd = os.open(a.output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w') as f:
        json.dump(report, f, indent=2)
        f.write('\n')

if __name__ == '__main__':
    main()
