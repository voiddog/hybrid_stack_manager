///
/// ┏┛ ┻━━━━━┛ ┻┓
/// ┃　　　　　　 ┃
/// ┃　　　━　　　┃
/// ┃　┳┛　  ┗┳　┃
/// ┃　　　　　　 ┃
/// ┃　　　┻　　　┃
/// ┃　　　　　　 ┃
/// ┗━┓　　　┏━━━┛
/// * ┃　　　┃   神兽保佑
/// * ┃　　　┃   代码无BUG！
/// * ┃　　　┗━━━━━━━━━┓
/// * ┃　　　　　　　    ┣┓
/// * ┃　　　　         ┏┛
/// * ┗━┓ ┓ ┏━━━┳ ┓ ┏━┛
/// * * ┃ ┫ ┫   ┃ ┫ ┫
/// * * ┗━┻━┛   ┗━┻━┛
/// @author qigengxin
/// @since 2019-01-11 17:37

import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'router.dart';

/// the data struct that native page return
class NativePageResult {
  /// the code that android platform return
  final int resultCode;

  /// the data that native page return
  final dynamic data;

  NativePageResult({this.resultCode, this.data});
}

class HybridPlugin {
  static HybridPlugin get instance {
    if (_singleton == null) {
      _singleton = HybridPlugin._internal();
    }
    return _singleton;
  }

  static HybridPlugin _singleton;

  /// 从 native 端打开一个页面
  /// [url] 页面路由
  /// [args] 页面参数
  /// [nativePageId] 当前堆栈对应的关键帧所在的 native page id
  Future<NativePageResult> openNativePage(
      {String url, String nativePageId, Map args}) async {
    assert(url != null);
    assert(nativePageId != null);
    Map param = {"url": url, "nativePageId": nativePageId, "args": args ?? {}};
    Map result = await _channel.invokeMethod("openNativePage", param) ?? {};
    return NativePageResult(
        resultCode: result["resultCode"], data: result["data"]);
  }

  /// 结束 native 的页面
  /// [nativePageId] 当前 native 页面对应的 pageId
  Future<void> finishNativePage(String nativePageId, [dynamic result]) async {
    assert(nativePageId != null);
    await _channel.invokeMethod("finishNativePage", {
      "nativePageId": nativePageId,
      "result": result
    });
  }

  /// 获取初始路由参数
  Future<Map> getInitRoute() async {
    return await _channel.invokeMethod("getInitRoute");
  }

  /// 设置当前 native page 的状态栏颜色
  Future<void> setStatusBarColor(
      {@required String nativePageId, @required Color color}) async {
    assert(nativePageId != null);
    assert(color != null);
    return await _channel.invokeMethod("setStatusBarColor",
        {"nativePageId": nativePageId, "color": color.value});
  }

  _setupChannelHandler() {
    _channel.setMethodCallHandler((MethodCall call) async {
      /// method name
      String methodName = call.method;
      switch (methodName) {
        case "pushFlutterPage": {
          Map args = call.arguments;
          if (args != null) {
            await VDRouter.instance.push(
              routerOptions: VDRouteOptions(
                pageName: args["pageName"],
                args: args["args"],
                nativePageId: args["nativePageId"]
              )
            );
          }
          break;
        }
        case "requestUpdateTheme": {
          // 请求更新主题色到 native 端，这里使用了一个测试接口，以后要注意
          var preTheme = SystemChrome.latestStyle;
          if (preTheme != null) {
            SystemChannels.platform.invokeMethod("SystemChrome.setSystemUIOverlayStyle", _toMap(preTheme));
          }
          break;
        }
      }
    });
  }

  Map<String, dynamic> _toMap(SystemUiOverlayStyle style) {
    return <String, dynamic>{
      'systemNavigationBarColor': style.systemNavigationBarColor?.value,
      'systemNavigationBarDividerColor':
          style.systemNavigationBarDividerColor?.value,
      'statusBarColor': style.statusBarColor?.value,
      'statusBarBrightness': style.statusBarBrightness?.toString(),
      'statusBarIconBrightness': style.statusBarIconBrightness?.toString(),
      'systemNavigationBarIconBrightness':
          style.systemNavigationBarIconBrightness?.toString(),
    };
  }

  /// demo channel
  Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// channel
  MethodChannel _channel;

  /// private constructor
  HybridPlugin._internal() {
    this._channel = MethodChannel('hybrid_stack_manager');
    _setupChannelHandler();
  }
}
