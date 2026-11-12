#!/usr/bin/env bash
# Fetches the interim Read models: Tesseract's "fast" LSTM recognisers for the first five
# languages and English, pinned to one commit of github.com/tesseract-ocr/tessdata_fast
# (Apache-2.0, per the repository's LICENSE as read on 12 Nov 2026), each checked against its
# SHA-256 before it is used. They land in app/src/main/assets/tessdata/ (git-ignored) and ride
# inside the APK: the app never downloads anything, it has no INTERNET permission.
#
# Interim and borrowed: docs/MODELS.md says so. Our own detector and recogniser (P2, items 12-13)
# replace them script by script when they beat them on the Ikshana set.
#
#   sh tools/models/fetch_tessdata.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEST="$ROOT/app/src/main/assets/tessdata"
COMMIT=87416418657359cb625c412a48b6e1d6d41c29bd
mkdir -p "$DEST"
while read -r lang sum; do
  f="$DEST/$lang.traineddata"
  if [ ! -s "$f" ] || [ "$(sha256sum "$f" | cut -c1-64)" != "$sum" ]; then
    curl -sSL -f -o "$f.part" "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/$COMMIT/$lang.traineddata"
    mv "$f.part" "$f"
  fi
  got="$(sha256sum "$f" | cut -c1-64)"
  [ "$got" = "$sum" ] || { echo "$lang.traineddata: sha256 $got, expected $sum"; rm -f "$f"; exit 1; }
  echo "ok $lang"
done <<'PINS'
eng 7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2
mal bd05cbf1b197e7810d2903419aedb06f9ef77bfedf50b358673c1d18d707cdb4
hin 4c73ffc59d497c186b19d1e90f5d721d678ea6b2e277b719bee4e2af12271825
tam d02fbec24be4b07e32e80d0ccfc3b6b67a3c5d61c9d0a7c8532677990912c6ec
kan bd31e6b6ae93271e3bcf5383d306d8eefbb91542937cd6d735a5930c970e61d8
tel d10691fddd5b67802e1c12800ebb321d3b8bcd8d24a2ac3ff206f93188c04ab5
PINS
