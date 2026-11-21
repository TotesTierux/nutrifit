package de.nutriscan.app;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 42;
    private static final int REQ_NOTIF = 43;
    private WebView web;
    private PermissionRequest pendingRequest;
    private ValueCallback<Uri[]> fileCallback;
    private static final int REQ_FILE = 44;
    private String pendingShared;
    private boolean pageReady;
    private String browserUA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0F766E"));
        getWindow().setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(0x00000010 /* SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR */);
        }

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F4F6F5"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        browserUA = s.getUserAgentString();
        s.setUserAgentString(browserUA + " NutriFit/2.0 (Android)");

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        pendingShared = sharedText(getIntent());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                deliverShared();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri u = Uri.parse(url);
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                try {
                    startActivityForResult(Intent.createChooser(pick, "Foto auswählen"), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boolean wantsCamera = false;
                        for (String r : request.getResources()) {
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) wantsCamera = true;
                        }
                        if (!wantsCamera) { request.deny(); return; }
                        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                        } else {
                            pendingRequest = request;
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
                        }
                    }
                });
            }
        });

        Notifs.ensureChannels(this);
        setContentView(web);
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html" + (getIntent().getStringExtra("openTab") != null ? "#" + getIntent().getStringExtra("openTab") : ""));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_NOTIF) {
            web.evaluateJavascript("window.onNotifPermission && window.onNotifPermission()", null);
            return;
        }
        if (requestCode == REQ_CAMERA && pendingRequest != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            } else {
                pendingRequest.deny();
            }
            pendingRequest = null;
        }
    }

    private String sharedText(Intent i) {
        if (i != null && Intent.ACTION_SEND.equals(i.getAction())) {
            CharSequence t = i.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (t != null) return t.toString();
        }
        return null;
    }

    private void deliverShared() {
        if (!pageReady || pendingShared == null) return;
        final String js = "window.onSharedText && window.onSharedText(" + JSONObject.quote(pendingShared) + ")";
        pendingShared = null;
        web.postDelayed(new Runnable() {
            @Override
            public void run() { web.evaluateJavascript(js, null); }
        }, 300);
    }

    private File dataFile(String name) {
        return new File(getFilesDir(), name);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String t = sharedText(intent);
        if (t != null) { pendingShared = t; deliverShared(); }
        String ot = intent.getStringExtra("openTab");
        if ("fast".equals(ot) || "today".equals(ot) || "train".equals(ot)) {
            web.evaluateJavascript("window.applyHabitStops && applyHabitStops(); window.openTabFromNative && window.openTabFromNative('" + ot + "')", null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.onAndroidBack ? window.onAndroidBack() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"true".equals(value)) MainActivity.super.onBackPressed();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.evaluateJavascript("typeof stopScan==='function' && stopScan()", null);
    }

    private class Bridge {
        @JavascriptInterface
        public void quickState(String json) { QuickActions.setFromApp(MainActivity.this, json); }

        @JavascriptInterface
        public int appVersion() {
            try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; }
            catch (Exception e) { return 0; }
        }

        @JavascriptInterface
        public boolean canInstall() {
            return Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls();
        }

        @JavascriptInterface
        public void openInstallSettings() {
            Intent i = new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES", Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(i); } catch (Exception ignored) { }
        }

        /** Lädt die neue APK und übergibt sie dem Android-Installer (der Nutzer bestätigt). */
        @JavascriptInterface
        public void installUpdate(final String url) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String err = null;
                    HttpURLConnection c = null;
                    try {
                        URL u = new URL(url);
                        if (!"https".equals(u.getProtocol())) throw new Exception("https only");
                        c = (HttpURLConnection) u.openConnection();
                        c.setConnectTimeout(15000);
                        c.setReadTimeout(60000);
                        c.setInstanceFollowRedirects(true);
                        c.setRequestProperty("User-Agent", "NutriFit-Updater");
                        int st = c.getResponseCode();
                        if (st != 200) throw new Exception("HTTP " + st);
                        long len = c.getContentLength();
                        android.content.pm.PackageInstaller pi = getPackageManager().getPackageInstaller();
                        android.content.pm.PackageInstaller.SessionParams sp = new android.content.pm.PackageInstaller.SessionParams(
                                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                        sp.setAppPackageName(getPackageName());
                        int sid = pi.createSession(sp);
                        android.content.pm.PackageInstaller.Session ses = pi.openSession(sid);
                        java.io.OutputStream out = ses.openWrite("nutrifit.apk", 0, len > 0 ? len : -1);
                        InputStream in = c.getInputStream();
                        byte[] buf = new byte[65536];
                        long done = 0;
                        int n, lastPct = -1;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                            done += n;
                            if (len > 0) {
                                final int pct = (int) (done * 100 / len);
                                if (pct != lastPct) {
                                    lastPct = pct;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() { web.evaluateJavascript("window.onUpdateProgress && onUpdateProgress(" + pct + ")", null); }
                                    });
                                }
                            }
                        }
                        in.close();
                        ses.fsync(out);
                        out.close();
                        Intent done2 = new Intent(MainActivity.this, InstallReceiver.class);
                        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                        PendingIntent cb = PendingIntent.getBroadcast(MainActivity.this, sid, done2, flags);
                        ses.commit(cb.getIntentSender());
                        ses.close();
                    } catch (Exception e) {
                        err = e.getMessage() == null ? e.toString() : e.getMessage();
                    } finally {
                        if (c != null) c.disconnect();
                    }
                    final String js = "window.onUpdateDone && onUpdateDone(" + JSONObject.quote(err == null ? "" : err) + ")";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { web.evaluateJavascript(js, null); }
                    });
                }
            }).start();
        }

        @JavascriptInterface
        public void habitOngoing(String hid, String title, String startMs) {
            Notifs.showHabit(MainActivity.this, hid, title, Long.parseLong(startMs));
        }

        @JavascriptInterface
        public void habitClear(String hid) { Notifs.clearHabit(MainActivity.this, hid); }

        @JavascriptInterface
        public String takeHabitStops() { return Notifs.takeHabitStops(MainActivity.this); }

        @JavascriptInterface
        public void saveFile(String name, String content) {
            if (!name.matches("[a-z]+\\.json")) return;
            try {
                File tmp = dataFile(name + ".tmp");
                FileOutputStream out = new FileOutputStream(tmp);
                out.write(content.getBytes("UTF-8"));
                out.close();
                tmp.renameTo(dataFile(name));
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public String loadFile(String name) {
            if (!name.matches("[a-z]+\\.json")) return null;
            try {
                File f = dataFile(name);
                if (!f.exists()) return null;
                FileInputStream in = new FileInputStream(f);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                in.close();
                return out.toString("UTF-8");
            } catch (Exception e) { return null; }
        }

        @JavascriptInterface
        public String notifPermission() {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                return shouldShowRequestPermissionRationale("android.permission.POST_NOTIFICATIONS") ? "ask" : "unknown";
            }
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            return nm.areNotificationsEnabled() ? "granted" : "blocked";
        }

        @JavascriptInterface
        public void requestNotifPermission() {
            if (Build.VERSION.SDK_INT >= 33) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF); }
                });
            }
        }

        @JavascriptInterface
        public void openNotifSettings() {
            Intent i = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
            i.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(i); } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void scheduleNotifs(String json) { Notifs.schedule(MainActivity.this, json); }

        @JavascriptInterface
        public void setOngoing(String startMs, String goalMs, String title, String text) {
            Notifs.showOngoing(MainActivity.this, Long.parseLong(startMs), Long.parseLong(goalMs), title, text);
        }

        @JavascriptInterface
        public void clearOngoing() { Notifs.clearOngoing(MainActivity.this); }

        @JavascriptInterface
        public void httpGet(final String id, final String url) {
            httpRequest(id, "GET", url, "{}", null);
        }

        /** Allgemeine HTTPS-Anfrage (z. B. Gemini). Antwort kommt über window.__httpCb zurück. */
        @JavascriptInterface
        public void httpRequest(final String id, final String method, final String url, final String headersJson, final String body) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int status = 0;
                    String resp = "";
                    HttpURLConnection c = null;
                    try {
                        URL u = new URL(url);
                        if (!"https".equals(u.getProtocol())) throw new Exception("https only");
                        c = (HttpURLConnection) u.openConnection();
                        boolean post = "POST".equals(method);
                        c.setConnectTimeout(15000);
                        c.setReadTimeout(post ? 90000 : 12000);
                        c.setInstanceFollowRedirects(true);
                        boolean isOff = u.getHost().endsWith("openfoodfacts.org");
                        c.setRequestProperty("User-Agent", isOff ? "NutriFit/2.0 (Android; private use)" : browserUA);
                        c.setRequestProperty("Accept-Language", "de-DE,de;q=0.9,en;q=0.6");
                        c.setRequestProperty("Accept", "text/html,application/json;q=0.9,*/*;q=0.8");
                        JSONObject h = new JSONObject(headersJson == null ? "{}" : headersJson);
                        java.util.Iterator<String> keys = h.keys();
                        while (keys.hasNext()) { String k = keys.next(); c.setRequestProperty(k, h.getString(k)); }
                        if (post) {
                            c.setRequestMethod("POST");
                            c.setDoOutput(true);
                            byte[] data = (body == null ? "" : body).getBytes("UTF-8");
                            c.setFixedLengthStreamingMode(data.length);
                            java.io.OutputStream os = c.getOutputStream();
                            os.write(data);
                            os.close();
                        }
                        status = c.getResponseCode();
                        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
                        if (in != null) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            byte[] buf = new byte[16384];
                            int n, total = 0;
                            while ((n = in.read(buf)) > 0 && total < 2000000) { out.write(buf, 0, n); total += n; }
                            in.close();
                            resp = out.toString("UTF-8");
                        }
                    } catch (Exception e) {
                        status = 0;
                        resp = "";
                    } finally {
                        if (c != null) c.disconnect();
                    }
                    final String js = "window.__httpCb(" + JSONObject.quote(id) + "," + status + "," + JSONObject.quote(resp) + ")";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { web.evaluateJavascript(js, null); }
                    });
                }
            }).start();
        }

        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("NutriFit", text));
        }

        @JavascriptInterface
        public void share(String subject, String text) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, text);
            Intent chooser = Intent.createChooser(i, "Backup teilen");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chooser);
        }
    }
}
