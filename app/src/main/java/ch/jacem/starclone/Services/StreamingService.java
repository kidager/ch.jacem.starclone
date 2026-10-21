package ch.jacem.starclone.Services;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import ch.jacem.starclone.BroadcastReceivers.ServeCancelAction;
import ch.jacem.starclone.Items.RemoteItem;
import ch.jacem.starclone.R;
import ch.jacem.starclone.Rclone;
import ch.jacem.starclone.util.FLog;
import ch.jacem.starclone.util.NotificationUtils;


public class StreamingService extends IntentService {

    private static final String TAG = "StreamingService";
    public static final String SERVE_PATH_ARG = "ch.jacem.starclone.streaming_service.arg1";
    public static final String REMOTE_ARG = "ch.jacem.starclone.streaming_service.arg2";
    public static final String SHOW_NOTIFICATION_TEXT = "ch.jacem.starclone.streaming_service.arg3";
    public static final String SERVE_PORT = "ch.jacem.starclone.streaming_service.arg4";
    public static final String SERVE_PROTOCOL = "ch.jacem.starclone.serve_protocol";
    public static final String ALLOW_REMOTE_ACCESS = "ch.jacem.starclone.allow_remote_access";
    public static final String AUTHENTICATION_USERNAME = "ch.jacem.starclone.username";
    public static final String AUTHENTICATION_PASSWORD = "ch.jacem.starclone.password";
    public static final int SERVE_HTTP = 11;
    public static final int SERVE_WEBDAV = 12;
    public static final int SERVE_FTP = 13;
    public static final int SERVE_DLNA = 14;
    private final String CHANNEL_ID = "ch.jacem.starclone.streaming_channel";
    private final String CHANNEL_NAME = "Streaming service";
    private final int PERSISTENT_NOTIFICATION_ID = 179;
    private Rclone rclone;
    private Process runningProcess;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.*
     */
    public StreamingService() {
        super("ch.jacem.starclone.streamingservice");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setNotificationChannel();
        rclone = new Rclone(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        final String servePath = intent.getStringExtra(SERVE_PATH_ARG);
        final RemoteItem remote = intent.getParcelableExtra(REMOTE_ARG);
        final Boolean showNotificationText = intent.getBooleanExtra(SHOW_NOTIFICATION_TEXT, false);
        final int protocol = intent.getIntExtra(SERVE_PROTOCOL, SERVE_HTTP);
        final int port = intent.getIntExtra(SERVE_PORT, 8080);
        final Boolean allowRemoteAccess = intent.getBooleanExtra(ALLOW_REMOTE_ACCESS, false);
        final String authenticationUsername = intent.getStringExtra(AUTHENTICATION_USERNAME);
        final String authenticationPassword = intent.getStringExtra(AUTHENTICATION_PASSWORD);

        int flags = PendingIntent.FLAG_IMMUTABLE;
        Intent foregroundIntent = new Intent(this, StreamingService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, foregroundIntent, flags);

        Intent cancelIntent = new Intent(this, ServeCancelAction.class);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_streaming)
                .setContentTitle(getString(R.string.streaming_service_notification_title))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_cancel_download, getString(R.string.cancel), cancelPendingIntent);

        if (showNotificationText) {
            Uri uri = Uri.parse("http://127.0.0.1:" + port);
            Intent webPageIntent = new Intent(Intent.ACTION_VIEW, uri);
            webPageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent webPagePendingIntent = PendingIntent.getActivity(this, 0, webPageIntent, flags);
            builder.setContentIntent(webPagePendingIntent);
            builder.setContentText(getString(R.string.streaming_service_notification_content, port));
        }

        startForeground(PERSISTENT_NOTIFICATION_ID, builder.build());

        switch (protocol) {
            case SERVE_FTP:
                runningProcess = rclone.serve(Rclone.SERVE_PROTOCOL_FTP, port, allowRemoteAccess, authenticationUsername, authenticationPassword, remote, servePath);
                break;
            case SERVE_WEBDAV:
                runningProcess = rclone.serve(Rclone.SERVE_PROTOCOL_WEBDAV, port, allowRemoteAccess, authenticationUsername, authenticationPassword, remote, servePath);
                break;
            case SERVE_DLNA:
                runningProcess = rclone.serve(Rclone.SERVE_PROTOCOL_DLNA, port, allowRemoteAccess, authenticationUsername, authenticationPassword, remote, servePath);
                break;
            case SERVE_HTTP:
            default:
                runningProcess = rclone.serve(Rclone.SERVE_PROTOCOL_HTTP, port, allowRemoteAccess, authenticationUsername, authenticationPassword, remote, servePath);
                break;
        }

        if (runningProcess != null) {
            try {
                runningProcess.waitFor();
            } catch (InterruptedException e) {
                FLog.e(TAG, "onHandleIntent: error waiting for process", e);
            }
        }

        if (runningProcess != null && runningProcess.exitValue() != 0) {
            rclone.logErrorOutput(runningProcess);
        }

        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != runningProcess) {
            runningProcess.destroy();
        }
    }

    private void setNotificationChannel() {
        NotificationUtils.createNotificationChannel(this,
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
                getString(R.string.streaming_service_notification_channel_description)
        );
    }
}
