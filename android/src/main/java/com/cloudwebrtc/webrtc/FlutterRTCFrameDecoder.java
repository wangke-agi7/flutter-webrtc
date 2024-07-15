package com.cloudwebrtc.webrtc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.util.Log;

import com.cloudwebrtc.webrtc.utils.AnyThreadSink;
import com.cloudwebrtc.webrtc.utils.EglUtils;

import org.webrtc.EglBase;
import org.webrtc.EncodedImage;
import org.webrtc.GlRectDrawer;
import org.webrtc.GlTextureFrameBuffer;
import org.webrtc.GlUtil;
import org.webrtc.RendererCommon;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoder;
import org.webrtc.VideoDecoderFallback;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrameDrawer;
import org.webrtc.video.CustomVideoDecoderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;

public class FlutterRTCFrameDecoder implements  EventChannel.StreamHandler {

    static public final String TAG = "FlutterRTCFrameDecoder";
    FlutterRTCFrameDecoder(BinaryMessenger messenger) {
        eventChannel = new EventChannel(messenger, "FlutterWebRTC.frameDecoderEvent");
        eventChannel.setStreamHandler(this);
    }
    private final EventChannel eventChannel;
    private EventChannel.EventSink eventSink;

    private VideoDecoder h264Decoder;
    private VideoDecoder h265Decoder;

    HashMap<Integer, String> seqTargets = new HashMap<Integer, String>();

    public void onListen(Object o, EventChannel.EventSink sink) {
        eventSink = new AnyThreadSink(sink);
    }

    @Override
    public void onCancel(Object o) {
        eventSink = null;
    }

    public void initDecoder() {
        EglBase.Context eglContext = EglUtils.getRootEglBaseContext();
        CustomVideoDecoderFactory videoDecoderFactory = new CustomVideoDecoderFactory(eglContext);
        {
            Log.i(TAG, "create h264Decoder");
            VideoDecoder decoder = videoDecoderFactory.createDecoder(
                    new VideoCodecInfo("H264", new HashMap<>(), new LinkedList<>()));

            if (decoder.getClass().equals(VideoDecoderFallback.class)) {
                VideoDecoderFallback fallback = (VideoDecoderFallback) decoder;
                h264Decoder = fallback.fallback;
                h264Decoder.initDecode(new VideoDecoder.Settings(0, 1280, 720),
                        (videoFrame, integer, integer1) -> {
                    onDecodeFrame(videoFrame, "h264");
                });
            }
        }
        {
            Log.i(TAG, "create h265Decoder");
            VideoDecoder decoder = videoDecoderFactory.createDecoder(
                    new VideoCodecInfo("H265", new HashMap<>(), new LinkedList<>()));
            if (decoder.getClass().equals(VideoDecoderFallback.class)) {
                VideoDecoderFallback fallback = (VideoDecoderFallback) decoder;
                h265Decoder = fallback.fallback;
                h265Decoder.initDecode(new VideoDecoder.Settings(0, 1280, 720),
                        (videoFrame, integer, integer1) -> {
                    onDecodeFrame(videoFrame, "h265");
                });
            }
        }
    }

    public void decodeFrame(MethodCall call) {
        String sourceFile = call.argument("source");
        String targetFile = call.argument("target");
        String codec = call.argument("codec");
        int width = call.argument("width");
        int height = call.argument("height");
        int seq = call.argument("seq");
        try {
            final File source = new File(sourceFile);
            final FileInputStream fis = new FileInputStream(source);
            Log.i(TAG, "decodeFrame source length: " + fis.available() + ", seq: " + seq +
                    ", target: " + targetFile +
                    ", source: " + sourceFile);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            EncodedImage.Builder builder = EncodedImage.builder();
            builder.setBuffer(byteBuffer, null);
            builder.setEncodedWidth(width);
            builder.setEncodedHeight(height);
            builder.setCaptureTimeNs(seq*1000);
            builder.setFrameType(EncodedImage.FrameType.VideoFrameKey);
            builder.setRotation(0);
            builder.setQp(0);
            EncodedImage encodedImage = builder.createEncodedImage();

            seqTargets.put(seq, targetFile);

            if (codec.equals("h264")) {
                h264Decoder.decode(encodedImage, new VideoDecoder.DecodeInfo(false, seq));
            } else if (codec.equals("h265")) {
                h265Decoder.decode(encodedImage, new VideoDecoder.DecodeInfo(false, seq));
            } else {
                Log.e(TAG, "Unsupported Codec: " + codec);
            }


        } catch (Exception e) {
            Log.e(TAG, "decodeFrame failed:" + e);
        }
    }

    private Bitmap frameToBitmap(VideoFrame frame){
        final Matrix drawMatrix = new Matrix();
        // Used for bitmap capturing.
        final GlTextureFrameBuffer bitmapTextureFramebuffer =
                new GlTextureFrameBuffer(GLES20.GL_RGBA);
        drawMatrix.reset();
        drawMatrix.preTranslate(0.5f, 0.5f);
        //控制图片的方向
        drawMatrix.preScale( -1f ,  -1f);
        drawMatrix.preScale(-1f, 1f); // We want the output to be upside down for Bitmap.
        drawMatrix.preTranslate(-0.5f, -0.5f);

        final int scaledWidth = (int) (1 * frame.getRotatedWidth());
        final int scaledHeight = (int) (1 * frame.getRotatedHeight());
        bitmapTextureFramebuffer.setSize(scaledWidth, scaledHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bitmapTextureFramebuffer.getFrameBufferId());
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, bitmapTextureFramebuffer.getTextureId(), 0);

        GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        VideoFrameDrawer frameDrawer = new VideoFrameDrawer();
        RendererCommon.GlDrawer drawer = new GlRectDrawer();
        frameDrawer.drawFrame(frame, drawer, drawMatrix, 0 /* viewportX */,
                0 /* viewportY */, scaledWidth, scaledHeight);

        final ByteBuffer bitmapBuffer = ByteBuffer.allocateDirect(scaledWidth * scaledHeight * 4);
        GLES20.glViewport(0, 0, scaledWidth, scaledHeight);
        GLES20.glReadPixels(
                0, 0, scaledWidth, scaledHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.checkNoGLES2Error("EglRenderer.notifyCallbacks");

        final Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(bitmapBuffer);


        return bitmap;
    }

    private void onDecodeFrame(VideoFrame videoFrame, String codec) {
        int seq = (int) (videoFrame.getTimestampNs() / 1000);
        Bitmap bitmap = frameToBitmap(videoFrame);
        if (bitmap == null) {
            Log.e(TAG, "onDecodeFrame[" + codec + "] can't create bitmap ");
            return;
        }
        String targetFile = seqTargets.get(seq);
        if (targetFile == null || targetFile.equals("")) {
            Log.e(TAG, "onDecodeFrame[" + codec + "] can't find targetFile seq: " + seq);
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(targetFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Log.i(TAG, "onDecodeFrame[" + codec + "] write file: " + targetFile);
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("seq", seq);
            params.put("target", targetFile);
            eventSink.success(params);
        } catch ( Exception e) {
            Log.e(TAG, "decodeFrame[" + codec + "] write to file err: " + e + ", targetFile: " + targetFile);
        }
    }

}
