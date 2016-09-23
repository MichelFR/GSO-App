package de.janssen.android.gsoplan.service;

import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.dataclasses.Const;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;

public class MyService extends IntentService implements Runnable {
    private Intent intent;
    private Logger _logger;

    public MyService() {
        super("ServiceConnector");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        _logger = new Logger(this.getFilesDir(), "MyService");
        this.intent = intent;
        Bundle extras = intent.getExtras();
        Boolean fromFrontend = false;
        Boolean quiet = false;
        if (extras != null) {
            fromFrontend = extras.getBoolean("fromFrontend", false);
            quiet = extras.getBoolean("quiet", false);

        }
        if (fromFrontend)
            _logger.Info("Starting sync triggerd by Frontend");
        else
            _logger.Info("Starting sync triggerd by Alarm");
        new BackgroundSync(this, this, fromFrontend, quiet).execute(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        _logger.Trace("MyService is getting destroyed!");
        super.onDestroy();
    }

    @Override
    public void run() {
        Bundle extras = intent.getExtras();

        if (extras != null) {
            Messenger messenger = (Messenger) extras.get("MESSENGER");
            if (messenger != null) {
                Message msg = Message.obtain();
                msg.arg1 = Activity.RESULT_OK;
                try {
                    _logger.Info("Starting MessageHandler(Refresh)");
                    messenger.send(msg);
                } catch (android.os.RemoteException e1) {

                    _logger.Error("Exception while sending Service-Message", e1);
                }
            } else {
                _logger.Info("Sending Broadcastrefresh");

                Intent intent = new Intent(Const.BROADCASTREFRESH);
                intent.putExtra("message", Activity.RESULT_OK);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }
    }


}
