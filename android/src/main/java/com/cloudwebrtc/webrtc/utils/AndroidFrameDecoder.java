package com.cloudwebrtc.webrtc.utils;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.util.Size;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class AndroidFrameDecoder {

    private static final String TAG = "AndroidFrameDecoder";

    public static synchronized Size decode(InputStream is, long length, int width, int height)
            throws Exception {
        Size ret = new Size(width, height);
        MediaFormat format =
                MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);

        MediaCodec codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);

        codec.configure(format, null, null, 0);

        Log.d(TAG, "outputFormat: " + codec.getOutputFormat());

        codec.start();

        long timeoutUs = 1000000;

        for (; ; ) {
            int inputBufferId = codec.dequeueInputBuffer(timeoutUs);

            Log.d(TAG, "inputBufferId: " + inputBufferId);

            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferId);
                if (inputBuffer == null) {
                    Log.e(TAG, "inputBuffer is null");
                    continue;
                }

                byte[] buffer = new byte[(int) length];
                int read;
                if ((read = is.read(buffer)) != -1) {
                    Log.d(TAG, "Data put into buffer: " + read + " bytes" + ", capacity: " + inputBuffer.capacity());
                    inputBuffer.put(buffer, 0, read);

                    codec.queueInputBuffer(inputBufferId, 0, read, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    Log.d(TAG, "Reached the end");
                }
                break;
            }
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        for (; ; ) {
            int decoderStatus = codec.dequeueOutputBuffer(info, timeoutUs);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                Log.d(TAG, "no output from decoder available");
            }  else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = codec.getOutputFormat();
                Log.d(TAG, "decoder  output format changed: " + newFormat);
                ret = new Size(newFormat.getInteger(MediaFormat.KEY_WIDTH), newFormat.getInteger(MediaFormat.KEY_HEIGHT));
            } else if (decoderStatus < 0) {
                Log.e(TAG, "unknown decoder status: " + decoderStatus);
                //                        fail("unexpected result from decoder.dequeueOutputBuffer: " +
                // decoderStatus);
            } else { // decoderStatus >= 0
                Log.d(TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");
                break;
//                boolean doRender = (info.size != 0);
//
//                // As soon as we call releaseOutputBuffer, the buffer will be forwarded
//                // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
//                // that the texture will be available before the call returns, so we
//                // need to wait for the onFrameAvailable callback to fire.
//                codec.releaseOutputBuffer(decoderStatus, doRender);
//                if (doRender) {
//                    Log.d(TAG, "awaiting decode of frame ");
//                    outputSurface.awaitNewImage();
//                    outputSurface.drawImage();
//
//                    bitmap = outputSurface.saveFrame();
//                    break;
//                }
//
//                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                    //                    Log.d(TAG,"output EOS");
//                    break;
//                }
            }
        }

        codec.stop();
        codec.release();

        return ret;
    }
}
