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

# ─────────────────────────── 插入片段 ───────────────────────────

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

    print("\n补丁完成。")


if __name__ == "__main__":
    main()
