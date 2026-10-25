package com.github.catvod.net;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Set;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Rewrites GitHub <em>download</em> URLs through a mirror so APK / runtime-module downloads keep
 * working on networks where {@code github.com} is unreachable.
 *
 * <p>Only release-asset and raw-file URLs are rewritten. API calls ({@code api.github.com}) are
 * intentionally left untouched so that update checks still see real JSON instead of mirror HTML.
 */
public final class GitHubMirrorInterceptor implements Interceptor {

    /** Mirror prefix, e.g. {@code https://gh-proxy.com/} or {@code https://ghfast.top/}. */
    public static volatile String mirror = "https://gh-proxy.com/";

    /** Set to {@code false} to fall back to direct connections. */
    public static volatile boolean enabled = true;

    private static final Set<String> MIRRORED_HOSTS =
            Set.of("github.com", "objects.githubusercontent.com", "raw.githubusercontent.com", "codeload.github.com");

    /** Enables / disables mirroring and optionally switches to another mirror prefix. */
    public static void configure(boolean on, String prefix) {
        enabled = on;
        if (prefix != null && !prefix.isEmpty()) mirror = prefix.endsWith("/") ? prefix : prefix + "/";
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url();
        if (!enabled || mirror.isEmpty() || !shouldMirror(url)) return chain.proceed(request);
        HttpUrl mirrored = HttpUrl.get(mirror + url);
        return chain.proceed(request.newBuilder().url(mirrored).build());
    }

    private boolean shouldMirror(HttpUrl url) {
        if (!MIRRORED_HOSTS.contains(url.host())) return false;
        String path = url.encodedPath();
        return path.contains("/releases/download/")
                || path.contains("/raw/")
                || url.host().startsWith("raw.")
                || url.host().startsWith("objects.")
                || url.host().startsWith("codeload.");
    }
}
