package de.alphacards.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

public class Scheduler {
    static PendingIntent pi(Context c, int code, String t, String m, String k, int flags) {
        Intent i = new Intent(c, Rec.class).putExtra("t", t).putExtra("m", m).putExtra("k", k);
        return PendingIntent.getBroadcast(c, code, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static long next(String hm, int dow) {
        String[] p = hm.split(":");
        Calendar k = Calendar.getInstance();
        k.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
        k.set(Calendar.MINUTE, Integer.parseInt(p[1]));
        k.set(Calendar.SECOND, 0);
        k.set(Calendar.MILLISECOND, 0);
        if (dow > 0) k.set(Calendar.DAY_OF_WEEK, dow);
        while (k.getTimeInMillis() <= System.currentTimeMillis()) {
            k.add(Calendar.DAY_OF_MONTH, dow > 0 ? 7 : 1);
        }
        return k.getTimeInMillis();
    }

    static void put(Context c, AlarmManager am, int code, long at, String t, String m, String k) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at,
            pi(c, code, t, m, k, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static void schedule(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            for (int i = 0; i < 160; i++) {
                if (i > 2 && i < 100) continue;
                PendingIntent x = pi(c, i, "", "", "", PendingIntent.FLAG_NO_CREATE);
                if (x != null) {
                    am.cancel(x);
                    x.cancel();
                }
            }
            String j = c.getSharedPreferences("rem", 0).getString("json", null);
            if (j == null) return;
            JSONObject o = new JSONObject(j);
            String daily = o.optString("daily", "");
            if (!daily.isEmpty()) {
                put(c, am, 0, next(daily, -1), "Alpha Cards",
                    "Lust auf eine kurze Runde? Ein paar Karten warten.", "daily");
            }
            put(c, am, 1, next(o.optString("weekly", "19:00"), Calendar.SUNDAY),
                "Alpha Cards \u00b7 Wochenr\u00fcckblick",
                "Dein Wochenr\u00fcckblick ist da. Wische dich durch deine Woche.", "weekly");
            String voc = o.optString("voc", "");
            JSONArray pool = o.optJSONArray("pool");
            if (!voc.isEmpty() && pool != null && pool.length() > 0) {
                put(c, am, 2, next(voc, -1), "Vokabel des Tages", "", "vocab");
            }
            String tm = daily.isEmpty() ? "18:00" : daily;
            String[] hm = tm.split(":");
            JSONArray ex = o.optJSONArray("exams");
            if (ex != null) {
                for (int i = 0; i < ex.length() && i < 60; i++) {
                    JSONArray e = ex.getJSONArray(i);
                    String[] d = e.getString(1).split("-");
                    Calendar k = Calendar.getInstance();
                    k.set(Integer.parseInt(d[0]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[2]),
                        Integer.parseInt(hm[0]), Integer.parseInt(hm[1]), 0);
                    k.add(Calendar.DAY_OF_MONTH, -1);
                    if (k.getTimeInMillis() > System.currentTimeMillis()) {
                        put(c, am, 100 + i, k.getTimeInMillis(), "Alpha Cards \u00b7 Pr\u00fcfung",
                            "Morgen ist deine Pr\u00fcfung: " + e.getString(0) + ". Noch eine \u00dcbungsrunde?", "exam");
                    }
                }
            }
        } catch (Exception e) {
        }
    }
}
