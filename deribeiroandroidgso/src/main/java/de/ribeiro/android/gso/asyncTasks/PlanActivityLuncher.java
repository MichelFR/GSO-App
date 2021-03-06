/*
 * PlanActivityLuncher.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.ribeiro.android.gso.asyncTasks;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

import de.ribeiro.android.gso.R;
import de.ribeiro.android.gso.activities.PlanActivity;
import de.ribeiro.android.gso.core.Tools;
import de.ribeiro.android.gso.core.UntisProvider;
import de.ribeiro.android.gso.dataclasses.Const;
import de.ribeiro.android.gso.runnables.ErrorMessage;

public class PlanActivityLuncher extends AsyncTask<Boolean, Integer, Boolean> {

    private PlanActivity parent;
    private Runnable preExec = null;

    public PlanActivityLuncher(PlanActivity parent) {
        this.parent = parent;
    }

    public PlanActivityLuncher(PlanActivity parent, Runnable preExec) {
        this.parent = parent;
        this.preExec = preExec;
    }

    @Override
    protected void onPreExecute() {
        if (preExec != null)
            preExec.run();
        super.onPreExecute();
    }

    protected Boolean doInBackground(Boolean... bool) {
        parent.ctxt.initViewPagerWaiting();
        selfCheck();
        parent.ctxt.appIsReady = true;
        parent.ctxt.executor.scheduleNext();
        return null;
    }


    /**
     * @param prevErrorCode Integer der den vorherigen Fehler angibt
     * @author Tobias Janssen
     * Fhrt die Laufzeitprfung durch, und ergreift ntige Mabahmen im Fehlerfall
     */
    private void selfCheck() {
        //Strukturprfung durchfhren
        int errorlevel = parent.ctxt.mProfil.stupid.checkStructure(parent.ctxt);
        switch (errorlevel) {
            case 0: // Alles in Ordnung

                try {
                    parent.ctxt.mProfil.stupid.clearData();
                    Tools.loadAllDataFiles(parent.ctxt.context, parent.ctxt.mProfil, parent.ctxt.mProfil.stupid);
                } catch (Exception e) {
                    parent.ctxt.handler.post(new ErrorMessage(parent.ctxt, e.getMessage()));
                }
                parent.ctxt.mProfil.stupid.sort();
                Calendar now = new GregorianCalendar();
                //prfen, wie alt bestehende daten sind
//	    if (now.getTimeInMillis() - parent.ctxt.mProfil.mylastResync >= parent.ctxt.mProfil.myResync * 1000 * 60)
//	    {
//		UntisProvider.contactStupidService(parent.ctxt.context, parent.ctxt.msgHandler);
//	    }
                parent.ctxt.initViewPager();

                break;
            case 1: // TypesDatei Datei fehlt
            case 2: // FILEELEMENT Datei fehlt
                // Backend beauftragen diese herunterzu laden
                UntisProvider.contactStupidService(parent.ctxt.context, parent.ctxt.msgHandler);


            case 3: // Keine Klasse ausgewhlt
                OnClickListener onClick = new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Tools.gotoSetup(parent.ctxt, Const.FIRSTSTART, true);
                    }
                };
                String message = "";
                if (parent.ctxt.newVersionReqSetup) {
                    message = parent.ctxt.context.getString(R.string.msg_newVersionReqSetup);
                } else {
                    message = parent.ctxt.context.getString(R.string.msg_noElement);
                }
                parent.ctxt.handler.post(new ErrorMessage(parent.ctxt, message, onClick, "Einstellungen öffnen"));
                break;
            case 6: // Elementenordner existiert nicht
                // neuen anlegen

                File elementDir = new File(parent.getFilesDir(), parent.ctxt.mProfil.getMyElement());
                elementDir.mkdir();
                //Backend daten laden lassen:
                UntisProvider.contactStupidService(parent, parent.ctxt.msgHandler);
                break;
            case 7: // Keine Daten fr diese Klasse vorhanden
                UntisProvider.contactStupidService(parent, parent.ctxt.msgHandler);
                break;
        }

    }
}
