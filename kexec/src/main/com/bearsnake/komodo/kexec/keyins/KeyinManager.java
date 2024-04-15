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
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class KeyinManager implements Manager, Runnable {

    private static final long THREAD_DELAY = 50;
    private static final String LOG_SOURCE = "KeyinMgr";

    private final ConcurrentLinkedQueue<KeyinHandler> _postedKeyinHandlers = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PostedKeyin> _postedKeyins = new ConcurrentLinkedQueue<>();

    static final HashMap<String, Class<?>> _handlerClasses = new HashMap<>();
    static {
        _handlerClasses.put(CJKeyinHandler.COMMAND.toUpperCase(), CJKeyinHandler.class);
        _handlerClasses.put(DKeyinHandler.COMMAND.toUpperCase(), DKeyinHandler.class);
        _handlerClasses.put(DJKeyinHandler.COMMAND.toUpperCase(), DJKeyinHandler.class);
        _handlerClasses.put(DUKeyinHandler.COMMAND.toUpperCase(), DUKeyinHandler.class);
        _handlerClasses.put(LGKeyinHandler.COMMAND.toUpperCase(), LGKeyinHandler.class);
        _handlerClasses.put(PREPKeyinHandler.COMMAND.toUpperCase(), PREPKeyinHandler.class);
        _handlerClasses.put(SJKeyinHandler.COMMAND.toUpperCase(), SJKeyinHandler.class);
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
        while (!_postedKeyins.isEmpty()) {
            var pk = _postedKeyins.poll();
            var split = pk.getText().split( " ", 2);
            var subSplit = split[0].split(",", 2);
            var cmd = subSplit[0].toUpperCase();
            var options = subSplit.length > 1 ? subSplit[1] : null;
            var arguments = split.length > 1 ? split[1] : null;
            var clazz = _handlerClasses.get(cmd);
            if (clazz != null) {
                // this is an intrinsic exec keyin
                try {
                    Constructor<?> ctor = clazz.getConstructor(ConsoleId.class, String.class, String.class);
                    var kh = (KeyinHandler)ctor.newInstance(pk.getConsoleIdentifier(), options, arguments);
                    if (Objects.equals(kh._options, "?") || Objects.equals(kh._arguments, "?")) {
                        var msg = kh.getHelp()[0];
                        Exec.getInstance().sendExecReadOnlyMessage(msg, kh._source);
                        continue;
                    }

                    if (!kh.checkSyntax()) {
                        var msg = String.format("Syntax error in %s keyin", kh.getCommand());
                        Exec.getInstance().sendExecReadOnlyMessage(msg, kh._source);
                        continue;
                    }

                    LogManager.logInfo(LOG_SOURCE, "Scheduling %s keyin", kh.getCommand());
                    _postedKeyinHandlers.add(kh);
                    Exec.getInstance().getExecutor().schedule(kh, 0, TimeUnit.MILLISECONDS);
                } catch (IllegalAccessException |
                         InvocationTargetException |
                         InstantiationException |
                         NoSuchMethodException ex) {
                    LogManager.logCatching(LOG_SOURCE, ex);
                    Exec.getInstance().stop(StopCode.ExecContingencyHandler);
                    return;
                }

                continue;
            } else {
                // TODO look for registered keyins
            }

            // not intrinsic, not registered
            var msg = String.format("Keyin not registered: %s", cmd);
            Exec.getInstance().sendExecReadOnlyMessage(msg, pk.getConsoleIdentifier());
        }
    }

    private void pruneOldKeyins() {
        var now = LocalDateTime.now();
        _postedKeyinHandlers.removeIf(pkh -> pkh._timeToPrune != null && now.isAfter(pkh._timeToPrune));
    }

    @Override
    public void boot(final boolean recoveryBoot) throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);
        _postedKeyins.clear();
        Exec.getInstance().getExecutor().scheduleWithFixedDelay(this,
                                                                THREAD_DELAY,
                                                                THREAD_DELAY,
                                                                TimeUnit.MILLISECONDS);
        LogManager.logTrace(LOG_SOURCE, "boot complete", recoveryBoot);
    }

    @Override
    public void dump(final PrintStream out,
                     final String indent,
                     final boolean verbose) {
        out.printf("%sKeyinManager ********************************\n", indent);

        out.printf("%s  Posted:\n", indent);
        for (var k : _postedKeyins) {
            out.printf("%s    %s:%s\n", indent, k.getConsoleIdentifier().toString(), k.getText());
        }
        out.printf("%s  Recent Handlers:\n", indent);
        for (var kh : _postedKeyinHandlers) {
            out.printf("%s    %s\n", indent, kh.toString());
        }
    }

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");
    }

    @Override
    public void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
        // nothing to do... i think.
    }

    @Override
    public void run() {
        try {
            checkPostedKeyins();
            pruneOldKeyins();
        } catch (Throwable t) {
            LogManager.logCatching(LOG_SOURCE, t);
            Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
        }
    }
}
