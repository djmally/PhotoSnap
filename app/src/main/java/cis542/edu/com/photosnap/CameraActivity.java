package cis542.edu.com.photosnap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


public class CameraActivity extends Activity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "CameraActivity";

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

    /* BEGIN PRE-GENERATED CODE */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_camera);
        ctx = this;

        //this.imageView = (ImageView) findViewById(R.id.imageView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //setContentView(R.layout.main);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv);
        cameraPreview = new CameraPreview(this, mSurfaceView);
        cameraPreview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.layout)).addView(cameraPreview);
        cameraPreview.setKeepScreenOn(true);

        cameraPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
                Toast.makeText(ctx, "Took a photo", Toast.LENGTH_LONG).show();
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

    /* END PRE-GENERATED CODE */

    @Override
    public void onResume() {
        super.onResume();

        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0) {
            try {
                mCamera = Camera.open(0);
                mCamera.startPreview();
                cameraPreview.setCamera(mCamera);
            } catch (RuntimeException ex) {
                Toast.makeText(this, "No camera found...", Toast.LENGTH_LONG).show();
            }
        }

        /* PEBBLE SECTION */
        // Start Pebble app
        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
        this.dataHandler = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveData(Context context, int i, PebbleDictionary pebbleTuples) {
                // ACK to prevent timeout
                PebbleKit.sendAckToPebble(context, i);

                // Get button press
                int buttonPressed = pebbleTuples.getInteger(DATA_KEY).intValue();

                // Take action
                switch (buttonPressed) {
                    case SELECT_BUTTON_KEY:
                        break;
                    case UP_BUTTON_KEY:
                        break;
                    case DOWN_BUTTON_KEY:
                        break;
                    default:
                        break;
                }
            }
        };

        // Register data handler
        PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataHandler);
        /* END PEBBLE SECTION */
    }

    @Override
    public void onPause() {

        if(mCamera != null) {
            mCamera.stopPreview();
            cameraPreview.getHolder().removeCallback(cameraPreview);
            mCamera.release();
            mCamera = null;
        }

        /* PEBBLE SECTION */
        // Unregister Activity-scoped BroadcastReceivers when Activity is paused
        if(dataHandler != null) {
            //unregisterReceiver(dataHandler);
            dataHandler = null;
        }
        /* END PEBBLE SECTION */

        super.onPause();
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

    private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            Log.d(TAG, "onShutter");
        }
    };

    private Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
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

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outputStream = null;

            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File directory = new File(sdCard.getAbsolutePath() + "/photosnap");
                directory.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(directory, fileName);

                outputStream = new FileOutputStream(outFile);
                outputStream.write(data[0]);
                outputStream.flush();
                outputStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " +
                            outFile.getAbsolutePath());
                refreshGallery(outFile);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    /*private void distpatchPictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }*/

    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timestamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                          Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        String mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;

    }

    /*protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }
    }*/

    private void sendStringToPebble(String message) {
        if(message.length() > BUFFER_LENGTH) {
            PebbleDictionary pebDict = new PebbleDictionary();
            pebDict.addString(DATA_KEY, message);
            PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, pebDict);
        } else {
            Log.i("sendStringToPebble", "String too long!");
        }
    }

}
