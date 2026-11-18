package de.cubevote;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/** Tägliche Erinnerung ans Voten (und Neustart der Erinnerung nach dem Hochfahren). */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL = "vote_reminder";
    private static final String ACTION_REMIND = "de.cubevote.REMIND";

    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            schedule(c);
            return;
        }
        if (ACTION_REMIND.equals(intent.getAction()) && !Sites.votedToday(c)) {
            notify(c);
        }
    }

    /** Erinnerung je nach Einstellung setzen oder löschen. */
    public static void schedule(Context c) {
        SharedPreferences p = Sites.prefs(c);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        if (!p.getBoolean("reminder", true)) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, p.getInt("remHour", 18));
        cal.set(Calendar.MINUTE, p.getInt("remMin", 0));
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_REMIND);
        return PendingIntent.getBroadcast(c, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void notify(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Vote-Erinnerung",
                    NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(c, VoteActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 2, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL)
                : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_menu_send)
                .setContentTitle("Cubeside: Heute schon gevotet?")
                .setContentText("Antippen, um jetzt alle Votes zu machen")
                .setContentIntent(pi)
                .setAutoCancel(true);
        try {
            nm.notify(1, b.build());
        } catch (SecurityException ignored) {
            // Benachrichtigungen nicht erlaubt
        }
    }
}
