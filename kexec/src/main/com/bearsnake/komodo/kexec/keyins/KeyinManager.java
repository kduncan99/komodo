/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        _handlerClasses.put(StopKeyinHandler.COMMAND.toUpperCase(), StopKeyinHandler.class);
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
            var split = pk.getText().split( " ", 2);
            var subSplit = split[0].split(",", 2);
            var cmd = subSplit[0].toUpperCase();
            var options = subSplit.length > 1 ? subSplit[1] : null;
            var arguments = split.length > 1 ? split[1] : null;
            var clazz = _handlerClasses.get(cmd);
            if (clazz != null) {
                try {
                    Constructor<?> ctor = clazz.getConstructor(ConsoleId.class, String.class, String.class);
                    var kh = (KeyinHandler)ctor.newInstance(pk.getConsoleIdentifier(), options, arguments);
                    Exec.getInstance().getExecutor().schedule(kh, 0, TimeUnit.MILLISECONDS);
                } catch (IllegalAccessException |
                         InvocationTargetException |
                         InstantiationException |
                         NoSuchMethodException ex) {
                    // TODO
                }
            } else {
                // TODO look for registered keyins
            }
        }
        _postedKeyins.clear();
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
        try {
            checkPostedKeyins();
            pruneOldKeyins();
        } catch (Throwable t) {
            LogManager.logCatching(LOG_SOURCE, t);
            Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
        }
    }
}
