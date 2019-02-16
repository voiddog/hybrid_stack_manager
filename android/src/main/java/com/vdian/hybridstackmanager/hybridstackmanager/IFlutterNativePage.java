package com.vdian.hybridstackmanager.hybridstackmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

import io.flutter.plugin.common.MethodChannel;

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
 * @since 2019/2/5 2:13 PM
 */
public interface IFlutterNativePage {
    /**
     * 是否 attach 到了 flutter engine
     */
    boolean isAttachToFlutter();

    /**
     * request attach to flutter
     */
    void attachFlutter();

    /**
     * request detach from flutter
     */
    void detachFlutter();

    /**
     * 请求打开 native page
     */
    void openNativePage(@NonNull String url, @Nullable Map<String, Object> args,
                        @NonNull MethodChannel.Result result);

    /**
     * flutter 请求结束 native 页面
     */
    void finishNativePage(@Nullable Object result);

    /**
     * 获取 native page id，传给 flutter 绑定关键用
     */
    String getNativePageId();

    /**
     * 获取初始路由
     */
    Map<String, Object> getInitRoute();

    /**
     * 通过一个 channel 生成一个 result code
     * @param result
     * @return
     */
    int generateRequestCodeByChannel(MethodChannel.Result result);

    /**
     * 获取 android 上下文环境
     * @return
     */
    Context getContext();

    /**
     * 更新状态栏颜色
     * @param color
     */
    void updateStatusBarColor(int color);
}
