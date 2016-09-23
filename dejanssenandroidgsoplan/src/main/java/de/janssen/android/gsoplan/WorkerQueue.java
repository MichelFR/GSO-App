/*
 * WorkerQueue.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */

package de.janssen.android.gsoplan;

import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.janssen.android.gsoplan.dataclasses.Const;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;

public class WorkerQueue implements Runnable {

    final Queue<AsyncTask<Boolean, Integer, Boolean>> tasks = new ArrayDeque<AsyncTask<Boolean, Integer, Boolean>>();
    final Queue<Boolean> tasksBools = new ArrayDeque<Boolean>();
    private ExecutorService monit;
    private AsyncTask<Boolean, Integer, Boolean> currentTask;
    private Boolean exitMonitThread;
    private Calendar born;
    private Boolean terminated = false;
    private Logger _logger;
    private final int TIMEOUT = 60;    //60 Sekunden

    /**
     * Erstellt eine neue WorkerQueue
     *
     * @author janssen
     */
    public WorkerQueue() {
        this._logger = new Logger(Const.APPFOLDER, "WorkerQueue");
        this.monit = Executors.newSingleThreadExecutor();
        this.exitMonitThread = false;
        this.monit.execute(this);
    }

    /**
     * @param newTask AsyncTask der ausgefhrt werden soll
     * @author janssen
     */
    public synchronized void post(final AsyncTask<Boolean, Integer, Boolean> newTask) {

        if (!terminated) {
            tasks.add(newTask);
            _logger.Info("Task added!");
        }
        if (currentTask == null) {
            _logger.Info("executing Task!");
            scheduleNext();
        } else if (currentTask != null) {
            if (this.currentTask.isCancelled()) {
                this.currentTask = null;
                scheduleNext();
            }
        }
    }

    /**
     * @author janssen
     * Fhrt den nchsten Task in der Queue aus
     */
    public synchronized void scheduleNext() {

        if (currentTask != null) {
            _logger.Info("Task list contains " + tasks.size() + " tasks");
            currentTask = tasks.poll();
            if (currentTask != null) {
                try {
                    currentTask = currentTask.execute();
                    born = new GregorianCalendar();
                } catch (Exception e) {
                    _logger.Error("Task crashed!", e);
                }
            }
        } else {
            currentTask = tasks.poll();
            if (currentTask != null) {
                currentTask = currentTask.execute();
                born = new GregorianCalendar();
            }
        }

    }

    public void run() {

        while (!this.exitMonitThread) {
            try {

                Thread.sleep(1000);
                if (currentTask != null) {
                    Status status = currentTask.getStatus();
                    if (status.equals(Status.FINISHED)) {
                        _logger.Info("Task finished!");
                        scheduleNext();
                    } else {
                        // Task luft noch
                        // prfen wie alt der ist
                        if (born.getTimeInMillis() + (TIMEOUT * 1000) < new GregorianCalendar().getTimeInMillis()) {
                            // ist lter als TIMEOUT sekunden
                            this.currentTask.cancel(true);
                            _logger.Info("Task timeout!");
                        }

                    }
                }
            } catch (Exception e) {

            }

        }

    }

    /**
     * @param timeInMillisToTerminate
     * @author Tobias Janssen
     * Wartet die angegebene Zeit auf die Terminierung des aktivien Tasks<p>
     * Wenn weitere Tasks anstehen, werden diese anschliesend ausgefhrt
     */
    public void awaitTermination(int timeInMillisToTerminate) {
        try {

            while (true) {
                if (this.currentTask != null)
                    Thread.sleep(500);

                if (tasks.isEmpty()) {
                    if (this.currentTask != null)
                        this.currentTask.get(timeInMillisToTerminate, TimeUnit.MILLISECONDS);
                    return;
                } else
                    scheduleNext();
            }
        } catch (Exception e) {

        }
    }

    /**
     * 14.11.12
     *
     * @author Tobias Janssen
     * Terminiert den aktiven Task im SerialExecutor
     */
    public void terminateActiveThread() {
        if (this.currentTask != null)
            this.currentTask.cancel(true);

    }

    /**
     * 16.01.13
     *
     * @author Tobias Janssen
     * Terminiert alle anstehenden Threads im SerialExecutor und sperrt diesen, damit keine weiteren angenommen werden
     */
    public void terminateAllThreads() {
        _logger.Info("Terminating threads called. Active Tasks: " + tasks.size());
        tasks.clear();
        this.terminated = true;
        terminateActiveThread();
        awaitTermination(2000);
        this.exitMonitThread = true;
    }
}
