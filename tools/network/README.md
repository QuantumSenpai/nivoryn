# Supported traffic workflow

Android: Settings or Home → Inspect network capture → choose classic `.pcap` under 20 MB. This runs an offline metadata replay, not live monitoring. Supported: untagged Ethernet IPv4 or raw IPv4, complete TCP/UDP packets. Unsupported: PCAPNG, IPv6, VLAN, fragments and encrypted payload inspection. Capture-wide port/SYN counts are heuristic thresholds, not a trained detector; no time-window or real-world accuracy claim.

Generate deterministic safe fixtures:

```sh
python3 tools/network/make-fixtures.py /private/tmp/nivoryn-network-fixtures
```

`benign.pcap` contains four SYNs to four ports; `port-sweep.pcap` contains forty to distinct ports and should trigger the >=32 destination-port threshold. Checksums are omitted: these are parser fixtures, not packets to send on a network. No fixture is executed or transmitted.

Separate Arm Linux route, not yet tested on Raspberry Pi:

```sh
# Requires Linux, GNU timeout, tcpdump/libpcap and capture privileges.
# Use only an interface/network you are authorised to inspect.
sh tools/network/capture-linux.sh eth0 ./authorised-capture.pcap
```

Capture stops after 60 seconds or 10000 packets, does not enable promiscuous mode, and limits snapshot size to 96 bytes. Interface link type may differ: only Ethernet/raw-IP captures can be imported by the Android parser. Kernel permissions are managed by the operator, not changed by the script. Treat captures as private data. There is no TLS interception, CA installation, Linux Llama runner or live Android VPN adapter in this increment.

Continuation: the parser now exposes typed directional flows with source/destination address/port, TCP/UDP, packet count, original IP-byte count and SYN-without-ACK count. At most 4096 distinct flows are accepted; an over-budget capture is rejected rather than silently certified. Header-complete snapshots can be parsed without retaining full payload, matching the Linux adapter's 96-byte snap length. Invalid TCP/UDP header lengths are excluded. Thresholds remain capture-wide heuristics, not duration/rate-based or trained accuracy claims.
