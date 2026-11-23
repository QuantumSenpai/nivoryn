# Official Llama provisioning

`download-supported.py` reproduces the tested published conversion in [MODEL_SETUP.md](../../docs/MODEL_SETUP.md). It is linked by the official ExecuTorch README and pinned to an immutable publisher revision and reviewed build hashes. It is not described as a locally exported or Meta-hosted `.pte`. Official Meta source download also succeeded after owner authentication. Android emulator generation ran with ExecuTorch 1.2.0; physical phone testing remains pending.

`provision.py` is the separate official-source download/export/catalogue path. Source download has run; the local exporter has not run successfully because full Python dependency installation hit an `antlr4-python3-runtime` build failure. The Android-tested conversion does not require that host exporter environment. Neither tool trusts checksums from arbitrary imported sidecars.

Candidate: Meta Llama 3.2 1B Instruct SpinQuant INT4 EO8, official revision `b8c08c95d0476ccb6fc86e9410818d602ecca19d`. Official Hugging Face metadata confirms a manual gate and filenames `consolidated.00.pth`, `params.json`, `tokenizer.model`. Llama 3.2 Community Licence/AUP applies.

Create an isolated environment on ORBIT, install the official exporter at v1.2.0 using its upstream setup instructions and install `huggingface_hub` there. Use that environment's Python for every command below. Authenticate locally using the HF CLI/account flow after licence/access approval. Do not paste tokens in chat or commit them.

```sh
python tools/model/provision.py download
python tools/model/provision.py export --executorch-source /Volumes/ORBIT/Nivoryn/tooling/executorch
python tools/model/provision.py catalogue --catalogue-output app/src/main/assets/trusted-model.json
zsh scripts/android.sh :app:assembleDebug
```

The exporter directory must be a clean official `pytorch/executorch` checkout tagged v1.2.0. Its Python environment must contain that compatible exporter and native dependencies. The script checks source hashes, repository/revision and export integrity; `source-provenance.json` and `export-provenance.json` stay alongside the heavy assets. This is a local trusted developer pipeline, not a signed vendor attestation of an arbitrary third-party export. Review those files before shipping the catalogue in a signed app.

Native 1.2.0 `llm_runner_helper.cpp` probes HF JSON, TikToken, SentencePiece and llama2c tokenizers by content. The official Llama 3 TikToken `tokenizer.model` is copied unchanged to the private resource filename `tokenizer.bin`; **no Llama 2 tokenizer conversion is applied**. Context7's Llama 2 conversion example does not establish Llama 3 compatibility. The published conversion was loaded/generated on Android 14 Arm64 emulator; any fresh local export needs separate verification.

The app stages and verifies each resource, activates only a complete pair with an atomic pointer, keeps the previous trusted pair, and resumes interrupted local imports by checking/reusing a reselected file prefix before final SHA-256 verification. Update/delete/rollback takes the same lock as generation and unloads the cached module first. There is no app network permission or background downloader: HF download recovery belongs to the provisioning tool; device setup is user-selected import.

The signed app may bundle a catalogue of up to four profiles using `{"models":[profile1,profile2]}`. The last profile is the preferred import target. Rollback is limited to a complete previously installed pair still trusted by that catalogue. An incompatible/corrupt pair cannot become active. Deletion removes active/previous/staged models, not encrypted history.

Sources: [official quantised model](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8), [export recipe](https://github.com/pytorch/executorch/blob/v1.2.0/examples/models/llama/config/llama_xnnpack_spinquant.yaml), [configuration schema](https://github.com/pytorch/executorch/blob/v1.2.0/extension/llm/export/config/llm_config.py), [native tokenizer loading](https://github.com/pytorch/executorch/blob/v1.2.0/extension/llm/runner/llm_runner_helper.cpp).

Authentication/cache compatibility: provisioning pins the official HF endpoint, disables HF telemetry, scopes heavy caches to ORBIT and preserves an existing local token location through documented `HF_TOKEN_PATH`. Alternatively authenticate with the same scoped `HF_HOME` or an owner-managed `HF_TOKEN`. Tokens are never printed. Context7 resolved `/huggingface/huggingface_hub` and verified these environment-variable semantics.
