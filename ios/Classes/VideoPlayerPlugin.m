// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "VideoPlayerPlugin.h"
#import "CLPlayerView.h"

int64_t FLTCMTimeToMillis(CMTime time) { return time.value * 1000 / time.timescale; }

@interface FLTFrameUpdater : NSObject
@property(nonatomic) int64_t textureId;
@property(nonatomic, readonly) NSObject<FlutterTextureRegistry>* registry;
- (void)onDisplayLink:(CADisplayLink*)link;
@end

@implementation FLTFrameUpdater
- (FLTFrameUpdater*)initWithRegistry:(NSObject<FlutterTextureRegistry>*)registry {
  NSAssert(self, @"super init cannot be nil");
  if (self == nil) return nil;
  _registry = registry;
  return self;
}

- (void)onDisplayLink:(CADisplayLink*)link {
  [_registry textureFrameAvailable:_textureId];
}
@end

@interface VideoPlayer : NSObject<FlutterTexture, FlutterStreamHandler>
@property(readonly, nonatomic) CLPlayerView* player;
@property(readonly, nonatomic) CADisplayLink* displayLink;
@property(nonatomic) FlutterEventChannel* eventChannel;
@property(nonatomic) FlutterEventSink eventSink;
@property(nonatomic, readonly) bool disposed;
@property(nonatomic, readonly) bool isPlaying;
@property(nonatomic, readonly) bool isLooping;
@property(nonatomic, readonly) bool isInitialized;
@property(nonatomic, readonly) UIView * parentView;
- (instancetype)initWithURL:(NSURL*)url isHorizontal:(BOOL)isHorizontal widthPercentage:(double)widthPercentage heightPercentage:(double)heightPercentage frameUpdater:(FLTFrameUpdater*)frameUpdater;
- (void)play;
- (void)pause;
- (void)setIsLooping:(bool)isLooping;
- (void)setIsBackLayer:(bool)isBackLayer;
- (void)updatePlayingState;
- (void)reload;
@end



@implementation VideoPlayer
#define KIsiPhoneX ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1125, 2436), [[UIScreen mainScreen] currentMode].size) : NO)

- (void)VideoView {
    UIApplication.sharedApplication.keyWindow.subviews.firstObject.backgroundColor = UIColor.clearColor;
    [UIApplication.sharedApplication.keyWindow addSubview:_parentView];
    dispatch_time_t timer = dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC);
    dispatch_after(timer, dispatch_get_main_queue(), ^{
        for (UIView * item in UIApplication.sharedApplication.keyWindow.subviews) {
            [self setPlayerView:item];
        }
    });
}

- (void)setPlayerView:(UIView *)v {
    if ([v conformsToProtocol:@protocol(FLTMainView)]) {
        [v addSubview:_player];
    }
    
    for (UIView * item in v.subviews) {
        [self setPlayerView:item];
    }
}



