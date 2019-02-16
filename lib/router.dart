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
import 'package:flutter/material.dart';
import 'hybrid_plugin.dart';
import 'dart:ui';
import 'dart:async';

typedef VDPageWidgetBuilder = Widget Function(
    BuildContext context, VDRouteOptions options);

/// 混合栈路由管理类
class VDRouter {
  /// 获取当前 Router 的单例
  static VDRouter get instance {
    if (_singleton == null) {
      throw Exception('must call Router.init(key) first');
    }
    return _singleton;
  }

  /// 初始化逻辑
  /// [key] 是获取 navigator state 用的
  static VDRouter init({@required GlobalKey<NavigatorState> key}) {
    _singleton = VDRouter._internal(key);
    return _singleton;
  }

  static VDRouter _singleton;

  /// 页面丢失时候
  VDMaterialPageRoute pageNotFoundBuilder;

  /// 路由表
  Map<String, VDPageWidgetBuilder> get route => _routes;

  /// 主动获取初始 route 页面
  Future<Map> getInitRoute() {
    return HybridPlugin.instance.getInitRoute();
  }

  /// 启动初始页面，因为第一次启动的时候，channel 还未准备好，只能通过 dart 层
  /// 来主动获取
  Future<dynamic> startInitRoute() async {
    Map initRoute = await getInitRoute();
    if (initRoute != null && initRoute["pageName"] != null) {
      String pageName = initRoute["pageName"];
      Map args = initRoute["args"];
      String nativePageId = initRoute["nativePageId"];
      return await push(
        routerOptions: VDRouteOptions(
          pageName: pageName,
          args: args,
        ),
        nativePageId: nativePageId
      );
    }
  }

  /// 按了返回键, true 表述消耗了返回键动作
  bool onBackPressed() {
    if (_keyPageMap.length > 0) {
      /// 存在关键帧
      _navState?.pop();
      return true;
    }
    return false;
  }

  /// push 一个 flutter page
  /// [routerOptions] 路由参数
  Future<dynamic> push({VDRouteOptions routerOptions, String nativePageId}) async {
    assert(routerOptions != null);
    var pageName = routerOptions.pageName;
    var builder = _routes[pageName];
    if (builder == null) {
      builder =
          pageNotFoundBuilder ?? (context, options) => _RouteNotFoundPage();
    }
    final pageRoute = VDMaterialPageRoute(
        settings: RouteSettings(name: pageName),
        builder: (BuildContext context) {
          return builder(context, routerOptions);
        },
        nativePageId: nativePageId,
        options: routerOptions);
    
    /// 检查监听
    _checkPageObserver();
    /// update current key page
    _keyPageMap[pageRoute] = nativePageId;
    /// open the route
    final navState = _navState;
    navState.push(pageRoute);
  }

  /// 从 native 端打开一个页面
  Future<NativePageResult> openNativePage(
      {@required BuildContext context, @required String url, Map args}) async {
    /// 遇到一个难题，无法知道当前的 nativePageId
    for (int i = _navigatorHistory.length - 1; i >= 0; --i) {
      Route<dynamic> cr = _navigatorHistory[i];
      if (cr is VDMaterialPageRoute<dynamic>) {
        if (cr.nativePageId != null) {
          // has native page id
          return await HybridPlugin.instance.openNativePage(
              url: url, args: args, nativePageId: cr.nativePageId);
        }
      }
    }
    return NativePageResult(
      resultCode: -1,
    );
  }

  /// 检查 page observe 是否工作正常
  void _checkPageObserver() {
    if (observer == null && _navState != null) {
      observer = _FlutterPageObserver();
      _navState.widget.observers.add(observer);
    }
  }

  /// route 移除回调
  void _onRouteRemove(Route<dynamic> route) {
    String nativePageId = _keyPageMap[route];
    if (nativePageId != null) {
      /// 说明移除的是关键帧
      /// 往前遍历找到非关键帧作为关键帧，如果找不到，结束 native 页面
      for (int i = 0; i < _navigatorHistory.length; ++i) {
        Route<dynamic> cr = _navigatorHistory[i];
        if (cr == route) {
          if (i + 1 < _navigatorHistory.length) {
            Route<dynamic> nr = _navigatorHistory[i + 1];
            if (!(nr is VDMaterialPageRoute) ||
                (nr as VDMaterialPageRoute).nativePageId == null) {
              // found next key page
              _keyPageMap[nr] = nativePageId;
              return;
            }
          }
        }
        break;
      }

      /// 没有找到符合要求的额关键帧，结束 native 页面
      HybridPlugin.instance.finishNativePage(nativePageId);
    }
  }

