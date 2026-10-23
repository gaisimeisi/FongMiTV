#!/usr/bin/env python3
"""
给 FongMi/media 打补丁：补上作者私有分支里的 3 个类 + 3 处接口扩展。

背景
----
FongMi/TV 的源码 import 了以下在公开 media3 里不存在的成员：
  androidx.media3.common.DiscMenuState
  androidx.media3.common.DiscMenuController
  androidx.media3.exoplayer.iso.IsoDiscPreparation
  Player.getDiscMenuState() / Player.getDiscMenuController()
  Player.Listener.onDiscMenuStateChanged(DiscMenuState)
  PlayerView.setDiscMenuController(...) / dispatchDiscMenuTouchEvent(...)

它们来自作者未公开的 media3 私有分支（只以 lib-*.aar 形式存在）。
本脚本按 TV 源码的全部调用点推出最小 API，用 stub 补齐，使编译可通过。

行为影响
--------
  IsoDiscPreparation.isIso()  ← 真实实现（判断 .iso 后缀），行为与官方一致
  DiscMenu*                   ← 空实现，仅蓝光原盘交互菜单失效
  在线播放 / 直播 / 普通本地视频不受任何影响

用法
----
  python3 patch_media3.py <media3 仓库根目录>
幂等：重复执行不会重复插入。
"""

import os
import sys

# ─────────────────────────── 新增文件内容 ───────────────────────────

DISC_MENU_STATE = '''package androidx.media3.common;

/**
 * Disc (Blu-ray) menu state.
 *
 * <p>Stand-in for the class shipped only in FongMi's private Media3 build. This build always
 * reports an unavailable menu, so every consumer falls back to plain playback.
 */
public final class DiscMenuState {

  /** No disc menu is available. */
  public static final int AVAILABILITY_UNAVAILABLE = 0;

  /** A disc menu is available. */
  public static final int AVAILABILITY_AVAILABLE = 1;

  /** No menu transition is in progress. */
  public static final int PHASE_IDLE = 0;

  /** A menu is being opened. */
  public static final int PHASE_OPENING = 1;

  /** A menu is on screen. */
  public static final int PHASE_ACTIVE = 2;

  /** The state used whenever no disc menu controller is attached. */
  public static final DiscMenuState EMPTY =
      new DiscMenuState(AVAILABILITY_UNAVAILABLE, PHASE_IDLE, false);

  /** One of {@link #AVAILABILITY_UNAVAILABLE} or {@link #AVAILABILITY_AVAILABLE}. */
  public final int availability;

  /** One of {@link #PHASE_IDLE}, {@link #PHASE_OPENING} or {@link #PHASE_ACTIVE}. */
  public final int phase;

  /** Whether navigation playback (menu-driven video) is currently running. */
  public final boolean navigationPlaybackOngoing;

  private DiscMenuState(int availability, int phase, boolean navigationPlaybackOngoing) {
    this.availability = availability;
    this.phase = phase;
    this.navigationPlaybackOngoing = navigationPlaybackOngoing;
  }

  @Override
  public String toString() {
    return "DiscMenuState{availability="
        + availability
        + ", phase="
        + phase
        + ", navigationPlaybackOngoing="
        + navigationPlaybackOngoing
        + "}";
  }
}
'''

DISC_MENU_CONTROLLER = '''package androidx.media3.common;

import java.util.function.Consumer;

/**
 * Disc (Blu-ray) menu controller.
 *
 * <p>Stand-in for the interface shipped only in FongMi's private Media3 build. No implementation
 * is provided, so {@link Player#getDiscMenuController()} returns {@code null} and every call site
 * short-circuits to normal playback.
 */
public interface DiscMenuController {

  /** Result of a request to open a disc menu. */
  enum OpenResult {
    /** The menu was opened. */
    OK,
    /** No menu is available for the current item. */
    UNAVAILABLE,
    /** Opening the menu timed out. */
    TIMED_OUT,
    /** The BD-J runtime module is not installed. */
    RUNTIME_MISSING
  }

  /**
   * Requests that a disc menu be opened.
   *
   * @param action One of {@code "menu"}, {@code "title-menu"} or {@code "popup"}.
   * @param callback Invoked with the result.
   * @return Whether the request was accepted.
   */
  boolean open(String action, Consumer<OpenResult> callback);

  /**
   * Sends a navigation action to the active menu.
   *
   * @param action One of {@code "up"}, {@code "down"}, {@code "left"}, {@code "right"},
   *     {@code "select"} or {@code "prev"}.
   * @return Whether the action was consumed.
   */
  boolean sendAction(String action);

  /** Returns whether a menu is currently on screen. */
  boolean isActive();

  /** Returns whether playback is being driven by menu navigation. */
  boolean isNavigationPlayback();

  /** Returns whether the menu can be driven by pointer/touch events. */
  boolean supportsPointer();
}
'''

