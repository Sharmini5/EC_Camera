package com.example.ec_camera;

import static com.example.ec_camera.Helper.isInternetAvailable;
import static com.example.ec_camera.Util.getDisplayOrientation;
import static com.example.ec_camera.Util.getDisplayRotation;
import static com.example.ec_camera.Util.getOptimalPreviewSize;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.google.android.gms.common.api.Api;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class EmployeeScanFragment extends Fragment implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public static final String TAG = EmployeeScanFragment.class.getSimpleName();
    private static final int MAX_FACE = 1;
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();
    private long start, end;
    private int counter = 0;
    private double fps;
    private int numberOfCameras;
    private Camera mCamera;
    private int cameraId = 0;
    private int mDisplayRotation;
    private int mDisplayOrientation;
    private int previewWidth;
    private int previewHeight;
    private SurfaceView mView;
    private FaceOverlayView mFaceView;
    private TextView capturedImageButton;
    private boolean isThreadWorking = false;
    private Handler handler;
    private FaceDetectThread detectThread = null;
    private int prevSettingWidth;
    private int prevSettingHeight;
    private android.media.FaceDetector fdet;
    private byte[] grayBuff;
    private int bufflen;
    private int[] rgbs;
    private FaceResult faces[];
    private FaceResult faces_previous[];
    private int Id = 0;
    private String BUNDLE_CAMERA_ID = "camera";
    private HashMap<Integer, Integer> facesCount = new HashMap<>();
    private boolean btnClicked = false;
    // private ProgressDialog progressDialog;
    private Gson gson = new Gson();
    private AmazonRekognition amazonRekognitionClient;
    private RelativeLayout relativeLayout;
    CognitoCredentialsProvider credentialsProvider;
