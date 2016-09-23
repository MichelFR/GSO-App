package de.janssen.android.gsoplan.asyncTasks;

///*
// * ZipFile.java
// * 
// * Tobias Janssen, 2013
// * GNU GENERAL PUBLIC LICENSE Version 2
// */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.janssen.android.gsoplan.dataclasses.Const;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

public class ZipFile extends AsyncTask<Boolean, Integer, Boolean> {

    private File inFile;
    private File outFile;
    public Exception exception;
    private Runnable postRun;
    private Context ctxt;

    /**
     * @param ctxt
     * @param getProfil  ()File
     * @param postRun
     * @param showDialog Boolean ob ein Dialog erzeugt werden soll
     */
    public ZipFile(File inFile, File outFile, Runnable postRun, Context ctxt) {
        this.inFile = inFile;
        this.outFile = outFile;
        this.postRun = postRun;
        this.ctxt = ctxt;

    }

    @Override
    protected Boolean doInBackground(Boolean... bool) {
        try {
            if (isCancelled()) {
                return null;
            }
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile));
            zip(inFile, zos);
            zos.close();
        } catch (Exception e) {
            this.exception = e;
        }

        return true;
    }

    private static final void zip(File base, ZipOutputStream zos) throws IOException {
        byte[] buffer = new byte[8192];
        int read = 0;
        FileInputStream in = new FileInputStream(base);
        ZipEntry entry = new ZipEntry(base.getName());
        zos.putNextEntry(entry);
        while (-1 != (read = in.read(buffer))) {
            zos.write(buffer, 0, read);
        }
        in.close();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (this.postRun != null)
            postRun.run();
    }

}
