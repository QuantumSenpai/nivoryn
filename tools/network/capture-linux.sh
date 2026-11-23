#!/bin/sh
# Explicit, bounded metadata capture for a Linux interface you are authorised to inspect.
set -eu
if [ "$#" -ne 2 ]; then
  echo 'Usage: capture-linux.sh INTERFACE OUTPUT.pcap (Linux; tcpdump/libpcap required)' >&2
  exit 2
fi
case "$1" in ''|*[!a-zA-Z0-9_.:-]*) echo 'Invalid interface' >&2; exit 2;; esac
case "$2" in /*.pcap|./*.pcap) ;; *) echo 'Use an absolute or ./ output path ending in .pcap' >&2; exit 2;; esac
if [ -e "$2" ]; then echo 'Refusing to overwrite an existing capture' >&2; exit 2; fi
umask 077
# No payload analysis, no promiscuous mode; hard packet/snap limits. timeout bounds idle capture.
exec timeout --signal=INT 60 tcpdump -p -i "$1" -s 96 -c 10000 -w "$2" 'ip and (tcp or udp)'
