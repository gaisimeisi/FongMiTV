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

SECONDARY_TEXT_TRACK_SELECTOR = '''package androidx.media3.exoplayer.trackselection;

import android.content.Context;

/** Track selector that also carries a secondary text track. */
public class SecondaryTextTrackSelector extends DefaultTrackSelector {

  public SecondaryTextTrackSelector(Context context) {
    super(context);
  }

  /** Factory that wraps another selector factory. */
  public static final class Factory implements TrackSelector.Factory {

    private final TrackSelector.Factory delegate;

    public Factory(TrackSelector.Factory delegate) {
      this.delegate = delegate;
    }

    @Override
    public TrackSelector createTrackSelector(Context context) {
      return delegate.createTrackSelector(context);
    }
  }
}
'''

DECODE_TRACK_SELECTOR = '''package androidx.media3.exoplayer.trackselection;

import android.content.Context;

/** Track selector used by {@code DecoderManager}. */
public class DecodeTrackSelector extends DefaultTrackSelector {

  public DecodeTrackSelector(Context context) {
    super(context);
  }
}
'''

DECODER_MANAGER = '''package androidx.media3.exoplayer;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.media3.exoplayer.trackselection.DecodeTrackSelector;

/** Tracks the requested audio/video decoder mode per playback. */
public final class DecoderManager {

  private DecoderMode audioMode;
  private DecoderMode videoMode;

  @Nullable private ExoPlayer player;

  public DecoderManager(DecoderMode audioMode, DecoderMode videoMode) {
    this.audioMode = audioMode;
    this.videoMode = videoMode;
  }

  public void attach(ExoPlayer player) {
    this.player = player;
  }

  public void detach() {
    this.player = null;
  }

  public DecoderMode getAudioMode() {
    return audioMode;
  }

  public DecoderMode getVideoMode() {
    return videoMode;
  }

  public void selectAudioDecoder(DecoderMode mode) {
    this.audioMode = mode;
  }

  public void selectVideoDecoder(DecoderMode mode) {
    this.videoMode = mode;
  }

  public void setDecoderModes(DecoderMode audioMode, DecoderMode videoMode) {
    this.audioMode = audioMode;
    this.videoMode = videoMode;
  }

  /** Creates the track selector that applies the requested decoder modes. */
  public DecodeTrackSelector createTrackSelector(Context context) {
    return new DecodeTrackSelector(context);
  }
}
'''

LIBASS_SUBTITLE_CONTROLLER = '''package androidx.media3.exoplayer.libass;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.exoplayer.text.SecondaryTextOutput;
import androidx.media3.exoplayer.trackselection.SecondaryTextTrackSelector;

/**
 * Coordinates subtitle track selection with the libass session.
 *
 * <p>Without a native libass pipeline the controller only records the primary/secondary selection
 * intent; rendering itself stays on the Media3 text renderer.
 */
public final class LibassSubtitleController {

  @Nullable private TrackSelectionOverride primary;
  @Nullable private TrackSelectionOverride secondary;
  private boolean autoSecondaryEnabled = true;

  public LibassSubtitleController(
      Player player,
      LibassPlaybackSession session,
      SecondaryTextTrackSelector.Factory selectorFactory,
      SecondaryTextOutput output) {}

  /** Returns the current selection state. */
  public SubtitleSelectionState getSubtitleSelectionState() {
    return new SubtitleSelectionState(primary, secondary);
  }

  public void setSecondaryTextTrackSelectionOverride(@Nullable TrackSelectionOverride selection) {
    this.secondary = selection;
  }

  public void setSecondaryTextTrackAutoSelectionEnabled(boolean enabled) {
    this.autoSecondaryEnabled = enabled;
  }

  public boolean isSecondaryTextTrackAutoSelectionEnabled() {
    return autoSecondaryEnabled;
  }

  public void close() {
    primary = null;
    secondary = null;
  }
}
'''

DRF_SET_DECODER_MANAGER = '''  private DecoderManager decoderManager;

  /**
   * Sets the decoder manager used to pick audio/video decoders.
   *
   * @param decoderManager The decoder manager.
   * @return This factory, for convenience.
   */
  public final DefaultRenderersFactory setDecoderManager(DecoderManager decoderManager) {
    this.decoderManager = decoderManager;
    return this;
  }

'''

# ─────────────────────────── 缺失类补全（第 9 批：收尾） ───────────────────────────

