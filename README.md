# Hive Vision — FTC ball detection

<p align="center"><img src="logo.png" alt="Hive Vision" width="360"/></p>

Full docs: <https://hivevisionftc.github.io/HiveVisionDocs/>

<details>
<summary><strong>Contents</strong></summary>

- [Deployment tradeoffs](#deployment-tradeoffs)
- [Features](#features)
- [Model details](#model-details)
- [PC tools](#pc-tools)
  - [Licensing](#licensing)
- [Quick start](#quick-start)
  - [Control Hub track (OpenCV)](#control-hub-track-opencv)
  - [Control Hub track (Lab — shadow-robust chromaticity, no model)](#control-hub-track-lab--shadow-robust-chromaticity-no-model)
  - [Limelight 3A FTC integration](#limelight-3a-ftc-integration)
- [Publishing the model](#publishing-the-model)
- [Performance](#performance)
  - [What these numbers mean](#what-these-numbers-mean)
- [Repo layout](#repo-layout)
- [Ball-chase API](#ball-chase-api)
- [Shipping](#shipping)
- [Demo clips](#demo-clips)
  - [Automatic video previews](#automatic-video-previews)
  - [Image samples](#image-samples)
- [Community](#community)
- [Updates and training data](#updates-and-training-data)
- [Training-data workflow](#training-data-workflow)
- [License](#license)

</details>

Hive Vision is a real-time ball-detection suite for FTC, ready to drop into
your robot stack. Every game object — the team's balls in their lane colors —
is found and tracked with two deployment options:

| Track | Where it runs | Cost | Artifacts |
|-------|---------------|------|-----------|
| **Limelight 3A (TFLite)** | on the Limelight 3A connected to the robot | **SSD-MobileNetV2** (300×300, uint8-in / float-out `.tflite`) | [`neural-net/weights/best_limelight3a_ssd_mobilenetv2_300x300.tflite`](neural-net/README.md) + [`labels.txt`](neural-net/weights/labels.txt) |
| **Control Hub (OpenCV)** | on the robot Control Hub itself | none — pure OpenCV, no model | [`TeamCode/BallDetectorPipeline.java`](cv/README.md) |

These are alternative robot deployments. Use the Limelight 3A track when the
robot has a Limelight 3A; use the Control Hub track when detection must run on
the Control Hub without a model or coprocessor — the raw-HSV track is the
absolute cheapest path. Both detect `yellow`, `red`, and `blue` in that class
order. The Control Hub detector returns color candidates and must be confirmed
across several frames before the robot acts on one.

Two earlier tracks — the **PC / ONNX Runtime (YOLOv8n)** track and the
**Control Hub Lab** chromaticity track — are **archived** (see
[`archive/`](archive/README.md)); they are not maintained and are kept for
reference only.

## Deployment tradeoffs

| Option | Strengths | Tradeoffs |
|--------|-----------|-----------|
| Limelight 3A (SSD-MobileNetV2) | on-robot neural detection, adaptable to shape, distance, blur, and changing backgrounds | Requires a Limelight 3A, model upload, and FTC-side result integration |
| Control Hub OpenCV | Lightweight, low-cost, and fast to run locally with no model or coprocessor | Less adaptable to lighting, white balance, camera resolution, and colors that resemble the balls; requires field retuning |

Use the Limelight 3A track when the robot can support it and detection
reliability matters more than the extra hardware and setup. Use OpenCV when
simplicity and low hardware cost matter most.

## Features

- **Two paths, one class order** — the Limelight 3A SSD and the Control Hub
  OpenCV/HSV track both report `yellow`, `red`, `blue`, so FTC-side code and
  labels stay in sync across deployments.
- **No coprocessor, no model** for the hub track — just EasyOpenCV on the
  Control Hub.
- **~2 confirmable blob candidates per frame** after gating (a 900–12000 px
  ball-size band, squareness, and fill) vs ~50 raw threshold blobs.
- **Best-blob picker per color** — nearest to the real ball's area and
  roundness, penalized at the frame edge, so robot panels of the same hue
  don't win.
- **Measured, not guessed** — recall vs the model's reference detections on a
  shared 293-frame truth: yellow 62%, red 35%, blue 53% (the CV track's own
  866-frame measurement is in
  [`cv/docs/cv_detector_report.md`](cv/docs/cv_detector_report.md)).
- **Drop-in FTC API** — [`api/`](api/) has `ftc_ball_chase_lib`: a tracker
  (`BallTracker`), a no-odometry chase state machine, a Pedro auto hybrid, and
  the one-line `BallHunt` — `new BallHunt(follower, limelight, intake).reds().collect(2).go(this)`.

## Model details

The Limelight 3A model was trained on synthetic renders plus real footage —
the SSD retrain now uses a corpus of roughly 14,000 labeled images:

- **SSD-MobileNetV2** (`best_limelight3a_ssd_mobilenetv2_300x300.tflite`) —
  the model on the Limelight 3A neural detector, trained for the 3A's required
  full-INT8 `TFLite_Detection_PostProcess` contract (details in
  [`neural-net/README.md`](neural-net/README.md)).

The dataset includes yellow, red, and blue balls, plus difficult examples
involving blur, occlusion, distance, field lighting, shadows, and
robot-colored distractions. Evaluation results were measured on separate
footage from the training images.

An earlier YOLOv8n reference model (`best.pt` / `best.onnx`) is archived under
[`archive/yolo-pc/`](archive/yolo-pc/README.md); it was the ground-truth source
the Control Hub tracks were originally tuned against and is no longer a
deployment target.

## PC tools

```bash
pip install -r requirements.txt
```

Python 3.10+ is required only for the optional PC viewers, evaluation, and
tuning tools. The Limelight 3A deployment uses the shipped SSD `.tflite`; the
Control Hub deployment uses the Java files and does not need Python.

### Licensing

Original Hive Vision code, documentation, and configuration are released
under the MIT License. The archived YOLO weights and Ultralytics-dependent
material under [`archive/yolo-pc/`](archive/yolo-pc/README.md) are distributed
subject to Ultralytics' AGPL-3.0 licensing terms; see
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Check those terms before
incorporating any YOLO artifacts into a closed-source product.

## Quick start

### Control Hub track (OpenCV)

```bash
# Preview the on-hub detector output on your own footage (same viewer keys)
python cv/tools/realtime_cv.py --source path/to/match.mp4

# Verify the shipped config against model truth (needs the match video)
python cv/tools/eval_cv_vs_yolo.py --config cv/tools/hsv_tuned.json --source path/to/match.mp4
```

### Control Hub track (Lab — shadow-robust chromaticity, no model)

```bash
# Preview the Lab detector on your own footage
python archive/lab/tools/realtime_lab.py --source path/to/match.mp4

# Live-tune the hue band + adaptive chroma floor on a field frame/webcam
python archive/lab/tools/lab_tuner.py path/to/frame.jpg

# Verify versus the same model truth (learned config in archive/lab/tools/lab_tuned.json)
python archive/lab/tools/eval_lab_vs_yolo.py --source path/to/match_raw.mp4 \
  --config archive/lab/tools/lab_tuned.json --compare-hsv
```

For the Limelight 3A, upload
**`neural-net/weights/best_limelight3a_ssd_mobilenetv2_300x300.tflite`** with
[`neural-net/weights/labels.txt`](neural-net/weights/labels.txt) and class order
`yellow_pollen`, `red_nectar`, `blue_nectar`. Read detections through the
Limelight API used by your FTC integration. For the Control Hub path, copy
the shared base `cv/TeamCode/BallBlobPipeline.java` plus
`cv/TeamCode/BallDetectorPipeline.java` (HSV) into
`TeamCode/`, register the processor with a `VisionPortal`, and read
`bestOf(BallColor)` — see [cv/README.md](cv/README.md).

### Limelight 3A FTC integration

Once the model is on the Limelight, [`ftc_ball_chase_lib/`](ftc_ball_chase_lib/README.md)
is the ready-to-read FTC-side library: a `BallTracker` perception core (confidence
+ staleness gating, a color-bound target lock, field projection), two drivers
using it — `BallChaseController` (no-odometry teleop/auto state machine) and
`BallChaseFollower` (Pedro Pathing auto hybrid) — and a fluent `BallWrangler`
verb API over Pedro or mecanum drivetrains. The one-liner:

```java
int picked = new BallHunt(follower, limelight, intake)
        .reds().collect(2).within(20)
        .go(this);   // blocking: drives, picks 2 red balls, hands control back
```

It compiles against FTC SDK 11.x + Pedro Pathing 2.x with a laptop self-test
(`\.compile_check.cmd -runwrapper`). Full class reference and tuning table:
[`ftc_ball_chase_lib/MODULES.md`](ftc_ball_chase_lib/MODULES.md).

## Publishing the model

For the **Limelight 3A** neural detector, upload
`neural-net/weights/best_limelight3a_ssd_mobilenetv2_300x300.tflite` with
`labels.txt`; its outputs are the SSD post-process tensors (`num`,
`scores [1,10]`, `class_ids [1,10]`, `boxes [1,10,4]`, normalized), read them
through the Limelight API used by your FTC integration (see
[`neural-net/README.md`](neural-net/README.md)). The 3A requires a full-INT8
SSD with `TFLite_Detection_PostProcess` — do not upload float in/out models.

## Performance

| Track | Recall | Precision | Note |
|-------|--------|-----------|------|
| Control Hub (OpenCV) | yellow 62.0% / red 34.5% / blue 52.8% | 10.5 / 8.6 / 11.1% | raw per-frame **candidate** precision — treat as a candidate signal |

Measured against a shared 293-frame truth (the Control Hub track's own
separate evaluation on its 866-frame dev video reports higher raw numbers —
yellow 68.7% / red 80.1% / blue 82.3% recall at 39.9 / 26.0 / 65.5% precision —
because that footage skews easier for HSV; see
[`cv/docs/cv_detector_report.md`](cv/docs/cv_detector_report.md)).

### What these numbers mean

These are **detector benchmarks with a specific methodology, not end-to-end
robot results.** Recall/precision above measure how well each track generates
*ball candidate detections* against YOLO reference detections on development
footage — they do **not** measure successful autonomous pickups, false pickup
attempts, cycle time, or match performance. "Yellow 62.0% recall" does not mean
"the robot picks up 62.0% of yellow balls." Treat the Control Hub numbers as
*candidate quality*: get from a candidate to a robot action via `BallTracker`'s
target lock (and optionally its multi-frame confirmation), then measure pickup
success on your own robot.

## Repo layout

```
hive-vision/
  neural-net/              Limelight track (SSD TFLite)
    weights/best_limelight3a_ssd_mobilenetv2_300x300.tflite  Limelight 3A model (5 MB, SSD-MobileNetV2)
    weights/labels.txt        class labels: pollen/nectar order
    scripts/                  SSD live viewer
  cv/                         Control Hub track (HSV)
    TeamCode/                 BallBlobPipeline.java + BallDetectorPipeline.java (drop-in processors)
    tools/                    tuning/eval tooling + shipped HSV config
    docs/                     evaluation report, cached model truth
  archive/lab/                  ARCHIVED Control Hub track (Lab chromaticity, retired 2026-10-11)
    TeamCode/                 BallBlobPipeline.java + LabBallDetectorPipeline.java (drop-in processor)
    tools/                    YOLO-learned config, fitter, sweep, tuner, viewer
    docs/                     evaluation report, cached YOLO reference detections
  archive/yolo-pc/             ARCHIVED PC / YOLO / ONNX track (retired 2026-10-11)
    weights/                  best.pt, best.onnx, float32 tflite (reference model only)
    scripts/                  YOLO/int8 PC viewers + exporters
    docs/                     retired PC/ONNX doc pages
  ftc_ball_chase_lib/          FTC-side library (BallTracker + chase drivers)
    final/                     BallTracker, BallChaseController/Follower, BallHunt, BallMath
    wrapper/                   fluent verb API over Pedro/mecanum drivetrains
    MODULES.md                 class reference; README.md = usage + calibration
  shipping/                   ready-to-ship handbook (artifacts, comparison, checklist)
  demo/                       short annotated example clips
  logo.png                    project logo
  LICENSE                     MIT (code) — see THIRD_PARTY_NOTICES.md for archive/yolo-pc/ (AGPL-3.0)
```

## Ball-chase API

This repo is a **monorepo**: the FTC ball-chase library ships inside it as
`ftc_ball_chase_lib/` rather than a separate repo, so it keeps this repo's
visibility. Copy that folder (or just `final/`) into your `TeamCode` and the
Limelight sighting turns into a robot action:

- `BallTracker` — perception core (confidence/staleness gating, target lock,
  multi-frame confirmation)
- `BallChaseController` — no-odometry chase state machine
- `BallChaseFollower` + `BallHunt` — Pedro auto hybrid + the one-line collect
- `wrapper/` — fluent verb API over Pedro/mecanum drivetrains

Full docs live in [api/](api/). The library has its own CI
(`.github/workflows/libtests.yml` compiles and self-tests it against the FTC
SDK + Pedro jars) and its own release notes
([`RELEASE_NOTES_v1.1.0.md`](RELEASE_NOTES_v1.1.0.md)). See it in action in
the [demo clips](#demo-clips): `demo/probiotix_api_robot_drive.mp4` is a robot
drive coded using the API, provided by Team 16765 ProBotiX.

## Shipping

Ready to deploy? Start at
[`shipping/SHIP_README.md`](shipping/SHIP_README.md) — one checklist and one
place listing every artifact, where it goes, the three-track comparison, the
int8 rationale, and the two remaining hardware-validation steps.

## Demo clips

These short 1920x1080 clips show the deployment paths on real footage.
They were sourced from the [kickoff video](https://www.youtube.com/watch?v=gO98TkgY0kI)
and are examples for visual inspection, not a replacement for field testing
or the precision/recall measurements in the CV report.
Confirm that you have permission to redistribute any source footage before
publishing these demo files outside this repository. Replace them with your
own footage if permission is unclear.

| Clip | Shows | Footage |
|------|-------|---------|
| [`demo/ssd_mobilenetv2.mp4`](demo/ssd_mobilenetv2.mp4) | Limelight 3A SSD-MobileNetV2 example | kickoff video |
| [`demo/control_hub_cv_example_1.mp4`](demo/control_hub_cv_example_1.mp4) | Control Hub/OpenCV example | kickoff video |
| [`demo/control_hub_cv_example_2.mp4`](demo/control_hub_cv_example_2.mp4) | Control Hub/OpenCV example | kickoff video |
| [`demo/cielab_demo.mp4`](demo/cielab_demo.mp4) | CIELAB (Lab track, archived) example | own match capture |
| [`demo/ssd_in_action.mp4`](demo/ssd_in_action.mp4) | Limelight 3A SSD tested in the real world | recorded by Javi Nashat (Lazer Robotics 23286) |
| [`demo/javi_v1_1_ssd.mp4`](demo/javi_v1_1_ssd.mp4) | Limelight 3A SSD v1.1 tested in the real world | recorded by Javi Nashat (Lazer Robotics 23286) |
| [`demo/probiotix_api_robot_drive.mp4`](demo/probiotix_api_robot_drive.mp4) | robot drive coded using the [ball-chase API](api/) | provided by Team 16765 ProBotiX |
| [`demo/sid_47_beavers_robot_drive.mp4`](demo/sid_47_beavers_robot_drive.mp4) | robot drive coded using the [ball-chase API](api/) | provided by Sid — FTC 13100 47 Beavers (Index 47.org) |

**Footage credits**

- `demo/ssd_in_action.mp4` — Limelight 3A SSD-MobileNetV2 tested in the real
  world, filmed by **Javi Nashat** (Lazer Robotics 23286). Contact/handles:
  Discord `@Destroyer` (handle `destroyer5782`).
- `demo/javi_v1_1_ssd.mp4` — Limelight 3A SSD-MobileNetV2 **v1.1** tested in
  the real world, recorded by **Javi Nashat** (Lazer Robotics 23286).
- `demo/probiotix_api_robot_drive.mp4` — robot drive coded using the
  [ball-chase API](api/), provided by **Team 16765 ProBotiX**
  (probotixbladel@gmail.com) from their robot testing — thank you for the footage.
- `demo/sid_47_beavers_robot_drive.mp4` — robot drive coded using the
  [ball-chase API](api/), provided by **Sid — FTC 13100 47 Beavers
  (Index 47.org)** — thank you for the footage.

### Automatic video previews

These animated previews loop automatically in GitHub. Open the MP4 links above
for the full-resolution clips with playback controls.

![SSD-MobileNetV2 example](demo/previews/ssd_mobilenetv2.gif)

![Control Hub OpenCV example 1](demo/previews/control_hub_cv_example_1.gif)

![Control Hub OpenCV example 2](demo/previews/control_hub_cv_example_2.gif)

![CIELAB example](demo/previews/cielab_demo.gif)

![SSD-MobileNetV2 in action](demo/previews/ssd_in_action.gif)

![SSD-MobileNetV2 v1.1 in action](demo/previews/javi_v1_1_ssd.gif)

![Probiotix robot drive](demo/previews/probiotix_api_robot_drive.gif)

![Sid 47 Beavers robot drive](demo/previews/sid_47_beavers_robot_drive.gif)

### Image samples

These samples use a screenshot from the same [kickoff video](https://www.youtube.com/watch?v=gO98TkgY0kI)
as input. OpenCV samples show the normal best-candidate output and the full
set of gated candidates.

| Model | Sample 1 | Sample 2 |
|-------|----------|----------|
| Control Hub/OpenCV | [best candidate](demo/samples/control_hub_cv_sample_1_best.jpg) | [all gated candidates](demo/samples/control_hub_cv_sample_2_all.jpg) |

## Community

Join the [Hive Vision Discord server](https://discord.gg/m2yPTprccv) to share
robot tests, training data, detector results, and setup questions. You can also
[open an issue on GitHub](https://github.com/sidhuharjas/Hive-Vision/issues)
for bugs, suggestions, or dataset contributions.

## Updates and training data

Hive Vision will be updated throughout the season, with a planned release or
model update every other week as new real footage, field conditions, camera
angles, and difficult examples become available. Updates may include refreshed
SSD-MobileNetV2 weights, improved OpenCV HSV settings, new evaluation
results, and new demo samples.

To contribute training data, join the Discord server and share a link in the
training-data discussion. Include the source, camera resolution, frame rate,
lighting conditions, and permission to use the footage. Useful submissions
include:

- Real robot footage with no balls in view, so the model learns backgrounds,
  robot parts, field markings, and empty-field negatives.
- A 360-degree view of the field from the camera position, when possible.
- Many ball views at different distances, angles, lighting conditions, and
  levels of motion blur.
- Tiny, partially hidden, overlapping, or edge-of-frame balls.
- Robot panels, tape, shadows, reflections, and other false-positive examples.

## Training-data workflow

1. Share the original video or image link in Discord or a GitHub issue.
2. Include the camera, resolution, frame rate, lighting, field location, and
  permission to use the footage.
3. Separate useful empty-field footage from clips containing visible balls.
4. Label visible balls as `yellow`, `red`, or `blue`; do not label objects that
  only resemble balls.
5. Keep a note describing difficult cases and what the current model missed.
6. After review, accepted data can be included in a future every-other-week
  dataset or model update.

Contributors whose training data or testing work is accepted may request a
certificate recognizing their contribution to Hive Vision. The certificate
can describe work such as real-robot data collection, labeling, hard-negative
mining, evaluation, or detector testing, and may be included in a portfolio or
resume. Requests can be made through a GitHub issue or the Discord server.
Recognition will describe the specific contribution and will not imply
authorship of the entire project.

## License

Original Hive Vision code, documentation, and configuration are MIT — see
[LICENSE](LICENSE). YOLO model artifacts and Ultralytics-dependent material
are AGPL-3.0 — see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
