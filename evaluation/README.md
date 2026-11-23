# Reproducible evaluation

Data master: `/Volumes/ORBIT/Nivoryn/datasets/uci-sms/`. Public source: Almeida & Hidalgo (2011), [UCI SMS Spam Collection](https://archive.ics.uci.edu/dataset/228/sms+spam+collection), DOI 10.24432/C5CC84, CC BY 4.0 according to UCI. The original readme/provenance are retained with the downloaded corpus; no corpus messages are committed or shown in the demo.

```sh
python3 evaluation/prepare-uci.py --directory /Volumes/ORBIT/Nivoryn/datasets/uci-sms
zsh scripts/android.sh :app:evaluateSmsCorpus -PsmsCorpus=/Volumes/ORBIT/Nivoryn/datasets/uci-sms/SMSSpamCollection -PevaluationOutput=/private/tmp/rules-spam-proxy.json --offline
python3 tools/training/train-sms.py --corpus /Volumes/ORBIT/Nivoryn/datasets/uci-sms/SMSSpamCollection --output /Volumes/ORBIT/Nivoryn/models/sms-spam-proxy-v1.json --report /private/tmp/classifier-evaluation.json
```

The opt-in evaluator calls the actual Kotlin deterministic detector. It is not hidden behind skipped tests and does not make normal builds require external data. Its results are a diagnostic **spam proxy**, not scam/fraud ground truth. Normalised duplicate messages are grouped before scoring; conflicting labels are excluded. No raw examples or credentials enter reports.

The separately bundled statistical model uses 4128 training / 1031 held-out messages after deduplication. Vocabulary is fitted on training only; seed 228, stratified 80/20 split, 4000 maximum unigram features, NFKC/lower, 600 tokens, per-token count cap 5, fixed threshold 0.99 and at least three known features. Kotlin/Python probability parity is tested against synthetic multilingual/normalisation cases. Threshold was not selected by optimising the test split. Template/near-duplicate families beyond exact normalisation were not grouped: residual overlap and old English spam labels limit generalisation.

Measured held-out results: TP 113, FP 0, TN 903, FN 15; precision 1.0, recall 0.8828125 on this split only. Zero observed false positives is not a guarantee; the test set has 903 negatives. This does not establish Indian scam, Hindi/Hinglish, adversarial or live-device performance. The bundled classifier emits a cautious spam-like signal, never a fraud verdict, and is separate from Llama/ExecuTorch.

Rule-category, Hindi/Hinglish and parser fixtures are authored development/regression cases, not representative accuracy evidence. No CICIDS/UNSW/NSL data or malware executables are used. Network tests are safe synthetic protocol fixtures. Real phone/offline Llama acceptance and Arm Linux workload tests remain the owner's later acceptance stage.