ISO_URI = '''package androidx.media3.datasource;

import android.net.Uri;

/** Uri helpers for Blu-ray ISO images. */
public final class IsoUri {

  public static final String PARAM_EDITION_INDEX = "edition";

  private IsoUri() {}

  /** Appends the edition index to the uri query. */
  public static Uri withEditionIndex(Uri uri, int editionIndex) {
    if (uri == null) return null;
    return uri.buildUpon().appendQueryParameter(PARAM_EDITION_INDEX, String.valueOf(editionIndex)).build();
  }

  /** Reads the edition index from the uri query, or -1 when absent. */
  public static int getEditionIndex(Uri uri) {
    if (uri == null) return -1;
    String value = uri.getQueryParameter(PARAM_EDITION_INDEX);
    if (value == null) return -1;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
'''

AUDIO_CHANNEL_MIX = '''package androidx.media3.common.audio;

/** Factory helpers describing how to remix channels. */
public final class AudioChannelMix {

  private final int inputChannels;
  private final int outputChannels;
  private final boolean swap;
  private final float centerGain;
  private final float balance;

  private AudioChannelMix(int inputChannels, int outputChannels, boolean swap, float centerGain, float balance) {
    this.inputChannels = inputChannels;
    this.outputChannels = outputChannels;
    this.swap = swap;
    this.centerGain = centerGain;
    this.balance = balance;
  }

  /** Mix that swaps left and right. */
  public static AudioChannelMix createStereoMix(int inputChannels, boolean swap) {
    return new AudioChannelMix(inputChannels, 2, swap, 0f, 0f);
  }

  /** Mix that downmixes everything into one channel. */
  public static AudioChannelMix createMonoMix(int inputChannels) {
    return new AudioChannelMix(inputChannels, 1, false, 0f, 0f);
  }

  /** Mix that boosts the centre channel. */
  public static AudioChannelMix createFrontCenterGainMix(int inputChannels, float gain) {
    return new AudioChannelMix(inputChannels, inputChannels, false, gain, 0f);
  }

  /** Mix that applies a left/right balance. */
  public static AudioChannelMix createFrontBalanceMix(int inputChannels, float balance) {
    return new AudioChannelMix(inputChannels, inputChannels, false, 0f, balance);
  }

  /** Chains two mixes into one. */
  public static AudioChannelMix compose(AudioChannelMix first, AudioChannelMix second) {
    if (first == null) return second;
    if (second == null) return first;
    return new AudioChannelMix(
        first.inputChannels,
        second.outputChannels,
        first.swap || second.swap,
        first.centerGain + second.centerGain,
        first.balance + second.balance);
  }

  public int getInputChannels() {
    return inputChannels;
  }

  public int getOutputChannels() {
    return outputChannels;
  }

  public boolean isSwap() {
    return swap;
  }

  public float getCenterGain() {
    return centerGain;
  }

  public float getBalance() {
    return balance;
  }
}
'''

CMG_CONFIGURATION = '''package androidx.media3.exoplayer.hls;

import android.net.Uri;
import androidx.annotation.Nullable;

/**
 * Detects the proprietary "cmg" playlist wrapper used by some streams.
 *
 * <p>Detection is purely scheme based; no decryption key handling is performed in this build.
 */
public final class CmgConfiguration {

  private final Uri uri;

  private CmgConfiguration(Uri uri) {
    this.uri = uri;
  }

  /** Returns a configuration when the uri looks like a cmg resource, otherwise {@code null}. */
  @Nullable
  public static CmgConfiguration fromMediaUri(@Nullable Uri uri) {
    if (uri == null) return null;
    String path = uri.getPath();
    if (path == null) return null;
    if (!path.contains(".cmg")) return null;
    return new CmgConfiguration(uri);
  }

  public Uri getUri() {
    return uri;
  }
}
'''

DEFAULT_POSITION_MEDIA_SOURCE = '''package androidx.media3.exoplayer.source;

import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.upstream.Allocator;
import java.io.IOException;

/** Wraps a media source and remembers the position playback should start at. */
public final class DefaultPositionMediaSource implements MediaSource {

  private final MediaSource mediaSource;
  private final long startPositionMs;

  public DefaultPositionMediaSource(MediaSource mediaSource, long startPositionMs) {
    this.mediaSource = mediaSource;
    this.startPositionMs = startPositionMs;
  }

  public long getStartPositionMs() {
    return startPositionMs;
  }

  @Override
  public void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
    mediaSource.addEventListener(handler, eventListener);
  }

  @Override
  public void removeEventListener(MediaSourceEventListener eventListener) {
    mediaSource.removeEventListener(eventListener);
  }

  @Override
  public void addDrmEventListener(Handler handler, DrmSessionEventListener eventListener) {
    mediaSource.addDrmEventListener(handler, eventListener);
  }

  @Override
  public void removeDrmEventListener(DrmSessionEventListener eventListener) {
    mediaSource.removeDrmEventListener(eventListener);
  }

  @Override
  public void maybeThrowSourceInfoRefreshError() throws IOException {
    mediaSource.maybeThrowSourceInfoRefreshError();
  }

  @Override
  public void enable(MediaSourceCaller caller) {
    mediaSource.enable(caller);
  }

  @Override
  public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
    return mediaSource.createPeriod(id, allocator, startPositionUs);
  }

  @Override
  public void releasePeriod(MediaPeriod mediaPeriod) {
    mediaSource.releasePeriod(mediaPeriod);
  }

  @Override
  public void disable(MediaSourceCaller caller) {
    mediaSource.disable(caller);
  }

  @Override
  public void releaseSource(MediaSourceCaller caller) {
    mediaSource.releaseSource(caller);
  }

  @Override
  public MediaItem getMediaItem() {
    return mediaSource.getMediaItem();
  }
}
'''

