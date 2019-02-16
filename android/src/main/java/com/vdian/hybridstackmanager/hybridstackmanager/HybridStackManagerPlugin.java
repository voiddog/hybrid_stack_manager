package com.vdian.hybridstackmanager.hybridstackmanager;

import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * HybridStackManagerPlugin
 */
public class HybridStackManagerPlugin extends SafeMethodCallHandler {

    private static HybridStackManagerPlugin instance;

    public static synchronized HybridStackManagerPlugin getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Must register plugin first");
        }
        return instance;
    }

    /**
     * Plugin registration.
     */
    public static synchronized void registerWith(Registrar registrar) {
        if (instance != null) {
            // unregister instance
            instance.channel.setMethodCallHandler(null);
            instance = null;
        }
        instance = new HybridStackManagerPlugin(registrar);
    }

    private final MethodChannel channel;

    private HybridStackManagerPlugin(Registrar registrar) {
        channel = new MethodChannel(registrar.messenger(), "hybrid_stack_manager");
        channel.setMethodCallHandler(this);
    }

    /**
     * 打开推送一个 flutter page
     *
     * @param pageName
     * @param args
     * @param nativePageId
     * @param result
     */
    void pushFlutterPager(String pageName, HashMap<String, Object> args, String nativePageId
            , @Nullable Result result) {
        if (channel != null) {
            HashMap<String, Object> channelArgs = new HashMap<>();
            channelArgs.put("args", args);
            channelArgs.put("pageName", pageName);
            channelArgs.put("nativePageId", nativePageId);
            channel.invokeMethod("pushFlutterPage", channelArgs, result);
        }
    }

    /**
     * 请求 flutter 更新主题
     *
     * @param result
     */
    void requestUpdateTheme(@Nullable Result result) {
        if (channel != null) {
            channel.invokeMethod("requestUpdateTheme", null, result);
        }
    }

    /**
     * 按了返回键，这里重写返回键是为了防止出现黑屏无法退出的情况
     * @param result
     */
    void onBackPressed(@Nullable Result result) {
        if (channel != null) {
            channel.invokeMethod("onBackPressed", null, result);
        } else if (result != null){
            result.error("-1", "channel is null", null);
        }
    }

    @Override
    protected void onSafeMethodCall(MethodCall call, SafeResult result) {
        switch (call.method) {
            case "getInitRoute": {
                IFlutterNativePage nativePage = FlutterStackManager.getInstance().getCurNativePage();
                if (nativePage != null) {
                    result.success(nativePage.getInitRoute());
                } else {
                    result.error("-1", "no native page found", null);
                }
                break;
            }
            case "openNativePage": {
                HashMap<String, Object> args = call.arguments();
                String url = (String) args.get("url");
                String nativePageId = (String) args.get("nativePageId");
                Map<String, Object> subArgs = (Map<String, Object>) args.get("args");
                IFlutterNativePage nativePage = FlutterStackManager.getInstance().getNativePageById(nativePageId);
                if (nativePage != null) {
                    nativePage.openNativePage(url, subArgs, result);
                } else {
                    result.error("-1", "no native page found", null);
                }
                break;
            }
            case "finishNativePage": {
                HashMap<String, Object> args = call.arguments();
                String nativePageId = (String) args.get("nativePageId");
                IFlutterNativePage nativePage = FlutterStackManager.getInstance().getNativePageById(nativePageId);
                if (nativePage != null) {
                    Object ret = args.get("result");
                    nativePage.finishNativePage(ret);
                } else {
                    result.error("-1", "no native page found", null);
                }
                break;
            }
            case "setStatusBarColor": {
                HashMap<String, Object> args = call.arguments();
                String nativePageId = (String) args.get("nativePageId");
                IFlutterNativePage nativePage = FlutterStackManager.getInstance().getNativePageById(nativePageId);
                if (nativePage != null) {
                    int color = (int) args.get("color");
                    nativePage.updateStatusBarColor(color);
                } else {
                    result.error("-1", "no native page found", null);
                }
                break;
            }
            default: {
                result.notImplemented();
            }
        }
    }
}
