# Third-party notices

Ikshana is GPL-3.0 (`LICENSE`). Everything below is someone else's work, listed the day it was
added, with its licence and where it lives in this repository. **Every dataset's licence is
pinned to the page it was downloaded from before a single line of it is used to train anything**;
where sources disagree, both readings are written here and the owner decides before training.

Last updated: 12 November 2026.

## In the app

| Piece | Licence | Where | Added |
|---|---|---|---|
| Kotlin standard library | Apache-2.0 | every module | 12 Nov 2026 |
| AndroidX Core, Activity, Lifecycle, Annotation, Startup | Apache-2.0 | `:app` (`gradle/libs.versions.toml`) | 12 Nov 2026 |
| Jetpack Compose (runtime, foundation, animation, ui, material3), BOM 2025.09.01 | Apache-2.0 | `:app` | 12 Nov 2026 |
| CameraX 1.4.2 (core, camera2, lifecycle, view) | Apache-2.0 | `:app` | 12 Nov 2026 |
| Tesseract OCR 5 (via tesseract4android 4.9.0, Robert Pösel) | Apache-2.0 | `:app`, from JitPack, filtered to its one group | 12 Nov 2026 |
| Leptonica (inside tesseract4android) | BSD-2-Clause | `:app` | 12 Nov 2026 |
| llama.cpp and ggml (build b12319, commit f3d7725a), the ggml authors; with the libraries it vendors into its multimodal library (stb_image, miniaudio, sheredom's subprocess.h, a hash library), each under its own notice in `third_party/llama.cpp/licenses` and `vendor/` | MIT (vendored pieces: MIT or public domain / MIT-0, per their headers) | `third_party/llama.cpp` (git submodule), built into `libikshana_vision.so` by `app/src/main/cpp` | 12 Nov 2026 |
| Space Grotesk, Florian Karsten | SIL Open Font License 1.1 | `app/src/main/res/font/space_grotesk_*.ttf` (copied from Stave) | 12 Nov 2026 |

LiteRT 1.4.0 (Apache-2.0) is named in the version catalogue and is **not yet a dependency**; it
joins `:app` with the first model (P2) and gets its row here that day.

## Build and test only (not in the APK)

| Piece | Licence | Where |
|---|---|---|
| Kotlin compiler (embeddable) 2.2.21, kotlinx-coroutines, trove4j, JetBrains annotations | Apache-2.0 (trove4j LGPL-2.1, fetched at build time, never shipped) | `tools/jvm/run.sh`, `jc.sh` |
| JUnit 4.13.2 | EPL-1.0 | `:engine` tests |
| Android Gradle Plugin 8.13.2, Gradle 8.13 | Apache-2.0 | build |

## Evaluation only (never trained on, never shipped)

| Piece | Licence | Where |
|---|---|---|
| VizWiz validation photos and questions (vizwiz.org), 60 photos taken by blind people, through a Hugging Face mirror | CC BY 4.0 as stated by vizwiz.org; **not yet pinned to that page** | outside the repository (`~/.ikshana-build/vizwiz`) |
| Three openly licensed candidate vision models in GGUF form (0.5B, 1B and 2B parameters; Apache-2.0 per their model cards) | Apache-2.0 | outside the repository (`~/.ikshana-build/models`); the one chosen gets a row above the day it ships |

## Copied from the family

The owner's own repositories, GPL-3.0 like this one. Each copied file says where it came from in
its first lines.

| From | What |
|---|---|
| `stave-android` | `tools/check_*.py`, `tools/verify_manifest.py` (turned around for a camera), `tools/jvm/run.sh`, `Checks.kt`, `Main.kt`, `EngineSuitesTest.kt`, `RunnerChecks.kt`, `Registry.kt`'s shape, the theme's palette and type, the CI workflows, the manifest's privacy block, `data_extraction_rules.xml`, `compose_stability.conf`, `keystore.properties.example` |
| `squish-android` | `tools/jvm/jc.sh` and `run_desktop.sh` (their shape; rewritten for a module engine) |

## Models

| Model | Licence | Source | Size | Tier |
|---|---|---|---|---|
| Tesseract `tessdata_fast` LSTM recognisers: eng, mal, hin, tam, kan, tel | Apache-2.0 (the repository's LICENSE, read 12 Nov 2026) | github.com/tesseract-ocr/tessdata_fast at commit 87416418657359cb625c412a48b6e1d6d41c29bd, SHA-256 pinned in `tools/models/fetch_tessdata.sh` | 20.1 MB together | **borrowed, interim** (see `docs/MODELS.md`) |

Every model gets a row here and in `docs/MODELS.md` the day it is added: its licence,
its source, its size, and whether it is fully ours, trained by us on open data, or fine-tuned
from open weights.

## Datasets

None used yet. The scene-text sets named in the brief (IndicSTR12, Bharat Scene Text, ICDAR MLT)
will each get a row with the licence as stated on the page it was downloaded from, the URL and
the date, **before** any of it is read by a training script.