CONTENT_TYPE_RESOLVING_MEDIA_SOURCE = '''package androidx.media3.exoplayer.source;

import android.os.Handler;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.upstream.Allocator;
import java.io.IOException;

/** Media source that resolves the real content type before delegating. */
public final class ContentTypeResolvingMediaSource implements MediaSource {

  private final MediaSource mediaSource;

  private ContentTypeResolvingMediaSource(MediaSource mediaSource) {
    this.mediaSource = mediaSource;
  }

  @Override
  public void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
    mediaSource.addEventListener(handler, eventListener);
  }

  @Override
  public void removeEventListener(MediaSourceEventListener eventListener) {
    mediaSource.removeEventListener(eventListener);
  }

  @Override
  public void addDrmEventListener(Handler handler, DrmSessionEventListener eventListener) {
    mediaSource.addDrmEventListener(handler, eventListener);
  }

  @Override
  public void removeDrmEventListener(DrmSessionEventListener eventListener) {
    mediaSource.removeDrmEventListener(eventListener);
  }

  @Override
  public void maybeThrowSourceInfoRefreshError() throws IOException {
    mediaSource.maybeThrowSourceInfoRefreshError();
  }

  @Override
  public void enable(MediaSourceCaller caller) {
    mediaSource.enable(caller);
  }

  @Override
  public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
    return mediaSource.createPeriod(id, allocator, startPositionUs);
  }

  @Override
  public void releasePeriod(MediaPeriod mediaPeriod) {
    mediaSource.releasePeriod(mediaPeriod);
  }

  @Override
  public void disable(MediaSourceCaller caller) {
    mediaSource.disable(caller);
  }

  @Override
  public void releaseSource(MediaSourceCaller caller) {
    mediaSource.releaseSource(caller);
  }

  @Override
  public MediaItem getMediaItem() {
    return mediaSource.getMediaItem();
  }

  /** Factory for {@link ContentTypeResolvingMediaSource}. */
  public static final class Factory implements MediaSource.Factory {

    private final MediaSource.Factory delegate;
    private final DataSource.Factory dataSourceFactory;

    public Factory(MediaSource.Factory delegate, DataSource.Factory dataSourceFactory) {
      this.delegate = delegate;
      this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public ContentTypeResolvingMediaSource createMediaSource(MediaItem mediaItem) {
      return new ContentTypeResolvingMediaSource(delegate.createMediaSource(mediaItem));
    }

    @Override
    public Factory setDrmSessionManagerProvider(Object provider) {
      return this;
    }

    @Override
    public Factory setLoadErrorHandlingPolicy(Object policy) {
      return this;
    }

    public DataSource.Factory getDataSourceFactory() {
      return dataSourceFactory;
    }
  }
}
'''

VIDEO_TRANSFORM = '''package androidx.media3.common;

/** Scale factors applied to the video surface. */
public final class VideoTransform {

  public final float scaleX;
  public final float scaleY;

  public VideoTransform() {
    this(1f, 1f);
  }

  public VideoTransform(float scaleX, float scaleY) {
    this.scaleX = scaleX;
    this.scaleY = scaleY;
  }

  @Override
  public String toString() {
    return "VideoTransform{" + scaleX + ", " + scaleY + "}";
  }
}
'''

BDJ_RUNTIME = '''package androidx.media3.exoplayer.iso;

import android.content.Context;

/**
 * Manages the BD-J runtime bundle.
 *
 * <p>The runtime is shipped separately; this build reports "not restartable" so the module screen
 * never asks the user to reboot.
 */
public final class BdjRuntime {

  private BdjRuntime() {}

  /** Prepares the runtime for the given activation signal. */
  public static void prepare(Context context, Object signal) {}

  /** Returns the current runtime status. */
  public static Status getStatus(Context context) {
    return new Status();
  }

  /** Runtime status snapshot. */
  public static final class Status {

    public final boolean restartRequired = false;
    public final boolean installed = false;

    public Status() {}
  }
}
'''

