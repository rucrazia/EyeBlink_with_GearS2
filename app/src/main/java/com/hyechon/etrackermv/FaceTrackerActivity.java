/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyechon.etrackermv;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.hyechon.etrackermv.camera.CameraSourcePreview;
import com.hyechon.etrackermv.camera.GraphicOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */

public final class FaceTrackerActivity extends Activity {
    private static final String TAG = "FaceTracker";

    public static int count_eye_blink = 0;
    public static int count_eye_blink2 = 0;
    private static int count_eye_blink_watch = 0;
    public static int count_put_get_FTAtoSA = 0;
    private static int count_smartphone_blink = 0;
    private int count_set_SA = 0;
    public static short Noti_flag_main = 1;
    private short counting_array = 0;
    private String total_blinking = "";
    private int blinking_count_array[] = new int[20];
    public static boolean device_flag = false;
    public static boolean start_flag = false;;
    public static boolean count_flag = false;
    private static boolean count_flag_watch = false;

    BackThread mThread;

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private WebView mWebView;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    /* Tizen */
    private static TextView mTextView;
    private static MessageAdapter mMessageAdapter;
    private boolean mIsBound = false;
    private ListView mMessageListView;
    private ConsumerService mConsumerService = null;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        final Button btn_start = (Button) findViewById(R.id.start_button);
        final Button btn_google = (Button) findViewById(R.id.google_button);
        final Button btn_setting = (Button) findViewById(R.id.reset_button);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        setLayout();

        // 웹뷰에서 자바스크립트실행가능
        mWebView.getSettings().setJavaScriptEnabled(true);
        // 구글홈페이지 지정
        mWebView.loadUrl("http://www.google.com");
        // WebViewClient 지정
        mWebView.setWebViewClient(new WebViewClientClass());


        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String start;

                if(start_flag==false) {
                    btn_start.setText("검사 종료");
                    start_flag = true;
                    if(device_flag == false){
                    count_flag = true;}
                    else {
                        count_flag_watch = true;
                        mConsumerService.sendData("1min,10");   //start flag
                    }
                    count_smartphone_blink = 0;
                    count_eye_blink2 = 0;
                    count_eye_blink_watch = 0;
                    start = "start blink detection";
                }
                else{
                    btn_start.setText("검사 시작");
                    if(count_flag_watch == true){
                        mConsumerService.sendData("end");
                    }
                    start_flag = false;
                    count_flag = false;
                    count_flag_watch = false;
                    count_eye_blink2 = 0;
                    count_eye_blink = 0;
                    start = "stop blink detection";
                }

                Toast.makeText(FaceTrackerActivity.this, start, Toast.LENGTH_SHORT).show();

                if (mIsBound == true && mConsumerService != null) {
                    mConsumerService.findPeers();
                }

            }
        });

        btn_google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Set the google url
                mWebView.loadUrl("http://www.google.com");

            }
        });

        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count_put_get_FTAtoSA = count_set_SA;
                count_eye_blink2 = 0;
                count_eye_blink_watch = 0;

                if (mIsBound == true && mConsumerService != null) {
                    if (mConsumerService.closeConnection() == false) {
                        /** tizen disconnected massage  **/
                        /* updateTextView("Disconnected");
                        Toast.makeText(getApplicationContext(), R.string.ConnectionAlreadyDisconnected, Toast.LENGTH_LONG).show();
                        mMessageAdapter.clear(); */
                    }
                }
                Intent intent = new Intent(FaceTrackerActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        mThread = new BackThread(mHandler);
        mThread.setDaemon(true);
        mThread.start();

        /** Tizen **/
        mTextView = (TextView) findViewById(R.id.tvStatus);
        mMessageAdapter = new MessageAdapter();

        /** Bind service **/
        mIsBound = bindService(new Intent(FaceTrackerActivity.this, ConsumerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    public void toast_smartphone (){

        if (mIsBound == true && mConsumerService != null) {
            mConsumerService.findPeers();

            /** Tizen connected Message **/
            //Toast.makeText(getApplicationContext(),"Connected", Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Webview Layout
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class WebViewClientClass extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }


    private void setLayout(){
        mWebView = (WebView) findViewById(R.id.webview);
    }

    Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            if(msg.what==0){
                count_set_SA = count_eye_blink;

                if(Noti_flag_main == 2) {
                    if (count_eye_blink2 == 1) {
                 }
                }
                if(Noti_flag_main == 1) {
                    if (count_eye_blink2 == 1) {
                    }

                    if (count_eye_blink2 > 100) {
                    }
                }

                if(Noti_flag_main == 0) {
                    if (count_eye_blink2 == 1) {
                    }

                    if (count_eye_blink2 > 100) {
                    }
                }
            }
        };
    };


    class WebClient extends WebViewClient {
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };
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

    public void showToast(final String toast) { runOnUiThread(new Runnable() { public void run() { Toast.makeText(FaceTrackerActivity.this, toast, Toast.LENGTH_LONG).show(); } }); }


    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        /*Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    Thread.sleep(1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
                while (true) {
                    final TextView count_blink = (TextView) findViewById(R.id.count_blink);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            count_blink.setText(count_blink.toString());
                        }
                    });

                }
            }
        });
        thread.start();*/
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }

        /* Tizen */
        // Clean up connections
        if (mIsBound == true && mConsumerService != null) {
            if (mConsumerService.closeConnection() == false) {
                //updateTextView("Disconnected");
                //mMessageAdapter.clear();
            }
        }
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
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
            createCameraSource();
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
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /*
*
* */

    //==============================================================================================
    // Consumer function for smartphone
    //==============================================================================================

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mConsumerService = ((ConsumerService.LocalBinder) service).getService();
            //updateTextView("onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConsumerService = null;
            mIsBound = false;
            //updateTextView("onServiceDisconnected");
        }
    };

    private class MessageAdapter extends BaseAdapter {
        private static final int MAX_MESSAGES_TO_DISPLAY = 20;
        private List<Message> mMessages;

        public MessageAdapter() {
            mMessages = Collections.synchronizedList(new ArrayList<Message>());
        }

        @Override
        public int getCount() {
            return mMessages.size();
        }

        @Override
        public Object getItem(int position) {
            return mMessages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View messageRecordView = null;
            if (inflator != null) {
                messageRecordView = inflator.inflate(R.layout.message, null);
                TextView tvData = (TextView) messageRecordView.findViewById(R.id.tvData);
                Message message = (Message) getItem(position);
                tvData.setText(message.data);
            }
            return messageRecordView;
        }


        private final class Message {
            String data;

            public Message(String data) {
                super();
                this.data = data;
            }
        }
    }


