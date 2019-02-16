import 'package:flutter/material.dart';
import 'package:hybrid_stack_manager/hybrid_stack_manager.dart';

class ExamplePage extends StatefulWidget {

  final String title;

  ExamplePage({@required this.title});

  @override
  _ExamplePageState createState() => _ExamplePageState();
}

class _ExamplePageState extends State<ExamplePage> {

  String _retMessage;

  final Color color = _ColorMaker.getNextColor();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        elevation: 0,
        backgroundColor: color,
        title: Text(widget.title ?? "FlutterExample"),
        leading: IconButton(icon: Icon(Icons.arrow_back), onPressed: () {
          Navigator.of(context).pop("I am message from flutter example");
        },),
      ),
      body: Container(
        color: color,
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text('receive message: ', style: TextStyle(color: Colors.white),),
              Card(
                elevation: 10,
                child: Container(
                  padding: EdgeInsets.all(18.0),
                  constraints: BoxConstraints.expand(width: 200, height: 80),
                  child: Center(
                    child: Text(
                      _retMessage ?? "no message"
                    ),
                  ),
                ),
              ),
              RaisedButton(
                child: Text('Jump to flutter exmaple'),
                onPressed: () async {
                  String message = await Navigator.of(context).push(MaterialPageRoute(
                    builder: (context) => ExamplePage(title: 'Jump from flutter example',)
                  ));
                  if (mounted) {
                    setState(() {
                      _retMessage = message;
                    });
                  }
                },
              ),
              RaisedButton(child: Text('Jump to native'), onPressed: () {
                VDRouter.instance.openNativePage(url: "native://hybridstackmanager/example"
                , args: {"title": "Jump from flutter"});
              },)
            ],
          ),
        ),
      ),
    );
  }
}

class _ColorMaker {
  static int _currentIndex = 0;
  static List<Color> _colors = [
    Colors.red, Colors.orange, Colors.yellow[700], Colors.green,
    Colors.cyan, Colors.blue, Colors.purple
  ];

  static Color getNextColor() {
    Color ret = _colors[_currentIndex];
    _currentIndex = (_currentIndex + 1) % _colors.length;
    return ret;
  }
}