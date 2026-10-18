package de.alphacards.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

public class Rec extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(in.getAction())) {
            String k = in.getStringExtra("k");
            boolean skip = false;
            if ("daily".equals(k)) {
                try {
                    JSONObject o = new JSONObject(c.getSharedPreferences("rem", 0).getString("json", "{}"));
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                    skip = today.equals(o.optString("done", ""));
                } catch (Exception e) {
                }
            }
            if (!skip) {
                NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.createNotificationChannel(new NotificationChannel("alphacards", "Erinnerungen", NotificationManager.IMPORTANCE_DEFAULT));
                PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                Notification n = new Notification.Builder(c, "alphacards")
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(in.getStringExtra("t"))
                    .setContentText(in.getStringExtra("m"))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build();
                nm.notify((int) (System.currentTimeMillis() % 100000), n);
            }
        }
        Scheduler.schedule(c);
    }
}
