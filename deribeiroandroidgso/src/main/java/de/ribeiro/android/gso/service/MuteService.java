package de.ribeiro.android.gso.service;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioManager;

import java.io.File;

import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.core.FileOPs;
import de.ribeiro.android.gso.core.MyContext;
import de.ribeiro.android.gso.dataclasses.Const;

public class MuteService extends IntentService {
    private Logger _logger;

    public MuteService() {
        super("ServiceConnector");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyContext ctxt = new MyContext(this);
        if (ctxt.mProfil.muteEvents) {
            _logger = new Logger(Const.APPFOLDER, "MuteService");
            _logger.Info("Starting MuteService");
            try {
                setMuteOn();
            } catch (Exception e) {
                _logger.Info("Lock File already exists");
            }
        }

        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        _logger.Trace("MuteService is getting destroyed!");
        super.onDestroy();
    }

    private void createLockFile() throws Exception {
        AudioManager amanager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int oldmode = amanager.getRingerMode();
        File lockfile = new File(Const.APPFOLDER, "muteService.lock");
        if (!lockfile.exists()) {
            lockfile.createNewFile();
            String content = oldmode + ";";
            FileOPs.saveToFile(content, lockfile);
        } else {

            // pr�fen wie alt
            long diff = System.currentTimeMillis() - lockfile.lastModified();
            if (diff > 86400000)// wenn �lter als ein tag
            {
                lockfile.delete();
                _logger.Info("Lock file outdated");
                lockfile.createNewFile();
                String content = oldmode + ";";
                FileOPs.saveToFile(content, lockfile);
                return;
            }
            throw new Exception("Lock file exists");
        }

    }

    private void setMuteOn() throws Exception {
        createLockFile();
        AudioManager amanager;
        amanager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // turn ringer silent
        amanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        _logger.Info("Setting phone to silent mode");
    }

}
