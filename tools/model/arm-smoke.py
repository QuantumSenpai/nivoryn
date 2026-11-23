"""Run an actual pinned ExecuTorch Llama export on Arm; no simulated output or remote inference."""
import argparse
import hashlib
import json
import pathlib
import platform
import resource
import time
import torch
from executorch.extension.pybindings import _portable_lib as runtime
from pytorch_tokenizers.tiktoken import TiktokenTokenizer

p=argparse.ArgumentParser()
p.add_argument('--model',type=pathlib.Path,required=True)
p.add_argument('--tokenizer',type=pathlib.Path,required=True)
p.add_argument('--prompt',type=pathlib.Path,required=True)
p.add_argument('--report',type=pathlib.Path,required=True)
p.add_argument('--tokens',type=int,default=160)
a=p.parse_args(); assert 1<=a.tokens<=160
prompt=a.prompt.read_text(); assert 0<len(prompt)<=6000
started=time.monotonic(); torch.set_num_threads(4)
module=runtime._load_for_executorch(str(a.model))
metadata={name:module.run_method(name,[]) for name in module.method_names() if name!='forward'}
tokenizer=TiktokenTokenizer(str(a.tokenizer))
ids=tokenizer.encode(prompt,bos=False,eos=False,allowed_special='all')
context=int(metadata['get_max_context_len'][0]); assert len(ids)+a.tokens<=context
assert tokenizer.n_words==int(metadata['get_vocab_size'][0])
load_ms=(time.monotonic()-started)*1000
output=[]; begin=time.monotonic(); tokens=ids; pos=0; first=None
for i in range(a.tokens):
 if time.monotonic()-begin>60: break
 logits=module.forward([torch.tensor([tokens],dtype=torch.long),torch.tensor([pos],dtype=torch.long)])[0]
 token=int(torch.argmax(logits.reshape(-1,logits.shape[-1])[-1]).item())
 if first is None: first=(time.monotonic()-begin)*1000
 if token in metadata['get_eos_ids'][0]: break
 output.append(token); pos+=len(tokens); tokens=[token]
text=tokenizer.decode(output)
report={'scope':'Actual local ExecuTorch generation on identified host; not Android/phone performance or semantic validation.',
        'machine':platform.machine(),'platform':platform.platform(),'torch':torch.__version__,'executorch':'1.2.0',
        'metadata':metadata,'promptTokens':len(ids),'generatedTokens':len(output),'modelLoadAndTokenizerMs':load_ms,'firstTokenMs':first,
        'generationMs':(time.monotonic()-begin)*1000,'peakRssRaw':resource.getrusage(resource.RUSAGE_SELF).ru_maxrss,'output':text}
a.report.parent.mkdir(parents=True,exist_ok=True); a.report.write_text(json.dumps(report,indent=2)+'\n'); print(json.dumps(report,indent=2),flush=True)
