package de.cubevote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;

/** Eingebauter Browser, der die Vote-Seiten nacheinander abarbeitet. */
public class VoteActivity extends Activity {

    public static final String EXTRA_STATS_ONLY = "statsOnly";

    private WebView web;
    private TextView title, status;
    private ProgressBar progress;
    private Button skip;

    private List<JSONObject> sites;
    private int index = -1;
    private String name;
    private String script;
    private boolean finished = false;
    private int doneFor = -1;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        name = Sites.prefs(this).getString("name", "");
        sites = Sites.enabled(this);
        script = Sites.readAsset(this, "voter.js");

        // ---- Oberfläche ----
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.parseColor("#2E7D32"));
        int pad = dp(10);
        bar.setPadding(pad, pad / 2, pad / 2, pad / 2);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(16);
        status = new TextView(this);
        status.setTextColor(Color.parseColor("#E8F5E9"));
        status.setTextSize(13);
        texts.addView(title);
        texts.addView(status);
        bar.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        skip = new Button(this);
        skip.setText("Weiter ⏭");
        skip.setOnClickListener(v -> next());
        bar.addView(skip);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(1, sites.size()));

        web = new WebView(this);
        root.addView(bar);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6)));
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        // ---- Browser ----
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setSupportMultipleWindows(false);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.addJavascriptInterface(new Bridge(), "CVBridge");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String scheme = req.getUrl().getScheme();
                // Nur normale Webseiten laden, keine fremden Apps öffnen
                return !("http".equals(scheme) || "https".equals(scheme));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                inject(url);
            }
        });

        if (getIntent().getBooleanExtra(EXTRA_STATS_ONLY, false)) {
            showStats();
        } else if (name.isEmpty() || sites.isEmpty()) {
            Toast.makeText(this, "Bitte zuerst deinen Namen eintragen", Toast.LENGTH_LONG).show();
            finish();
        } else {
            next();
        }
    }

    private void inject(String url) {
        if (finished || index < 0 || index >= sites.size() || url == null) return;
        JSONObject site = sites.get(index);
        if (!url.contains(site.optString("match"))) return;
        String call = script + "\n;CV.run(" + site + "," + JSONObject.quote(name) + "," + index + ");";
        web.evaluateJavascript(call, null);
    }

    private void next() {
        if (finished) {
            finish();
            return;
        }
        index++;
        progress.setProgress(index);
        if (index >= sites.size()) {
            Sites.markVoted(this);
            Toast.makeText(this, "Fertig! Alle Votes erledigt ✅", Toast.LENGTH_LONG).show();
            showStats();
            return;
        }
        JSONObject site = sites.get(index);
        title.setText("Seite " + (index + 1) + "/" + sites.size() + ": " + site.optString("name"));
        status.setText("Lade Seite …");
        web.loadUrl(site.optString("url"));
    }

    private void showStats() {
        finished = true;
        title.setText("Deine Vote-Statistik");
        status.setText(Sites.votedToday(this) ? "Heute erledigt ✅" : "");
        skip.setText("Schließen");
        progress.setProgress(progress.getMax());
        web.loadUrl(Sites.STATS_URL + Uri.encode(name));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack() && finished) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }

    /** Wird aus voter.js aufgerufen. */
    private class Bridge {
        @JavascriptInterface
        public void done(int idx) {
            runOnUiThread(() -> {
                if (idx == index && idx != doneFor && !finished) {
                    doneFor = idx;
                    status.setText("Erledigt ✔");
                    web.postDelayed(() -> {
                        if (index == idx) next(); // nicht doppelt weiter, falls schon übersprungen
                    }, 1200);
                }
            });
        }

        @JavascriptInterface
        public void status(String text) {
            runOnUiThread(() -> {
                if (!finished) status.setText(text);
            });
        }
    }
}