ISO_DISC_PREPARATION = '''package androidx.media3.exoplayer.iso;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;

/**
 * Helpers for disc images.
 *
 * <p>Stand-in for the class shipped only in FongMi's private Media3 build. {@link #isIso} is a
 * real implementation so ISO handling in the app keeps working exactly as before.
 */
@UnstableApi
public final class IsoDiscPreparation {

  private IsoDiscPreparation() {}

  /**
   * Returns whether the given item points at an {@code .iso} image.
   *
   * @param item The media item.
   * @return Whether the item is an ISO image.
   */
  public static boolean isIso(@Nullable MediaItem item) {
    if (item == null || item.localConfiguration == null) {
      return false;
    }
    Uri uri = item.localConfiguration.uri;
    if (uri == null) {
      return false;
    }
    String path = uri.getPath();
    return path != null && path.toLowerCase(java.util.Locale.ROOT).endsWith(".iso");
  }
}
'''

# ─────────────────────────── 缺失类补全（第 5 批：libass 基础 + 字幕转录） ───────────────────────────

LIBASS_CONFIGURATION = '''package androidx.media3.exoplayer.libass;

/** Immutable libass runtime configuration. */
public final class LibassConfiguration {

  public final String fontConfig;
  public final String fontsDirectory;
  public final String defaultFontFamily;
  public final int maximumRenderPixels;
  public final int maximumGlyphCount;
  public final int maximumBitmapCacheSizeMb;

  private LibassConfiguration(Builder b) {
    this.fontConfig = b.fontConfig;
    this.fontsDirectory = b.fontsDirectory;
    this.defaultFontFamily = b.defaultFontFamily;
    this.maximumRenderPixels = b.maximumRenderPixels;
    this.maximumGlyphCount = b.maximumGlyphCount;
    this.maximumBitmapCacheSizeMb = b.maximumBitmapCacheSizeMb;
  }

  /** Builder for {@link LibassConfiguration}. */
  public static final class Builder {

    private String fontConfig;
    private String fontsDirectory;
    private String defaultFontFamily;
    private int maximumRenderPixels = 1920 * 1080;
    private int maximumGlyphCount = 3000;
    private int maximumBitmapCacheSizeMb = 64;

    public Builder setFontConfig(String fontConfig) {
      this.fontConfig = fontConfig;
      return this;
    }

    public Builder setFontsDirectory(String fontsDirectory) {
      this.fontsDirectory = fontsDirectory;
      return this;
    }

    public Builder setDefaultFontFamily(String defaultFontFamily) {
      this.defaultFontFamily = defaultFontFamily;
      return this;
    }

    public Builder setMaximumRenderPixels(int maximumRenderPixels) {
      this.maximumRenderPixels = maximumRenderPixels;
      return this;
    }

    public Builder setMaximumGlyphCount(int maximumGlyphCount) {
      this.maximumGlyphCount = maximumGlyphCount;
      return this;
    }

    public Builder setMaximumBitmapCacheSizeMb(int maximumBitmapCacheSizeMb) {
      this.maximumBitmapCacheSizeMb = maximumBitmapCacheSizeMb;
      return this;
    }

    public LibassConfiguration build() {
      return new LibassConfiguration(this);
    }
  }
}
'''

LIBASS_FONT_FILE = '''package androidx.media3.exoplayer.libass;

/** A font file discovered by the external-font scanner. */
public final class LibassFontFile {

  public final String family;
  public final String path;
  public final boolean bold;
  public final boolean italic;

  public LibassFontFile(String family, String path, boolean bold, boolean italic) {
    this.family = family;
    this.path = path;
    this.bold = bold;
    this.italic = italic;
  }

  @Override
  public String toString() {
    return "LibassFontFile{" + family + ", " + path + "}";
  }
}
'''

