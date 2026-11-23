# Evidence scope

Historical first increment only. Later real Llama generation, updated tests and genuine offline recording supersede the earlier missing-resource/fallback scope described below. Use `docs/PS3_VALIDATION.md` and `docs/reports/llama-runtime-2026-11-23/` for the final implementation evidence; do not combine these older measurements into a final accuracy claim.

`emulator-launch.json`: one actual Arm64 Android 14 emulator launch, airplane setting enabled. Includes raw startup/memory/frame diagnostics. Not physical-phone inference performance.

The separate ORBIT fallback-flow recording uses only the synthetic sentence “Please send your OTP to finish verification.” It shows deterministic detection and labelled curated guidance. It cannot demonstrate Llama, installed resources or full PS3 offline acceptance. No private user content, credentials or malware executable is included.

Readable recording: `/Volumes/ORBIT/Nivoryn/captures/nivoryn-readable-offline-demo-2026-11-23.mp4` (19.895 seconds, 891150 bytes). Container verified with ffprobe; an extracted frame at 8 seconds was visually checked and shows the actual high-risk rule result with local/no-AI scope. The instrumentation flow passed. The test harness holds the result for eight seconds only when `recording=true`; production timing is unchanged. Use `scripts/record-offline-demo.py` with an installed app and test APK to reproduce.