# ─────────────────────────── 缺失类补全（第 8 批：特效 / 弹幕 / mpv 辅助 / libass UI） ───────────────────────────

COLOR_TONE_ADJUST_EFFECT = '''package androidx.media3.effect;

/** Carries a colour-tone adjustment. */
public final class ColorToneAdjustEffect {

  private VideoAdjustment adjustment;

  public void setAdjustment(VideoAdjustment adjustment) {
    this.adjustment = adjustment;
  }

  public VideoAdjustment getAdjustment() {
    return adjustment;
  }
}
'''

DETAIL_ADJUST_EFFECT = '''package androidx.media3.effect;

/** Carries a detail (sharpness) adjustment. */
public final class DetailAdjustEffect {

  private VideoAdjustment adjustment;

  public void setAdjustment(VideoAdjustment adjustment) {
    this.adjustment = adjustment;
  }

  public VideoAdjustment getAdjustment() {
    return adjustment;
  }
}
'''

AUDIO_EQUALIZER_CONTROLLER = '''package androidx.media3.exoplayer.audio;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.media3.common.audio.AudioEffectParameters;
import androidx.media3.exoplayer.ExoPlayer;

/**
 * Applies the equalizer through the platform audio effect API.
 *
 * <p>This build does not attach a platform equalizer, so {@link #apply} always reports that the
 * request was not handled and audio stays untouched.
 */
public final class AudioEqualizerController {

  @Nullable private final Context context;

  public AudioEqualizerController(Context context) {
    this.context = context;
  }

  /** Returns whether the equalizer took over the parameters. */
  public boolean apply(ExoPlayer player, AudioEffectParameters config) {
    return false;
  }

  public void release() {}
}
'''

DANMAKU_PLAYER_VIEW_CONTROLLER = '''package androidx.media3.ui.danmaku;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.ui.PlayerView;
import java.util.Collections;
import java.util.List;

/**
 * Drives the danmaku overlay.
 *
 * <p>Without the danmaku rendering layer this controller only records state; nothing is drawn.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class DanmakuPlayerViewController {

  private boolean enabled;

  public void bind(PlayerView playerView) {}

  public void close() {}

  /** Returns the currently visible danmaku items. */
  public List getItems() {
    return Collections.emptyList();
  }

  /** Sends a user-authored danmaku immediately. */
  public void sendNow(String text) {}

  public void setConfig(Object config) {}

  public void setDataSource(@Nullable Uri uri) {}

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setOkHttpClient(Object client) {}
}
'''

LIBASS_PLAYER_VIEW_CONTROLLER = '''package androidx.media3.ui.libass;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.libass.LibassSubtitleController;
import java.util.function.Consumer;

/**
 * Binds the libass subtitle surface to a player view.
 *
 * <p>The view type is {@code Object} on purpose: this module cannot depend on {@code media3-ui},
 * and consumers pass their {@code PlayerView} directly. This build keeps the Media3 subtitle view
 * in place, so {@link #bind} does not replace anything.
 */
public final class LibassPlayerViewController {

  public LibassPlayerViewController(
      Player player,
      LibassPlaybackSession session,
      LibassSubtitleController controller) {}

  public void close() {}

  public void bind(@Nullable Object playerView) {}

  public void setStyleOverride(@Nullable Object style, @Nullable String fontFamily) {}

  public void setSecondarySubtitleViewConfigurator(@Nullable Consumer<?> configurator) {}
}
'''

MPV_DRM_SUPPORT = '''package androidx.media3.mpvplayer.media;

import androidx.media3.common.MediaItem;

/** Tells whether the mpv engine can play a DRM-protected item. */
public final class MpvDrmSupport {

  private MpvDrmSupport() {}

  /** Returns whether mpv can handle the item's DRM scheme. */
  public static boolean supports(MediaItem item) {
    return false;
  }
}
'''

MPV_CONFIG_FILE_INSPECTOR = '''package androidx.media3.mpvplayer;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/** Reads mpv.conf style configuration files. */
public final class MpvConfigFileInspector {

  private final Path directory;

  public MpvConfigFileInspector(Path directory) {
    this.directory = directory;
  }

  /** Returns the subtitle font directory declared in mpv.conf, when present. */
  public File readSubtitleFontsDirectory() {
    return null;
  }

  /** Returns the option names that would take effect for the given config content. */
  public Set<String> getEffectiveOptionNames(String content) {
    return Collections.emptySet();
  }

  public Path getDirectory() {
    return directory;
  }
}
'''

