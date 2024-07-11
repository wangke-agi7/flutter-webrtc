//
//  FlutterRTCFrameDecoder.m
//  flutter_webrtc
//
//  Created by BigSaimo on 2024/7/10.
//

#import "FlutterRTCFrameDecoder.h"
#import "WebRTC/RTCVideoDecoderH264.h"
#import "WebRTC/RTCVideoDecoderH265.h"
#import <objc/runtime.h>

@implementation FlutterRTCFrameDecoder

- (instancetype)init {
    _targetDict = [NSMutableDictionary new];
    _h264Decoder = [[RTCVideoDecoderH264 alloc] init];
    _h265Decoder = [[RTCVideoDecoderH265 alloc] init];
    __weak FlutterRTCFrameDecoder* weakSelf = self;
    [_h264Decoder setCallback:^(RTCVideoFrame * _Nonnull frame) {
        
        NSNumber* seq = [NSNumber numberWithInt:frame.timeStamp];
        NSString* target = [weakSelf.targetDict objectForKey:seq];
        if ([target isEqualToString:@""]) {
            return;
        }
        
        if ([frame.buffer isKindOfClass:[RTCCVPixelBuffer class]]) {
            RTCCVPixelBuffer *buffer = (RTCCVPixelBuffer *)frame.buffer;
            
            CIImage* ciimage = [CIImage imageWithCVPixelBuffer:buffer.pixelBuffer];
            UIImage* uiimage = [UIImage imageWithCIImage:ciimage];
            
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                [UIImageJPEGRepresentation(uiimage, 1.0) writeToFile:target atomically:YES];
                NSLog(@"DecodeFrame[h264] save to: %@", target);
                dispatch_async(dispatch_get_main_queue(), ^{
                    weakSelf.eventSink(@{@"seq": seq, @"target": target});
                });
            });
        }
    }];
    [_h265Decoder setCallback:^(RTCVideoFrame * _Nonnull frame) {
        
        NSNumber* seq = [NSNumber numberWithInt:frame.timeStamp];
        NSString* target = [weakSelf.targetDict objectForKey:seq];
        if ([target isEqualToString:@""]) {
            return;
        }
        
        if ([frame.buffer isKindOfClass:[RTCCVPixelBuffer class]]) {
            RTCCVPixelBuffer *buffer = (RTCCVPixelBuffer *)frame.buffer;
            
            CIImage* ciimage = [CIImage imageWithCVPixelBuffer:buffer.pixelBuffer];
            UIImage* uiimage = [UIImage imageWithCIImage:ciimage];
            
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                [UIImageJPEGRepresentation(uiimage, 1.0) writeToFile:target atomically:YES];
                NSLog(@"DecodeFrame[h265] save to: %@", target);
                dispatch_async(dispatch_get_main_queue(), ^{
                    weakSelf.eventSink(@{@"seq": seq, @"target": target});
                });
            });
        }
        
    }];
    return [super init];
}

- (FlutterEventSink)eventSink {
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setEventSink:(FlutterEventSink)eventSink {
  objc_setAssociatedObject(self, @selector(eventSink), eventSink,
                           OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (FlutterEventChannel*)eventChannel {
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setEventChannel:(FlutterEventChannel*)eventChannel {
  objc_setAssociatedObject(self, @selector(eventChannel), eventChannel,
                           OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (void)cacheTarget:(NSNumber*)seq :(NSString*)target {
    _targetDict[seq] = target;
}

#pragma mark - FlutterStreamHandler methods

- (FlutterError* _Nullable)onCancelWithArguments:(id _Nullable)arguments {
  self.eventSink = nil;
  return nil;
}

- (FlutterError* _Nullable)onListenWithArguments:(id _Nullable)arguments
                                       eventSink:(nonnull FlutterEventSink)sink {
  self.eventSink = sink;
  return nil;
}

@end

@implementation FlutterWebRTCPlugin (FrameDecoder)

- (void)decodeFrame:(nonnull NSDictionary*)constraints result:(nonnull FlutterResult)result {
    NSString* codec = constraints[@"codec"];
    NSString* source = constraints[@"source"];
    NSString* target = constraints[@"target"];
    NSNumber* width = constraints[@"width"];
    NSNumber* height = constraints[@"height"];
    NSNumber* seq = constraints[@"seq"];
    
    NSLog(@"decodeFrame seq: %@", seq);
    
    NSData *data = [[NSFileManager defaultManager] contentsAtPath:source];
    if (data == nil) {
        NSLog(@"decodeFrame Can't read file");
        return;
    }
    
    NSDate* now = [NSDate date];
    
    RTCEncodedImage* frame = [[RTCEncodedImage alloc] init];
    frame.buffer = data;
    frame.encodedWidth = width.intValue;
    frame.encodedHeight = height.intValue;
    frame.frameType = RTCFrameTypeVideoFrameKey;
    frame.captureTimeMs = [now timeIntervalSince1970]*1000;
    frame.timeStamp = seq.intValue;
    frame.rotation = RTCVideoRotation_0;
    
    RTCCodecSpecificInfoH264* codecSpecificInfo = [[RTCCodecSpecificInfoH264 alloc] init];
    
    if ([@"h264" isEqualToString:codec]) {
        [self.frameDecoder cacheTarget:seq :target];
        [self.frameDecoder.h264Decoder decode:frame missingFrames:NO codecSpecificInfo: codecSpecificInfo renderTimeMs:[now timeIntervalSince1970]*1000];
    } else if ([@"h265" isEqualToString:codec]) {
        [self.frameDecoder cacheTarget:seq :target];
        [self.frameDecoder.h265Decoder decode:frame missingFrames:NO codecSpecificInfo: codecSpecificInfo renderTimeMs:[now timeIntervalSince1970]*1000];
    }
}


@end

