# Control Hub (OpenCV HSV)

One model-free hub track: `cv/TeamCode/BallDetectorPipeline.java` (HSV),
extending the shared base `cv/TeamCode/BallBlobPipeline.java`. Setup, tuning
commands, and viewer tools are in
[cv/README.md](https://github.com/sidhuharjas/Hive-Vision/blob/main/cv/README.md),
or follow the step-by-step: [Set up the HSV track](hub_hsv_setup.md).
This page covers the decision and what to expect from the outputs.

> A second hub track — Lab chromaticity — is **archived (2026-10-11)**: source
> in [`archive/lab/`](https://github.com/sidhuharjas/Hive-Vision/blob/main/archive/lab/README.md),
> setup notes in [Set up the Lab track](hub_lab_setup.md). No teams were using
> it; the Limelight 3A SSD covers the shadow case it used to own.

## Pick

- **HSV** when you want the cheapest path on a stable-lighting field. For
  shadows, use the Limelight 3A SSD track.
- A ball deep in shadow is too desaturated for *any* color-only detector — that
  case belongs to the Limelight 3A track.

## What to know

- Expect **7–11% precision** on a real field. The hub outputs are candidates —
  confirm each across several frames before the robot acts.
- The HSV config was fit/tuned from (now-archived) YOLO reference detections on
  published footage; re-check recall on *your* field's white balance before
  relying on it.