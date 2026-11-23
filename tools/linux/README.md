# Arm Linux workloads

Android APK inspection is static and cannot observe arbitrary other apps. Linux offers a separate, narrowly scoped observation adapter:

```sh
python3 tools/linux/observe-process.py --pid 1234 --samples 3 --output ./process-observations.json
```

Run on Linux against an **already-running process owned by your current UID**. The tool never launches or installs a submitted program, reads command lines/environment variables, captures message content, changes permissions or starts a service. It reads bounded `/proc/PID/status`, `/proc/PID/maps`, executable-location category and socket descriptor counts. Output uses exclusive creation and owner-only permissions. Process exit/reuse/permission denial causes a failed observation rather than an invented result. At most 10 samples at one-second intervals, 512 descriptors and 64 KiB per proc file are supported.

Flags are explainable observations: executable in a temporary location, writable/executable mappings and many open sockets. Build tools/JITs/network servers can explain them; these are not malware verdicts. Architecture/platform are recorded. No Arm Linux hardware execution is claimed yet. Pure classification tests: `python3 tools/linux/test_observer.py`.

Legitimate traffic capture is separate: `tools/network/capture-linux.sh` uses the distribution's tcpdump/libpcap with authorised interface privileges and bounded packet count/duration/snapshot size. Transfer its PCAP to Android for metadata replay. Android does not expose equivalent `/proc` or cross-app capture permissions to this app. There is no TLS interception or CA installation.

Platform reference: [Linux kernel proc filesystem documentation](https://docs.kernel.org/filesystems/proc.html).
