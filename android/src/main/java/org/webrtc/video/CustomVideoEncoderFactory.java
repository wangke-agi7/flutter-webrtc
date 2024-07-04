package org.webrtc.video;

import androidx.annotation.Nullable;

import com.cloudwebrtc.webrtc.SimulcastVideoEncoderFactoryWrapper;

import org.webrtc.EglBase;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CustomVideoEncoderFactory implements VideoEncoderFactory {
    private SoftwareVideoEncoderFactory softwareVideoEncoderFactory = new SoftwareVideoEncoderFactory();
    private SimulcastVideoEncoderFactoryWrapper simulcastVideoEncoderFactoryWrapper;

    private boolean forceSWCodec  = false;

    private List<String> forceSWCodecs = new ArrayList<>();

    private VideoCodecInfo[] fakedCodecs = new VideoCodecInfo[0];

    public CustomVideoEncoderFactory(EglBase.Context sharedContext,
                                     boolean enableIntelVp8Encoder,
                                     boolean enableH264HighProfile) {
        this.simulcastVideoEncoderFactoryWrapper = new SimulcastVideoEncoderFactoryWrapper(sharedContext, enableIntelVp8Encoder, enableH264HighProfile);
    }

    public void setForceSWCodec(boolean forceSWCodec) {
        this.forceSWCodec = forceSWCodec;
    }

    public void setForceSWCodecList(List<String> forceSWCodecs) {
        this.forceSWCodecs = forceSWCodecs;
    }

    @Nullable
    @Override
    public VideoEncoder createEncoder(VideoCodecInfo videoCodecInfo) {
        if(forceSWCodec) {
            return softwareVideoEncoderFactory.createEncoder(videoCodecInfo);
        }

        if(!forceSWCodecs.isEmpty()) {
            if(forceSWCodecs.contains(videoCodecInfo.name)) {
                return softwareVideoEncoderFactory.createEncoder(videoCodecInfo);
            }
        }

        return simulcastVideoEncoderFactoryWrapper.createEncoder(videoCodecInfo);
    }

    private VideoCodecInfo[] doGetSupportedCodecs() {
        if(forceSWCodec && forceSWCodecs.isEmpty()) {
            return softwareVideoEncoderFactory.getSupportedCodecs();
        }
        return simulcastVideoEncoderFactoryWrapper.getSupportedCodecs();
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        VideoCodecInfo[] codecs = doGetSupportedCodecs();
        List<VideoCodecInfo> addingCodecs = new ArrayList();
        for (VideoCodecInfo codec : fakedCodecs) {
            if (findCodec(codecs, codec) == null) {
                addingCodecs.add(codec);
            }
          }
        if (addingCodecs.size() > 0) {
            List<VideoCodecInfo> moreCodecs = new ArrayList(codecs.length + addingCodecs.size());
            Collections.addAll(moreCodecs, codecs);
            moreCodecs.addAll(addingCodecs);
            codecs = moreCodecs.toArray(new VideoCodecInfo[moreCodecs.size()]);
        }
        return codecs;
    }

    private VideoCodecInfo findCodec(VideoCodecInfo[] videoCodecInfos, VideoCodecInfo videoCodec) {
        for (VideoCodecInfo codec : videoCodecInfos) {
          if (codec.equals(videoCodec)) {
            return codec;
          }
        }
        return null;
    }

    public void setFakeCodecs(VideoCodecInfo[] codecs) {
        if (codecs == null) codecs = new VideoCodecInfo[0];
        fakedCodecs = codecs;
    }
}
