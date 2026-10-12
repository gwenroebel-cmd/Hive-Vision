# Archive — PC / YOLO / ONNX track (retired)

> **ARCHIVED (2026-10-11).** The PC / ONNX Runtime (YOLOv8n) track is retired.
> It is not maintained, not shipped, and not what the Limelight 3A runs. It was
> kept here only because the HSV (`cv/`) track was originally tuned against its
> reference detections — that tuning is baked into `cv/tools/hsv_tuned.json`
> and no longer needs the model. Everything in this folder is for reference
> only.

The live, supported detector is the **Limelight 3A SSD-MobileNetV2** at
[`../../neural-net/weights/best_limelight3a_ssd_mobilenetv2_300x300.tflite`](../../neural-net/weights/best_limelight3a_ssd_mobilenetv2_300x300.tflite).

## What lives here

| Path | What it was |
|------|-------------|
| `weights/best.pt` | YOLOv8n Ultralytics source checkpoint. Still the *ground-truth generator* for the HSV eval (`cv/tools/eval_cv_vs_yolo.py`) and its CI — those loaders point here. Not a robot deployment. |
| `weights/best.onnx` (+ `.onnx.data`) | YOLOv8n ONNX export, 960×960, opset 12. **No Limelight runs ONNX** — neural detectors accept `.tflite`/`.hef` only. |
| `weights/best_limelight3a_float32.tflite` | float32 YOLO TFLite test artifact. float32 in/out, so the 3A neural detector will **not** load it. PC testing only. |
| `scripts/detect_video_realtime.py` | PC real-time YOLO viewer (OpenCV window, `best.pt`). |
| `scripts/detect_video_tflite.py` / `detect_video_tflite_realtime.py` | PC viewers for the float32/int8 YOLO TFLite exports. |
| `scripts/demo_int8.py` | PC demo for the dynamic-range int8 YOLO export. |
| `scripts/export_annotated.py` | Batch annotated-MP4 exporter (YOLO). |
| `scripts/export_int8_tflite.py` | YOLO → dynamic-range int8 TFLite quantizer. |
| `demo/` | YOLO example clips, preview GIFs, and confidence-threshold sample stills. |
| `docs/` | The retired "PC / ONNX Runtime" doc pages (was `docs/onnx_pc*`, `docs/detector_tracks/onnx_pc*`). |

## Known caveat (why full-INT8 was impossible here)

The int8 YOLO export is **dynamic-range** (int8 weights, float activations).
Full-int8 activations were not possible for this architecture: the Detect
head's per-stride branches keep separate quantization scales, which TFLite
forbids on the `CONCAT`, and a single mixed box+score tensor collapses the
score rows to zero under the box-dominated scale. This whole track is archived,
so the caveat is moot — the SSD retrain does not share it.
