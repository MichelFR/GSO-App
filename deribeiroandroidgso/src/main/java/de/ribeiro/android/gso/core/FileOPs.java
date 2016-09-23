/*
 * FileOPs.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.ribeiro.android.gso.core;

import android.app.ProgressDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.dataclasses.Const;

public class FileOPs {
    public static Boolean ready = true;
    private static Logger _logger = new Logger(Const.APPFOLDER, "FileOps");

    /**
     * @param file File Objekt mit der Angabe der Datei
     * @return den Inhalt der Datei
     * @author Tobias Janssen
     * <p/>
     * Liest den Inhalt einer Datei aus
     */
    public static String readFromFile(File file) throws Exception {
        ready = false;
        String output = "";
        try {
            FileInputStream fis = new FileInputStream(file);
            if (!file.canRead()) {
                _logger.Error("File " + file.getName() + " is not accessable!");
                java.lang.Thread.sleep(2000);
            }

            long length = file.length();
            // prfen, ob das file grer als int ist
            if (java.lang.Integer.MAX_VALUE < length) {
                int bytesLength = java.lang.Integer.MAX_VALUE;
                long bytesRead = 0;
                while (bytesRead < length) {
                    byte[] buffer = new byte[bytesLength];
                    fis.read(buffer, 0, bytesLength);
                    output += new String(buffer);
                    bytesRead += bytesLength;
                    // prfen, ob die noch ausstehenden Daten grer als int Max
                    // sind
                    if (length - bytesRead > java.lang.Integer.MAX_VALUE) {
                        // ja, sind immer noch grer
                        bytesLength = java.lang.Integer.MAX_VALUE;

                    } else {
                        // nein, nun passt es
                        bytesLength = (int) (length - bytesRead);
                    }

                }
                fis.close();
                _logger.Info("Successful Read File " + file.getName() + " with " + bytesRead + " bytes");
                ready = true;
            } else {
                // dies sollte der normale fall sein
                byte[] buffer = new byte[(int) length];
                fis.read(buffer);
                output = new String(buffer);
                fis.close();
                _logger.Info("Successful Read File " + file.getName() + " with " + length + " bytes");
                ready = true;
            }

        } catch (FileNotFoundException e) {
            ready = true;
            _logger.Error("Loading file failed: File doesn't exist!", e);
            throw new FileNotFoundException("Loading file failed: File doesn't exist!");
        } catch (NullPointerException e) {
            ready = true;
            _logger.Error("Loading file failed: Filename is incorrect!", e);
            throw new NullPointerException("Loading of Element - file failed: Filename is incorrect!");
        } catch (IOException e) {
            ready = true;
            _logger.Error("Loading file failed: Filename is incorrect", e);
            throw new IOException("Loading of Element - file failed: Filename is incorrect!");
        } catch (Exception e) {
            ready = true;
            _logger.Error("Loading file failed: ", e);
            throw new IOException("Loading of Element - file failed!");
        }

        return output;

    }

    /**
     * @param ctxt        MyContext der Applikation
     * @param fileContent der String, der gespeichert werden soll
     * @param dir         der String, der den Unterordner enthlt
     * @param file        File Objekt mit der Angabe der Datei
     * @param pd          der ProgressDialog, der incrementiert werden soll
     * @author Tobias Janssen
     * <p/>
     * Speichert den Angegeben String in der angegebenen Datei
     */
    public static void saveToFile(MyContext ctxt, String fileContent, String dir, String file, ProgressDialog pd) {
        ready = false;
        java.io.File theFile = new java.io.File(ctxt.context.getFilesDir() + "/" + dir, file);

        try {
            // FileOutputStream fos = context.openFileOutput(file.getName(),
            // Context.MODE_PRIVATE);
            FileOutputStream fos = new FileOutputStream(theFile);
            fos.write(fileContent.getBytes());
            pd.setProgress(pd.getProgress() + 50);
            fos.close();
            ready = true;
        } catch (Exception e) {
            ready = true;
        }
        ready = true;

    }

    /**
     * @param fileContent der String, der gespeichert werden soll
     * @param file        File 	Objekt mit der Angabe der Datei
     * @author Tobias Janssen
     * <p/>
     * Speichert den Angegeben String in der angegebenen Datei
     */
    public static void saveToFile(String fileContent, File file) throws Exception {
        ready = false;
        //prfen, ob ordner existiert
        String filestring = file.getAbsolutePath();
        String dirname = filestring.substring(0, filestring.lastIndexOf("/"));
        File dir = new File(dirname);
        if (!dir.exists())
            dir.mkdir();

        FileOutputStream fos = new FileOutputStream(file);

        try {
            fos.write(fileContent.getBytes());
            fos.close();
            ready = true;
        } catch (Exception e) {
            ready = true;
            throw new Exception("Fehler beim schreiben der Dateien");
        }
        ready = true;
    }

    /**
     * @param obj
     * @param file
     * @throws Exception
     */
    public static void saveObject(Object obj, File file) throws Exception {
        OutputStream fos = null;
        if (!file.exists())
            file.createNewFile();
        try {
            fos = new FileOutputStream(file);
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(obj);
            o.close();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                throw e;
            }
        }

    }

    /**
     * @param file
     * @return
     * @throws Exception
     */
    public static Object loadObject(File file) throws Exception {

        InputStream fis = null;
        Object result = null;
        try {
            fis = new FileInputStream(file);
            ObjectInputStream o = new ObjectInputStream(fis);
            result = o.readObject();
            o.close();
        } catch (Exception e) {
            _logger.Error("Error loading file " + file.getName(), e);
            throw e;
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
                throw e;
            }
        }
        return result;
    }

}