  /// route replace 回调
  void _onRouteReplace(Route<dynamic> oldRoute, Route<dynamic> newRoute) {
    String nativePageId = _keyPageMap[oldRoute];
    if (nativePageId != null) {
      /// 当前 route 是关键帧
      _keyPageMap.remove(oldRoute);
      _keyPageMap[newRoute] = nativePageId;
    }
  }

  /// route pop 回调
  void _onRoutePop(Route<dynamic> route) {
    String nativePageId = _keyPageMap[route];
    if (nativePageId != null) {
      /// 当前 route 是关键帧
      _keyPageMap.remove(route);
      HybridPlugin.instance.finishNativePage(nativePageId, route.currentResult);
    }
  }

  VDRouter._internal(GlobalKey<NavigatorState> key) {
    _navigatorStateKey = key;
  }

  /// flutter 页面的导航器 NavigatorState
  GlobalKey<NavigatorState> _navigatorStateKey;
  NavigatorState get _navState => _navigatorStateKey?.currentState;

  /// flutter navigator observer
  _FlutterPageObserver observer;

  /// Navigator 内部的 history
  final List<Route<dynamic>> _navigatorHistory = <Route<dynamic>>[];

  /// 关键帧 map
  final Map<Route<dynamic>, String> _keyPageMap = {};

  Map<String, VDPageWidgetBuilder> _routes = Map();
}

/// 从 native 路由过来的参数
class VDRouteOptions {
  /// 页面参数
  final Map args;

  /// 获取url中的页面名
  final String pageName;

  VDRouteOptions({@required this.pageName, this.args});
}

/// 页面栈监听器
class _FlutterPageObserver extends NavigatorObserver {
  @override
  void didPush(Route route, Route previousRoute) {
    VDRouter.instance._navigatorHistory.add(route);
    super.didPush(route, previousRoute);
  }

  @override
  void didPop(Route route, Route previousRoute) {
    VDRouter.instance._navigatorHistory.remove(route);
    super.didPop(route, previousRoute);
    VDRouter.instance._onRoutePop(route);
    _repairFrameSchedule();
  }

  @override
  void didRemove(Route route, Route previousRoute) {
    VDRouter.instance._navigatorHistory.remove(route);
    super.didRemove(route, previousRoute);
    VDRouter.instance._onRouteRemove(route);
  }

  @override
  void didReplace({Route newRoute, Route oldRoute}) {
    int index = VDRouter.instance._navigatorHistory.indexOf(oldRoute);
    if (index >= 0) {
      VDRouter.instance._navigatorHistory.removeAt(index);
      VDRouter.instance._navigatorHistory.insert(index, newRoute);
    }
    super.didReplace(newRoute: newRoute, oldRoute: oldRoute);
    VDRouter.instance._onRouteReplace(oldRoute, newRoute);
  }

  _repairFrameSchedule() {
    /// need to repair onDrawFrame callback
    Future.delayed(Duration(microseconds: 0), () {
      window.onBeginFrame(null);
      window.onDrawFrame();
    });
  }
}

class VDMaterialPageRoute<T> extends MaterialPageRoute<T> {
  final VDRouteOptions options;
  final String nativePageId;

  VDMaterialPageRoute(
      {WidgetBuilder builder,
      this.options,
      this.nativePageId,
      RouteSettings settings = const RouteSettings()})
      :super(builder: builder, settings: settings);

  @override
  Duration get transitionDuration => 
    nativePageId != null ? Duration(milliseconds: 0) : super.transitionDuration;

  @override
  T get currentResult => _currentResult;

  @override
  bool didPop([T result]) {
    if (result != null) {
      /// 获取 result，缓存起来，最后给 channel 结果通知用
      _currentResult = result;
    }

    /// 这里修复下 duration 为 0 的时候，TransitionRoute 中 _handleStatusChanged
    /// 异常的问题
    if (overlayEntries.isNotEmpty) overlayEntries.first.opaque = false;
    return super.didPop(result);
  }

  dynamic _currentResult;
}

/// route 匹配失败显示的页面
class _RouteNotFoundPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Center(
        child: Text("页面丢失"),
      ),
    );
  }
}