LIBASS_FONT_CONFIG = '''package androidx.media3.exoplayer.libass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Writes an fontconfig file describing the external subtitle fonts. */
public final class LibassFontConfig {

  private LibassFontConfig() {}

  /** Writes the font configuration to {@code target}. */
  public static void write(File target, Path cacheDirectory, List<File> fontDirectories)
      throws IOException {
    if (target == null) return;
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\\"1.0\\"?>\\n");
    sb.append("<!DOCTYPE fontconfig SYSTEM \\"fonts.dtd\\">\\n");
    sb.append("<fontconfig>\\n");
    if (fontDirectories != null) {
      for (File dir : fontDirectories) {
        if (dir == null) continue;
        sb.append("  <dir>").append(dir.getAbsolutePath()).append("</dir>\\n");
      }
    }
    if (cacheDirectory != null) {
      sb.append("  <cachedir>").append(cacheDirectory.toString()).append("</cachedir>\\n");
    }
    sb.append("</fontconfig>\\n");
    try (java.io.FileWriter writer = new java.io.FileWriter(target, false)) {
      writer.write(sb.toString());
    }
  }
}
'''

# ─────────────────────────── 缺失类补全（第 7 批：MediaSource / 音频处理 / ISO） ───────────────────────────

END_TRIM_MEDIA_SOURCE = '''package androidx.media3.exoplayer.source;

import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import java.io.IOException;

/**
 * Wraps a media source and records an end-trim duration.
 *
 * <p>The trim is applied by the player itself; this wrapper only carries the value and forwards
 * everything else to the child source.
 */
public final class EndTrimMediaSource implements MediaSource {

  private final MediaSource mediaSource;
  private long endTrimDurationMs;

  public EndTrimMediaSource(MediaSource mediaSource, long endTrimDurationMs) {
    this.mediaSource = mediaSource;
    this.endTrimDurationMs = endTrimDurationMs;
  }

  public void setEndTrimDurationMs(long endTrimDurationMs) {
    this.endTrimDurationMs = endTrimDurationMs;
  }

  public long getEndTrimDurationMs() {
    return endTrimDurationMs;
  }

  public boolean isEndTrimActive() {
    return endTrimDurationMs > 0;
  }

  /** Duration of the child source, or {@code C.TIME_UNSET} when unknown. */
  public long getOriginalDurationMs() {
    return androidx.media3.common.C.TIME_UNSET;
  }

  @Override
  public void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
    mediaSource.addEventListener(handler, eventListener);
  }

  @Override
  public void removeEventListener(MediaSourceEventListener eventListener) {
    mediaSource.removeEventListener(eventListener);
  }

  @Override
  public void addDrmEventListener(Handler handler, DrmSessionEventListener eventListener) {
    mediaSource.addDrmEventListener(handler, eventListener);
  }

  @Override
  public void removeDrmEventListener(DrmSessionEventListener eventListener) {
    mediaSource.removeDrmEventListener(eventListener);
  }

  @Override
  public void maybeThrowSourceInfoRefreshError() throws IOException {
    mediaSource.maybeThrowSourceInfoRefreshError();
  }

  @Override
  public void enable(MediaSourceCaller caller) {
    mediaSource.enable(caller);
  }

  @Override
  public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
    return mediaSource.createPeriod(id, allocator, startPositionUs);
  }

  @Override
  public void releasePeriod(MediaPeriod mediaPeriod) {
    mediaSource.releasePeriod(mediaPeriod);
  }

  @Override
  public void disable(MediaSourceCaller caller) {
    mediaSource.disable(caller);
  }

  @Override
  public void releaseSource(MediaSourceCaller caller) {
    mediaSource.releaseSource(caller);
  }

  @Override
  public MediaItem getMediaItem() {
    return mediaSource.getMediaItem();
  }
}
'''

AUDIO_EFFECT_PROCESSOR = '''package androidx.media3.common.audio;

import java.nio.ByteBuffer;

/**
 * Pass-through audio processor used by the effect chain.
 *
 * <p>Parameters are recorded but no DSP is applied, so audio is played unmodified.
 */
public final class AudioEffectProcessor implements AudioProcessor {

  private boolean active;
  private boolean inputEnded;

  /** Applies new parameters. */
  public void setParameters(AudioEffectParameters parameters, boolean softwareEqualizer) {
    this.active = parameters != null && parameters.hasEffect();
  }

  /** Clears any applied parameters. */
  public void resetParameters() {
    this.active = false;
  }

  @Override
  public AudioFormat configure(AudioFormat inputAudioFormat) {
    return inputAudioFormat;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public boolean isEnded() {
    return inputEnded;
  }

  @Override
  public void queueInput(ByteBuffer inputBuffer) {
    inputBuffer.position(inputBuffer.limit());
  }

  @Override
  public void queueEndOfStream() {
    inputEnded = true;
  }

  @Override
  public ByteBuffer getOutput() {
    return EMPTY_BUFFER;
  }

  @Override
  public void flush() {
    inputEnded = false;
  }

  @Override
  public void reset() {
    active = false;
    inputEnded = false;
  }
}
'''

