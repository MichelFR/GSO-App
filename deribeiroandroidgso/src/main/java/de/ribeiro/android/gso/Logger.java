/*
 * Logger.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.ribeiro.android.gso;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

public final class Logger {
    private File path;
    private Calendar cal;
    private String cls;

    public Logger(Context ctxt, String cls) {
        this.path = ctxt.getFilesDir();
        this.cls = cls;
    }

    /**
     * @param folder Folder der App
     * @param cls    Klasse die geloggt wird
     */
    public Logger(File folder, String cls) {
        this.path = folder;
        this.cls = cls;
    }

    public static String readFromFile(File file) throws Exception {
        String output = "";
        try {
            FileInputStream fis = new FileInputStream(file);
            if (!file.canRead()) {
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
            } else {
                // dies sollte der normale fall sein
                byte[] buffer = new byte[(int) length];
                fis.read(buffer);
                output = new String(buffer);
                fis.close();
            }

        } catch (IOException e) {

        }

        return output;

    }

    /**
     * @param message
     * @param exception
     * @param cls
     */
    public void Error(String message, Exception exception) {
        log("Error", message + "\n" + exception.getMessage() + "\n" + "Stack-Trace:\n\n" + Log.getStackTraceString(exception) + "\n");
    }

    /**
     * @param message
     * @param cls
     */
    public void Error(String message) {
        log("Error", message);
    }

    /**
     * @param message
     * @param cls
     */
    public void Warning(String message) {
        log("Warning", message);
    }

    /**
     * @param message
     * @param cls
     */
    public void Critical(String message) {
        log("Critical", message);
    }

    /**
     * @param message
     * @param exception
     * @param cls
     */
    public void Critical(String message, Exception exception) {
        log("Critical", message + "\n" + exception.getMessage() + "\n" + "Stack-Trace:\n\n" + Log.getStackTraceString(exception) + "\n");
    }

    /**
     * @param message
     * @param cls
     */
    public void Info(String message) {
        log("Info", message);
    }

    /**
     * @param message
     * @param cls
     */
    public void Trace(String message) {
        log("Trace", message);
    }

    private void log(String level, String message) {
        try {
            if (path != null) {
                File logFile = new File(path, "log.txt");

                String mlog = level + " - " + getCurrentDate();
                while (mlog.length() != 25)
                    mlog += " ";
                mlog += "| In " + cls;
                while (mlog.length() != 70)
                    mlog += " ";
                mlog += "| ";
                message = mlog + message + "\n";

                FileWriter fw;
                if (logFile.exists()) {
                    if (logFile.length() > 1024 * 1024 * 3) // Wenn grer 3Mb
                    {
                        String log = readFromFile(logFile);
                        String[] logArray = log.split("\n");
                        logFile.createNewFile();
                        log = "";
                        //die ersten 2000 zeilen entfernen

                        if (logArray.length > 2000) {
                            fw = new FileWriter(logFile, false);
                            for (int i = 2000; i < logArray.length; i++) {
                                fw.append(logArray[i] + "\n");
                            }
                            fw.close();
                        } else {
                            if (logFile.canWrite()) {
                                fw = new FileWriter(logFile, true);
                                fw.append(message);
                                fw.close();
                            }
                        }
                    } else {
                        if (logFile.canWrite()) {
                            fw = new FileWriter(logFile, true);
                            fw.append(message);
                            fw.close();
                        }

                    }
                } else {
                    fw = new FileWriter(logFile, true);
                    fw.append(message);
                    fw.close();
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * @return
     */
    private String getCurrentDate() {
        cal = new GregorianCalendar();

        String minute = java.lang.String.valueOf(cal.get(Calendar.MINUTE));
        if (minute.length() == 1)
            minute = "0" + minute;
        String second = java.lang.String.valueOf(cal.get(Calendar.SECOND));
        if (second.length() == 1)
            second = "0" + second;
        String hour = java.lang.String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
        if (hour.length() == 1)
            hour = "0" + hour;
        String month = java.lang.String.valueOf(cal.get(Calendar.MONTH) + 1);
        if (month.length() == 1)
            month = "0" + month;
        String day = java.lang.String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        if (day.length() == 1)
            day = "0" + day;


        return day + "." + month + ". " + hour + ":" + minute;
    }
}
