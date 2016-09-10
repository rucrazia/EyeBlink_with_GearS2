package com.hyechon.etrackermv;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.hyechon.etrackermv.camera.CameraSourcePreview;
import com.hyechon.etrackermv.camera.GraphicOverlay;

public class MainActivity extends Activity {

    private static final String TAG = "FaceTracker";

    public CameraSource mCameraSource;

    public CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    Intent intent1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //intent1 = new Intent(this, ServiceClass.class);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            //requestCameraPermission();
        }

        Button btn_play = (Button) findViewById(R.id.button);
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Intent intent = new Intent(getApplicationContext(),ServiceClass.class);

                /*
                Intent intent = new Intent("com.hyechon.etrackermv.ServiceClass");
                intent.setPackage("com.hyechon.etrackermv");*/
                Log.d("a", "onclick");
                //startService(new Intent("com.hyechon.etrackermv.ServiceClass"));
                //startService(intent);
            }
        });

    }


    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        if (!wasRestored) {
            player.cueVideo("wKJ9KzGQq0w");
        }
    }


    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_view);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true) //Detection for front camera
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new LargestFaceFocusingProcessor(detector, new BlinkTracker()));
        //new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
        //.build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)//using front camera
                .setRequestedFps(30.0f)
                .build();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource

            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }


    //==============================================================================================
    // Eye Blink Tracker
    //==============================================================================================

    public class BlinkTracker extends Tracker<Face> {
        private final double OPEN_THRESHOLD = 0.85;
        private final double CLOSE_THRESHOLD = 0.40;

        private int state = 0;

        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            float left = face.getIsLeftEyeOpenProbability();
            float right = face.getIsRightEyeOpenProbability();
            if ((left == Face.UNCOMPUTED_PROBABILITY) ||
                    (right == Face.UNCOMPUTED_PROBABILITY)) {
                // At least one of the eyes was not detected.
                return;
            }

            switch (state) {
                case 0:
                    if ((left > OPEN_THRESHOLD) && (right > OPEN_THRESHOLD)) {
                        // Both eyes are initially open
                        Log.i("BlinkTracker", "eye open");
                        state = 1;
                    }
                    break;

                case 1:
                    if ((left < CLOSE_THRESHOLD) && (right < CLOSE_THRESHOLD)) {
                        // Both eyes become closed
                        Log.i("BlinkTracker", "blink occurred!");
                        state = 0;
                    }
                    break;

                /*case 2:
                    if ((left > OPEN_THRESHOLD) && (right > OPEN_THRESHOLD)) {
                        // Both eyes are open again
                        Log.i("BlinkTracker", "blink occurred!");
                        state = 0;
                    }
                    break;*/
            }
        }

    }
}
