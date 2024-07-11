import 'dart:async';

import 'package:flutter/services.dart';

class FlutterWebRTCEventChannel {
  FlutterWebRTCEventChannel._internal() {
    EventChannel('FlutterWebRTC.Event')
        .receiveBroadcastStream()
        .listen(eventListener, onError: errorListener);

    EventChannel("FlutterWebRTC.frameDecoderEvent")
        .receiveBroadcastStream()
        .listen(frameDecoderEventListener, onError: frameDecoderErrorListener);
  }

  static final FlutterWebRTCEventChannel instance = FlutterWebRTCEventChannel._internal();

  final StreamController<Map<String, dynamic>> handleEvents = StreamController.broadcast();

  final StreamController<Map<String, dynamic>> frameDecoderHandleEvents =
      StreamController.broadcast();

  void eventListener(dynamic event) async {
    final Map<dynamic, dynamic> map = event;
    handleEvents.add(<String, dynamic>{map['event'] as String: map});
  }

  void errorListener(Object obj) {
    if (obj is Exception) {
      throw obj;
    }
  }

  void frameDecoderEventListener(dynamic event) async {
    final Map<dynamic, dynamic> map = event;
    frameDecoderHandleEvents.add(Map<String, dynamic>.from(map));
  }

  void frameDecoderErrorListener(Object obj) {
    if (obj is Exception) {
      throw obj;
    }
  }
}
