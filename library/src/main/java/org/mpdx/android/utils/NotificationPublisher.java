package org.mpdx.android.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.mpdx.android.R;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.MainActivity;
import org.mpdx.android.features.base.BaseActivity;
import org.mpdx.android.features.tasks.model.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import dagger.hilt.android.AndroidEntryPoint;

import static java.util.Calendar.HOUR_OF_DAY;

@AndroidEntryPoint
public class NotificationPublisher extends BroadcastReceiver {
    private static final String NOTIFICATION_ID = "notification_id";
    private static final String NOTIFICATION_TYPE = "notification_type";
    private static final String NOTIFICATION_TITLE = "notification_title";
    private static final String NOTIFICATION_MESSAGE = "notification_message";
    private static final String NOTIFICATION_TIME = "notification_time";

    private static int notificationId = 1;
    private static Map<String, PendingIntent> scheduled;

    @Inject
    AppPrefs prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra(NOTIFICATION_TYPE);
        if (type == null || !isNotificationsEnabled(context)) {
            return;
        }

        String channelId = context.getString(R.string.settings_notifications_task_due);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.notifications_task_due_channel_description));
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        Intent newIntent = new Intent(context, MainActivity.class);
        newIntent.putExtra(BaseActivity.EXTRA_DEEP_LINK_TYPE, type);
        newIntent.putExtra(BaseActivity.EXTRA_DEEP_LINK_ID, intent.getStringExtra(NOTIFICATION_ID));
        newIntent.putExtra(BaseActivity.EXTRA_DEEP_LINK_TIME, intent.getLongExtra(NOTIFICATION_TIME, 0L));
        newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(newIntent);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(notificationId++, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(intent.getStringExtra(NOTIFICATION_TITLE))
                .setContentText(intent.getStringExtra(NOTIFICATION_MESSAGE))
                .setSmallIcon(R.drawable.mpdx_logo_notification_icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId++, builder.build());
    }

    boolean isNotificationsEnabled(Context context) {
        return prefs.isTaskDueNotificationEnabled();
    }

    public static void scheduleTask(Context context, Task task, String contactNames, int count) {
        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, task.getId());
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_TITLE,
                (task.getActivityType() != null ? task.getActivityType() : "") +
                (contactNames != null ? " - " + contactNames : ""));
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_MESSAGE,
                (count > 1 ? context.getString(R.string.alert_grouped, task.getSubject(), count - 1) : task.getSubject()) +
                        " - " + SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(task.getStartAt()));
        notificationIntent.putExtra(NOTIFICATION_TIME, task.getStartAt().getTime());

        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_TYPE, BaseActivity.DEEP_LINK_TYPE_TASK);

        Date alertTime = task.getStartAt();
        if (DateUtils.isMidnight(alertTime)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(alertTime);

            calendar.set(HOUR_OF_DAY, 13);
            calendar.set(Calendar.MINUTE, 30);
            alertTime = calendar.getTime();
        } else {
            alertTime = new Date(alertTime.getTime() - (task.getNotificationTimeBeforeSeconds() * 1000));
        }
        scheduleAlarm(context, notificationIntent, task.getId(), alertTime);
    }

    private static void scheduleAlarm(Context context, Intent intent, String taskId, Date alertTime) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId++,
                intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        recordScheduled(taskId, pendingIntent);

        alarmManager.set(AlarmManager.RTC_WAKEUP, alertTime.getTime(), pendingIntent);
    }

    private static void recordScheduled(String id, PendingIntent intent) {
        if (scheduled == null) {
            scheduled = new Hashtable<>();
        }
        scheduled.put(id, intent);
    }
}