LIBASS_PLAYBACK_SESSION = '''package androidx.media3.exoplayer.libass;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.text.SubtitleTranscript;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.text.SubtitleParser;

/**
 * Owns the libass rendering session for one playback.
 *
 * <p>This build keeps the object graph intact but reports {@link #isAvailable()} as {@code false}:
 * the native libass pipeline is not wired up, so subtitle rendering stays on the Media3 path
 * (SRT / WebVTT / embedded tracks work normally, ASS styling does not).
 */
public final class LibassPlaybackSession {

  /** Extractor and parser overrides produced by the libass pipeline. */
  public static final class MediaComponents {

    public final ExtractorsFactory extractorsFactory;
    public final SubtitleParser.Factory subtitleParserFactory;

    public MediaComponents(
        ExtractorsFactory extractorsFactory, SubtitleParser.Factory subtitleParserFactory) {
      this.extractorsFactory = extractorsFactory;
      this.subtitleParserFactory = subtitleParserFactory;
    }
  }

  private final LibassConfiguration configuration;
  private final boolean enabled;

  public LibassPlaybackSession(LibassConfiguration configuration, boolean enabled) {
    this.configuration = configuration;
    this.enabled = enabled;
  }

  /** Returns whether the native libass pipeline can be used. */
  public boolean isAvailable() {
    return false;
  }

  /** Builds the extractor/parser overrides for a media item. */
  public MediaComponents createMediaComponents(
      MediaItem item, ExtractorsFactory extractorsFactory, SubtitleTranscript transcript) {
    return new MediaComponents(extractorsFactory, null);
  }

  public void setPreloadMediaItem(@Nullable MediaItem item) {}

  public void setBottomPositionFraction(float fraction) {}

  public void setSecondaryBottomPositionFraction(float fraction) {}

  public void setFontScale(float scale, boolean apply) {}

  public LibassConfiguration getConfiguration() {
    return configuration;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void close() {}
}
'''

SUBTITLE_TRANSCRIPT = '''package androidx.media3.exoplayer.text;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;

/** External subtitle resources attached to a media item. */
public final class SubtitleTranscript {

  @Nullable private final MediaItem item;

  public SubtitleTranscript(@Nullable MediaItem item) {
    this.item = item;
  }

  @Nullable
  public MediaItem getMediaItem() {
    return item;
  }

  @Override
  public String toString() {
    return "SubtitleTranscript{" + item + "}";
  }
}
'''

SUBTITLE_TRANSCRIPT_SESSION = '''package androidx.media3.exoplayer.text;

import androidx.annotation.Nullable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.mpvplayer.MpvSubtitleContentLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks external subtitle resources per media item. */
public final class SubtitleTranscriptSession {

  private final Map<MediaItem, SubtitleTranscript> transcripts = new ConcurrentHashMap<>();

  @Nullable private MpvSubtitleContentLoader loader;
  @Nullable private Player player;

  public void setLoader(MpvSubtitleContentLoader loader) {
    this.loader = loader;
  }

  public void setPlayer(Player player) {
    this.player = player;
  }

  public void setPreloadMediaItem(@Nullable MediaItem item) {}

  /** Returns (creating if needed) the transcript for the given item. */
  public SubtitleTranscript forMediaItem(MediaItem item) {
    return transcripts.computeIfAbsent(item, SubtitleTranscript::new);
  }

  /** Returns the parsed content of a subtitle track. */
  public SubtitleContent getContent(Format format, long offsetMs) {
    return SubtitleContent.UNSUPPORTED;
  }

  public void close() {
    transcripts.clear();
    loader = null;
    player = null;
  }
}
'''

SUBTITLE_TRANSCRIPT_PARSER_FACTORY = '''package androidx.media3.exoplayer.text;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.extractor.text.DefaultSubtitleParserFactory;
import androidx.media3.extractor.text.SubtitleParser;

/**
 * Parser factory backed by the stock Media3 parsers.
 *
 * <p>The libass transcript path would install its own parser here; without it we delegate to the
 * stock implementation so external SRT / WebVTT files keep working.
 */
public final class SubtitleTranscriptParserFactory implements SubtitleParser.Factory {

  private final SubtitleTranscript transcript;
  private final SubtitleParser.Factory delegate = new DefaultSubtitleParserFactory();

  public SubtitleTranscriptParserFactory(SubtitleTranscript transcript) {
    this.transcript = transcript;
  }

  public SubtitleTranscript getTranscript() {
    return transcript;
  }

  @Override
  public boolean supportsFormat(Format format) {
    return delegate.supportsFormat(format);
  }

  @Override
  public int getCueReplacementBehavior(Format format) {
    return delegate.getCueReplacementBehavior(format);
  }

  @Override
  public SubtitleParser create(Format format) {
    return delegate.create(format);
  }
}
'''

# ─────────────────────────── 缺失类补全（第 4 批：字幕内容） ───────────────────────────

