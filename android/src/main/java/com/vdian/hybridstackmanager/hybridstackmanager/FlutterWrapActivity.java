package com.vdian.hybridstackmanager.hybridstackmanager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.flutter.app.FlutterActivityDelegate;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterMain;
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
 * <p>
 * flutter 容器
 *
 * @author qigengxin
 * @since 2019/2/5 2:26 PM
 */
public class FlutterWrapActivity extends AppCompatActivity implements PluginRegistry,
        FlutterActivityDelegate.ViewFactory, FlutterView.Provider, IFlutterNativePage {

    public static final String EXTRA_MAP_ARGS = "ext_args";
    public static final String EXTRA_RESULT_KEY = "ext_result_key";
    public static final String EXTRA_STRING_PAGE_NAME = "page_name";

    /**
     * 直接打开 flutter 页面
     * @param context 上下文
     * @param pageName 目标 flutter 页面名
     * @param args 目标 flutter 页面参数
     */
    public static void start(@NonNull Context context, @NonNull String pageName,
                             @Nullable HashMap<String, Object> args) {
        Intent intent = new Intent(context, FlutterWrapActivity.class);
        intent.putExtra(EXTRA_STRING_PAGE_NAME, pageName);
        if (args != null) {
            intent.putExtra(EXTRA_MAP_ARGS, args);
        }
        context.startActivity(intent);
    }

    /**
     * 返回打开 flutter 页面的 intent
     * @param pageName 目标 flutter 页面名
     * @param args 目标 flutter 页面参数
     */
    public static Intent startIntent(@NonNull Context context, @NonNull String pageName,
                                   @Nullable HashMap<String, Object> args) {
        Intent intent = new Intent(context, FlutterWrapActivity.class);
        intent.putExtra(EXTRA_STRING_PAGE_NAME, pageName);
        if (args != null) {
            intent.putExtra(EXTRA_MAP_ARGS, args);
        }
        return intent;
    }

    private static AtomicLong sGlobalPageId = new AtomicLong(1);
    private static int FLAG_ATTACH = 1;
    private static int FLAG_SURFACE_CREATED = 2;
    private static int MAX_REQUEST_CODE = 100;
    // 注意会持有 context，传递 application
    @SuppressLint("StaticFieldLeak")
    private static FlutterNativeView sFlutterNativeView;

    @Override
    public boolean isAttachToFlutter() {
        return hasFlag(flag, FLAG_ATTACH);
    }

    @Override
    public void attachFlutter() {
        localAttachFlutter();
    }

    @Override
    public void detachFlutter() {
        localDetachFlutter();
    }

    @Override
    public void openNativePage(@NonNull String url, @Nullable Map<String, Object> args,
                               @NonNull MethodChannel.Result result) {
        onNativePageRoute(url, new HashMap<>(args), generateRequestCodeByChannel(result));
    }

    @Override
    public void finishNativePage(@Nullable Object result) {
        // 先让 flutter 脱离渲染
        detachFlutter();
        if (result == null) {
            setResult(RESULT_OK);
        } else {
            Intent data = new Intent();
            FlutterStackManagerUtil.updateIntent(data, EXTRA_RESULT_KEY, result);
            setResult(RESULT_OK, data);
        }
        // 结束当前 native 页面
        finish();
    }

    @Override
    public String getNativePageId() {
        return nativePageId;
    }

    /**
     * 获取初始路由，如果是首次打开页面，需要通过 flutter 来主动获取路由信息
     */
    @Override
    public Map<String, Object> getInitRoute() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("pageName", pageName);
        if (initArgs != null) {
            ret.put("args", initArgs);
        }
        ret.put("nativePageId", nativePageId);
        return ret;
    }

    @Override
    public int generateRequestCodeByChannel(MethodChannel.Result result) {
        int requestCode = generateRequestCode();
        if (requestCode < 0) {
            result.error("-1", "Not enough request code, Fuck what??, map size:"
                    + resultChannelMap.size(), null);
            return requestCode;
        }
        resultChannelMap.put(requestCode, result);
        return requestCode;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void updateStatusBarColor(int color) {
        statusBarColor = color;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            if (window != null) {
                window.setStatusBarColor(statusBarColor);
            }
        }
    }

    @Override
    public FlutterView createFlutterView(Context context) {
        FlutterNativeView flutterNativeView = createFlutterNativeView();
        return new FlutterView(this, null, flutterNativeView);
    }

    @Override
    public FlutterNativeView createFlutterNativeView() {
        if (sFlutterNativeView == null) {
            isCreatePage = true;
            sFlutterNativeView = new FlutterNativeView(getApplicationContext());
        }
        return sFlutterNativeView;
    }

    @Override
    public boolean retainFlutterNativeView() {
        return true;
    }

    @Override
    public Registrar registrarFor(String key) {
        return delegate.registrarFor(key);
    }

    @Override
    public boolean hasPlugin(String key) {
        return delegate.hasPlugin(key);
    }

    @Override
    public <T> T valuePublishedByPlugin(String pluginKey) {
        return delegate.valuePublishedByPlugin(pluginKey);
    }

    @Override
    public FlutterView getFlutterView() {
        if (delegate == null) {
            return null;
        }
        return delegate.getFlutterView();
    }

    // 当前页面的 id
    public final String nativePageId = String.valueOf(sGlobalPageId.getAndIncrement());
    /**
     * 当前 flutter 页面的 page name
     */
    @Nullable
    protected String pageName;
    protected HashMap<String, Object> initArgs;
    // 是否是首次启动 flutter engine
    protected boolean isCreatePage;
    // flutter activity 代理类
    protected FlutterActivityDelegate delegate;
    protected ViewGroup rootView;
    protected SparseArray<MethodChannel.Result> resultChannelMap = new SparseArray<>();
    // 是否需要在 destroy 的时候调用 destroy surface
    protected boolean needDestroySurface = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // push the page to stack manager
        FlutterStackManager.getInstance().addNativePage(this);
        // 从 saveInstance 或者 intent 中获取 uri
        if (savedInstanceState != null) {
            pageName = savedInstanceState.getString(EXTRA_STRING_PAGE_NAME);
            Serializable intentArgs = savedInstanceState.getSerializable(EXTRA_MAP_ARGS);
            if (intentArgs instanceof HashMap<?, ?>) {
                initArgs = (HashMap<String, Object>) intentArgs;
            }
        } else {
            Intent intent = getIntent();
            Serializable intentArgs = intent.getSerializableExtra(EXTRA_MAP_ARGS);
            if (intentArgs instanceof HashMap<?, ?>) {
                initArgs = (HashMap<String, Object>) intentArgs;
            }
            if (intent.hasExtra(EXTRA_STRING_PAGE_NAME)) {
                pageName = intent.getStringExtra(EXTRA_STRING_PAGE_NAME);
            } else {
                String uri = intent.getDataString();
                updateParamFromUrl(uri, initArgs);
                // parse page name from uri
                pageName = getPageNameByUri(uri);
            }
        }
        // 如果没有路由信息，退出页面
        if (TextUtils.isEmpty(pageName)) {
            finish();
            return;
        }
        if (sFlutterNativeView == null) {
            isCreatePage = true;
        }
        // init flutter
        try {
            FlutterMain.startInitialization(getApplicationContext());
            attachFlutter();
            System.out.println("onCreate: " + nativePageId);
            delegate.onCreate(savedInstanceState);
        } catch (Throwable t) {
            // 初始化异常
            finish();
            onFlutterInitFailure(t);
            return;
        }
        // 引擎初始化完毕，更新状态栏颜色
        updateStatusBarColor(statusBarColor);
        if (isCreatePage) {
            // 首次打开 flutter engine 注册插件
            onRegisterPlugin();
        }
        // 非首次打开，设置沉浸式主题，因为不熟首次打开，flutter 不会请求 native 来更新沉浸式主题，需要手动请求更新
        if (!isCreatePage) {
            preFlutterApplyTheme();
            HybridStackManagerPlugin.getInstance().requestUpdateTheme(new MethodChannel.Result() {
                @Override
                public void success(@Nullable Object o) {
                    postFlutterApplyTheme();
                }

                @Override
                public void error(String s, @Nullable String s1, @Nullable Object o) {
                    postFlutterApplyTheme();
                }

                @Override
                public void notImplemented() {
                    postFlutterApplyTheme();
                }
            });
        } else {
            preFlutterApplyTheme();
            postFlutterApplyTheme();
        }
        // 创建根视图
        rootView = createContentView();
        setContentView(rootView);
        MethodChannel.Result result = setupLaunchView(true);
        if (!isCreatePage) {
            // 首次打开需要 flutter 来主动获取路由，非首次打开由当前页面来打开路由
            HybridStackManagerPlugin.getInstance().pushFlutterPager(pageName, initArgs,
                    nativePageId, result);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (delegate != null) {
            System.out.println("onNewIntent: " + nativePageId);
            delegate.onNewIntent(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachFlutter();
        if (delegate != null) {
            System.out.println("onStart: " + nativePageId);
            delegate.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachFlutter();
        if (delegate != null) {
            System.out.println("onResume: " + nativePageId);
            delegate.onResume();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        attachFlutter();
        if (delegate != null) {
            System.out.println("onPostResume: " + nativePageId);
            delegate.onPostResume();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (delegate != null) {
            System.out.println("onUserLeaveHint: " + nativePageId);
            delegate.onUserLeaveHint();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (delegate != null) {
            System.out.println("onPause: " + nativePageId);
            delegate.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (delegate != null) {
            System.out.println("onStop: " + nativePageId);
            delegate.onStop();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putString(EXTRA_STRING_PAGE_NAME, pageName);
            outState.putSerializable(EXTRA_MAP_ARGS, initArgs);
        }
    }

    @Override
    protected void onDestroy() {
        cancelAllResult();
        FlutterStackManager.getInstance().removeNativePage(this);
        if (delegate != null) {
            System.out.println("onDestroy: " + nativePageId);
            // 手动把 handler 设置为 null，防止内存泄漏
            getFlutterView().setMessageHandler("flutter/platform", null);
            delegate.onDestroy();
        }
        detachFlutter();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (delegate != null) {
            delegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (delegate != null) {
            delegate.onActivityResult(requestCode, resultCode, data);
        }
        if (data == null) {
            sendResultChannel(requestCode, resultCode, null);
        } else {
            if (data.hasExtra(EXTRA_RESULT_KEY)) {
                // 从 flutter 过来的数据
                sendResultChannel(requestCode, resultCode, data.getExtras().get(EXTRA_RESULT_KEY));
            } else {
                HashMap<String, Object> retMap = new HashMap<>();
                if (data.getExtras() != null) {
                    for (String key : data.getExtras().keySet()) {
                        retMap.put(key, data.getExtras().get(key));
                    }
                }
                sendResultChannel(requestCode, resultCode, retMap);
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (delegate != null) {
            delegate.onTrimMemory(level);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (delegate != null) {
            delegate.onLowMemory();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (delegate != null) {
            delegate.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        HybridStackManagerPlugin.getInstance().onBackPressed(new MethodChannel.Result() {
            @Override
            public void success(@Nullable Object o) {
                if (!(o instanceof Boolean) || !((Boolean) o)) {
                    FlutterWrapActivity.super.onBackPressed();
                }
            }

            @Override
            public void error(String s, @Nullable String s1, @Nullable Object o) {
                FlutterWrapActivity.super.onBackPressed();
            }

            @Override
            public void notImplemented() {
                FlutterWrapActivity.super.onBackPressed();
            }
        });
    }

    protected void onFlutterInitFailure(Throwable t) {}

    protected void onRegisterPlugin() {
        try {
            Class.forName("io.flutter.plugins.GeneratedPluginRegistrant")
                    .getDeclaredMethod("registerWith", PluginRegistry.class)
                    .invoke(null, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void preFlutterApplyTheme() {}

    protected void postFlutterApplyTheme() {}

    protected ViewGroup createContentView() {
        ViewGroup ret = new FrameLayout(this);
        ret.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View background = new View(this);
        background.setBackground(getLaunchScreenDrawableFromActivityTheme());
        background.setClickable(true);
        ret.addView(background);
        return ret;
    }

    /**
     * 配置启动页
     * 默认显示占位图，等待首帧和 channel 回调之后显示页面
     */
    protected void setupLaunchView() {
        setupLaunchView(false);
    }

    protected MethodChannel.Result setupLaunchView(boolean needWaitForResult) {
        View maskView = null;
        final FlutterView flutterView = getFlutterView();
        int i = 0;
        // 移除前一个 flutter view，然后选择最顶层的 view 为 maskview
        while(i < rootView.getChildCount()) {
            View child = rootView.getChildAt(i);
            if (child instanceof FlutterView && child != flutterView) {
                rootView.removeViewAt(i);
            } else {
                i++;
                maskView = child;
            }
        }

        if (flutterView.getParent() != rootView) {
            // add flutter view to root view
            rootView.addView(flutterView, 0,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                            , ViewGroup.LayoutParams.MATCH_PARENT));
        }

        // add surface callback
        flutterView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                flag = flag | FLAG_SURFACE_CREATED;
                needDestroySurface = true;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                flag = flag & (~FLAG_SURFACE_CREATED);
                if (flutterView.getFlutterNativeView() != null) {
                    /// not detach, need not to destroy
                    needDestroySurface = false;
                }
            }
        });
        final AtomicInteger visibleCount = (isCreatePage || !needWaitForResult) ?
                new AtomicInteger(1) : new AtomicInteger(0);
        final View finalMaskView = maskView;
        // 通过 result 和 first frame listener，判定首帧时间
        MethodChannel.Result result = new MethodChannel.Result() {
            @Override
            public void success(@Nullable Object result) {
                if (visibleCount.incrementAndGet() >= 2 && finalMaskView != null) {
                    finalMaskView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void error(String s, @Nullable String s1, @Nullable Object o) {}

            @Override
            public void notImplemented() {}
        };
        flutterView.addFirstFrameListener(new FlutterView.FirstFrameListener() {
            @Override
            public void onFirstFrame() {
                flutterView.removeFirstFrameListener(this);
                if (visibleCount.incrementAndGet() >= 2 && finalMaskView != null) {
                    finalMaskView.setVisibility(View.INVISIBLE);
                }
            }
        });
        // 此处修复 flutter 和 native 沉浸式同步的问题
        ViewCompat.requestApplyInsets(flutterView);
        return result;
    }

    /**
     * 发送 result 的结果
     */
    protected void sendResultChannel(int requestCode, int resultCode, @Nullable Object result) {
        MethodChannel.Result resultChannel = resultChannelMap.get(requestCode);
        if (resultChannel != null) {
            resultChannelMap.remove(requestCode);
            HashMap<String, Object> ret = new HashMap<>();
            ret.put("resultCode", resultCode);
            ret.put("data", result);
            resultChannel.success(ret);
        }
    }

    /**
     * 结束所有的 result channel
     */
    protected void cancelAllResult() {
        for (int i = 0, size = resultChannelMap.size(); i < size; ++i) {
            MethodChannel.Result result = resultChannelMap.valueAt(i);
            HashMap<String, Object> ret = new HashMap<>();
            ret.put("resultCode", Activity.RESULT_CANCELED);
            result.success(ret);
        }
        resultChannelMap.clear();
    }

    /**
     * 请求生成 request code
     * @return < 0 generate failure
     */
    protected int generateRequestCode() {
        if (resultChannelMap.size() == 0) {
            return 1;
        }
        int count = 0;
        int start = resultChannelMap.keyAt(resultChannelMap.size() - 1) + 1;
        while (count < MAX_REQUEST_CODE) {
            start = (start > MAX_REQUEST_CODE) ? (start - MAX_REQUEST_CODE) : start;
            if (resultChannelMap.get(start) == null) {
                // found request code
                return start;
            }
            count++;
        }
        // failure to generate
        return -1;
    }

    /**
     * 从 uri 中获取 flutter 的 page name
     * @param uri
     * @return
     */
    protected String getPageNameByUri(String uri) {
        if (uri == null) {
            return null;
        }
        Uri u = Uri.parse(uri);
        return u.getQueryParameter("flutterPageName");
    }

    /**
     * 请求跳转 native 页面
     * @param uri
     * @param args
     * @param requestCode
     */
    protected void onNativePageRoute(String uri, HashMap<String, Object> args, int requestCode) {
        if (uri == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        FlutterStackManagerUtil.updateIntent(intent, args);
        startActivityForResult(intent, requestCode);
    }

    /**
     * attach flutter
     */
    private void localAttachFlutter() {
        if (hasFlag(flag, FLAG_ATTACH)) {
            return;
        }
        flag |= FLAG_ATTACH;
        System.out.println("Attach flutter :" + nativePageId);
        final IFlutterNativePage curNativePage = FlutterStackManager.getInstance().getCurNativePage();
        if (curNativePage != null) {
            System.out.println("flutter detach from native page: " + curNativePage.getNativePageId());
            curNativePage.detachFlutter();
        }
        // 设置当前页面是 native page
        FlutterStackManager.getInstance().setCurNativePage(this);
        final FlutterActivityDelegate localDt = new FlutterActivityDelegate(this, this);
        delegate = localDt;
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            System.out.println("OnCreateDelegate: " + nativePageId);
            localDt.onCreate(null);
            setupLaunchView();
        }
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            System.out.println("onStartDelegate: " + nativePageId);
            localDt.onStart();
        }
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            System.out.println("OnResume: " + nativePageId);
            localDt.onResume();
            System.out.println("OnPostResume: " + nativePageId);
            localDt.onPostResume();
        }
    }

    /**
     * detach flutter
     */
    private void localDetachFlutter() {
        if (!hasFlag(flag, FLAG_ATTACH)) {
            return;
        }
        flag &= ~FLAG_ATTACH;
        FlutterActivityDelegate localDt = delegate;
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            System.out.println("OnUserLeaveHint: " + nativePageId);
            localDt.onUserLeaveHint();
            System.out.println("OnPause: " + nativePageId);
            localDt.onPause();
        }
        // 如果 flutter 的 surface 已经 create 了，先 destroy
        FlutterView flutterView = getFlutterView();
        if (hasFlag(flag, FLAG_SURFACE_CREATED) || needDestroySurface) {
            flag &= ~FLAG_SURFACE_CREATED;
            needDestroySurface = false;
            System.out.println("OnSurfaceDestroy: " + nativePageId);
            FlutterStackManagerUtil.onSurfaceDestroyed(flutterView, flutterView.getFlutterNativeView());
        }
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            System.out.println("OnStop: " + nativePageId);
            localDt.onStop();
        }
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            // 这是设置 handler 为 null 防止内存泄漏
            flutterView.setMessageHandler("flutter/platform", null);
            System.out.println("OnDestroy: " + nativePageId);
            localDt.onDestroy();
        }
        delegate = null;
        if (FlutterStackManager.getInstance().getCurNativePage() == this) {
            FlutterStackManager.getInstance().setCurNativePage(null);
        }
    }

    /**
     * 私有保护区域
     */
    // 当前页面的状态
    private int flag = 0;
    private int statusBarColor = 1073741824;

    @Nullable
    private Drawable getLaunchScreenDrawableFromActivityTheme() {
        TypedValue typedValue = new TypedValue();
        if (!getTheme().resolveAttribute(
                android.R.attr.windowBackground,
                typedValue,
                true)) {
            return null;
        }
        if (typedValue.resourceId == 0) {
            return null;
        }
        try {
            return getResources().getDrawable(typedValue.resourceId);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean hasFlag(int flag, int target) {
        return (flag&target) == target;
    }

    private void updateParamFromUrl(@Nullable String url, @Nullable HashMap<String, Object> map) {
        if (url == null || map == null) {
            return;
        }
        Uri uri = Uri.parse(url);
        for (String key : uri.getQueryParameterNames()) {
            if (map.containsKey(key)) {
                continue;
            }
            map.put(key, uri.getQueryParameter(key));
        }
    }
}
