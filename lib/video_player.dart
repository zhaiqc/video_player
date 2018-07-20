
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:meta/meta.dart';


final MethodChannel _channel = const MethodChannel('flutter.io/videoPlayer');
class VideoPlayerController  {
  static Future<Null> setInit(String url) => _channel.invokeMethod("Init", <String, dynamic>{'url': url});

  static Future<bool>  setPort() => _channel.invokeMethod("setPort");

  static Future<Null> setDispose() =>_channel.invokeMethod("Dispose");
}