SUBTITLE_CONTENT = '''package androidx.media3.common.text;

import androidx.annotation.Nullable;
import androidx.media3.common.text.Cue;
import java.util.Collections;
import java.util.List;

/** Parsed subtitle payload handed between the player engines. */
public final class SubtitleContent {

  /** Signals that the track could not be read. */
  public static final SubtitleContent UNSUPPORTED =
      new SubtitleContent(Collections.emptyList(), null);

  /** Parsed cues, in playback order. */
  public final List<Cue> cues;

  /** MIME type the cues were parsed from, when known. */
  @Nullable public final String mimeType;

  public SubtitleContent(List<Cue> cues, @Nullable String mimeType) {
    this.cues = cues == null ? Collections.emptyList() : cues;
    this.mimeType = mimeType;
  }

  /** Returns whether reading the track failed. */
  public boolean isUnsupported() {
    return this == UNSUPPORTED;
  }

  /** Returns whether there is anything to show. */
  public boolean isEmpty() {
    return cues.isEmpty();
  }
}
'''

DEFAULT_SUBTITLE_CONTENT_LOADER = '''package androidx.media3.exoplayer.text;

import android.net.Uri;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.datasource.DataSource;
import java.io.IOException;

/**
 * Loads an external subtitle file into {@link SubtitleContent}.
 *
 * <p>This build does not run a subtitle decoder outside of the player pipeline, so loading always
 * reports {@link SubtitleContent#UNSUPPORTED}. Subtitle rendering itself is unaffected and keeps
 * using the tracks delivered by ExoPlayer.
 */
public final class DefaultSubtitleContentLoader {

  private final DataSource.Factory dataSourceFactory;

  public DefaultSubtitleContentLoader(DataSource.Factory dataSourceFactory) {
    this.dataSourceFactory = dataSourceFactory;
  }

  /** Returns the load result for the given subtitle resource. */
  public SubtitleContent load(MediaItem item, Uri uri, Format format) throws IOException {
    return SubtitleContent.UNSUPPORTED;
  }

  public DataSource.Factory getDataSourceFactory() {
    return dataSourceFactory;
  }
}
'''

MPV_SUBTITLE_CONTENT_LOADER = '''package androidx.media3.mpvplayer;

import android.net.Uri;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.text.SubtitleContent;
import java.io.IOException;

/** Lets the mpv engine read an external subtitle file through app code. */
public interface MpvSubtitleContentLoader {

  /** Loads the subtitle resource, or returns {@link SubtitleContent#UNSUPPORTED}. */
  SubtitleContent load(MediaItem item, Uri uri, Format format) throws IOException;
}
'''

# ─────────────────────────── 缺失类补全（第 3 批：音频效果） ───────────────────────────

