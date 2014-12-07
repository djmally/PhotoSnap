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
    private final String TAG = "CameraPreview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    List<Camera.Size> mSupportedPreviewSizes;
    Camera mCamera;

    CameraPreview(Context context, SurfaceView sv) {
        super(context);

        mSurfaceView = sv;

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        this.mCamera = camera;
        if(mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();

            Camera.Parameters params = mCamera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(params);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if(mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if(mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center child SurfaceView within parent
            if(width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // Surface created, acquire camera and draw
        try {
            if(mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException ex) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", ex);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop previewing
        if(mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if(sizes == null) { return null; }

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for(Camera.Size sz : sizes) {
            double ratio = (double) sz.width / sz.height;
            if(Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) { continue; }
            if(Math.abs(sz.height - targetHeight) < minDiff) {
                optimalSize = sz;
                minDiff = Math.abs(sz.height - targetHeight);
            }
        }

        if(optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for(Camera.Size sz : sizes) {
                if(Math.abs(sz.height - targetHeight) < minDiff) {
                    optimalSize = sz;
                    minDiff = Math.abs(sz.height = targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if(mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();

            int previewWidth = previewSizes.get(0).width;
            int previewHeight = previewSizes.get(0).height;

            params.setPreviewSize(previewWidth, previewHeight);
            requestLayout();

            mCamera.setParameters(params);
            mCamera.startPreview();
        }
    }
}
