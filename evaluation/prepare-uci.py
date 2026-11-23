"""Extract only named, bounded UCI data members. Does not unpack arbitrary archive paths."""
import argparse
import hashlib
import json
import pathlib
import zipfile
p=argparse.ArgumentParser()
p.add_argument('--directory',type=pathlib.Path,required=True)
a=p.parse_args()
archive=a.directory/'source.zip'
if not 0<archive.stat().st_size<=2_000_000:
    raise SystemExit('Unexpected archive size')
with zipfile.ZipFile(archive) as z:
    for name in ['SMSSpamCollection','readme']:
        info=z.getinfo(name)
        if not 0<info.file_size<=2_000_000:
            raise SystemExit('Unexpected member size')
        data=z.read(name)
        (a.directory/name).write_bytes(data)
metadata={'source':'https://archive.ics.uci.edu/dataset/228/sms+spam+collection','citation':'Almeida, T. & Hidalgo, J. (2011). SMS Spam Collection. DOI 10.24432/C5CC84',
          'licence':'CC BY 4.0 as stated by UCI; retain original readme and provenance','archiveSha256':hashlib.sha256(archive.read_bytes()).hexdigest(),
          'scope':'Independent spam/ham proxy, not contemporary Indian scam ground truth; no model training performed'}
(a.directory/'provenance.json').write_text(json.dumps(metadata,indent=2)+'\n')
print(json.dumps(metadata,indent=2))
