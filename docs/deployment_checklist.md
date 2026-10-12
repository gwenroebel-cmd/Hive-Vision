# Deployment checklist

Everything needed to deploy the full two-track stack, gathered in one place.
Work through the on-match-day checklist below, then the artifact table and
build/verify notes for whoever flashes the robot.

## On match day

What to check, in order, without re-reading the handbook:

- [ ] Model + labels on the 3A, engine = CPU, correct pipeline
- [ ] Confidence 0.35–0.45 (0.25 = false positives)
- [ ] Network-table name matches the FTC hardware map
- [ ] FTC code filters your alliance class and gates on confidence
- [ ] Hub track (if used) confirmed across frames, not single-frame
- [ ] Recall sanity-checked on your field's lighting, not just demo clips
- [ ] **Still unverified on hardware**: the SSD `.tflite` actually loading and
      reporting on a real 3A — PC runners verify the model, not the device

## What ships where

| Destination | Artifact | Path |
|-------------|----------|------|
| Limelight 3A | SSD-MobileNetV2 300×300 (uint8-in, float out) | `../neural-net/weights/best_limelight3a_ssd_mobilenetv2_300x300.tflite` |
| Limelight 3A | class labels | `../neural-net/weights/labels.txt` |
| Control Hub | HSV Java pipeline + shared base | `../cv/TeamCode/BallDetectorPipeline.java` (extends `BallBlobPipeline.java`) |
| Control Hub *(archived)* | Lab chromaticity Java pipeline (retired 2026-10-11) | `../archive/lab/TeamCode/LabBallDetectorPipeline.java` (extends `BallBlobPipeline.java`) |
| Control Hub *(archived)* | Lab learned config (retired 2026-10-11) | `../archive/lab/tools/lab_tuned.json` |
| — | source checkpoint (HSV ground truth only, re-training) | `../archive/yolo-pc/weights/best.pt` |

Class order is `yellow_pollen`, `red_nectar`, `blue_nectar` — keep
`labels.txt` matching on the 3A. The SSD-MobileNetV2 3A model emits
**`TFLite_Detection_PostProcess`** outputs (`num`, `scores`, `class_ids`,
`boxes`) already decoded — no NMS needed on your side.

## The tracks, compared

Measured against a 293-frame real-match dataset, using the archived YOLO
reference detections (conf ≥ 0.35; identical reference for both color
detectors; all tracks share the same geometry gates):

| Tracker | Runs on | Principle | Yellow rec/prec | Red rec/prec | Blue rec/prec | Dark-red recall |
|---------|---------|-----------|-----------------|--------------|---------------|-----------------|
| **HSV (cv/)** | Control Hub | tuned HSV window | 62.0 / 10.5 | 34.5 / 8.6 | 52.8 / 11.1 | 40.0% |
| **Lab (archived)** | Control Hub *(retired 2026-10-11)* | learned Lab hue band + adaptive chroma floor | 45.5 / 7.5 | 46.0 / 8.7 | 54.5 / 8.1 | **95.5%** |

What the numbers mean:

- **These are candidate-detection benchmarks against archived YOLO reference detections, not end-to-end results.** They measure whether each track *sees a ball candidate* on development footage — not successful autonomous pickups, false pickup attempts, cycle time, or match performance. A 62.0% recall does not mean the robot picks up 62.0% of yellow balls.
- **The archived YOLO model is the reference.** It was the only track that
  found shapes, so it is the only one that recovers low-chroma / deep-shadow
  balls. Treat its (archived) output as the reference and the Control Hub
  tracks as candidate signals (7–11% precision on this footage — the field is
  full of yellow/red/blue patches).
- **Lab beats HSV on red** in recall and precision, and crushes it in shadows
  (95.5% vs 40% dark-frame recall). That is its purpose: Lab hue is
  shadow-invariant and its chroma floor adapts downward per frame. (Lab is
  archived — the Limelight SSD is the shadow path now.)
- **HSV keeps yellow and blue precision.** Its yellow window was hand-tuned on
  luckier footage; Lab is rigidly fit on this one.
- Pick a track per robot, not per color: HSV when you want the absolute
  cheapest path, Limelight SSD when shadows and shape matter. Never rely on a
  color detector in the darkest corners — use the SSD.
- **Same config, two measurements.** The CV report
  (`cv/docs/cv_detector_report.md`) measured this same shipped HSV config on its
  own 866-frame dev video and got higher numbers (yellow 68.7% / red 80.1% /
  blue 82.3% recall at 39.9 / 26.0 / 65.5% precision) — different footage, not a
  different detector. The shared-truth head-to-head table above is the
  apples-to-apples cross-track comparison.

Full detail: [`../archive/lab/docs/lab_detector_report.md`](../archive/lab/docs/lab_detector_report.md)
and [`../cv/docs/cv_detector_report.md`](../cv/docs/cv_detector_report.md).

## Verified

- [x] Java pipelines compile against the FTC SDK (`bestOf(BallColor)`)
- [x] Config + report + README numbers in sync
- [ ] **Upload the SSD tflite → 3A once on hardware, confirm the pipeline loads
      and reports detections** (PC runner verifies the model, not the device)
- [ ] **Run at confidence 0.35–0.45 on the 3A** — 0.25 produces visible false
      positives (it is a threshold property, not a quantization defect)

## Commands used to build these artifacts

```bash
# re-run the Lab metrics (raw, un-annotated footage; Lab track is archived)
python ../archive/lab/tools/eval_lab_vs_yolo.py --source path/to/match_raw.mp4 \
  --config ../archive/lab/tools/lab_tuned.json --compare-hsv \
  --truth-json ../archive/lab/docs/yolo_truth_lab.json
```
