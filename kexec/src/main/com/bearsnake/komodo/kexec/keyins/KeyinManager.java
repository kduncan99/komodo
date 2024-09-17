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
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KeyinManager implements Manager {

    private static final int POLL_DELAY = 100;
    private static final String LOG_SOURCE = "KeyinMgr";

    private final ConcurrentLinkedQueue<KeyinHandler> _postedKeyinHandlers = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PostedKeyin> _postedKeyins = new ConcurrentLinkedQueue<>();

    static final HashMap<String, Class<?>> _handlerClasses = new HashMap<>();
    static {
        // AC
        // AP
        // AT
        _handlerClasses.put(BKeyinHandler.COMMAND.toUpperCase(), BKeyinHandler.class);
        _handlerClasses.put(BLKeyinHandler.COMMAND.toUpperCase(), BLKeyinHandler.class);
        _handlerClasses.put(CJKeyinHandler.COMMAND.toUpperCase(), CJKeyinHandler.class);
        // CK
        _handlerClasses.put(CSKeyinHandler.COMMAND.toUpperCase(), CSKeyinHandler.class);
        // CTL
        _handlerClasses.put(DKeyinHandler.COMMAND.toUpperCase(), DKeyinHandler.class);
        // DC
        _handlerClasses.put(DFKeyinHandler.COMMAND.toUpperCase(), DFKeyinHandler.class);
        // DISPLVL
        _handlerClasses.put(DJKeyinHandler.COMMAND.toUpperCase(), DJKeyinHandler.class);
        _handlerClasses.put(DNKeyinHandler.COMMAND.toUpperCase(), DNKeyinHandler.class);
        _handlerClasses.put(DUKeyinHandler.COMMAND.toUpperCase(), DUKeyinHandler.class);
        // E
        // EJ (non-standard keyin - explains each known jump key)
        // ERUNS
        // FA
        // FB
        // FC
        // FF
        _handlerClasses.put(FSKeyinHandler.COMMAND.toUpperCase(), FSKeyinHandler.class);
        _handlerClasses.put(HELPKeyinHandler.COMMAND.toUpperCase(), HELPKeyinHandler.class);
        // HU
        // II
        // IN
        // IT
        // JH
        // KEYINS
        // LB
        // LC
        _handlerClasses.put(LGKeyinHandler.COMMAND.toUpperCase(), LGKeyinHandler.class);
        // MD
        _handlerClasses.put(MFDKeyinHandler.COMMAND.toUpperCase(), MFDKeyinHandler.class);
        // MR
        // MS
        // MU
        // PATHS
        // PM
        // PR
        _handlerClasses.put(PREPKeyinHandler.COMMAND.toUpperCase(), PREPKeyinHandler.class);
        // RC
        // RD
        // RE
        // RL
        // RM
        // RP
        // RS
        _handlerClasses.put(RVKeyinHandler.COMMAND.toUpperCase(), RVKeyinHandler.class);
        // SEC
        _handlerClasses.put(SJKeyinHandler.COMMAND.toUpperCase(), SJKeyinHandler.class);
        _handlerClasses.put(SMKeyinHandler.COMMAND.toUpperCase(), SMKeyinHandler.class);
        // SP
        _handlerClasses.put(SQKeyinHandler.COMMAND.toUpperCase(), SQKeyinHandler.class);
        // SR
        // SS
        // ST
        _handlerClasses.put(SUKeyinHandler.COMMAND.toUpperCase(), SUKeyinHandler.class);
        // SV
        // SX
        // T
        // TB
        // TF
        // TIP
        // TIPSS
        // TM
        // TP
        // TS
        // UL
        _handlerClasses.put(UPKeyinHandler.COMMAND.toUpperCase(), UPKeyinHandler.class);
        // X
        _handlerClasses.put(StopKeyinHandler.COMMAND.toUpperCase(), StopKeyinHandler.class);
    }

    private Poller _poller = null;

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
                        for (var msg : kh.getSyntax()) {
                            Exec.getInstance().sendExecReadOnlyMessage(msg, kh._source);
                        }
                        continue;
                    }

                    if (!kh.checkSyntax()) {
                        var msg = String.format("Syntax error in %s keyin", kh.getCommand());
                        Exec.getInstance().sendExecReadOnlyMessage(msg, kh._source);
                        continue;
                    }

                    LogManager.logInfo(LOG_SOURCE, "Scheduling %s keyin", kh.getCommand());
                    _postedKeyinHandlers.add(kh);
                    new Thread(kh).start();
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

    static Class<?> getHandlerClass(final String command) {
        return _handlerClasses.get(command);
    }

    static Collection<String> getHandlerCommands() {
        return _handlerClasses.keySet();
    }

    private void pruneOldKeyins() {
        var now = LocalDateTime.now();
        _postedKeyinHandlers.removeIf(pkh -> pkh._timeToPrune != null && now.isAfter(pkh._timeToPrune));
    }

    @Override
    public void boot(final boolean recoveryBoot) throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);
        _postedKeyins.clear();
        _poller = new Poller();
        new Thread(_poller).start();

        LogManager.logTrace(LOG_SOURCE, "boot complete", recoveryBoot);
    }

    @Override
    public void close() {
        LogManager.logTrace(LOG_SOURCE, "close()");
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
        _poller._terminate = true;
        _poller = null;
    }

    // TODO Can we make this more performant? Use wait() instead of sleep(), and notify() somewhere that makes sense
    private class Poller implements Runnable {

        public boolean _terminate = false;

        @Override
        public void run() {
            while (!_terminate) {
                try {
                    checkPostedKeyins();
                    pruneOldKeyins();
                } catch (Throwable t) {
                    LogManager.logCatching(LOG_SOURCE, t);
                    Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
                }
                Exec.sleep(POLL_DELAY);
            }
        }
    }
}