AUDIO_EFFECT_PARAMETERS = '''package androidx.media3.common.audio;

/** Immutable snapshot of the audio post-processing configuration. */
public final class AudioEffectParameters {

  /** Everything off. */
  public static final AudioEffectParameters DEFAULT = new Builder().build();

  private final int[] frequencies;
  private final short[] levels;
  private final float stability;
  private final int boostMillibels;
  private final int preampMillibels;
  private final int centerGainMillibels;
  private final float balance;
  private final int channelMode;
  private final boolean loudnessEnabled;

  private AudioEffectParameters(Builder b) {
    this.frequencies = b.frequencies;
    this.levels = b.levels;
    this.stability = b.stability;
    this.boostMillibels = b.boostMillibels;
    this.preampMillibels = b.preampMillibels;
    this.centerGainMillibels = b.centerGainMillibels;
    this.balance = b.balance;
    this.channelMode = b.channelMode;
    this.loudnessEnabled = b.loudnessEnabled;
  }

  /** Centre-channel gain only makes sense with a real centre speaker. */
  public static boolean isCenterGainAvailable(int channelCount) {
    return channelCount >= 3;
  }

  /** Returns whether any equalizer band is configured. */
  public boolean hasBands() {
    return levels.length > 0;
  }

  /** Returns whether any processing at all is configured. */
  public boolean hasEffect() {
    return hasBands()
        || stability > 0f
        || boostMillibels != 0
        || preampMillibels != 0
        || centerGainMillibels != 0
        || balance != 0f
        || loudnessEnabled
        || channelMode != AudioChannelMode.AUTO;
  }

  /** Returns whether a centre gain applies to the given layout. */
  public boolean hasCenterGain(int channelCount) {
    return centerGainMillibels != 0 && isCenterGainAvailable(channelCount);
  }

  /** Returns whether the mix needs an output limiter to avoid clipping. */
  public boolean shouldLimitOutput(int channelCount) {
    return hasBands() || boostMillibels > 0 || preampMillibels > 0 || hasCenterGain(channelCount);
  }

  public float getCenterGainFactor() {
    return centerGainMillibels / 100.0f;
  }

  public int[] getEqualizerFrequencies() {
    return frequencies.clone();
  }

  public short[] getEqualizerLevelsMillibels() {
    return levels.clone();
  }

  public float getStability() {
    return stability;
  }

  public int getBoostMillibels() {
    return boostMillibels;
  }

  public int getPreampMillibels() {
    return preampMillibels;
  }

  public int getCenterGainMillibels() {
    return centerGainMillibels;
  }

  public float getBalance() {
    return balance;
  }

  public int getChannelMode() {
    return channelMode;
  }

  public boolean isLoudnessEnabled() {
    return loudnessEnabled;
  }

  /** Builder for {@link AudioEffectParameters}. */
  public static final class Builder {

    private int[] frequencies = new int[0];
    private short[] levels = new short[0];
    private float stability = 0f;
    private int boostMillibels = 0;
    private int preampMillibels = 0;
    private int centerGainMillibels = 0;
    private float balance = 0f;
    private int channelMode = AudioChannelMode.AUTO;
    private boolean loudnessEnabled = false;

    public Builder setEqualizerBands(int[] frequencies, short[] levels) {
      this.frequencies = frequencies == null ? new int[0] : frequencies.clone();
      this.levels = levels == null ? new short[0] : levels.clone();
      return this;
    }

    public Builder setStability(float stability) {
      this.stability = stability;
      return this;
    }

    public Builder setBoostMillibels(int boostMillibels) {
      this.boostMillibels = boostMillibels;
      return this;
    }

    public Builder setPreampMillibels(int preampMillibels) {
      this.preampMillibels = preampMillibels;
      return this;
    }

    public Builder setCenterGainMillibels(int centerGainMillibels) {
      this.centerGainMillibels = centerGainMillibels;
      return this;
    }

    public Builder setBalance(float balance) {
      this.balance = balance;
      return this;
    }

    public Builder setChannelMode(int channelMode) {
      this.channelMode = channelMode;
      return this;
    }

    public Builder setLoudnessEnabled(boolean loudnessEnabled) {
      this.loudnessEnabled = loudnessEnabled;
      return this;
    }

    public AudioEffectParameters build() {
      return new AudioEffectParameters(this);
    }
  }
}
'''

# ─────────────────────────── 缺失类补全（第 2 批：数据类） ───────────────────────────

SUBTITLE_OFFSETS = '''package androidx.media3.common.text;

/** Per-track subtitle timing offsets, in milliseconds. */
public final class SubtitleOffsets {

  /** No offset at all. */
  public static final SubtitleOffsets ZERO = new SubtitleOffsets(0L, 0L);

  /** Offset applied to the primary subtitle track. */
  public final long primaryMs;

  /** Offset applied to the secondary subtitle track. */
  public final long secondaryMs;

  public SubtitleOffsets(long primaryMs, long secondaryMs) {
    this.primaryMs = primaryMs;
    this.secondaryMs = secondaryMs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SubtitleOffsets)) return false;
    SubtitleOffsets other = (SubtitleOffsets) o;
    return primaryMs == other.primaryMs && secondaryMs == other.secondaryMs;
  }

  @Override
  public int hashCode() {
    return (int) (primaryMs * 31 + secondaryMs);
  }

  @Override
  public String toString() {
    return "SubtitleOffsets{primary=" + primaryMs + ", secondary=" + secondaryMs + "}";
  }
}
'''

SUBTITLE_SELECTION_STATE = '''package androidx.media3.common.text;

import androidx.annotation.Nullable;
import androidx.media3.common.TrackSelectionOverride;

/**
 * Which text track is currently primary, and which extra track is shown as secondary.
 *
 * <p>Without an FFmpeg/libass text pipeline this build never tracks secondary tracks, so {@link
 * #EMPTY} answers {@code false} for everything.
 */
public final class SubtitleSelectionState {

  /** No track is selected. */
  public static final SubtitleSelectionState EMPTY = new SubtitleSelectionState(null, null);

  @Nullable private final TrackSelectionOverride primary;
  @Nullable private final TrackSelectionOverride secondary;

  public SubtitleSelectionState(
      @Nullable TrackSelectionOverride primary, @Nullable TrackSelectionOverride secondary) {
    this.primary = primary;
    this.secondary = secondary;
  }

  /** Returns whether the given override is the primary subtitle track. */
  public boolean isPrimary(@Nullable TrackSelectionOverride selection) {
    return selection != null && selection.equals(primary);
  }

  /** Returns whether the given override is an active secondary subtitle track. */
  public boolean isActiveSecondary(@Nullable TrackSelectionOverride selection) {
    return selection != null && selection.equals(secondary);
  }
}
'''

