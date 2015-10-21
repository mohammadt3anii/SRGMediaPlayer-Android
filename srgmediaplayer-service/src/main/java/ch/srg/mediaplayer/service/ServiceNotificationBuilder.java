package ch.srg.mediaplayer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;

import android.text.TextUtils;

import ch.srg.mediaplayer.service.utils.AppUtils;

public class ServiceNotificationBuilder {
    boolean live;
    boolean playing;
    String title;
    Bitmap notificationBitmap;
    PendingIntent pendingIntent;

    public ServiceNotificationBuilder(boolean live, boolean playing, String title, Bitmap notificationBitmap, PendingIntent pendingIntent) {
        this.live = live;
        this.playing = playing;
        this.title = title;
        this.notificationBitmap = notificationBitmap;
        this.pendingIntent = pendingIntent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceNotificationBuilder that = (ServiceNotificationBuilder) o;

        if (live != that.live) return false;
        if (playing != that.playing) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (notificationBitmap != null ? !notificationBitmap.equals(that.notificationBitmap) : that.notificationBitmap != null) return false;
        if (pendingIntent != null ? !pendingIntent.equals(that.pendingIntent) : that.pendingIntent != null) return false;

        return true;

    }

    public Notification buildNotification(Context context, MediaSessionCompat mediaSessionCompat) {
        /* XXX: stackbuilder needed for back navigation */

        Intent pauseIntent = new Intent(context, MediaPlayerService.class);
        pauseIntent.setAction(playing ? MediaPlayerService.ACTION_PAUSE : MediaPlayerService.ACTION_RESUME);
        pauseIntent.putExtra(MediaPlayerService.ARG_FROM_NOTIFICATION, true);
        PendingIntent piPause = PendingIntent.getService(context, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(MediaPlayerService.ACTION_STOP, null, context, MediaPlayerService.class);
        PendingIntent piStop = PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String appName = AppUtils.getApplicationName(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();
        style.setMediaSession(mediaSessionCompat.getSessionToken());
        style.setShowActionsInCompactView(0);

        builder.setStyle(style);
        builder.setContentIntent(pendingIntent);

        if (!live) {
            if (playing) {
                builder.addAction(R.drawable.ic_pause_white_36dp, context.getString(R.string.service_notification_pause), piPause);
            } else {
                builder.addAction(R.drawable.ic_play_arrow_white_36dp, context.getString(R.string.service_notification_resume), piPause);
            }
        }

        builder.addAction(R.drawable.ic_stop_white_36dp, context.getString(R.string.service_notification_stop), piStop);
        builder.setContentTitle(appName);

        if (!TextUtils.isEmpty(title)) {
            builder.setContentText(title);
        }

        builder.setSmallIcon(R.drawable.ic_play_arrow_white_24dp);
        if (notificationBitmap != null) {
            builder.setLargeIcon(notificationBitmap);
        }

        builder.setOngoing(true);

        return builder.build();
    }
}
