package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.opencv.core.Size;

@TeleOp(name = "teleopWebcam")
public class teleopWebcam extends LinearOpMode {

    private DcMotor leftFront = null;
    private DcMotor leftRear = null;
    private DcMotor rightFront = null;
    private DcMotor rightRear = null;

    @Override
    public void runOpMode() {
        leftFront = hardwareMap.get(DcMotor.class, "left_front");
        leftRear = hardwareMap.get(DcMotor.class, "left_rear");
        rightFront = hardwareMap.get(DcMotor.class, "right_front");
        rightRear = hardwareMap.get(DcMotor.class, "right_rear");
        
        leftFront.setDirection(DcMotor.Direction.FORWARD);
        leftRear.setDirection(DcMotor.Direction.FORWARD);
        rightFront.setDirection(DcMotor.Direction.REVERSE);
        rightRear.setDirection(DcMotor.Direction.REVERSE);

        BallDetectorPipeline baller = new BallDetectorPipeline();

        VisionPortal portal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .setCameraResolution(new Size(1280, 720))
                .addProcessor(baller)
                .enableLiveView(true)
                .build();

        BallDetectorPipeline.BallColor targetColor = BallDetectorPipeline.BallColor.RED;

        telemetry.addData("Status", "Initialized. Waiting for Start.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            if (gamepad1.x) {
                targetColor = BallDetectorPipeline.BallColor.BLUE;
            } else if (gamepad1.b) {
                targetColor = BallDetectorPipeline.BallColor.RED;
            } else if (gamepad1.y) {
                targetColor = BallDetectorPipeline.BallColor.YELLOW;
            }

            BallDetectorPipeline.BallBlob target = baller.bestOf(targetColor);

            if (target != null) {
                double error = target.cxNorm - 0.5;
                double steering = error * 1.5; 
                double baseSpeed = 0.3; 
                double leftPower = baseSpeed + steering;
                double rightPower = baseSpeed - steering;

                leftFront.setPower(leftPower);
                leftRear.setPower(leftPower);
                rightFront.setPower(rightPower);
                rightRear.setPower(rightPower);

                telemetry.addData("Target Color", targetColor);
                telemetry.addData("Status", "Tracking Target");
                telemetry.addData("Ball cxNorm", "%.2f", target.cxNorm);
                telemetry.addData("Ball cyNorm", "%.2f", target.cyNorm);
            } else {
                leftFront.setPower(0);
                leftRear.setPower(0);
                rightFront.setPower(0);
                rightRear.setPower(0);

                telemetry.addData("Target Color", targetColor);
                telemetry.addData("Status", "Searching...");
            }

            telemetry.update();
        }

        portal.close();
    }
}
