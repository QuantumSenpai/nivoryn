"""Synthetic protocol fixtures, not malware or a real traffic evaluation corpus."""
import argparse
import pathlib
import struct
p = argparse.ArgumentParser()
p.add_argument('output', type=pathlib.Path)
a = p.parse_args()
a.output.mkdir(parents=True, exist_ok=True)
for name, count in [('benign', 4), ('port-sweep', 40)]:
    with (a.output / (name + '.pcap')).open('wb') as f:
        f.write(struct.pack('<IHHIIII', 0xa1b2c3d4, 2, 4, 0, 0, 65535, 101))
        for i in range(count):
            packet = bytearray(40)
            packet[0] = 0x45
            packet[2:4] = struct.pack('>H', 40)
            packet[8:10] = bytes([64, 6])
            packet[12:20] = bytes([10, 0, 0, 1, 10, 0, 0, 2])
            packet[20:24] = struct.pack('>HH', 12345, i + 1)
            packet[32:34] = bytes([0x50, 2])
            f.write(struct.pack('<IIII', 1, i, len(packet), len(packet)))
            f.write(packet)
