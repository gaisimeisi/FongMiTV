package com.fongmi.android.tv.player.exo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.DecoderManager;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager;
import androidx.media3.exoplayer.text.TextOutput;
import androidx.media3.exoplayer.text.TextRenderer;
import androidx.media3.exoplayer.trackselection.DecodeTrackSelector;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.track.LangUtil;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.github.catvod.utils.Auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ExoUtil {

    public static ExoPlayer buildPlayer(DefaultPreloadManager.Builder preloadManagerBuilder) {
        ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(App.get()).setSkipSilenceEnabled(SpeedSetting.isSkipSilence());
        ExoPlayer player = preloadManagerBuilder.buildExoPlayer(playerBuilder);
        player.setAudioAttributes(AudioAttributes.DEFAULT, true);
        player.setHandleAudioBecomingNoisy(true);
        player.setPlayWhenReady(true);
        return player;
    }

    static LoadControl buildLoadControl(int maxPreloadBufferBytes) {
        int buffer = PlayerSetting.getBuffer();
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMsForStreaming(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * buffer, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * buffer, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .setBufferDurationsMsForLocalPlayback(DefaultLoadControl.DEFAULT_MIN_BUFFER_FOR_LOCAL_PLAYBACK_MS * buffer, DefaultLoadControl.DEFAULT_MAX_BUFFER_FOR_LOCAL_PLAYBACK_MS * buffer, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_FOR_LOCAL_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_FOR_LOCAL_PLAYBACK_MS)
                .setPlayerTargetBufferBytes(PlayerId.PRELOAD.name, maxPreloadBufferBytes)
                .build();
    }

    public static Map<String, String> extractHeaders(MediaItem item) {
        Bundle extras = item.requestMetadata.extras;
        if (extras == null) return new HashMap<>();
        return extras.keySet().stream().filter(key -> extras.getString(key) != null).collect(Collectors.toMap(key -> key, extras::getString));
    }

    public static Auth.Context extractAuth(MediaItem item) {
        return item.localConfiguration != null && item.localConfiguration.tag instanceof Auth.Context auth ? auth : null;
    }

    static DecodeTrackSelector buildTrackSelector(DecoderManager decoderManager) {
        DecodeTrackSelector trackSelector = decoderManager.createTrackSelector(App.get());
        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters();
        if (DecodeSetting.isPreferAAC()) builder.setPreferredAudioMimeType(MimeTypes.AUDIO_AAC);
        builder.setPreferredTextLanguages(LangUtil.getPreferredTextLanguages());
        builder.setTunnelingEnabled(DecodeSetting.isTunnelingEnabled());
        builder.setForceHighestSupportedBitrate(true);
        trackSelector.setParameters(builder.build());
        return trackSelector;
    }

    static RenderersFactory buildRenderersFactory() {
        return new ExoRenderersFactory(null, null, null, null);
    }

    static RenderersFactory buildRenderersFactory(AudioProcessor audioProcessor, TextOutput secondaryTextOutput, LibassPlaybackSession libassPlaybackSession, DecoderManager decoderManager) {
        return new ExoRenderersFactory(audioProcessor, secondaryTextOutput, libassPlaybackSession, decoderManager);
    }

    /**
     * 上游 5.8.7 新增的 5 参重载（多了 motionFps，用于 FFmpeg 运动补偿渲染器）。
     * 我们的 media3 aar 是从 release-1.11.0-fongmi 编译的，不含
     * androidx.media3.decoder.ffmpeg.FfmpegMotionRenderersFactory，
     * 所以这里忽略 motionFps，走普通 RenderersFactory。
     */
    static RenderersFactory buildRenderersFactory(AudioProcessor audioProcessor, androidx.media3.exoplayer.text.SecondaryTextOutput secondaryTextOutput, LibassPlaybackSession libassPlaybackSession, DecoderManager decoderManager, int motionFps) {
        return new ExoRenderersFactory(audioProcessor, secondaryTextOutput, libassPlaybackSession, decoderManager);
    }

    private static AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioOutputPlaybackParams, @Nullable AudioProcessor audioProcessor) {
        DefaultAudioSink.Builder builder = new DefaultAudioSink.Builder(context).setEnableFloatOutput(enableFloatOutput).setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams);
        // 关闭音频直通时必须显式给出 AudioCapabilities。
        // 原来传的是 null，AudioTrack 无法确定采样率/声道支持 → 音频轨道建不起来，
        // 表现是「视频正常播放、播放状态 PLAYING，但 AudioFlinger 一直 Standby，完全没声音」。
        // 注意：这段只在直通关闭时执行，而直通以前默认是 true，所以这个问题一直没暴露。
        if (!DecodeSetting.isAudioPassThrough()) {
            builder.setAudioOutputProvider(new AudioTrackAudioOutputProvider.Builder(
                    androidx.media3.common.AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES).build());
        }
        if (audioProcessor != null) builder.setAudioProcessors(new AudioProcessor[]{audioProcessor});
        return builder.build();
    }

    private static final class ExoRenderersFactory extends DefaultRenderersFactory {

        @Nullable private final AudioProcessor audioProcessor;
        @Nullable private final TextOutput secondaryTextOutput;
        @Nullable private final LibassPlaybackSession libassPlaybackSession;

        private ExoRenderersFactory(@Nullable AudioProcessor audioProcessor, @Nullable TextOutput secondaryTextOutput, @Nullable LibassPlaybackSession libassPlaybackSession, @Nullable DecoderManager decoderManager) {
            super(App.get());
            this.audioProcessor = audioProcessor;
            this.secondaryTextOutput = secondaryTextOutput;
            this.libassPlaybackSession = libassPlaybackSession;
            if (decoderManager != null) setDecoderManager(decoderManager);
            setEnableDecoderFallback(false);
            setEnableSurfaceControlFrameRetention(true);
            experimentalSetEnableMediaCodecVideoRendererPrewarming(true);
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON);
            setDolbyVisionOutputPolicy(DecodeSetting.getDolbyVisionOutputPolicy());
        }

        @Override
        protected AudioSink buildAudioSink(@NonNull Context context, boolean enableFloatOutput, boolean enableAudioOutputPlaybackParams) {
            return ExoUtil.buildAudioSink(context, enableFloatOutput, enableAudioOutputPlaybackParams, audioProcessor);
        }

        @Override
        protected void buildMiscellaneousRenderers(@NonNull Context context, @NonNull Handler eventHandler, int extensionRendererMode, @NonNull ArrayList<Renderer> out) {
            super.buildMiscellaneousRenderers(context, eventHandler, extensionRendererMode, out);
            if (libassPlaybackSession != null && libassPlaybackSession.isAvailable()) out.add(libassPlaybackSession.createClockRenderer());
        }

        @Override
        protected void buildTextRenderers(@NonNull Context context, @NonNull TextOutput output, @NonNull Looper outputLooper, int extensionRendererMode, @NonNull ArrayList<Renderer> out) {
            super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out);
            if (secondaryTextOutput != null) out.add(new TextRenderer(secondaryTextOutput, outputLooper));
        }
    }
}
