/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.kexec.Configuration;
import com.bearsnake.komodo.kexec.consoles.ConsoleManager;
import com.bearsnake.komodo.kexec.keyins.KeyinManager;
import com.bearsnake.komodo.logger.LogManager;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Exec {

    private static Exec _instance = null;
    private static final String LOG_SOURCE = "Exec";

    private boolean _allowRecoveryBoot;
    private Configuration _configuration;
    private ScheduledThreadPoolExecutor _executor;
    private Phase _phase;
    private StopCode _stopCode;

    private ConsoleManager _consoleManager;
    private KeyinManager _keyinManager;

    public Exec() {
        _allowRecoveryBoot = false;
        _phase = Phase.NotStarted;

        _configuration = new Configuration(); // TODO load the thing

        _consoleManager = new ConsoleManager();
        _keyinManager = new KeyinManager();

        _instance = new Exec();
    }

    public Configuration getConfiguration() { return _configuration; }
    public ConsoleManager getConsoleManager() { return _consoleManager; }
    public ScheduledThreadPoolExecutor getExecutor() { return _executor; }
    public static Exec getInstance() { return _instance; }
    public KeyinManager getKeyinManager() { return _keyinManager; }
    public Phase getPhase() { return _phase; }
    public StopCode getStopCode() { return _stopCode; }
    public boolean isRecoveryBootAllowed() { return _allowRecoveryBoot; }
    public boolean isStopped() { return _phase == Phase.Stopped; }

    public void boot() throws Exception {
        _executor = new ScheduledThreadPoolExecutor((int)_configuration.ExecThreads);
        _consoleManager.boot();
        _keyinManager.boot();
        // TODO
    }

    public void close() {
        _executor.close();
        _executor = null;
    }

    public void initialize() throws Exception {
        _configuration = new Configuration();
        _consoleManager.initialize();
        _keyinManager.initialize();
    }

    public void stop(final StopCode stopCode) {
        _stopCode = stopCode;
        _phase = Phase.Stopped;
        _executor.shutdownNow();
        try {
            if (!_executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LogManager.logWarning(LOG_SOURCE, "Executor failed to terminate");
            }
        } catch (InterruptedException ex) {
            // nothing to do here
        }
        _executor = null;

        _consoleManager.stop();
        _keyinManager.stop();
    }
}