/*
*
* */

    //==============================================================================================
    // Eye Blink Tracker
    //==============================================================================================

    public class BlinkTracker extends Tracker<Face> {
        private final double OPEN_THRESHOLD = 0.60;
        private final double CLOSE_THRESHOLD = 0.60;

        FaceTrackerActivity FA = new FaceTrackerActivity();

        private int state = 0;

        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            //Log.i("BlinkTracker", "start");

            float left = face.getIsLeftEyeOpenProbability();
            float right = face.getIsRightEyeOpenProbability();
            if ((left == Face.UNCOMPUTED_PROBABILITY) ||
                    (right == Face.UNCOMPUTED_PROBABILITY)) {
                // At least one of the eyes was not detected.
                return;
            }
            if (start_flag == true) {
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
                            count_eye_blink++;
                            count_eye_blink2 = 0;
                }
                        break;

                }
            }
            else{

            }
        }

    }

    class BackThread extends Thread {

        int mBackValue = 0;
        Handler mHandler;
        public BackThread(Handler handler) {
            mHandler=handler;
        }

        @Override
        public void run() {
            while(true) {
                mBackValue++;
                Message msg = Message.obtain();
                msg.what = 0;
                msg.arg1 = count_eye_blink;
                mHandler.sendMessage(msg);
                try {
                    Thread.sleep(100);

                    /** Phone **/
                    if(device_flag == false){
                    count_smartphone_blink ++;
                        if(count_smartphone_blink == 600){
                            showToast(getString(count_eye_blink));

                                    count_smartphone_blink = 0;
                        }
                        if(count_flag==true){
                    count_eye_blink2++;
                        Log.d(TAG,"Blink_phone" + String.valueOf(count_eye_blink2));
                    }}

                    /** Watch **/
                    if(device_flag == true){
                    if(count_flag_watch==true){

                        count_eye_blink_watch++;
                        Log.d(TAG,"Blink_watch" + String.valueOf(count_eye_blink_watch));

                        if(count_eye_blink_watch == 600){

                                blinking_count_array[counting_array] = count_eye_blink;
                                Log.d(TAG, "Blink_watch_array = " + String.valueOf(count_eye_blink_watch));
                                Log.d(TAG, "counting_array = " + String.valueOf(counting_array));
                                String blink_count_array = "";

                                blink_count_array = "1min" +","+ String.valueOf(count_eye_blink);
                            if (mIsBound == true && mConsumerService != null) {
                                mConsumerService.findPeers();
                            }
                                mConsumerService.sendData(String.valueOf(blink_count_array));
                                count_eye_blink = 0;
                                count_eye_blink_watch = 0;
                                counting_array++;

                        }
                    }
                    else{
                        count_eye_blink_watch = 0;
                        count_eye_blink = 0;
                    }
                        if(count_eye_blink2 != 0) {
                            Log.d(TAG, "Blink_watch = " + String.valueOf(count_eye_blink2));
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


