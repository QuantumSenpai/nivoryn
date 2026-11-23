"""Offline release-maintainer signing; never embeds a private key in an app/resource pack."""
import argparse
import base64
import json
import pathlib
import subprocess
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--payload',type=pathlib.Path,required=True)
p.add_argument('--key',type=pathlib.Path,required=True)
p.add_argument('--key-id',required=True)
p.add_argument('--output',type=pathlib.Path,required=True)
a=p.parse_args()
payload=a.payload.read_bytes()
if not 0<len(payload)<=1_000_000 or not 0<len(a.key_id)<=100:
    raise SystemExit('Resource exceeds limits')
j=json.loads(payload)
if j.get('schema')!=1 or not j.get('source') or not j.get('licence') or not j.get('version',0)>0:
    raise SystemExit('Missing pack schema/provenance')
sig=subprocess.run(['openssl','dgst','-sha256','-sign',str(a.key)],input=payload,capture_output=True,check=True).stdout
if not 256<=len(sig)<=512:
    raise SystemExit('Use a separately pinned RSA-2048 or RSA-4096 publisher key')
envelope={'keyId':a.key_id,'payload':base64.b64encode(payload).decode(),'signature':base64.b64encode(sig).decode()}
# Refuse overwriting an existing release envelope.
with a.output.open('x') as f: json.dump(envelope,f,separators=(',',':')); f.write('\n')
