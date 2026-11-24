# Local Llama setup

Built with Llama. Runtime: dependency-locked `org.pytorch:executorch-android:1.2.0`, CPU/XNNPACK, Arm64 Android. On 23 November 2026 an Android 14 Arm64 emulator in airplane mode loaded the actual 1B SpinQuant model and generated an explanation accepted by the app's evidence/advice validator. This establishes emulator compatibility, **not physical-phone performance or universal output correctness**. The first two prompt attempts were rejected; rejection was preserved and the prompt corrected rather than bypassing validation.

## Set up on Android (0.2.0)

1. Open **Home → Set up local AI**. Basic checks already work without it.
2. Check the displayed device information. The supported model needs Arm64 Android, about 1.14 GB to download and at least 2.6 GB free during installation, in addition to existing resources. 4 GB RAM or more is recommended; the real inference check determines readiness.
3. Read the bundled Llama licence/notice and publisher access instructions. Confirm that you meet the licence and acceptable-use terms. No developer account token is bundled or requested.
4. Keep **Allow mobile data** off to use an unmetered connection. Select **Download and verify local AI**.
5. Watch the actual downloaded-byte progress. Android manages network pauses/retries and resumption, including after process interruption. Manual pause is not supported. **Cancel setup operation** removes download copies; previously verified installed resources are kept.
6. Wait for exact size/hash verification, private resource activation and the real Llama inference check. Download completion alone does not establish readiness.
7. Once **Ready for offline explanations** appears, disconnect or enable airplane mode and run a supported check. Each result names its actual explanation mode. A rejected generation still uses clearly labelled curated fallback.
8. Use **Delete model resources** to remove active, previous and partial resources/download copies. Saved checks are separate and remain unchanged.

The download source is the exact upstream-linked published conversion below, pinned to a reviewed revision. Android DownloadManager stores temporary files in app-specific device storage; verified resources move into private no-backup model storage. No Android path points to the Mac's ORBIT volume. If access later requires an account or gated licence acceptance, follow the publisher's legitimate steps in your browser and use advanced import. Do not paste account tokens into the app.

**Advanced import:** expand **Advanced: import existing resources**, select the exact supported model export and matching tokenizer, then select **Test local inference**. Arbitrary `.pte`, GGUF and training checkpoints are refused. Failed/cancelled import never activates an incomplete resource pair.

## Tested resources

The app pins this published conversion, linked directly by the [official ExecuTorch v1.2.0 Llama README](https://github.com/pytorch/executorch/blob/v1.2.0/examples/models/llama/README.md):

- [executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET](https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET), revision `911221e0f49d3354cbd4697a9c09feee25cadec7`.
- `model.pte`: 1,135,951,488 bytes; SHA-256 `8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b`.
- `tokenizer.bin`: 2,183,982 bytes; SHA-256 `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55`.

This is a published conversion, **not a local export or a Meta-hosted checkpoint**. Its original recipe predates runtime 1.2.0. Actual Android forward/generation, including Llama cache/SDPA/Hadamard and quantised embedding operators, was exercised with the pinned 1.2.0 AAR. Generic macOS Python portable bindings lack those LLM kernels and failed forward execution; metadata loading alone was not counted as inference.

The owner obtained official Meta access and authenticated locally. The [official SpinQuant source](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8), revision `b8c08c95d0476ccb6fc86e9410818d602ecca19d`, was downloaded to ORBIT with `tools/model/provision.py download`. Its TikToken tokenizer hash matches the tested published conversion. No non-Llama substitute or external inference endpoint is used.

## Phone installation

Accept the Llama 3.2 Community Licence and acceptable use policy using your own account. Never paste credentials into chat. On this Mac verified files already exist in `/Volumes/ORBIT/Nivoryn/models/llama-3.2-1b-executorch-public/`.

Reproduce using an isolated Python environment containing `huggingface_hub`:

```sh
python tools/model/download-supported.py
zsh scripts/android.sh :app:assemblePreview
```

The downloader scopes caches to ORBIT, pins the publisher revision, verifies independently reviewed build hashes, retains resumable downloads and includes licence/notice files. It does not trust imported checksum sidecars. For advanced import, copy `model.pte` and `tokenizer.bin` to a phone folder; install the APK; open Home → Set up local AI → Advanced: import existing resources → Import model export, then Import matching official tokenizer. Select each using Android's picker. Original `.pth`, `.safetensors`, GGUF and arbitrary `.pte` files are unsupported.

Allow at least 1.3 GB free **app-private** storage, plus the source copy and any previous version. Phone paths never use ORBIT. Only a size/hash-verified complete pair activates atomically. Interrupted copies can be reselected to resume. Update/delete/rollback unloads cached inference under the same lock; rollback requires a still-trusted complete prior pair. “Test local inference” must report actual Llama mode; curated fallback is not success. Cold-start in airplane mode and follow PHONE_ACCEPTANCE.md.

## Compatibility and limits

SpinQuant: INT4 group-32 linear weights, dynamic per-token INT8 activations, 8-bit embedding/output weights. Export metadata: context/sequence 2048, KV cache, dynamic shapes and custom SDPA. App total sequence limit 1024; output at most 2048 characters, one concurrent job, 30-second generation deadline, cancellable native stop. Model loading is outside that deadline and has no hard-kill guarantee. The process-owned module is cached and its context reset per request.

Native v1.2.0 [tokenizer loading](https://github.com/pytorch/executorch/blob/v1.2.0/extension/llm/runner/llm_runner_helper.cpp) probes TikToken content. `tokenizer.model` is preserved unchanged as `tokenizer.bin`; no Llama 2 conversion is applied. The catalogue is a build-pinned trust root, not proof that arbitrary imported files are safe.

Prompt JSON syntax is prefilled, but every explanation word is generated by Llama. Only the authored fact catalogue and approved action IDs enter the prompt; raw input, quotes, hosts and attacker instructions do not. Per-sentence citations/vocabulary, unsafe instructions/contact details, certainty, inverse reassurance and action references are checked; trailing output or overflow is rejected. Accepted modes distinguish model references, locally attached source references when exactly one known finding supplies context, and limited no-warning checks. Local attribution never substitutes prose or applies to ambiguous multiple findings. UNKNOWN inputs bypass inference; rejected output uses labelled curated fallback. This is bounded validation, not complete semantic entailment verification. Remediation text remains curated. The statistical SMS classifier is independent of Llama.

Phone RAM, latency, battery and frame pacing remain unverified. Initial emulator native logs reported about 1.52 GiB process RSS; this is not a complete device memory budget or a phone measurement. Do not infer memory needs from parameter arithmetic.

## Licence and source export

Llama 3.2 Community Licence/AUP applies. The APK includes `LLAMA32_LICENSE.txt`, `LLAMA_NOTICE.txt` and an offline viewer; the downloader includes both notices. Weights are outside Git/APK.

For a fresh export of the authorised official checkpoint, see tools/model/README.md and `provision.py`. Source download ran; the exporter did not yet run successfully because its full Python dependency installation encountered an `antlr4-python3-runtime` build failure. Do not describe the tested published conversion as a locally reproduced export.
