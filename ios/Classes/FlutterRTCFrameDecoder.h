//
//  FlutterRTCFrameDecoder.h
//  flutter_webrtc
//
//  Created by BigSaimo on 2024/7/10.
//

#if TARGET_OS_IPHONE
#import <Flutter/Flutter.h>
#elif TARGET_OS_OSX
#import <FlutterMacOS/FlutterMacOS.h>
#endif

#import <WebRTC/WebRTC.h>

#import "FlutterWebRTCPlugin.h"

@interface FlutterRTCFrameDecoder : NSObject <FlutterStreamHandler>
@property(nonatomic, strong, nullable) FlutterEventSink eventSink;
@property(nonatomic, strong, nullable) FlutterEventChannel* eventChannel;
@property(nonatomic, strong, nonnull) RTCVideoDecoderH264* h264Decoder;
@property(nonatomic, strong, nonnull) RTCVideoDecoderH265* h265Decoder;
@property(nonatomic, strong) NSMutableDictionary<NSNumber*, NSString*>* _Nullable targetDict;

- (void)cacheTarget:(NSNumber*)seq :(NSString*)target;

@end

@interface FlutterWebRTCPlugin (FrameDecoder)

- (void)decodeFrame:(nonnull FlutterMethodCall*)call result:(nonnull FlutterResult)result;

@end
