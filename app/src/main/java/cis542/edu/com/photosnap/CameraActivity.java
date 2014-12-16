package cis542.edu.com.photosnap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;


public class CameraActivity extends Activity {

    private static final String FACE_API_KEY = "TtjwpsetM3mshPPfof6EoclZgtFYp1E7yvNjsnMy7IzSF5UJwE";

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "CameraActivity";
    private static final int ZOOM_IN = 0;
    private static final int ZOOM_OUT = 1;

    private static final String REKOGNITION_API_KEY = "OEersPbHc9zeGpah";
    private static final String REKOGNITION_API_SECRET = "gsB0eHs2DHmr0rR2";

    /* PEBBLE DATA CONSTANTS */
    private static final UUID PEBBLE_APP_UUID = UUID.fromString("3fb49826-013e-45d2-baaf-f10a2735556c");
    private static final int DATA_KEY = 0;
    private static final int SELECT_BUTTON_KEY =  0;
    private static final int UP_BUTTON_KEY     =  1;
    private static final int DOWN_BUTTON_KEY   =  2;
    private static final int BUFFER_LENGTH     = 64;
    /* PEBBLE DATA CONSTANTS */

    /* PEBBLE VARIABLES */
    private PebbleKit.PebbleDataReceiver dataHandler;
    /* PEBBLE VARIABLES */

    private View v;
    private Context ctx;
    private CameraPreview cameraPreview;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private BroadcastReceiver mDataReceiver;
    private BroadcastReceiver mConnectedReceiver;
    private BroadcastReceiver mDisconnectedReceiver;
    private BroadcastReceiver mAckReceiver;
    private BroadcastReceiver mNackReceiver;
    private File outFile;
    private String mName = "asdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_camera);
        ctx = this;

        boolean pebbleConnected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i(TAG, "Pebble is " + (pebbleConnected ? "connected" : "disconnected"));

