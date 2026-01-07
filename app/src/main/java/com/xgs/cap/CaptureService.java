package com.xgs.cap;

import static android.graphics.Bitmap.CompressFormat.JPEG;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureService extends ICaptureService.Stub {
    private boolean isServiceRunning = true;
    private int port;

    @Override
    public void destroy() {
        isServiceRunning = false;
    }

    @Override
    public void doCap(int port) {
        Log.d(MainActivity.TAG, "call doCap...");
        this.port = port;
        doAction();
    }

    /**
     * @noinspection BusyWait
     */
    public void doAction() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                if (isServiceRunning) {
                    try (Socket socket = new Socket(InetAddress.getLocalHost(), port)) {
                        OutputStream out = socket.getOutputStream();

                        String path = captureSecureWindow();
                            if (path != null) {
                                out.write(("截图成功,保存在：" + path).getBytes(StandardCharsets.UTF_8));
                            } else {
                                out.write(("截图失败！").getBytes(StandardCharsets.UTF_8));
                            }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

    }

    static String captureSecureWindow() {
        try {
            String displaySize = runCmd("cmd window size").strip();
            String[] widthAndHigh = displaySize.substring("Physical size: ".length()).split("x");
            Log.d(MainActivity.TAG, "widthAndHigh:" + widthAndHigh[0] + " x " + widthAndHigh[1]);


            Class<?> Builde_Cls = Class.forName("android.window.ScreenCapture$CaptureArgs$Builder");
            Object buildInstance = Builde_Cls.newInstance();
            Method setSourceCrop = Builde_Cls.getDeclaredMethod("setSourceCrop", Rect.class);
            Method setCaptureSecureLayers = Builde_Cls.getDeclaredMethod("setCaptureSecureLayers", boolean.class);

            Method build = Builde_Cls.getDeclaredMethod("build");
            setSourceCrop.invoke(buildInstance, new Rect(0, 0, Integer.parseInt(widthAndHigh[0]), Integer.parseInt(widthAndHigh[1])));
            setCaptureSecureLayers.invoke(buildInstance, true);
            Object captureArgs = build.invoke(buildInstance);

            Class<?> screenCapture = Class.forName("android.window.ScreenCapture");
            Class<?> syncCaptureListenerClass = Class.forName("android.window.ScreenCapture$SynchronousScreenCaptureListener");

            Method createSyncCaptureListener = screenCapture.getDeclaredMethod("createSyncCaptureListener");
            Object syncScreenCapture = createSyncCaptureListener.invoke(screenCapture);

            Class<?> windowManagerGlobalclass = Class.forName("android.view.WindowManagerGlobal");
            Class<?> captureArgsClass = Class.forName("android.window.ScreenCapture$CaptureArgs");
            Object windowManagerService = windowManagerGlobalclass.getMethod("getWindowManagerService").invoke(null);
            Method captureDisplayMethod = windowManagerService.getClass().getMethod("captureDisplay", int.class, captureArgsClass,
                    Class.forName("android.window.ScreenCapture$ScreenCaptureListener"));
            captureDisplayMethod.invoke(windowManagerService, 0, captureArgs, syncScreenCapture);

            Method getBufferMethod = syncCaptureListenerClass.getMethod("getBuffer");
            Object screenshotHardwareBuffer = getBufferMethod.invoke(syncScreenCapture);

            Class<?> screenshotHardwareBufferClass = Class.forName("android.window.ScreenCapture$ScreenshotHardwareBuffer");
            Method asBitmapMethod = screenshotHardwareBufferClass.getMethod("asBitmap");

            Bitmap bitmap = (Bitmap) asBitmapMethod.invoke(screenshotHardwareBuffer);
            Bitmap.Config SCREENSHOT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
            bitmap = bitmap.copy(SCREENSHOT_BITMAP_CONFIG, true);

            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            String formattedString = sdf.format(now);

            String savePath = "/data/local/tmp/" + formattedString + ".jpg";
            File imageFile = new File(savePath);
            OutputStream outputStream = new FileOutputStream(imageFile);
            boolean success = bitmap.compress(JPEG, 100, outputStream);
            if (success) {
                Log.d(MainActivity.TAG, "screencap saved successfully to: " + imageFile.getAbsolutePath());
            } else {
                Log.d(MainActivity.TAG, "Failed to save screencap.");
                imageFile.delete();
            }
            return savePath;
        } catch (Exception e) {
            Log.d(MainActivity.TAG, "screencap err:" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    public static String runCmd(String cmd) {
        String result = "";
        try {
            java.lang.Process process = Runtime.getRuntime().exec(cmd);
            InputStream inputStream = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while ((line = br.readLine()) != null) {
//                Log.d("XGS", line);
                result += line + "\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}