package org.telegram.messenger.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;

import android.os.Handler;
import android.os.Looper;

import androidx.exifinterface.media.ExifInterface;

import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class HiddenCameraManager implements Camera.PictureCallback, Camera.PreviewCallback, Camera.AutoFocusCallback {
    private final Context context;
    private Camera camera;
    private SurfaceTexture surface;
    private Consumer<String> onPhotoTaken;

    public HiddenCameraManager(Context context) {
        this.context = context;
    }

    public void takePhoto(boolean front, Consumer<String> onPhotoTaken) {
        if (front && isCameraAvailable(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
            this.onPhotoTaken = onPhotoTaken;
            initCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else if (!front && isCameraAvailable(Camera.CameraInfo.CAMERA_FACING_BACK)) {
            this.onPhotoTaken = onPhotoTaken;
            initCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
    }

    private boolean isCameraAvailable(int facing) {
        boolean result = false;
        if (context != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == facing) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private void initCamera(int facing) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                camera = Camera.open(facing);
                handler.post(() -> {
                    try {
                        if (camera != null) {
                            surface = new SurfaceTexture(123);
                            camera.setPreviewTexture(surface);
                            Camera.Parameters params = camera.getParameters();
                            if (facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                params.setRotation(270);
                            } else {
                                params.setRotation(90);
                            }
                            if (autoFocusSupported(camera)) {
                                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            }
                            Camera.Size size = params.getSupportedPictureSizes().stream().max((a, b) ->
                                 Long.compare((long) a.height * (long) a.width, (long) b.height * (long) b.width)
                            ).get();
                            params.setPictureSize(size.width, size.height);

                            camera.setParameters(params);
                            camera.setPreviewCallback(HiddenCameraManager.this);
                            camera.startPreview();
                            muteSound();
                        }
                    } catch (IOException ignored) {
                        releaseCamera();
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }

    private boolean autoFocusSupported(Camera camera) {
        if (camera != null) {
            Camera.Parameters params = camera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            return focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        return false;
    }

    private void muteSound() {
        if (context != null) {
            AudioManager mgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mgr.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
            } else {
                mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
            }
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            surface.release();
            camera = null;
            surface = null;
        }
        muteSound();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        try {
            camera.setPreviewCallback(null);
            camera.takePicture(null, null, this);
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
        }
    }

    @Override
    public void onAutoFocus(boolean b, Camera camera) {
        if (camera != null) {
            try {
                camera.takePicture(null, null, this);
                this.camera.autoFocus(null);
            } catch (Exception e) {
                e.printStackTrace();
                releaseCamera();
            }
        }
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        savePicture(bytes);
        releaseCamera();
    }

    private void savePicture(byte[] bytes) {
        try {
            File pictureFileDir = ApplicationLoader.getFilesDirFixed();
            if (bytes == null) {
                onPhotoTaken.accept(null);
            }
            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                onPhotoTaken.accept(null);
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("(hh:mm:ss)(dd.MM.yyyy)", Locale.US);
            String date = dateFormat.format(new Date());
            String photoFile = "hidden photo " + date + ".jpg";

            String filePath = pictureFileDir.getPath() + File.separator + photoFile;
            File pictureFile = new File(filePath);
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(bytes);
            fos.close();
            onPhotoTaken.accept(filePath);
        } catch (Exception e) {
            onPhotoTaken.accept(null);
        }
    }
}
