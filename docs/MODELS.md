# Models

Every model that ever runs in the app, ours or borrowed, with its tier, its score from the
harness, its size, and what runs when a phone cannot carry it. **Every claim about a model is a
number on this page**; nothing is judged by eye or by ear. A model is not trusted on the phone
until its conversion is checked against the reference run on an input the conversion never saw.

Last updated: 12 November 2026.

## What exists today

**One borrowed model, interim, and none of ours yet.** At the owner's request on 12 Nov 2026
("do the complete end to end dev"), Read runs on Tesseract's fast LSTM recognisers (Apache-2.0)
while our own OCR is built. It is the last tier of Read (`ocr.tesseract`), below ours and their
twins, so ours take over the day they are in the pack. **It has no harness score yet**, because
the harness and the Ikshana set (P1) do not exist yet; that is the first number this page owes.

| Model | Tier | Harness score | Measured | Size |
|---|---|---|---|---|
| `ocr.tesseract` (mal+eng loaded; hin, tam, kan, tel available) | **borrowed, interim**: not ours, not fine-tuned | none yet (P1) | moto g84, 12 Nov: 350-530 ms a frame at 960 x 1280, LSTM only, on the analysis thread (logged, on a frame with no text) | 20.1 MB for six languages |

Money runs on the same borrowed reader, English only, through `engine/money` (fully ours rules; the
numbers are read by the borrowed model). Every other mode that needs a model falls to Read now, by the chain in `docs/FALLBACKS.md`.

The pure-arithmetic pieces that do the work of a model in P0 are executed, not scored:

| Piece | Tier | How it is checked | Number |
|---|---|---|---|
| Light level (`LightLevel`) | fully ours | `LightChecks`: means on known planes, band edges, hysteresis | exact on the fixtures; band edges are P0 values, to be set from the Ikshana set (P1) |

## The ids the app knows

`engine/.../mode/Modes.kt` (`Models`) names these; the app looks for `assets/models/<id>.tflite`.

| Id | Mode(s) | Tier (planned) | Lighter twin or fallback | Harness score | Size | Status |
|---|---|---|---|---|---|---|
| `ocr.detector` | Read, Translate | trained by us on open data (DBNet-class, ~15M) | `ocr.detector.lite`; then Light level | — | — | not built |
| `ocr.recogniser` | Read, Translate | trained by us on open data (CTC, ~10M, a head per script) | `ocr.recogniser.lite`; then Light level | — | — | not built |
| `ocr.detector.lite` | Read, Translate | trained by us on open data | Light level | — | — | not built |
| `ocr.recogniser.lite` | Read, Translate | trained by us on open data | Light level | — | — | not built |
| `currency` | Money | fully ours (small CNN on photos we take) | Read (the numerals on the note) | — | — | not built |
| `vlm` | Describe, Find, Ask | fine-tuned from open weights (SmolVLM2-class, Apache-2.0) | `vlm.lite`; then Read / Describe | — | — | not built |
| `vlm.lite` | Describe, Find, Ask | fine-tuned from open weights | Read / Describe | — | — | not built |
| `translate` | Translate | fine-tuned from open weights (IndicTrans2 distilled, MIT) | `translate.lite`; then Read untranslated | — | — | not built |
| `ocr.tesseract` | Read | borrowed, interim | Light level | — | 20.1 MB | **in the app** |
| `translate.lite` | Translate | fine-tuned from open weights | Read untranslated | — | — | not built |

The gate numbers the brief sets, so they are written before anything is measured against them:
Malayalam CER under 8% on the clean two thirds of the set before v0.1; currency top-1 above 99%
held out; Describe under five seconds on the moto; translation BLEU against the reference run
within a stated margin.
