/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class KeyinManager implements Manager, Runnable {

    private static final long THREAD_DELAY = 100;
    private static final String LOG_SOURCE = "KeyinMgr";

    private final LinkedList<KeyinHandler> _postedKeyinHandlers = new LinkedList<>();
    private final LinkedList<PostedKeyin> _postedKeyins = new LinkedList<>();

    private static final HashMap<String, Class<?>> _handlerClasses = new HashMap<>();
    static {
        _handlerClasses.put(StopKeyinHandler.COMMAND, StopKeyinHandler.class);
    }

    public KeyinManager(){
        Exec.getInstance().managerRegister(this);
    }

    public synchronized void postKeyin(final ConsoleId source,
                                       final String text) {
        _postedKeyins.add(new PostedKeyin(source, text));
    }

    private void checkPostedKeyins() {
        for (var pk : _postedKeyins) {
            // TODO
        }
    }

    private void pruneOldKeyins() {
        var now = LocalDateTime.now();
        _postedKeyinHandlers.removeIf(pkh -> now.isAfter(pkh._timeToPrune));
    }

    @Override
    public void boot() throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "boot()");
        _postedKeyins.clear();
        Exec.getInstance().getExecutor().scheduleWithFixedDelay(this,
                                                                THREAD_DELAY,
                                                                THREAD_DELAY,
                                                                TimeUnit.MILLISECONDS);
    }

    @Override
    public void dump(PrintStream out, String indent) {
        // TODO
    }

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");
    }

    @Override
    public synchronized void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
        // nothing to do... i think.
    }

    @Override
    public synchronized void run() {
        checkPostedKeyins();
        pruneOldKeyins();
    }
}
