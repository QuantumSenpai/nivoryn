# Authenticated local indicator packs

The app accepts RSA/SHA-256 signatures over exact payload bytes only when the publisher's X.509 DER public key is pinned separately in the signed application's `trusted-rule-keys.json` asset (`{"publisher-id":"base64-public-key"}`). No keys are shipped in this development build. A key embedded in a downloaded envelope is never trusted. Never commit a publisher private key; keep release keys outside source, protected by the owner.

Payload schema:

```json
{"schema":1,"source":"publisher and provenance URL","licence":"appropriate data licence","version":1,"issued":1795392000000,"expires":1795996800000,"domains":[],"apkSha256":[],"certificateSha256":[]}
```

Times are UTC epoch milliseconds, with at most 90 days validity. Domains are ASCII/IDNA names; hashes are lowercase SHA-256. Each array has at most 10000 entries; payload <=1 MB. Release maintainers must verify data provenance, licence, false positives and publisher identity before signing. Empty arrays are not a threat feed or detection evidence.

```sh
python3 tools/rules/sign-pack.py --payload reviewed-pack.json --key /secure/offline/publisher.pem --key-id reviewed-publisher --output signed-pack.json
```

Import in Settings → Rules and indicators. Verification precedes activation; expired/future packs and silent version downgrades are rejected. Active/previous files are atomic. Explicit rollback re-verifies signature and freshness. Stale lists stay visible but cannot produce matches. Deletion removes imported packs, not built-in rules/history. Matching a domain, APK digest or certificate is evidence with source/version/freshness, not universal malware/phishing proof. There is no automatic network update or SSRF surface.

Signature API reference: [Java Signature](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/Signature.html). Tests generate ephemeral test-only keys and verify valid import, tampering, downgrade refusal and rollback.