- (instancetype)initWithURL:(NSURL*)url isHorizontal:(BOOL)isHorizontal widthPercentage:(double)widthPercentage heightPercentage:(double)heightPercentage frameUpdater:(FLTFrameUpdater*)frameUpdater{
  self = [super init];
    double wph = 750 / 453.56;
    double scale = 1.5;
    CGRect rect = CGRectMake(0, isHorizontal?0:KIsiPhoneX?100:56, UIScreen.mainScreen.bounds.size.width*widthPercentage, UIScreen.mainScreen.bounds.size.height * heightPercentage);
    CGRect parentRect = CGRectMake(rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
    _parentView = [[UIView alloc] initWithFrame:parentRect];
    _parentView.clipsToBounds = true;
  _isInitialized = false;
  _isPlaying = false;
  _disposed = false;

    CLPlayerView *playerView  = [[CLPlayerView alloc] initWithFrame:CGRectMake(0, 90, 200, 300)];
    _player = playerView;
    [self.parentView addSubview:_player];
    
    
    if (rect.size.width / rect.size.height > wph  || isHorizontal == false)  {
        double height = rect.size.width / wph;
        _player.frame = CGRectMake(0, - height * 0 , rect.size.width, height);
    }
    else {
        _player.frame = CGRectMake(rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
    }
    
    //全屏是否隐藏状态栏，默认一直不隐藏
    _player.fullStatusBarHiddenType = FullStatusBarHiddenFollowToolBar;
    //转子颜色
    _player.strokeColor = [UIColor redColor];
    //工具条消失时间，默认10s
    _player.toolBarDisappearTime = 8;
    //顶部工具条隐藏样式，默认不隐藏
    _player.topToolBarHiddenType = TopToolBarHiddenSmall;
    //视频地址
    _player.url = url;
    //播放
    [_player playVideo];
    //返回按钮点击事件回调,小屏状态才会调用，全屏默认变为小屏
    [_player backButton:^(UIButton *button) {
        NSLog(@"返回按钮被点击");
    }];
    //播放完成回调
    [_player endPlay:^{
        NSLog(@"播放完成");
    }];
    
    [self VideoView];
   
  return self;
}

- (void)observeValueForKeyPath:(NSString*)path
                      ofObject:(id)object
                        change:(NSDictionary*)change
                       context:(void*)context {
 
}

- (void)reload {
    [_player playVideo];
}

- (void)updatePlayingState {
  if (!_isInitialized) {
    return;
  }
  if (_isPlaying) {
    [_player playVideo];
  } else {
    [_player destroyPlayer];
  }
  _displayLink.paused = !_isPlaying;
}

- (void)sendInitialized {
  if (_eventSink && _isInitialized) {
      CGSize size = self.player.intrinsicContentSize;
    _eventSink(@{
      @"event" : @"initialized",
      @"duration" : @([self duration]),
      @"width" : @(size.width),
      @"height" : @(size.height),
    });
  }
}

- (void)play {
  _isPlaying = true;
  [self updatePlayingState];
}

- (void)pause {
  _isPlaying = false;
  [self updatePlayingState];
}

- (int64_t)position {
//  return FLTCMTimeToMillis([_player currentTime]);
    return 100;
}

- (int64_t)duration {
//  return FLTCMTimeToMillis([[_player currentItem] duration]);
//    return self.player.duration;
    return 0;
}

- (void)seekTo:(int)location {
//  [_player seekToTime:CMTimeMake(location, 1000)];
//    self.player
}

- (void)setIsLooping:(bool)isLooping {
  _isLooping = isLooping;
}

- (void)setIsBackLayer:(bool)isBackLayer {
    if (_player) {
        if (isBackLayer) {
            [UIApplication.sharedApplication.keyWindow sendSubviewToBack:_player];
            
        }
        else {
            [UIApplication.sharedApplication.keyWindow bringSubviewToFront:_player];
        }
    }
}

- (void)setVolume:(double)volume {
//  _player.volume = (volume < 0.0) ? 0.0 : ((volume > 1.0) ? 1.0 : volume);
}


- (FlutterError* _Nullable)onCancelWithArguments:(id _Nullable)arguments {
  _eventSink = nil;
  return nil;
}

- (FlutterError* _Nullable)onListenWithArguments:(id _Nullable)arguments
                                       eventSink:(nonnull FlutterEventSink)events {
  _eventSink = events;
  [self sendInitialized];
  return nil;
}

- (void)dispose {
  _disposed = true;
  [_displayLink invalidate];
  [[NSNotificationCenter defaultCenter] removeObserver:self];
      _player.frame = CGRectMake(0, 0, 0, 0);
    [_parentView removeFromSuperview];
    [_player removeFromSuperview];
    _player = nil;
    
    
    [_player destroyPlayer];
  [_eventChannel setStreamHandler:nil];
}

@end

@interface VideoPlayerPlugin ()
@property(readonly, nonatomic) NSObject<FlutterTextureRegistry>* registry;
@property(readonly, nonatomic) NSObject<FlutterBinaryMessenger>* messenger;
@property(readonly, nonatomic) NSMutableDictionary* players;
@end

@implementation VideoPlayerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel =
      [FlutterMethodChannel methodChannelWithName:@"flutter.io/videoPlayer"
                                  binaryMessenger:[registrar messenger]];
  VideoPlayerPlugin* instance =
      [[VideoPlayerPlugin alloc] initWithRegistry:[registrar textures]
                                           messenger:[registrar messenger]];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (instancetype)initWithRegistry:(NSObject<FlutterTextureRegistry>*)registry
                       messenger:(NSObject<FlutterBinaryMessenger>*)messenger {
  self = [super init];
  NSAssert(self, @"super init cannot be nil");
  _registry = registry;
  _messenger = messenger;
  _players = [NSMutableDictionary dictionaryWithCapacity:1];
  return self;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"init" isEqualToString:call.method]) {
    for (NSNumber* textureId in _players) {
      [_registry unregisterTexture:[textureId unsignedIntegerValue]];
      [[_players objectForKey:textureId] dispose];
    }
    [_players removeAllObjects];
  } else if ([@"create" isEqualToString:call.method]) {
    NSDictionary* argsMap = call.arguments;
    NSString* dataSource = argsMap[@"dataSource"];
      BOOL isHorizontal = [argsMap[@"isHorizontal"] boolValue];
      double widthPercentage = [argsMap[@"widthPercentage"] doubleValue];
      double heightPercentage = [argsMap[@"heightPercentage"] doubleValue];
    FLTFrameUpdater* frameUpdater = [[FLTFrameUpdater alloc] initWithRegistry:_registry];
      VideoPlayer* player = [[VideoPlayer alloc] initWithURL:[NSURL URLWithString:dataSource] isHorizontal:isHorizontal widthPercentage:widthPercentage heightPercentage:heightPercentage frameUpdater:frameUpdater];
    int64_t textureId = [_registry registerTexture:player];
    frameUpdater.textureId = textureId;
    FlutterEventChannel* eventChannel = [FlutterEventChannel
        eventChannelWithName:[NSString stringWithFormat:@"flutter.io/videoPlayer/videoEvents%lld",
                                                        textureId]
             binaryMessenger:_messenger];
    [eventChannel setStreamHandler:player];
    player.eventChannel = eventChannel;
      //[player setIsBackLayer:true];
    _players[@(textureId)] = player;

    result(@{ @"textureId" : @(textureId) });
  } else {
      NSDictionary* argsMap = call.arguments;
    if ([@"setBackLayer" isEqualToString:call.method]) {
    
        result(nil);
    }
    
    int64_t textureId = ((NSNumber*)argsMap[@"textureId"]).unsignedIntegerValue;
    VideoPlayer* player = _players[@(textureId)];
    if ([@"dispose" isEqualToString:call.method]) {
      [_registry unregisterTexture:textureId];
      [_players removeObjectForKey:@(textureId)];
      [player dispose];
    } else if ([@"setLooping" isEqualToString:call.method]) {
      [player setIsLooping:[argsMap objectForKey:@"looping"]];
      result(nil);
    } else if ([@"setVolume" isEqualToString:call.method]) {
      [player setVolume:[[argsMap objectForKey:@"volume"] doubleValue]];
      result(nil);
    } else if ([@"play" isEqualToString:call.method]) {
      [player play];
      result(nil);
    } else if ([@"position" isEqualToString:call.method]) {
      result(@([player position]));
    } else if ([@"seekTo" isEqualToString:call.method]) {
      [player seekTo:[[argsMap objectForKey:@"location"] intValue]];
      result(nil);
    } else if ([@"pause" isEqualToString:call.method]) {
      [player pause];
      result(nil);
    } else if ([@"reload" isEqualToString:call.method]) {
        [player reload];
        result(nil);
    } else {
      result(FlutterMethodNotImplemented);
    }
  }
}


@end