VIDEO_ADJUSTMENT = '''package androidx.media3.effect;

/** Snapshot of user colour / detail adjustments, applied by the video effect chain. */
public final class VideoAdjustment {

  public final float saturation;
  public final float contrast;
  public final float brightness;
  public final float gamma;
  public final float hue;
  public final float redGain;
  public final float blueGain;
  public final float sharpness;
  public final float threshold;
  public final float shadowLift;

  private VideoAdjustment(Builder b) {
    this.saturation = b.saturation;
    this.contrast = b.contrast;
    this.brightness = b.brightness;
    this.gamma = b.gamma;
    this.hue = b.hue;
    this.redGain = b.redGain;
    this.blueGain = b.blueGain;
    this.sharpness = b.sharpness;
    this.threshold = b.threshold;
    this.shadowLift = b.shadowLift;
  }

  /** Builder for {@link VideoAdjustment}. */
  public static final class Builder {

    private float saturation = 1f;
    private float contrast = 1f;
    private float brightness = 0f;
    private float gamma = 1f;
    private float hue = 0f;
    private float redGain = 1f;
    private float blueGain = 1f;
    private float sharpness = 0f;
    private float threshold = 0f;
    private float shadowLift = 0f;

    public Builder setSaturation(float value) {
      this.saturation = value;
      return this;
    }

    public Builder setContrast(float value) {
      this.contrast = value;
      return this;
    }

    public Builder setBrightness(float value) {
      this.brightness = value;
      return this;
    }

    public Builder setGamma(float value) {
      this.gamma = value;
      return this;
    }

    public Builder setHue(float value) {
      this.hue = value;
      return this;
    }

    public Builder setRedGain(float value) {
      this.redGain = value;
      return this;
    }

    public Builder setBlueGain(float value) {
      this.blueGain = value;
      return this;
    }

    public Builder setSharpness(float value) {
      this.sharpness = value;
      return this;
    }

    public Builder setThreshold(float value) {
      this.threshold = value;
      return this;
    }

    public Builder setShadowLift(float value) {
      this.shadowLift = value;
      return this;
    }

    public VideoAdjustment build() {
      return new VideoAdjustment(this);
    }
  }
}
'''

# ─────────────────────────── 缺失类补全（第 1 批：枚举/常量） ───────────────────────────

COMMON_DECODER_MODE = '''package androidx.media3.common;

/** How a track should be decoded. */
public enum DecoderMode {
  /** Let the player choose. */
  AUTO,
  /** Force the platform decoder (MediaCodec). */
  HARDWARE,
  /** Force software decoding through FFmpeg. */
  FFMPEG
}
'''

EXOPLAYER_DECODER_MODE = '''package androidx.media3.exoplayer;

/** Decoder selection stored by the ExoPlayer stack. */
public enum DecoderMode {
  /** Platform decoder. */
  MEDIACODEC,
  /** FFmpeg software decoder. */
  FFMPEG;

  /** Converts to the app-facing representation. */
  public androidx.media3.common.DecoderMode toCommonDecoderMode() {
    return this == FFMPEG
        ? androidx.media3.common.DecoderMode.FFMPEG
        : androidx.media3.common.DecoderMode.HARDWARE;
  }

  /** Converts from the app-facing representation. */
  public static DecoderMode fromCommonDecoderMode(androidx.media3.common.DecoderMode mode) {
    if (mode == androidx.media3.common.DecoderMode.FFMPEG) {
      return FFMPEG;
    }
    return MEDIACODEC;
  }
}
'''

AUDIO_CHANNEL_MODE = '''package androidx.media3.common.audio;

/** Output channel layout selection. */
public final class AudioChannelMode {

  /** Let the player keep the source layout. */
  public static final int AUTO = 0;

  /** Downmix everything to mono. */
  public static final int MONO = 1;

  /** Force stereo output. */
  public static final int STEREO = 2;

  /** Swap left and right channels. */
  public static final int SWAP = 3;

  private AudioChannelMode() {}

  /** Clamps an arbitrary value into the valid range. */
  public static int clamp(int mode) {
    return (mode >= AUTO && mode <= SWAP) ? mode : AUTO;
  }

  /** Returns whether the mode means "leave it alone". */
  public static boolean isAuto(int mode) {
    return clamp(mode) == AUTO;
  }

  /** Returns whether the mode can be applied to the given channel count. */
  public static boolean isAvailable(int mode, int channelCount) {
    switch (clamp(mode)) {
      case MONO:
        return channelCount > 1;
      case STEREO:
      case SWAP:
        return channelCount >= 2;
      case AUTO:
      default:
        return false;
    }
  }

  /** Resolves a mode against the actual channel count, falling back to AUTO. */
  public static int resolve(int mode, int channelCount) {
    int clamped = clamp(mode);
    return isAvailable(clamped, channelCount) ? clamped : AUTO;
  }

  /** Returns whether the mode supports a left/right balance setting. */
  public static boolean canBalance(int mode) {
    return clamp(mode) == STEREO;
  }
}
'''