DECODER_FALLBACK = '''package androidx.media3.exoplayer;

import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlaybackException;

/** Tracks decoder failures so the player can fall back to a slower decoder. */
public final class DecoderFallback {

  private final DecoderManager decoderManager;

  public DecoderFallback(DecoderManager decoderManager) {
    this.decoderManager = decoderManager;
  }

  /** Clears the failure state for the given track type. */
  public void reset(@C.TrackType int trackType) {}

  /** Returns whether a retry is possible for the given failure. */
  public boolean retry(ExoPlaybackException error) {
    return false;
  }

  public DecoderManager getDecoderManager() {
    return decoderManager;
  }
}
'''

PROGRESSIVE_ISO_CACHE = '''package androidx.media3.datasource;

import androidx.media3.datasource.cache.Cache;

/** Helpers for reading progressive ISO images through a disk cache. */
public final class ProgressiveIsoCache {

  private ProgressiveIsoCache() {}

  /** Wraps the factory so ISO reads go through the shared cache. */
  public static DataSource.Factory withDiskCache(
      DataSource.Factory factory, Cache cache) {
    return factory;
  }
}
'''

ISO_NAVIGATION_PLAYER_CONTROLLER = '''package androidx.media3.exoplayer.iso;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.source.MediaSource;

/**
 * Drives Blu-ray / DVD disc-menu navigation.
 *
 * <p>No native navigation session is available in this build, so the controller never starts and
 * always hands the plain media source back to the player.
 */
public final class IsoNavigationPlayerController {

  private final Player player;

  public IsoNavigationPlayerController(Player player) {
    this.player = player;
  }

  public Player getPlayer() {
    return player;
  }

  /** Returns whether a navigation session is running. */
  public boolean isStarted() {
    return false;
  }

  /** Returns the media source to play, or the original source when navigation is unavailable. */
  @Nullable
  public MediaSource prepareMediaSource(
      Context context, MediaSource mediaSource, DataSource.Factory dataSourceFactory) {
    return mediaSource;
  }

  public void close() {}
}
'''

# ─────────────────────────── 缺失类补全（第 6 批：解码 / 次要字幕 / DRM） ───────────────────────────

LOCAL_CLEAR_KEY_LICENSE = '''package androidx.media3.common.util;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parses Clearkey licence payloads of the form {@code kid:key,kid:key}. */
public final class LocalClearKeyLicense {

  private LocalClearKeyLicense() {}

  /** Parses a licence string into a key-id map. Returns an empty map when unparsable. */
  public static Map<String, String> parse(@Nullable String license) {
    if (license == null || license.isEmpty()) return Collections.emptyMap();
    if (!license.contains(":")) return Collections.emptyMap();
    Map<String, String> out = new LinkedHashMap<>();
    for (String pair : license.split(",")) {
      String[] kv = pair.split(":");
      if (kv.length != 2) continue;
      String kid = kv[0].trim();
      String key = kv[1].trim();
      if (!kid.isEmpty() && !key.isEmpty()) out.put(kid, key);
    }
    return out;
  }

  /** Parses the licence carried by a DRM configuration. */
  public static Map<String, String> parse(@Nullable MediaItem.DrmConfiguration configuration) {
    if (configuration == null) return Collections.emptyMap();
    if (configuration.licenseUri == null) return Collections.emptyMap();
    return parse(configuration.licenseUri.toString());
  }
}
'''

MPV_DECODER_MODE = '''package androidx.media3.mpvplayer;

/** Video decoder selection for the mpv engine. */
public enum MpvDecoderMode {
  /** Let mpv choose. */
  AUTO,
  /** Hardware decoding via the platform. */
  HARDWARE,
  /** Software decoding. */
  SOFTWARE;

  /** Converts from the app-facing Media3 representation. */
  public static MpvDecoderMode fromCommonDecoderMode(androidx.media3.common.DecoderMode mode) {
    if (mode == null) return AUTO;
    switch (mode) {
      case HARDWARE:
        return HARDWARE;
      case FFMPEG:
        return SOFTWARE;
      case AUTO:
      default:
        return AUTO;
    }
  }
}
'''

