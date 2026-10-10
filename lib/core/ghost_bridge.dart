import 'package:flutter/services.dart';

/// Android owns execution and audio. Flutter is an interchangeable interface.
class GhostBridge {
  static const _methods = MethodChannel('ghost/methods');
  static const _events = EventChannel('ghost/events');

  Stream<Map<String, dynamic>> get snapshots => _events
      .receiveBroadcastStream()
      .map((event) => Map<String, dynamic>.from(event as Map));

  Future<void> invoke(String method, [Map<String, Object?>? arguments]) async {
    await _methods.invokeMethod<Object?>(method, arguments);
  }
}
