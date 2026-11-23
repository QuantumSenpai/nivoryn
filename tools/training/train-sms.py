"""Bounded Multinomial NB spam proxy. Split/deduplicate before fitting vocabulary; no cloud calls."""
import argparse
import collections
import hashlib
import json
import math
import pathlib
import random
import re
import unicodedata

TOKEN=re.compile(r'[a-z0-9]+|[\u0900-\u097f]+')
def tokens(text): return TOKEN.findall(unicodedata.normalize('NFKC',text).lower())[:600]
def counts(text): return {t:min(n,5) for t,n in collections.Counter(tokens(text)).items()}
def main():
 p=argparse.ArgumentParser(); p.add_argument('--corpus',type=pathlib.Path,required=True); p.add_argument('--output',type=pathlib.Path,required=True); p.add_argument('--report',type=pathlib.Path,required=True); a=p.parse_args()
 rows=[]; groups=collections.defaultdict(list)
 for line in a.corpus.read_text().splitlines():
  label,text=line.split('\t',1); assert label in ['ham','spam']
  key=re.sub(r'\s+',' ',unicodedata.normalize('NFKC',text).lower().strip()); groups[key].append((label=='spam',text))
 unique=[v[0] for v in groups.values() if len({x[0] for x in v})==1]
 train=[]; test=[]
 for positive in [False,True]:
  group=[r for r in unique if r[0]==positive]; random.Random(228).shuffle(group); n=max(1,len(group)//5)
  test.extend(group[:n]); train.extend(group[n:])
 df=collections.Counter(t for _,text in train for t in counts(text))
 vocab=[t for t,_ in sorted(df.items(),key=lambda pair:(-pair[1],pair[0])) if df[t]>=2][:4000]; vocabulary=set(vocab)
 totals=[collections.Counter(),collections.Counter()]; documents=[0,0]
 for y,text in train:
  documents[int(y)]+=1; totals[int(y)].update({t:n for t,n in counts(text).items() if t in vocabulary})
 likelihoods=[{t:math.log((totals[c][t]+1)/(sum(totals[c].values())+len(vocab))) for t in vocab} for c in [0,1]]
 weights={t:likelihoods[1][t]-likelihoods[0][t] for t in vocab}
 prior=math.log(documents[1]/documents[0]); threshold=0.99
 def score(text):
  features=counts(text); known=[t for t in features if t in weights]
  logit=prior+sum(features[t]*weights[t] for t in known)
  return 1/(1+math.exp(-max(-40,min(40,logit)))) if len(known)>=3 else 0
 tp=fp=tn=fn=0
 for y,text in test:
  predicted=score(text)>=threshold
  if y and predicted: tp+=1
  elif y: fn+=1
  elif predicted: fp+=1
  else: tn+=1
 source='UCI SMS Spam Collection / Almeida & Hidalgo (2011), DOI 10.24432/C5CC84, CC BY 4.0'
 model={'schema':1,'version':'uci-mnb-v1','source':source,'normalisation':'NFKC/lower/token-v1','scope':'Historical English SMS spam proxy; not fraud, Hindi or sender verification','prior':prior,'threshold':threshold,'weights':weights}
 a.output.parent.mkdir(parents=True,exist_ok=True); a.output.write_text(json.dumps(model,ensure_ascii=True,separators=(',',':'))+'\n')
 report={'source':source,'corpusSha256':hashlib.sha256(a.corpus.read_bytes()).hexdigest(),'rawRows':sum(len(v) for v in groups.values()),'deduplicatedRows':len(unique),'trainRows':len(train),'testRows':len(test),'seed':228,'split':'Stratified 80/20 after NFKC whitespace/case deduplication; conflicting duplicate groups excluded. Vocabulary fitted on training only. Fixed threshold 0.99, not tuned on test.','tp':tp,'fp':fp,'tn':tn,'fn':fn,'precision':tp/(tp+fp) if tp+fp else None,'recall':tp/(tp+fn) if tp+fn else None,'warning':'Spam labels are not scam ground truth. No contemporary Indian scam, multilingual or real-world accuracy claim.','modelSha256':hashlib.sha256(a.output.read_bytes()).hexdigest()}
 a.report.parent.mkdir(parents=True,exist_ok=True); a.report.write_text(json.dumps(report,indent=2)+'\n'); print(json.dumps(report,indent=2))
if __name__=='__main__': main()
