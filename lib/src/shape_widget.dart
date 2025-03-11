import 'package:flutter/widgets.dart';
import 'package:wear_plus/src/wear.dart';

/// Shape of a Wear device
enum WearShape {
  /// The display is square
  square,

  /// The display is round
  round,
}

/// Builds a child for a [WatchShape]
typedef WatchShapeBuilder = Widget Function(
  BuildContext context,
  WearShape shape,
  Widget? child,
);

/// Builder widget for watch shapes
@immutable
class WatchShape extends StatefulWidget {
  /// Constructor
  const WatchShape({
    super.key,
    required this.builder,
    this.child,
  });

  /// Built when the shape changes
  final WatchShapeBuilder builder;

  /// Optional child that will not get rebuilt when the shape changes
  final Widget? child;

  /// Call [WatchShape.of(context)] to retrieve the shape further down
  /// in the widget hierarchy.
  static WearShape of(BuildContext context) {
    return _InheritedShape.of(context).shape;
  }

  @override
  State<StatefulWidget> createState() => _WatchShapeState();
}

class _WatchShapeState extends State<WatchShape> {
  // Default to round until the platform returns the shape
  // round being the most common form factor for WearOS
  var _shape = WearShape.round;

  @override
  void initState() {
    super.initState();
    _initState();
  }

  void _initState() async {
    final shape = await Wear.instance.getShape();
    if (!mounted) return;
    setState(
      () => _shape = (shape == 'round' ? WearShape.round : WearShape.square),
    );
  }

  @override
  Widget build(BuildContext context) {
    return _InheritedShape(
      shape: _shape,
      child: Builder(
        builder: (context) {
          return widget.builder(context, _shape, widget.child);
        },
      ),
    );
  }
}

class _InheritedShape extends InheritedWidget {
  /// Constructor
  const _InheritedShape({
    required this.shape,
    required super.child,
  });

  final WearShape shape;

  static _InheritedShape of(BuildContext context) {
    return context.dependOnInheritedWidgetOfExactType<_InheritedShape>()!;
  }

  @override
  bool updateShouldNotify(_InheritedShape oldWidget) =>
      shape != oldWidget.shape;
}
