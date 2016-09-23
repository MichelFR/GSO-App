/*
 * AboutGSOPlan.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.janssen.android.gsoplan.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.R;
import de.janssen.android.gsoplan.asyncTasks.ZipFile;
import de.janssen.android.gsoplan.dataclasses.Const;

public class AboutGSOPlan extends Activity {
    private File root;
    private File logFile;
    private File zippedLog;
    private Context ctxt;
    private CharSequence text;
    private Logger _logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_gsoplan);
        _logger = new Logger(this, "AboutGSOPlan");
        _logger.Info("AboutGSOPlan called");
    }


    public void openPlayStore(View view) {
        _logger.Info("openPlayStore clicked");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=de.janssen.android.gsoplan"));
        startActivity(intent);
    }

    public void sendLogFile(View view) {
        _logger.Info("sendLogFile clicked");
        this.root = Environment.getExternalStorageDirectory();
        this.logFile = new File(this.getFilesDir(), Const.FILELOG);
        this.zippedLog = new File(root, "log.zip");
        this.ctxt = this;
        Button button = (Button) findViewById(R.id.button_sendlog);
        this.text = button.getText();
        button.setText(Const.EXPORTLOGBUTTONWAITTEXT);
        Runnable preExecute = new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, Const.DEVEMAIL);
                intent.putExtra(Intent.EXTRA_SUBJECT, Const.EXPORTLOGEMAILSUBJECT);
                intent.putExtra(Intent.EXTRA_TEXT, Const.EXPORTLOGEMAILBODY);

                if (!zippedLog.exists() || !zippedLog.canRead()) {
                    Toast.makeText(ctxt, "Attachment Error", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                Uri uri = Uri.fromFile(zippedLog);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                Button button = (Button) findViewById(R.id.button_sendlog);
                button.setText(text);
                startActivity(Intent.createChooser(intent, Const.EXPORTLOGINTENT));
            }
        };

        ZipFile task = new ZipFile(logFile, zippedLog, preExecute, this);
        task.execute();

    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}
