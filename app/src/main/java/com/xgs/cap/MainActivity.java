package com.xgs.cap;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.petterp.floatingx.FloatingX;
import com.petterp.floatingx.assist.FxScopeType;
import com.petterp.floatingx.assist.helper.FxAppHelper;
import com.petterp.floatingx.listener.control.IFxAppControl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuProvider;

public class MainActivity extends Activity implements Runnable, Shizuku.OnRequestPermissionResultListener, ServiceConnection {
    private ScrollView sc;
    private TextView showData;
    private Intent shizukuIntent;
    private boolean isRunning = false;
    private boolean permissionIsGranted = false;
    private Shizuku.UserServiceArgs mUserServiceArgs;
    private final int port = 34567;
    public static final String TAG = "XGS";
    IFxAppControl fxAppControl = null;
    private IBinder mRemoteBinder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        shizukuIntent = getPackageManager().getLaunchIntentForPackage(ShizukuProvider.MANAGER_APPLICATION_ID);
        mUserServiceArgs = new Shizuku.UserServiceArgs(new ComponentName(getPackageName(), CaptureService.class.getName()))
                .daemon(false)
                .debuggable(false)
                .processNameSuffix("xgs_cap_all")
                .version(1);
        bindService();

    }

    @Override
    public void onRequestPermissionResult(int requestCode, int grantResult) {
        permissionIsGranted = grantResult == 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shizukuIntent != null && Shizuku.pingBinder()) {
            permissionIsGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            if (permissionIsGranted) {
                Shizuku.removeRequestPermissionResultListener(this);
            } else {
                Shizuku.addRequestPermissionResultListener(this);
                Shizuku.requestPermission(0);
            }
        }

        FxAppHelper helper = FxAppHelper.builder()
                .setContext(getApplicationContext())
                .setLayout(R.layout.item_floating)
                .setScopeType(FxScopeType.SYSTEM_AUTO)
                .build();
        fxAppControl = FloatingX.install(helper);
        fxAppControl.setClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click button..");
                if (!permissionIsGranted) return;
                if (!isRunning) {
                    Toast.makeText(getApplicationContext(), "服务异常", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mRemoteBinder != null && mRemoteBinder.pingBinder()) {
                    ICaptureService iUserService = ICaptureService.Stub.asInterface(mRemoteBinder);
                    try {
                        iUserService.doCap(port);
                    } catch (RemoteException ignored) {
                    }
                    Toast.makeText(getApplicationContext(), "截图中", Toast.LENGTH_SHORT).show();

                }
            }
        });
        if (!fxAppControl.isShow()) {
            fxAppControl.show();
        }

    }


    private void bindService() {
        new Thread(this).start();// Socket服务端
        Shizuku.bindUserService(mUserServiceArgs, this);
        Log.d(TAG, "bindUserService..");

    }

    // 进程间通信，如果不理解此处为什么要用到socket，请百度
    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                BufferedReader br = new BufferedReader(new InputStreamReader(server.accept().getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                sb.append(line).append('\n');
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), sb.toString(), Toast.LENGTH_SHORT).show();
                });
                Log.d(TAG, "scoket recv content:" + line);
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected..");
        isRunning = true;
        mRemoteBinder=service;
    }

    @Override
    protected void onDestroy() {
        isRunning = false;
        Shizuku.unbindUserService(mUserServiceArgs, this, true);
        super.onDestroy();
        Log.d(TAG, "onDestroy..");

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isRunning = false;
        Log.d(TAG, "onServiceDisconnected..");

    }
}