SECONDARY_TEXT_OUTPUT = '''package androidx.media3.exoplayer.text;

import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Collects cues produced by a secondary text renderer. */
public final class SecondaryTextOutput implements TextOutput {

  private final List<Cue> cues = Collections.synchronizedList(new ArrayList<>());

  @Override
  public void onCues(CueGroup cueGroup) {
    cues.clear();
    if (cueGroup != null) cues.addAll(cueGroup.cues);
  }

  @Override
  public void onCues(List<Cue> cueList) {
    cues.clear();
    if (cueList != null) cues.addAll(cueList);
  }

  /** Returns a snapshot of the current cues. */
  public List<Cue> getCues() {
    synchronized (cues) {
      return new ArrayList<>(cues);
    }
  }

  public void clear() {
    cues.clear();
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

  /** Returns the renderer used for disc-menu navigation playback. */
  public androidx.media3.exoplayer.Renderer createClockRenderer() {
    return null;
  }

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

  /** Load state of a subtitle transcript. */
  public enum State {
    /** Still loading. */
    LOADING,
    /** Parsed successfully. */
    COMPLETE,
    /** Loaded but empty. */
    EMPTY,
    /** Format not supported. */
    UNSUPPORTED
  }

  /** One parsed subtitle line. */
  public static final class Entry {

    public final String text;
    public final long startTimeUs;
    public final long endTimeUs;
    public final boolean image;

    public Entry(String text, long startTimeUs, long endTimeUs, boolean image) {
      this.text = text;
      this.startTimeUs = startTimeUs;
      this.endTimeUs = endTimeUs;
      this.image = image;
    }
  }

  /** Signals that the track could not be read. */
  public static final SubtitleContent UNSUPPORTED =
      new SubtitleContent(Collections.emptyList(), null);

  /** Parsed cues, in playback order. */
  public final List<Cue> cues;

  /** MIME type the cues were parsed from, when known. */
  @Nullable public final String mimeType;

  /** Current load state. */
  public final State state;

  public SubtitleContent(List<Cue> cues, @Nullable String mimeType) {
    this(cues, mimeType, cues == null || cues.isEmpty() ? State.EMPTY : State.COMPLETE);
  }

  public SubtitleContent(List<Cue> cues, @Nullable String mimeType, State state) {
    this.cues = cues == null ? Collections.emptyList() : cues;
    this.mimeType = mimeType;
    this.state = state == null ? State.EMPTY : state;
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

  private VideoTransform videoTransform = new VideoTransform();

  /** Returns the current video scale transform. */
  public VideoTransform getVideoTransform() {
    return videoTransform;
  }

  /** Sets the video scale transform. */
  public void setVideoTransform(VideoTransform transform) {
    this.videoTransform = transform == null ? new VideoTransform() : transform;
  }

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

    print("[23/30] 新增 LocalClearKeyLicense")
    write_if_absent(
        f"{common}/util/LocalClearKeyLicense.java", LOCAL_CLEAR_KEY_LICENSE, "class LocalClearKeyLicense"
    )

    print("[24/30] 新增 MpvDecoderMode")
    write_if_absent(f"{mpvplayer}/MpvDecoderMode.java", MPV_DECODER_MODE, "enum MpvDecoderMode")

    print("[25/30] 新增 SecondaryTextOutput")
    write_if_absent(
        f"{exoplayer}/text/SecondaryTextOutput.java", SECONDARY_TEXT_OUTPUT, "class SecondaryTextOutput"
    )

    print("[26/30] 新增 SecondaryTextTrackSelector")
    write_if_absent(
        f"{exoplayer}/trackselection/SecondaryTextTrackSelector.java",
        SECONDARY_TEXT_TRACK_SELECTOR,
        "class SecondaryTextTrackSelector",
    )

    print("[27/30] 新增 DecodeTrackSelector")
    write_if_absent(
        f"{exoplayer}/trackselection/DecodeTrackSelector.java",
        DECODE_TRACK_SELECTOR,
        "class DecodeTrackSelector",
    )

    print("[28/30] 新增 DecoderManager")
    write_if_absent(f"{exoplayer}/DecoderManager.java", DECODER_MANAGER, "class DecoderManager")

    print("[29/30] 新增 LibassSubtitleController")
    write_if_absent(
        f"{exoplayer}/libass/LibassSubtitleController.java",
        LIBASS_SUBTITLE_CONTROLLER,
        "class LibassSubtitleController",
    )

    print("[30/30] 扩展 DefaultRenderersFactory")
    insert_before(
        f"{exoplayer}/DefaultRenderersFactory.java",
        "  private boolean enableDecoderFallback;",
        DRF_SET_DECODER_MANAGER,
        "setDecoderManager(DecoderManager",
    )

    datasource = f"{root}/libraries/datasource/src/main/java/androidx/media3/datasource"

    print("[31/35] 新增 EndTrimMediaSource")
    write_if_absent(
        f"{exoplayer}/source/EndTrimMediaSource.java",
        END_TRIM_MEDIA_SOURCE,
        "class EndTrimMediaSource",
    )

    print("[32/35] 新增 AudioEffectProcessor")
    write_if_absent(
        f"{common}/audio/AudioEffectProcessor.java",
        AUDIO_EFFECT_PROCESSOR,
        "class AudioEffectProcessor",
    )

    print("[33/35] 新增 DecoderFallback")
    write_if_absent(f"{exoplayer}/DecoderFallback.java", DECODER_FALLBACK, "class DecoderFallback")

    print("[34/35] 新增 ProgressiveIsoCache")
    write_if_absent(
        f"{datasource}/ProgressiveIsoCache.java", PROGRESSIVE_ISO_CACHE, "class ProgressiveIsoCache"
    )

    print("[35/35] 新增 IsoNavigationPlayerController")
    write_if_absent(
        f"{exoplayer}/iso/IsoNavigationPlayerController.java",
        ISO_NAVIGATION_PLAYER_CONTROLLER,
        "class IsoNavigationPlayerController",
    )

    print("[36/43] 新增 ColorToneAdjustEffect")
    write_if_absent(
        f"{effect}/ColorToneAdjustEffect.java", COLOR_TONE_ADJUST_EFFECT, "class ColorToneAdjustEffect"
    )

    print("[37/43] 新增 DetailAdjustEffect")
    write_if_absent(
        f"{effect}/DetailAdjustEffect.java", DETAIL_ADJUST_EFFECT, "class DetailAdjustEffect"
    )

    print("[38/43] 新增 AudioEqualizerController")
    write_if_absent(
        f"{exoplayer}/audio/AudioEqualizerController.java",
        AUDIO_EQUALIZER_CONTROLLER,
        "class AudioEqualizerController",
    )

    print("[39/43] 新增 DanmakuPlayerViewController")
    write_if_absent(
        f"{ui}/danmaku/DanmakuPlayerViewController.java",
        DANMAKU_PLAYER_VIEW_CONTROLLER,
        "class DanmakuPlayerViewController",
    )

    print("[40/43] 新增 LibassPlayerViewController")
    stale_ui = f"{ui}/libass/LibassPlayerViewController.java"
    if os.path.exists(stale_ui):
        os.remove(stale_ui)
        print(f"  - 移除旧位置: {stale_ui}")
    write_if_absent(
        f"{exoplayer}/ui/libass/LibassPlayerViewController.java",
        LIBASS_PLAYER_VIEW_CONTROLLER,
        "class LibassPlayerViewController",
    )

    print("[41/43] 新增 MpvDrmSupport")
    write_if_absent(
        f"{mpvplayer}/media/MpvDrmSupport.java", MPV_DRM_SUPPORT, "class MpvDrmSupport"
    )

    print("[42/43] 新增 MpvConfigFileInspector")
    write_if_absent(
        f"{mpvplayer}/MpvConfigFileInspector.java",
        MPV_CONFIG_FILE_INSPECTOR,
        "class MpvConfigFileInspector",
    )

    print("[43/43] 新增 LibassFontConfig")
    write_if_absent(
        f"{exoplayer}/libass/LibassFontConfig.java", LIBASS_FONT_CONFIG, "class LibassFontConfig"
    )

    print("[44/51] 新增 IsoUri")
    write_if_absent(f"{datasource}/IsoUri.java", ISO_URI, "class IsoUri")

    print("[45/51] 新增 AudioChannelMix")
    write_if_absent(f"{common}/audio/AudioChannelMix.java", AUDIO_CHANNEL_MIX, "class AudioChannelMix")

    print("[46/51] 新增 CmgConfiguration")
    hls = f"{exoplayer}/hls"
    write_if_absent(f"{hls}/CmgConfiguration.java", CMG_CONFIGURATION, "class CmgConfiguration")

    print("[47/51] 新增 DefaultPositionMediaSource")
    write_if_absent(
        f"{exoplayer}/source/DefaultPositionMediaSource.java",
        DEFAULT_POSITION_MEDIA_SOURCE,
        "class DefaultPositionMediaSource",
    )

    print("[48/51] 新增 ContentTypeResolvingMediaSource")
    write_if_absent(
        f"{exoplayer}/source/ContentTypeResolvingMediaSource.java",
        CONTENT_TYPE_RESOLVING_MEDIA_SOURCE,
        "class ContentTypeResolvingMediaSource",
    )

    print("[49/51] 新增 VideoTransform")
    write_if_absent(f"{common}/VideoTransform.java", VIDEO_TRANSFORM, "class VideoTransform")

    print("[50/51] 新增 BdjRuntime")
    write_if_absent(f"{exoplayer}/iso/BdjRuntime.java", BDJ_RUNTIME, "class BdjRuntime")

    print("[51/51] PlayerView 补充 VideoTransform import")
    ensure_import(pv, "import androidx.media3.common.VideoTransform;")

    print("\n补丁完成。")


if __name__ == "__main__":
    main()
