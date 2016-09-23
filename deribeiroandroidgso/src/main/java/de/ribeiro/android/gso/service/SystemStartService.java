package de.ribeiro.android.gso.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.dataclasses.Const;

public class SystemStartService extends BroadcastReceiver {
    private Logger _logger;

    @Override
    public void onReceive(Context context, Intent intent) {
        _logger = new Logger(Const.APPFOLDER, "SystemStartService");
        _logger.Info("Starting System Service");
        Intent serviceIntent = new Intent(context, AlarmStarter.class);
        context.startService(serviceIntent);

    }
}
