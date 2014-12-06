package cis542.edu.com.photosnap;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * Created by David on 12/6/2014.
 */
public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;

    private static final String TAG = "CameraPreview";

    private static final int K_STATE_PREVIEW = 0;
    private static final int K_STATE_BUSY    = 1;
    private static final int K_STATE_FROZEN  = 2;

    CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified
        // when the underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        if(mCamera == camera) { return; }

        stopPreviewAndFreeCamera();

        mCamera = camera;

        if(mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();

            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            mCamera.startPreview();
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Surface has been created, tell camera where to draw preview
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException ex) {
            Log.d(TAG, "Error setting camera preview: " + ex.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mHolder.getSurface() == null) {
            // Preview surface does not exist
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception ex) {
            Log.d(TAG, "Tried to stop nonexistent preview");
        }

        // Set preview size and make any resize, rotate, or reformatting changes
        // (Optional)

        // Start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception ex) {
            Log.d(TAG, "Error starting camera preview: " + ex.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop preview
        if(mCamera != null) {
            // Call stopPreview() to stop updating preview surface
            mCamera.stopPreview();
        }
    }

    // When this function returns, mCamera will be null
    private void stopPreviewAndFreeCamera() {
        if(mCamera != null) {
            // Stop updating preview surface
            mCamera.stopPreview();

            // Stop using camera so that other applications can use it.
            // Should be released immediately during onPause() and re-open()
            // on onResume()
            mCamera.release();

            mCamera = null;
        }
    }
}
