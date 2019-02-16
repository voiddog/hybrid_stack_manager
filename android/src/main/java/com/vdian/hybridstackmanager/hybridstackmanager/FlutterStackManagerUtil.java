package com.vdian.hybridstackmanager.hybridstackmanager;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import io.flutter.view.FlutterNativeView;
import io.flutter.view.FlutterView;

/**
 * ┏┛ ┻━━━━━┛ ┻┓
 * ┃　　　　　　 ┃
 * ┃　　　━　　　┃
 * ┃　┳┛　  ┗┳　┃
 * ┃　　　　　　 ┃
 * ┃　　　┻　　　┃
 * ┃　　　　　　 ┃
 * ┗━┓　　　┏━━━┛
 * * ┃　　　┃   神兽保佑
 * * ┃　　　┃   代码无BUG！
 * * ┃　　　┗━━━━━━━━━┓
 * * ┃　　　　　　　    ┣┓
 * * ┃　　　　         ┏┛
 * * ┗━┓ ┓ ┏━━━┳ ┓ ┏━┛
 * * * ┃ ┫ ┫   ┃ ┫ ┫
 * * * ┗━┻━┛   ┗━┻━┛
 *
 * @author qigengxin
 * @since 2018/11/13 1:07 PM
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FlutterStackManagerUtil {

    public static void updateIntent(Intent intent, HashMap<String, Object> args) {
        for (String key : args.keySet()) {
            updateIntent(intent, key, args.get(key));
        }
    }

    public static void updateIntent(Intent intent, String key, @Nullable Object value) {
        if (value == null) {
            intent.removeExtra(key);
        } else {
            if (value instanceof Integer) {
                intent.putExtra(key, (int)value);
            } else if (value instanceof Long) {
                intent.putExtra(key, (long)value);
            } else if (value instanceof Float) {
                intent.putExtra(key, (float)value);
            } else if (value instanceof Double) {
                intent.putExtra(key, (long)value);
            } else if (value instanceof String) {
                intent.putExtra(key, (String)value);
            } else if (value instanceof Serializable) {
                intent.putExtra(key, (Serializable)value);
            } else {
                Log.e("updateIntent", "unknow value: " + value);
            }
        }
    }

    public static void onSurfaceDestroyed(FlutterView flutterView, FlutterNativeView flutterNativeView) {
        try {
            Method nativeSurfaceDestroyed = FlutterView.class.getDeclaredMethod("nativeSurfaceDestroyed", long.class);
            nativeSurfaceDestroyed.setAccessible(true);
            nativeSurfaceDestroyed.invoke(flutterView, flutterNativeView.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
