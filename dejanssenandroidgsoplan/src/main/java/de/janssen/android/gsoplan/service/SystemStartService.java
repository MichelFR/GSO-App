package de.janssen.android.gsoplan.service;

import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.dataclasses.Const;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
