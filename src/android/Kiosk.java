package com.example.template.kiosk.plugin;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;


public class Kiosk extends CordovaPlugin {

    private static int LOCKED_SYSTEM_UI_FLAGS = (0
//        | View.SYSTEM_UI_LAYOUT_FLAGS
        | View.SYSTEM_UI_FLAG_LOW_PROFILE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    );

    /**
     * 执行, 这里是 cordova 插件的执行入口
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "lockLauncher": {
                boolean locked = args.getBoolean(0);
                lockLauncher(locked, callbackContext);
                return true;
            }
            case "isLocked": {
                callbackContext.error("isLocked not implemented");
                return true;
            }
            case "switchLauncher": {
                switchLauncher(callbackContext);
                return true;
            }
            case "deleteDeviceAdmin": {
                deleteDeviceAdmin(callbackContext);
                return true;
            }
            case "isKeepScreenOn": {
                isKeepScreenOn(callbackContext);
                return true;
            }
            default: {
                return false;
            }
        }
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View decorView = cordova.getActivity().getWindow().getDecorView();
                decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        decorView.setSystemUiVisibility(LOCKED_SYSTEM_UI_FLAGS);
                    }
                });
            }
        });
    }

    /**
     * API: 获取屏幕常亮状态
     * @param callbackContext
     */
    private void isKeepScreenOn(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean b = webView.getView().getKeepScreenOn();
                    callbackContext.success(b ? "true" : "false");
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    /**
     * API: 切换 Launcher
     * @param callbackContext
     */
    private void switchLauncher(CallbackContext callbackContext) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Intent chooser = Intent.createChooser(intent, "Select destination...");
            if (intent.resolveActivity(cordova.getActivity().getPackageManager()) != null) {
                cordova.getActivity().startActivity(chooser);
                callbackContext.success("success");
            } else {
                // 这里不知道是什么意思.
                callbackContext.error("packageManager cannot resolved");
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    /**
     * API: 删除设备管理权限
     * @param callbackContext
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void deleteDeviceAdmin(CallbackContext callbackContext) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager)cordova.getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
            String packageName = cordova.getActivity().getPackageName();
            if (dpm.isDeviceOwnerApp(packageName)) {
                dpm.clearDeviceOwnerApp(packageName);
                callbackContext.success("success");
            } else {
                callbackContext.error("当前应用不是设备管理者");
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    /**
     * API: 锁定 Launcher
     * @param locked
     * @param callbackContext
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void lockLauncher(boolean locked, CallbackContext callbackContext) {
        try {
            if (locked) {
                // 1. 保持屏幕常亮
                // note(杨逸): 此操作无需管理权限
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.getView().setKeepScreenOn(true);
                    }
                });

                // 2. 执行需要管理权限的操作
                DevicePolicyManager dpm = (DevicePolicyManager)cordova.getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
                String packageName = cordova.getActivity().getPackageName();
                if (!dpm.isDeviceOwnerApp(packageName)) {
                    callbackContext.error("设备管理权限未配置, 请先使用 adb 赋予应用设备管理权限");
                    return;
                }

                ComponentName deviceAdmin = new ComponentName(cordova.getActivity(), MyAdmin.class);
                if (!dpm.isAdminActive(deviceAdmin)) {
                    callbackContext.error("设备管理权限未激活, 请在系统设置中激活应用的设备管理权限");
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // 禁用锁屏模式
                    dpm.setKeyguardDisabled(deviceAdmin, true);
                }

                // 将当前应用设置为锁定状态.
                dpm.setLockTaskPackages(deviceAdmin, new String[]{ packageName });
//                dpm.setStatusBarDisabled(deviceAdmin, true);
                cordova.getActivity().startLockTask();
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        View decorView = cordova.getActivity().getWindow().getDecorView();
                        decorView.setSystemUiVisibility(LOCKED_SYSTEM_UI_FLAGS);
                    }
                });
            } else {
                // 解锁
                cordova.getActivity().stopLockTask();
            }

            callbackContext.success("success");
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }
}
