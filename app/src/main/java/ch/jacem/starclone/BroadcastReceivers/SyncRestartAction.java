package ch.jacem.starclone.BroadcastReceivers;

import static ch.jacem.starclone.workmanager.SyncWorker.EXTRA_TASK_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ch.jacem.starclone.workmanager.SyncManager;

/**
 * This class requires a receiver declaration in the manifest
 */
public class SyncRestartAction extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SyncManager sm = new SyncManager(context);
        sm.queue(intent.getLongExtra(EXTRA_TASK_ID, -1));
    }
}
