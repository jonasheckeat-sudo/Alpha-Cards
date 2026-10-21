package de.alphacards.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    WebView w;
    String pending;

    void showError(String text) {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.BLACK);
        TextView t = new TextView(this);
        t.setText("Alpha Cards Fehler:\n\n" + text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(11);
        t.setPadding(24, 120, 24, 24);
        sv.addView(t);
        setContentView(sv);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final SharedPreferences p = getSharedPreferences("crash", 0);
        String last = p.getString("t", null);
        if (last != null) {
            p.edit().remove("t").apply();
            showError(last);
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler((th, e) -> {
            p.edit().putString("t", Log.getStackTraceString(e)).commit();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        });
        try {
            start();
        } catch (Throwable e) {
            showError(Log.getStackTraceString(e));
        }
    }

    void start() {
        getWindow().setStatusBarColor(Color.parseColor("#0a0b10"));
        getWindow().setNavigationBarColor(Color.parseColor("#0a0b10"));
        w = new WebView(this);
        w.setBackgroundColor(Color.parseColor("#0a0b10"));
        setContentView(w);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        w.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override
            public void onPageFinished(WebView v, String u) {
                flush();
            }
        });
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        w.addJavascriptInterface(new Bridge(), "AC");
        take(getIntent());
        w.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
    }

    public class Bridge {
        @JavascriptInterface
        public String takeResults() {
            SharedPreferences p = getSharedPreferences("rem", 0);
            String r = p.getString("results", "[]");
            p.edit().remove("results").apply();
            return r;
        }

        @JavascriptInterface
        public void setReminders(String j) {
            getSharedPreferences("rem", 0).edit().putString("json", j).apply();
            Scheduler.schedule(MainActivity.this);
        }

        @JavascriptInterface
        public void share(String t) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, t);
            startActivity(Intent.createChooser(i, "Teilen"));
        }
    }

    void take(Intent i) {
        if (i != null && i.getData() != null) {
            String d = i.getData().getQueryParameter("d");
            if (d != null) pending = d;
        }
    }

    void flush() {
        if (pending != null && w != null) {
            w.evaluateJavascript("window.impDeck&&impDeck(" + org.json.JSONObject.quote(pending) + ")", null);
            pending = null;
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        take(i);
        flush();
    }

    @Override
    public void onBackPressed() {
        if (w == null) {
            finish();
            return;
        }
        w.evaluateJavascript("(function(){var m=document.querySelector('.modal');if(m){m.remove();return 1}return 0})()",
            new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String v) {
                    if (!"1".equals(v)) finish();
                }
            });
    }
}