        // BAD IDEAS WHEE
        StrictMode.ThreadPolicy policy = new StrictMode.
                ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Start Pebble app
        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mSurfaceView = (SurfaceView) findViewById(R.id.sv);
        cameraPreview = new CameraPreview(this, mSurfaceView);
        cameraPreview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.layout)).addView(cameraPreview);
        cameraPreview.setKeepScreenOn(true);

        cameraPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePhotoTakeRequest();
            }
        });


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0) {
            mCamera = null;
            try {
                mCamera = Camera.open(0);
                mCamera.startPreview();
                cameraPreview.setCamera(mCamera);
            } catch (RuntimeException ex) {
                Toast.makeText(this, "No camera found...", Toast.LENGTH_LONG).show();
            }
        }
        this.dataHandler = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int id, final PebbleDictionary pebbleTuples) {
                Log.i(TAG, "Received data from pebble");
                // ACK to prevent timeout
                PebbleKit.sendAckToPebble(context, id);

                // Get button press
                int buttonPressed = pebbleTuples.getUnsignedIntegerAsLong(DATA_KEY).intValue();

                // Take action
                switch (buttonPressed) {
                    case SELECT_BUTTON_KEY:
                        handlePhotoTakeRequest();
                        break;
                    case UP_BUTTON_KEY:
                        zoomCamera(ZOOM_IN);
                        break;
                    case DOWN_BUTTON_KEY:
                        zoomCamera(ZOOM_OUT);
                        break;
                    default:
                        break;
                }
            }
        };
        mDataReceiver = PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataHandler);
        registerPebbleHandlers();
        super.onResume();
    }

    @Override
    public void onPause() {

        if(mCamera != null) {
            mCamera.stopPreview();
            cameraPreview.getHolder().removeCallback(cameraPreview);
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }

        /* PEBBLE SECTION */
        // Unregister Activity-scoped BroadcastReceivers when Activity is paused
        if(dataHandler != null) {
            dataHandler = null;
        }
        try {
            unregisterReceiver(mDataReceiver); // This crashes things and I don't know why
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Data receiver not registered");
            mDataReceiver = null;
        }
        try {
            unregisterReceiver(mConnectedReceiver);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Connected receiver not registered");
            mConnectedReceiver = null;
        }
        try {
            unregisterReceiver(mDisconnectedReceiver);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Disconnected receiver not registered");
            mDisconnectedReceiver = null;
        }
        try {
            unregisterReceiver(mAckReceiver);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "ACK receiver not registered");
            mAckReceiver = null;
        }
        try {
            unregisterReceiver(mNackReceiver);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "NACK receiver not registered");
            mNackReceiver = null;
        }
        /* END PEBBLE SECTION */

        super.onPause();
    }

    private void registerPebbleHandlers() {
        mConnectedReceiver =
        PebbleKit.registerPebbleConnectedReceiver(getApplicationContext(), new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(getLocalClassName(), "Pebble connected!");
            }
        });

        mDisconnectedReceiver =
        PebbleKit.registerPebbleDisconnectedReceiver(getApplicationContext(), new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(getLocalClassName(), "Pebble disconnected!");
            }
        });

        mAckReceiver =
        PebbleKit.registerReceivedAckHandler(getApplicationContext(), new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                Log.i(getLocalClassName(), "Received ack for transaction " + transactionId);
            }

        });

        mNackReceiver =
        PebbleKit.registerReceivedNackHandler(getApplicationContext(), new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                Log.i(getLocalClassName(), "Received nack for transaction " + transactionId);
            }
        });


    }

    private void recognizeFace() {
        // Find the last picture
        File imageFile = null;
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        if(cursor.moveToFirst()) {
            String imageLocation = cursor.getString(1);
            imageFile = new File(imageLocation);
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 4, bitmap.getHeight() / 4, true);
            OutputStream outputStream = new FileOutputStream(imageFile);
            resized.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        String name = "";
        //Bitmap bitmap = BitmapFactory.decodeFile("/storage/emulated/0/photosnap/woods.jpg");
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();


        final String encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);
        //params.add(new BasicNameValuePair("base64", encodedImage));


        try {
            HttpClient httpclient = new DefaultHttpClient();
            //httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpPost httppost = new HttpPost("http://rekognition.com/func/api/");

            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.addTextBody("api_key", REKOGNITION_API_KEY);
            multipartEntityBuilder.addTextBody("api_secret", REKOGNITION_API_SECRET);
            multipartEntityBuilder.addTextBody("jobs", "face_celebrity");

            multipartEntityBuilder.addTextBody("base64", encodedImage);
            HttpEntity entity = multipartEntityBuilder.build();
            httppost.setEntity(entity);

            HttpResponse response = httpclient.execute(httppost);

            try {
                String jsonString = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(jsonString);
                mName = jsonObject.getJSONArray("face_detection").getJSONObject(0).getString("name").split(":")[0];
                Log.d(TAG, "test");
            } catch (JSONException ex) {
                ex.printStackTrace();
            }

            httpclient.getConnectionManager().shutdown();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        /*try {
            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            HttpPost httppost = new HttpPost("https://lambda-face-recognition.p.mashape.com/recognize?album=CELEBS&albumkey=b1ccb6caa8cefb7347d0cfb65146d5e3f84608f6ee55b1c90d37ed4ecca9b273");
            httppost.addHeader("X-Mashape-Key", FACE_API_KEY);

            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            //multipartEntityBuilder.addTextBody("album", "CELEBS");
            //multipartEntityBuilder.addTextBody("albumkey", "b1ccb6caa8cefb7347d0cfb65146d5e3f84608f6ee55b1c90d37ed4ecca9b273");
            FileBody fileBody = new FileBody(imageFile);
            multipartEntityBuilder.addPart("file", fileBody);
            //multipartEntityBuilder.addTextBody("urls", "http://www.lambdal.com/tiger.jpg");
            HttpEntity entity = multipartEntityBuilder.build();
            httppost.setEntity(entity);

            HttpResponse response = httpclient.execute(httppost);

            try {
                String jsonString = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(jsonString);
                Log.d(TAG, "test");
            } catch (JSONException ex) {
                ex.printStackTrace();
            }

            httpclient.getConnectionManager().shutdown();
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/
        /*try {
            com.mashape.unirest.http.HttpResponse<JsonNode> response =
            Unirest.post("https://lambda-face-recognition.p.mashape.com/recognize")
                    .header("X-Mashape-Key", FACE_API_KEY)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("file", imageFile)
                    .field("albumkey", "b1ccb6caa8cefb7347d0cfb65146d5e3f84608f6ee55b1c90d37ed4ecca9b273")
                    .asJson();
            Log.d(TAG, "TEST");
        } catch (UnirestException ex) {
            ex.printStackTrace();
        }*/

       /* try {
            Process process = Runtime.getRuntime().exec("curl -X POST --include https://lambda-face-recognition.p.mashape.com/recognize?album=CELEBS&albumkey=b1ccb6caa8cefb7347d0cfb65146d5e3f84608f6ee55b1c90d37ed4ecca9b273   -H X-Mashape-Key: TtjwpsetM3mshPPfof6EoclZgtFYp1E7yvNjsnMy7IzSF5UJwE  -F files=@" + imageFile.getAbsolutePath());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = "";
            while((line = bufferedReader.readLine()) != null) {
                Log.d(TAG, line);
            }
            while((line = bufferedReader1.readLine()) != null) {
                Log.d(TAG, line);
            }
            Log.d(TAG, "TEST");
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/

    }

    /* Called when we tap the screen or press the select button on the Pebble */
    private void handlePhotoTakeRequest() {
        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
        recognizeFace();
        sendStringToPebble(mName);
        Toast.makeText(ctx, "Took a photo", Toast.LENGTH_LONG).show();
        mName = "";
    }

    private void zoomCamera(int zoomDir) {
        Camera.Parameters params = mCamera.getParameters();
        if(!params.isZoomSupported()) {
            Log.d(TAG, "Zoom not supported");
            return;
        }
        int maxZoom = params.getMaxZoom();
        if(zoomDir == ZOOM_IN) {
            params.setZoom(params.getZoom() + 5);
        } else if(zoomDir == ZOOM_OUT) {
            params.setZoom(params.getZoom() - 5);
        }
        if(params.getZoom() > maxZoom) {
            params.setZoom(maxZoom);
        }
        if(params.getZoom() < 0) {
            params.setZoom(0);
        }
        mCamera.setParameters(params);
    }

    private void resetCamera() {
        mCamera.startPreview();
        cameraPreview.setCamera(mCamera);
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outputStream = null;

            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File directory = new File(sdCard.getAbsolutePath() + "/photosnap");
                directory.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                outFile = new File(directory, fileName);

                outputStream = new FileOutputStream(outFile);
                outputStream.write(data[0]);
                outputStream.flush();
                outputStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " +
                        outFile.getAbsolutePath());
                refreshGallery(outFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    private void sendStringToPebble(String message) {
        //if(message.length() > BUFFER_LENGTH) {
            PebbleDictionary pebDict = new PebbleDictionary();
            pebDict.addString(DATA_KEY, message);
            PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, pebDict);
        //} else {
            Log.i("sendStringToPebble", "String too long!");
       //}
    }

    private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            Log.d(TAG, "onShutter");
        }
    };

    private Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            resetCamera();

            Log.d(TAG, "onPictureTaken - raw");
        }
    };

    private Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            resetCamera();

            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

}
