package de.alphacards.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

public class Rec extends BroadcastReceiver {
    static final String ANS = "de.alphacards.ANSWER";
    static final String CH = "alphacards";

    @Override
    public void onReceive(Context c, Intent in) {
        String act = in.getAction();
        try {
            if (ANS.equals(act)) {
                answer(c, in);
                return;
            }
            if (!Intent.ACTION_BOOT_COMPLETED.equals(act)) {
                fire(c, in);
            }
        } catch (Exception e) {
        }
        Scheduler.schedule(c);
    }

    NotificationManager nm(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CH, "Erinnerungen", NotificationManager.IMPORTANCE_DEFAULT));
        return nm;
    }

    PendingIntent open(Context c) {
        return PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    void fire(Context c, Intent in) throws Exception {
        String k = in.getStringExtra("k");
        JSONObject o = new JSONObject(c.getSharedPreferences("rem", 0).getString("json", "{}"));
        String title = in.getStringExtra("t");
        String text = in.getStringExtra("m");
        if ("daily".equals(k)) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            if (today.equals(o.optString("done", ""))) return;
            JSONArray dues = o.optJSONArray("dues");
            int n = 0;
            long now = System.currentTimeMillis();
            if (dues != null) {
                for (int i = 0; i < dues.length(); i++) {
                    if (dues.optLong(i) <= now) n++;
                }
            }
            if (n == 0) return;
            title = "Neue Karten warten";
            text = n + (n == 1 ? " Karte" : " Karten") + " im Allgemein-Modus bereit. Eine kurze Runde?";
        }
        if ("vocab".equals(k)) {
            vocab(c, o);
            return;
        }
        Notification n = new Notification.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open(c))
            .setAutoCancel(true)
            .build();
        nm(c).notify((int) (System.currentTimeMillis() % 100000) + 10, n);
    }

    void vocab(Context c, JSONObject o) throws Exception {
        JSONArray pool = o.optJSONArray("pool");
        if (pool == null || pool.length() == 0) return;
        JSONObject card = pool.getJSONObject(new Random().nextInt(pool.length()));
        int nid = 7000;
        Intent ai = new Intent(c, Rec.class).setAction(ANS)
            .putExtra("id", card.getString("id"))
            .putExtra("a", card.getString("a"))
            .putExtra("q", card.getString("q"))
            .putExtra("nid", nid);
        PendingIntent api = PendingIntent.getBroadcast(c, 500, ai,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        RemoteInput ri = new RemoteInput.Builder("ans").setLabel("Deine Antwort").build();
        Notification.Action act = new Notification.Action.Builder(
            Icon.createWithResource(c, android.R.drawable.ic_menu_send), "Antworten", api)
            .addRemoteInput(ri).build();
        String d = card.optString("d", "");
        Notification n = new Notification.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Vokabel des Tages" + (d.isEmpty() ? "" : " \u00b7 " + d))
            .setContentText(card.getString("q"))
            .setStyle(new Notification.BigTextStyle().bigText(card.getString("q")))
            .addAction(act)
            .setContentIntent(open(c))
            .build();
        nm(c).notify(nid, n);
    }

    static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}\\s]", "").replaceAll("\\s+", " ").trim();
    }

    static String strip(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    void answer(Context c, Intent in) throws Exception {
        Bundle r = RemoteInput.getResultsFromIntent(in);
        String typed = r == null ? "" : String.valueOf(r.getCharSequence("ans"));
        String a = in.getStringExtra("a");
        String q = in.getStringExtra("q");
        String id = in.getStringExtra("id");
        int nid = in.getIntExtra("nid", 7000);
        String x = norm(typed), y = norm(a);
        boolean ok = x.equals(y) || strip(x).equals(strip(y));
        android.content.SharedPreferences p = c.getSharedPreferences("rem", 0);
        JSONArray res = new JSONArray(p.getString("results", "[]"));
        res.put(new JSONObject().put("id", id).put("ok", ok).put("t", System.currentTimeMillis()));
        p.edit().putString("results", res.toString()).apply();
        Notification n = new Notification.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(ok ? "Richtig!" : "Nicht ganz")
            .setContentText(q + " \u2192 " + a)
            .setContentIntent(open(c))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build();
        nm(c).notify(nid, n);
    }
}