PLAYER_METHODS = '''  /**
   * Returns the current disc menu state.
   *
   * <p>Always {@link DiscMenuState#EMPTY} in this build.
   */
  default DiscMenuState getDiscMenuState() {
    return DiscMenuState.EMPTY;
  }

  /**
   * Returns the disc menu controller, or {@code null} when the current item has no disc menu.
   *
   * <p>Always {@code null} in this build.
   */
  @Nullable
  default DiscMenuController getDiscMenuController() {
    return null;
  }

'''

LISTENER_METHOD = '''    /**
     * Called when the disc menu state changes.
     *
     * @param state The new {@link DiscMenuState}.
     */
    default void onDiscMenuStateChanged(DiscMenuState state) {}

'''

PLAYERVIEW_METHODS = '''  /**
   * Sets the disc menu controller used for Blu-ray menus.
   *
   * <p>No-op in this build.
   *
   * @param controller The controller, or {@code null} to clear it.
   */
  public void setDiscMenuController(@Nullable DiscMenuController controller) {}

  /**
   * Forwards a touch event to the disc menu.
   *
   * <p>No-op in this build.
   *
   * @param event The touch event.
   */
  public void dispatchDiscMenuTouchEvent(MotionEvent event) {}

'''


def write_if_absent(path, content, marker):
    if os.path.exists(path):
        existing = open(path, encoding="utf-8").read()
        if marker in existing:
            print(f"  = 已存在，跳过: {os.path.basename(path)}")
            return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print(f"  + 新建: {path}")


def insert_before(path, anchor, snippet, marker, once=True):
    text = open(path, encoding="utf-8").read()
    if marker in text:
        print(f"  = 已打过补丁，跳过: {os.path.basename(path)}")
        return
    idx = text.find(anchor)
    if idx < 0:
        raise SystemExit(f"锚点未找到: {anchor!r} in {path}")
    text = text[:idx] + snippet + text[idx:]
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print(f"  ~ 已插入 {marker.split()[0]} -> {os.path.basename(path)}")


def ensure_import(path, imp):
    text = open(path, encoding="utf-8").read()
    if imp in text:
        return
    lines = text.split("\n")
    # 插到第一个 import 之前（保持字母序不严格，但保证在 package 之后）
    for i, line in enumerate(lines):
        if line.startswith("import ") and not line.startswith("import static"):
            lines.insert(i, imp)
            break
    else:
        raise SystemExit(f"找不到 import 区: {path}")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))
    print(f"  ~ 补充 import {imp} -> {os.path.basename(path)}")


