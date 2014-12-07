package cis542.edu.com.photosnap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


public class CameraActivity extends Activity {

    private ImageView imageView;

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


    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "CameraActivity";


    /* BEGIN PRE-GENERATED CODE */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        this.imageView = (ImageView) findViewById(R.id.imageView);
        Button capturebutton = (Button) findViewById(R.id.button_capture);
        capturebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                distpatchPictureIntent();
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
        // Start watch app
        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);

        dataHandler = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveData(Context context, int i, PebbleDictionary pebbleTuples) {
                // ACK to prevent timeout
                PebbleKit.sendAckToPebble(context, i);

                // Get button press
                int buttonPressed = pebbleTuples.getInteger(DATA_KEY).intValue();

                // Take action
                switch(buttonPressed) {
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

        PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataHandler);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister Activity-scoped BroadcastReceivers when Activity is paused
        if(dataHandler != null) {
            unregisterReceiver(dataHandler);
            dataHandler = null;
        }
    }


    private void distpatchPictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }
    }

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
