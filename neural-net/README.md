# Hive Vision — Limelight 3A (TFLite)

The model track of Hive Vision: an SSD-MobileNetV2 detector trained on a corpus
of synthetic renders of the team's CAD ball plus real match footage. This is
the model the Limelight 3A actually runs:

| File | What it is | Use on |
| ---- | ---------- | ------ |
| `weights/best_limelight3a_ssd_mobilenetv2_300x300.tflite` | **SSD-MobileNetV2** — the model the 3A runs. uint8 300×300 input, float32 `TFLite_Detection_PostProcess` outputs, 5.0 MB | Limelight 3A (neural detector) |
| `weights/labels.txt` | `yellow_pollen`, `red_nectar`, `blue_nectar` | Limelight 3A labels |

The model uses the SSD `TFLite_Detection_PostProcess` contract the 3A requires
(full-INT8 uint8 input, float32 outputs: num detections, scores `[1,10]`,
class ids `[1,10]` 0=yellow 1=red 2=blue, boxes `[1,10,4]` normalized
ymin,xmin,ymax,xmax). Upload it with `labels.txt` to the 3A's Neural Detector;
read detections through the Limelight API used by your FTC integration.

An earlier YOLOv8n reference model and its ONNX/float32 exports are archived
under [`../archive/yolo-pc/`](../archive/yolo-pc/README.md); they are not
maintained and never ran on a Limelight.

## Viewer (PC / dev)

```bash
# test the Limelight 3A SSD model over a video (live window)
python scripts/detect_video_ssd_realtime.py --source path/to/video.mp4
```

This runs the exact artifact the 3A runs, so what you see on screen is what
the camera reports. It is a model inspection tool, not a substitute for
testing on Limelight 3A hardware.

## Accuracy context

This model's detections are the **reference detections** every Hive Vision
Control Hub metric is measured against — see `cv/docs/cv_detector_report.md`.
