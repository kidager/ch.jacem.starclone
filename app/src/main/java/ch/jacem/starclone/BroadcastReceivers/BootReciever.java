package ch.jacem.starclone.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ch.jacem.starclone.Services.TriggerService;

public class BootReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            new TriggerService(context).queueTrigger();
        }
    }
}