//    private IOSDialog dialog0;

    FaceLogging facelogging;
    AWSImageRekognition awsImageRekognition;

    private boolean isCancelLoginRequest= false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_employee_scan, container, false);

        getClientId();

        credentialsProvider = new CognitoCredentialsProvider(Constants.POOLID, Regions.valueOf(Constants.REGION));

        amazonRekognitionClient = new AmazonRekognitionClient(credentialsProvider);
        amazonRekognitionClient.setEndpoint("uploadfaces.us-east-1.amazonaws.com");
        mView = (SurfaceView) rootView.findViewById(R.id.captured_photo);

        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Now create the OverlayView:
        mFaceView = new FaceOverlayView(getActivity().getBaseContext());

        relativeLayout = (RelativeLayout) rootView.findViewById(R.id.camera_fragment);
        relativeLayout.addView(mFaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        capturedImageButton = (TextView) rootView.findViewById(R.id.photo_button);
        capturedImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btnClicked = true;

                System.out.println("Camera Login :Capture");
                isCancelLoginRequest = false;


            }
        });

        handler = new Handler();
        faces = new FaceResult[MAX_FACE];
        faces_previous = new FaceResult[MAX_FACE];
        for (int i = 0; i < MAX_FACE; i++) {
            faces[i] = new FaceResult();
            faces_previous[i] = new FaceResult();
        }

        if (savedInstanceState != null) cameraId = savedInstanceState.getInt(BUNDLE_CAMERA_ID, 0);

        Button logintocentreButton = (Button) rootView.findViewById(R.id.centre_login_button);
        logintocentreButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {
                    Toast.makeText(getActivity().getBaseContext(), "Centre Logout!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        logintocentreButton.setVisibility(View.GONE);

        return rootView;

    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CAMERA_ID, cameraId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //Find the total number of cameras available
        try {
            numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraId = i;
                }
            }

            mCamera = Camera.open(cameraId);

            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFaceView.setFront(true);
            }

            try {
                mCamera.setPreviewDisplay(mView.getHolder());
            } catch (Exception e) {
                Log.e(TAG, "Could not preview the image.", e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // We have no surface, return immediately:
        try {
            try {
                if (surfaceHolder.getSurface() == null) {
                    return;
                }
                // Try to stop the current preview:
                try {
                    mCamera.stopPreview();
                } catch (Exception e) {
                    // Ignore...
                }

                configureCamera(width, height);
                setDisplayOrientation();
                setErrorCallback();

                // Create media.FaceDetector
                float aspect = (float) previewHeight / (float) previewWidth;
                fdet = new FaceDetector(prevSettingWidth, (int) (prevSettingWidth * aspect), MAX_FACE);

                bufflen = previewWidth * previewHeight;
                grayBuff = new byte[bufflen];
                rgbs = new int[bufflen];
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                Log.e(TAG, "Out of Memory for camera image process.", e);
            }

            // Everything is configured! Finally start the camera preview again:
            try {
                startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        try {
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.setErrorCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPreviewFrame(byte[] _data, Camera _camera) {
        try {
            if (!isThreadWorking) {
                if (counter == 0) start = System.currentTimeMillis();

                isThreadWorking = true;
                waitForFdetThreadComplete();
                detectThread = new FaceDetectThread(handler, getActivity().getBaseContext());
                detectThread.setData(_data);
                detectThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getClientId() {
        // get from local storage
        try {
            SharedPreferences preferences = getActivity().getSharedPreferences(Constants.LOCALSTORAGEKEY, Context.MODE_PRIVATE);
            String client = preferences.getString(Constants.LOCALSTORAGECLIENT, "");

            if (client != null && client != "") {
//                Client clientobject = gson.fromJson(client, Client.class); // json to class object
//                clientid = clientobject.getClientid();
                System.out.println(1);
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    private String currentDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");
        String currentTimeStamp = dateFormat.format(new Date());
        return currentTimeStamp;
    }

    private void setErrorCallback() {
        try {
            mCamera.setErrorCallback(mErrorCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDisplayOrientation() {
        // Now set the display orientation:
        try {
            mDisplayRotation = getDisplayRotation(getActivity());
            mDisplayOrientation = getDisplayOrientation(mDisplayRotation, cameraId);

            mCamera.setDisplayOrientation(mDisplayOrientation);

            if (mFaceView != null) {
                mFaceView.setDisplayOrientation(mDisplayOrientation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void configureCamera(int width, int height) {
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            // Set the PreviewSize and AutoFocus:
            setOptimalPreviewSize(parameters, width, height);
            setAutoFocus(parameters);
            // And set the parameters:
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
        try {
            List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
            float targetRatio = (float) width / height;
            Camera.Size previewSize = getOptimalPreviewSize(getActivity(), previewSizes, targetRatio);
            previewWidth = previewSize.width;
            previewHeight = previewSize.height;

            Log.e(TAG, "previewWidth" + previewWidth);
            Log.e(TAG, "previewHeight" + previewHeight);

            /**
             * Calculate size to scale full frame bitmap to smaller bitmap
             * Detect face in scaled bitmap have high performance than full bitmap.
             * The smaller image size -> detect faster, but distance to detect face shorter,
             * so calculate the size follow your purpose
             */
            if (previewWidth / 4 > 360) {
                prevSettingWidth = 360;
                prevSettingHeight = 270;
            } else if (previewWidth / 4 > 320) {
                prevSettingWidth = 320;
                prevSettingHeight = 240;
            } else if (previewWidth / 4 > 240) {
                prevSettingWidth = 240;
                prevSettingHeight = 160;
            } else {
                prevSettingWidth = 160;
                prevSettingHeight = 120;
            }

            cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

            mFaceView.setPreviewWidth(previewWidth);
            mFaceView.setPreviewHeight(previewHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setAutoFocus(Camera.Parameters cameraParameters) {
        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    private void startPreview() {
        try {
            if (mCamera != null) {
                isThreadWorking = false;
                mCamera.startPreview();
                mCamera.setPreviewCallback(this);
                counter = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitForFdetThreadComplete() {
        if (detectThread == null) {
            return;
        }

        if (detectThread.isAlive()) {
            try {
                detectThread.join();
                detectThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private String compareFaces(ByteBuffer sourceImageBytes) {
        String result = "";
        float similarityThreshold = Constants.SIMILARITYTHRESHOLD;
        int maxFaces = Constants.MAXFACES;
        System.out.println("Camera Login :send to Rekognition");
        try {
            Image source = new Image().withBytes(sourceImageBytes);

            //Search collection for faces similar to the largest face in the image.

            // AWSHelper.listCollections(amazonRekognitionClient);

//            System.out.println("clientid:" + clientid);

            SearchFacesByImageResult searchFacesByImageResult = AWSHelper.callSearchFacesByImage("1", source, similarityThreshold, maxFaces, amazonRekognitionClient);

            List<FaceMatch> faceImageMatches = searchFacesByImageResult.getFaceMatches();

            for (FaceMatch face : faceImageMatches) {
                System.out.println(gson.toJson(face));

                String externalImageId = face.getFace().getExternalImageId();
                System.out.println(externalImageId);

                result = externalImageId;
                //String array[] = externalImageId.split("_");
                // System.out.println(array[0]);

                //  result = Integer.parseInt(array[0]);
            }
            //System.out.println("Faces matching largest face in image :" + result);
            System.out.println("Camera Login :face matched");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    private void sendFaceLoggingNetworkRequest(String faceID) {

        try {
            System.out.println("Camera Login :Send Login request 1");
            Map<String, String> headers = new HashMap<String, String>();
            //headers.put("X-Authorization", MainActivity.staff_token);
            headers.put("mobile-api", String.valueOf(true));
            headers.put("Content-Type", "application/json");

            Map<String, String> map = new HashMap<String, String>();
            map.put("face_id", String.valueOf(faceID));




        } catch (Exception e) {
            e.printStackTrace();
            try {
                String error = (Constants.ALERT_FAILED_CAMERA_LOGIN);
                showNetworkErrorMessage(error);
                //progressDialog.dismiss();
                try {
//                    dialog0.dismiss();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }


    }

    private void sendNetworkRequest(String staffid) {
        try {
            System.out.println("Camera Login :Send Login request");
            Map<String, String> map = new HashMap<String, String>();
            map.put("external_image_id", staffid);
            map.put("clientid", "1");



        } catch (Exception e) {
            e.printStackTrace();
            try {
//                dialog0.dismiss();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private class FaceDetectThread extends Thread {
        private Handler handler;
        private byte[] data = null;
        private Context ctx;
        private Bitmap faceCroped;

        public FaceDetectThread(Handler handler, Context ctx) {
            this.ctx = ctx;
            this.handler = handler;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public void run() {
            // Log.i("FaceDetectThread", "running");

            try {
                float aspect = (float) previewHeight / (float) previewWidth;
                int w = prevSettingWidth;
                int h = (int) (prevSettingWidth * aspect);

                ByteBuffer bbuffer = ByteBuffer.wrap(data);
                bbuffer.get(grayBuff, 0, bufflen);

                gray8toRGB32(grayBuff, previewWidth, previewHeight, rgbs);
                Bitmap bitmap = Bitmap.createBitmap(rgbs, previewWidth, previewHeight, Bitmap.Config.RGB_565);

                Bitmap bmp = Bitmap.createScaledBitmap(bitmap, w, h, false);

                float xScale = (float) previewWidth / (float) prevSettingWidth;
                float yScale = (float) previewHeight / (float) h;

                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(cameraId, info);
                int rotate = mDisplayOrientation;
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mDisplayRotation % 180 == 0) {
                    if (rotate + 180 > 360) {
                        rotate = rotate - 180;
                    } else rotate = rotate + 180;
                }

                switch (rotate) {
                    case 90:
                        bmp = ImageUtils.rotate(bmp, 90);
                        xScale = (float) previewHeight / bmp.getWidth();
                        yScale = (float) previewWidth / bmp.getHeight();
                        break;
                    case 180:
                        bmp = ImageUtils.rotate(bmp, 180);
                        break;
                    case 270:
                        bmp = ImageUtils.rotate(bmp, 270);
                        xScale = (float) previewHeight / (float) h;
                        yScale = (float) previewWidth / (float) prevSettingWidth;
                        break;
                }

                fdet = new android.media.FaceDetector(bmp.getWidth(), bmp.getHeight(), MAX_FACE);

                android.media.FaceDetector.Face[] fullResults = new android.media.FaceDetector.Face[MAX_FACE];
                fdet.findFaces(bmp, fullResults);

                for (int i = 0; i < MAX_FACE; i++) {
                    if (fullResults[i] == null) {
                        faces[i].clear();
                    } else {
                        PointF mid = new PointF();
                        fullResults[i].getMidPoint(mid);

                        mid.x *= xScale;
                        mid.y *= yScale;

                        float eyesDis = fullResults[i].eyesDistance() * xScale;
                        float confidence = fullResults[i].confidence();
                        float pose = fullResults[i].pose(android.media.FaceDetector.Face.EULER_Y);
                        int idFace = Id;

                        Rect rect = new Rect((int) (mid.x - eyesDis * 1.20f), (int) (mid.y - eyesDis * 0.55f), (int) (mid.x + eyesDis * 1.20f), (int) (mid.y + eyesDis * 1.85f));

                        /**
                         * Only detect face size > 100x100
                         */
                        if (rect.height() * rect.width() > 100 * 100) {
                            // Check this face and previous face have same ID?
                            for (int j = 0; j < MAX_FACE; j++) {
                                float eyesDisPre = faces_previous[j].eyesDistance();
                                PointF midPre = new PointF();
                                faces_previous[j].getMidPoint(midPre);

                                RectF rectCheck = new RectF((midPre.x - eyesDisPre * 1.5f), (midPre.y - eyesDisPre * 1.15f), (midPre.x + eyesDisPre * 1.5f), (midPre.y + eyesDisPre * 1.85f));

                                if (rectCheck.contains(mid.x, mid.y) && (System.currentTimeMillis() - faces_previous[j].getTime()) < 1000) {
                                    idFace = faces_previous[j].getId();
                                    break;
                                }
                            }

                            if (idFace == Id) Id++;

                            faces[i].setFace(idFace, mid, eyesDis, confidence, pose, System.currentTimeMillis());

                            faces_previous[i].set(faces[i].getId(), faces[i].getMidEye(), faces[i].eyesDistance(), faces[i].getConfidence(), faces[i].getPose(), faces[i].getTime());

                            //
                            // if focus in a face 5 frame -> take picture face display in RecyclerView
                            // because of some first frame have low quality
                            //
                            if (btnClicked) {


                                if (faces != null && faces.length > 0) {

                                    System.out.println("Camera Login : Faces :" +faces.length);
                                    final Bitmap face1 = bitmap;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                            ImageUtils.SaveImage(face1);
                                        }
                                    });
                                    faceCroped = ImageUtils.cropFace(faces[i], bitmap, rotate);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                            ImageUtils.SaveImage(faceCroped);
                                        }
                                    });
                                    if (faceCroped != null) {
                                        handler.post(new Runnable() {
                                            public void run() {
                                                try {
                                                    // convert bitmap to ByteBuffer
                                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                                    faceCroped.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                                    ByteBuffer byteBuffer = ByteBuffer.wrap(stream.toByteArray());

                                                    //storeImage(faceCroped);
                                                    boolean internet = isInternetAvailable(getActivity().getApplicationContext());
                                                    if (internet) {
                                                        // api call
                                                        awsImageRekognition = new AWSImageRekognition();
                                                        awsImageRekognition.execute(byteBuffer);
                                                    } else {

                                                        showNetworkErrorMessage(Constants.ALERT_FAILED_NETWORK_ISSUE);
                                                        //Helper.showSnackbar(relativeLayout, Constants.INTERNET_ERROR, Snackbar.LENGTH_LONG);
                                                        //   progressDialog.dismiss();
                                                        try {
//                                                            dialog0.dismiss();
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                    btnClicked = false;


                                } else {

                                    try {
                                        Toast.makeText(getActivity().getBaseContext(), "Can't recognise any face, Try Again ", Toast.LENGTH_LONG).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    btnClicked = false;
                                    try {
//                                        dialog0.dismiss();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }


                            }
                        }
                    }
                }

                handler.post(new Runnable() {
                    public void run() {
                        try {
                            //send face to FaceView to draw rect
                            mFaceView.setFaces(faces);

                            //Calculate FPS (Detect Frame per Second)
                            end = System.currentTimeMillis();
                            counter++;
                            double time = (double) (end - start) / 1000;
                            if (time != 0) fps = counter / time;

                            mFaceView.setFPS(fps);

                            if (counter == (Integer.MAX_VALUE - 1000)) counter = 0;

                            isThreadWorking = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void gray8toRGB32(byte[] gray8, int width, int height, int[] rgb_32s) {
            final int endPtr = width * height;
            int ptr = 0;
            while (true) {
                if (ptr == endPtr) break;

                final int Y = gray8[ptr] & 0xff;
                rgb_32s[ptr] = 0xff000000 + (Y << 16) + (Y << 8) + Y;
                ptr++;
            }
        }
    }

    private class AWSImageRekognition extends AsyncTask<ByteBuffer, String, String> {
        @Override
        protected String doInBackground(ByteBuffer... params) {
            String sImageID = compareFaces(params[0]);
            return sImageID;
        }

        @Override
        protected void onPostExecute(String faceImageID) {
            //process staffid
            System.out.println("AWSImageRekognition :" + faceImageID);

            if (faceImageID != null && !faceImageID.equals("")) {
                facelogging = new FaceLogging();
                facelogging.execute(faceImageID);


            } else {
                // Toast.makeText(getActivity().getBaseContext(), "Login failed!", Toast.LENGTH_LONG).show();
                try {
                    String error = Constants.ALERT_FAILED_CAMERA_LOGIN;
                    showLoggingFailedMessage(error);
                    //progressDialog.dismiss();
                    try {
//                        dialog0.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void staffLogin(String faceResponse) {
        int staffid = 0;
        try {
            JSONObject jObject = new JSONObject(faceResponse);
            String id = jObject.getString("id");
            staffid = Integer.parseInt(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (staffid > 0) {
            StaffDetails staffjob = new StaffDetails();
            staffjob.execute();
            sendNetworkRequest(""+staffid);

        } else {
            Toast.makeText(getActivity().getBaseContext(), "Login failed!", Toast.LENGTH_LONG).show();
            try {
                String error = Constants.ALERT_FAILED_CAMERA_LOGIN;
                showLoggingFailedMessage(error);
                //progressDialog.dismiss();
                try {
//                    dialog0.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class FaceLogging extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            sendFaceLoggingNetworkRequest(params[0]);
            //send faceid to middleware.
            sendNetworkRequest(params[0]);
            return "success";
        }

        @Override
        protected void onPostExecute(String message) {
            //process message
            try {
                Toast.makeText(getActivity().getBaseContext(), "Login Success!", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
//            progressDialog.dismiss();
        }
    }


    private class StaffDetails extends AsyncTask<Integer, Void, String> {
        @Override
        protected String doInBackground(Integer... params) {
            sendNetworkRequest(""+params[0]);
            return "success";
        }

        @Override
        protected void onPostExecute(String message) {
            //process message
            try {
                Toast.makeText(getActivity().getBaseContext(), "Login Success!", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //progressDialog.dismiss();
        }
    }

    private class AWSCollection extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            AWSHelper.listCollections(amazonRekognitionClient);

            return "success";
        }

        @Override
        protected void onPostExecute(String message) {
            //process message
        }
    }

    private void showLoggingFailedMessage(String message) {
//        // String  error = Constants.ALERT_FAILED;
//        try {
////            CustomAlertDialog.showDialog(getActivity(), Constants.ALERT_TITLE_CAMERA_WHOOPS, message, (ContextCompat.getDrawable(getActivity(), R.drawable.ic_face_logging_failed)), Constants.ALERT_BUTTON_TRY_AGAIN, new AlertOkListner() {
////                @Override
////                public void onResultOk(String result) {
////                    //nothing to do dialog dismiss
////                }
//            }, null, null);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private void showfailedMessage(String message) {
//         String  error = Constants.ALERT_FAILED
    }

    private void showNetworkErrorMessage(String message) {
        //showNetworkErrorMessage( Constants.ALERT_FAILED_NETWORK_ISSUE);
    }

    private void cancelCurrentLoginRequests(){

        try {
            if(facelogging!= null && facelogging.getStatus() == AsyncTask.Status.RUNNING){
                facelogging.cancel(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if(awsImageRekognition!= null && awsImageRekognition.getStatus() == AsyncTask.Status.RUNNING){
                awsImageRekognition.cancel(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}


