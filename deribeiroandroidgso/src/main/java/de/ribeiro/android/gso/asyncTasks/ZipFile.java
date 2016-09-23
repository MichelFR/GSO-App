package de.ribeiro.android.gso.asyncTasks;

///*
// * ZipFile.java
// * 
// * Tobias Janssen, 2013
// * GNU GENERAL PUBLIC LICENSE Version 2
// */

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile extends AsyncTask<Boolean, Integer, Boolean> {

    public Exception exception;
    private File inFile;
    private File outFile;
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

    @Override
    protected void onPostExecute(Boolean result) {
        if (this.postRun != null)
            postRun.run();
    }

}
