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
@property(nonatomic, readonly) UIView * parentView;
@property(nonatomic) FlutterEventChannel* eventChannel;
@property(nonatomic) FlutterEventSink eventSink;

- (instancetype)initWithURL:(NSURL*)url  frameUpdater:(FLTFrameUpdater*)frameUpdater;
- (void)dispose;

//+(VideoPlayer*)shareVideoPlayer;

@end



@implementation VideoPlayer
#define KIsiPhoneX ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1125, 2436), [[UIScreen mainScreen] currentMode].size) : NO)

static VideoPlayer  *sharevideoPlayer = nil;
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

- (instancetype)initWithURL:(NSURL*)url frameUpdater:(FLTFrameUpdater*)frameUpdater{
    self = [super init];
    float heights = KIsiPhoneX?UIScreen.mainScreen.bounds.size.height-100:UIScreen.mainScreen.bounds.size.height-76;
    CGRect rect = CGRectMake(0, 0, UIScreen.mainScreen.bounds.size.width, UIScreen.mainScreen.bounds.size.height);
    CGRect parentRect = CGRectMake(rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
    _parentView = [[UIView alloc] initWithFrame:parentRect];
    _parentView.clipsToBounds = true;
    
    
    CLPlayerView *playerView  = [[CLPlayerView alloc] initWithFrame:_parentView.bounds];
    _player = playerView;
    [self.parentView addSubview:_player];
    
    
    
    //全屏是否隐藏状态栏，默认一直不隐藏
    _player.fullStatusBarHiddenType = FullStatusBarHiddenFollowToolBar;
    //转子颜色
    _player.strokeColor = [UIColor orangeColor];
    //工具条消失时间，默认10s
    _player.toolBarDisappearTime = 8;
    //顶部工具条隐藏样式，默认不隐藏
    _player.topToolBarHiddenType = TopToolBarHiddenNever;
    _player.fullStatusBarHiddenType = FullStatusBarHiddenAlways;
    _player.videoFillMode = VideoFillModeResizeAspect;
    //视频地址
    _player.url = url;
    //播放
    [_player playVideo];
    //返回按钮点击事件回调,小屏状态才会调用，全屏默认变为小屏
    [_player backButton:^(UIButton *button) {
        NSLog(@"返回按钮被点击");
        [self dispose];
    }];
    //播放完成回调
    [_player endPlay:^{
        NSLog(@"播放完成");
    }];
    
    [self VideoView];
    
    return self;
}


- (void)dispose {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [_parentView removeFromSuperview];
    [_player destroyPlayer];
    [_eventChannel setStreamHandler:nil];
}

- (nonnull CVPixelBufferRef)copyPixelBuffer {
    return nil;
}

- (FlutterError * _Nullable)onCancelWithArguments:(id _Nullable)arguments {
    _eventSink = nil;
    return nil;
}

- (FlutterError * _Nullable)onListenWithArguments:(id _Nullable)arguments eventSink:(nonnull FlutterEventSink)events {
    _eventSink = events;
    return nil;
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
    
    VideoPlayer *player;
    
   if ([@"Init" isEqualToString:call.method]) {

       [_players removeAllObjects];
       
    NSDictionary* argsMap = call.arguments;
    NSString* dataSource = argsMap[@"url"];
       FLTFrameUpdater* frameUpdater = [[FLTFrameUpdater alloc] initWithRegistry:_registry];
       player = [[VideoPlayer alloc] initWithURL:[NSURL URLWithString:dataSource]  frameUpdater:frameUpdater];
       
       int64_t textureId = [_registry registerTexture:player];
       frameUpdater.textureId = textureId;
       
       
    result nil;
   }
//   else if ([@"Dispose" isEqualToString:call.method]){
//       plager.parentView.hidden = YES;
//       [plager.player pausePlay];
//       result nil;
//   }
}


@end
