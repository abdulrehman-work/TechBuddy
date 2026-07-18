package org.seniorconnect.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * TV-style YouTube screen. Full screen from entry, auto-play, auto-advance,
 * no picture-in-picture, no state kept between sessions. A touch shield
 * swallows every touch on the video; only the app's own Home, Next, and
 * Stop buttons are tappable. Ads are never blocked, skipped, or clicked —
 * the shield plus a passive notice is the only ad behavior (see AGENTS.md).
 */
public final class YouTubeActivity extends Activity {

    private WebView webView;
    private TextView adNotice;
    private TextView unavailableNotice;
    private String playlistJson;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube);

        adNotice = findViewById(R.id.yt_ad_notice);
        unavailableNotice = findViewById(R.id.yt_unavailable);
        webView = findViewById(R.id.yt_webview);

        playlistJson = loadShuffledPlaylistForCountry();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setSupportMultipleWindows(true); // popups get no window
        webView.addJavascriptInterface(new Bridge(), "SeniorConnect");
        webView.setWebViewClient(new WebViewClient() {
            // The only page is our bundled player. Every navigation attempt —
            // links, redirects, intent:// and app deep links, the YouTube app,
            // external browsers — is ignored.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }
        });

        // The shield consumes all touches over the video, including ads and
        // install prompts, so a stray tap cannot leave the screen.
        findViewById(R.id.yt_touch_shield).setOnClickListener(v -> { });

        findViewById(R.id.yt_home_button).setOnClickListener(v -> finish());
        findViewById(R.id.yt_stop_button).setOnClickListener(v -> finish());
        findViewById(R.id.yt_next_button).setOnClickListener(v -> {
            showAdNotice();
            webView.evaluateJavascript("nextVideo()", null);
        });

        showAdNotice();
        // Any well-formed https origin works for the embed; claiming to be
        // youtube.com itself gets rejected by the player.
        webView.loadDataWithBaseURL(
                "https://seniorconnect.invalid",
                readAsset("player.html"),
                "text/html",
                "utf-8",
                null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // No state survives: leaving ends the session; re-entering starts fresh.
        finish();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void showAdNotice() {
        runOnUiThread(() -> adNotice.setVisibility(View.VISIBLE));
    }

    /** Picks this device's country list (or the default), shuffled so every
     *  session starts from a random video. Returns a JSON array of video IDs. */
    private String loadShuffledPlaylistForCountry() {
        try {
            JSONObject root = new JSONObject(readAsset("playlists/playlists.json"));
            JSONObject countries = root.getJSONObject("countries");
            String country = Locale.getDefault().getCountry().toUpperCase(Locale.ROOT);
            if (!countries.has(country)) {
                country = root.getString("default_country");
            }
            JSONArray videos = countries.getJSONObject(country).getJSONArray("videos");
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < videos.length(); i++) {
                ids.add(videos.getJSONObject(i).getString("id"));
            }
            Collections.shuffle(ids);
            return new JSONArray(ids).toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    private String readAsset(String name) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(name), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            return "";
        }
        return sb.toString();
    }

    private final class Bridge {
        @JavascriptInterface
        public String getPlaylistJson() {
            return playlistJson;
        }

        /** Content clock advancing past ~1s means the actual video (not an ad)
         *  is playing, so the passive ad notice can come down. */
        @JavascriptInterface
        public void onContentProgress(double seconds) {
            if (seconds > 1.0) {
                runOnUiThread(() -> adNotice.setVisibility(View.GONE));
            }
        }

        @JavascriptInterface
        public void onVideoChanged() {
            showAdNotice();
        }

        @JavascriptInterface
        public void onAllVideosFailed() {
            runOnUiThread(() -> {
                adNotice.setVisibility(View.GONE);
                unavailableNotice.setVisibility(View.VISIBLE);
            });
        }
    }
}
