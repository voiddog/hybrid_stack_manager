import 'package:flutter/material.dart';

import 'package:hybrid_stack_manager/hybrid_stack_manager.dart';
import 'example.dart';

void main() {
  GlobalKey<NavigatorState> navKey = GlobalKey<NavigatorState>();
  VDRouter.init(key: navKey);
  runApp(MaterialApp(
    navigatorKey: navKey,
    home: EmptyPage(),
  ));
  VDRouter.instance.route.addAll({
    "example": (context, options) {
      return ExamplePage(title: options?.args["title"]);
    }
  });
  VDRouter.instance.startInitRoute();
}

class EmptyPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
    );
  }
}