def main():
    if len(sys.argv) < 2:
        raise SystemExit("用法: python3 patch_media3.py <media3 仓库根目录>")
    root = sys.argv[1].rstrip("/\\")
    common = f"{root}/libraries/common/src/main/java/androidx/media3/common"
    exo_iso = f"{root}/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/iso"
    ui = f"{root}/libraries/ui/src/main/java/androidx/media3/ui"

    print("[1/6] 新增 DiscMenuState")
    write_if_absent(f"{common}/DiscMenuState.java", DISC_MENU_STATE, "class DiscMenuState")

    print("[2/6] 新增 DiscMenuController")
    write_if_absent(f"{common}/DiscMenuController.java", DISC_MENU_CONTROLLER, "interface DiscMenuController")

    print("[3/6] 新增 IsoDiscPreparation")
    write_if_absent(f"{exo_iso}/IsoDiscPreparation.java", ISO_DISC_PREPARATION, "class IsoDiscPreparation")

    print("[4/6] 扩展 Player 接口")
    insert_before(
        f"{common}/Player.java",
        "  @State\n  int getPlaybackState();",
        PLAYER_METHODS,
        "getDiscMenuState()",
    )

    print("[5/6] 扩展 Player.Listener")
    insert_before(
        f"{common}/Player.java",
        "    default void onPlaybackStateChanged(@State int playbackState) {}",
        LISTENER_METHOD,
        "onDiscMenuStateChanged(DiscMenuState",
    )

    print("[6/6] 扩展 PlayerView")
    pv = f"{ui}/PlayerView.java"
    ensure_import(pv, "import androidx.media3.common.DiscMenuController;")
    insert_before(
        pv,
        "  public void setPlayer(@Nullable Player player) {",
        PLAYERVIEW_METHODS,
        "setDiscMenuController(@Nullable DiscMenuController",
    )

    exoplayer = f"{root}/libraries/exoplayer/src/main/java/androidx/media3/exoplayer"

    print("[7/9] 新增 common.DecoderMode")
    write_if_absent(f"{common}/DecoderMode.java", COMMON_DECODER_MODE, "enum DecoderMode")

    print("[8/9] 新增 exoplayer.DecoderMode")
    write_if_absent(f"{exoplayer}/DecoderMode.java", EXOPLAYER_DECODER_MODE, "enum DecoderMode")

    print("[9/9] 新增 AudioChannelMode")
    write_if_absent(f"{common}/audio/AudioChannelMode.java", AUDIO_CHANNEL_MODE, "class AudioChannelMode")

    effect = f"{root}/libraries/effect/src/main/java/androidx/media3/effect"

    print("[10/12] 新增 SubtitleOffsets")
    write_if_absent(f"{common}/text/SubtitleOffsets.java", SUBTITLE_OFFSETS, "class SubtitleOffsets")

    print("[11/12] 新增 SubtitleSelectionState")
    write_if_absent(
        f"{common}/text/SubtitleSelectionState.java",
        SUBTITLE_SELECTION_STATE,
        "class SubtitleSelectionState",
    )

    print("[12/12] 新增 VideoAdjustment")
    write_if_absent(f"{effect}/VideoAdjustment.java", VIDEO_ADJUSTMENT, "class VideoAdjustment")

    print("[13/13] 新增 AudioEffectParameters")
    write_if_absent(
        f"{common}/audio/AudioEffectParameters.java",
        AUDIO_EFFECT_PARAMETERS,
        "class AudioEffectParameters",
    )

    mpvplayer = f"{root}/libraries/mpvplayer/src/main/java/androidx/media3/mpvplayer"

    print("[14/16] 新增 SubtitleContent")
    write_if_absent(f"{common}/text/SubtitleContent.java", SUBTITLE_CONTENT, "class SubtitleContent")

    print("[15/16] 新增 DefaultSubtitleContentLoader")
    write_if_absent(
        f"{exoplayer}/text/DefaultSubtitleContentLoader.java",
        DEFAULT_SUBTITLE_CONTENT_LOADER,
        "class DefaultSubtitleContentLoader",
    )

    print("[16/22] 新增 MpvSubtitleContentLoader")
    stale = f"{mpvplayer}/MpvSubtitleContentLoader.java"
    if os.path.exists(stale):
        os.remove(stale)
        print(f"  - 移除旧位置: {stale}")
    write_if_absent(
        f"{exoplayer}/mpvplayer/MpvSubtitleContentLoader.java",
        MPV_SUBTITLE_CONTENT_LOADER,
        "interface MpvSubtitleContentLoader",
    )

    print("[17/22] 新增 LibassConfiguration")
    write_if_absent(
        f"{exoplayer}/libass/LibassConfiguration.java", LIBASS_CONFIGURATION, "class LibassConfiguration"
    )

    print("[18/22] 新增 LibassFontFile")
    write_if_absent(f"{exoplayer}/libass/LibassFontFile.java", LIBASS_FONT_FILE, "class LibassFontFile")

    print("[19/22] 新增 LibassPlaybackSession")
    write_if_absent(
        f"{exoplayer}/libass/LibassPlaybackSession.java",
        LIBASS_PLAYBACK_SESSION,
        "class LibassPlaybackSession",
    )

    print("[20/22] 新增 SubtitleTranscript")
    write_if_absent(
        f"{exoplayer}/text/SubtitleTranscript.java", SUBTITLE_TRANSCRIPT, "class SubtitleTranscript"
    )

    print("[21/22] 新增 SubtitleTranscriptSession")
    write_if_absent(
        f"{exoplayer}/text/SubtitleTranscriptSession.java",
        SUBTITLE_TRANSCRIPT_SESSION,
        "class SubtitleTranscriptSession",
    )

    print("[22/22] 新增 SubtitleTranscriptParserFactory")
    write_if_absent(
        f"{exoplayer}/text/SubtitleTranscriptParserFactory.java",
        SUBTITLE_TRANSCRIPT_PARSER_FACTORY,
        "class SubtitleTranscriptParserFactory",
    )

    print("\n补丁完成。")


if __name__ == "__main__":
    main()
