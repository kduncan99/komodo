/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ConsoleException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class ConsoleManager implements Manager, Runnable {

    private static final String LOG_SOURCE = "ConsMgr";
    private static final long THREAD_DELAY = 50; // how often do we run the thread in msecs

    private final HashMap<ConsoleId, Console> _consoles = new HashMap<>();
    private ConsoleId _primaryConsoleId;
    private final LinkedList<ReadOnlyMessage> _queuedReadOnlyMessages = new LinkedList<>();
    private final HashMap<MessageId, ReadReplyMessage> _queuedReadReplyMessages = new HashMap<>();

    public ConsoleManager() {}

    public void boot() {
        var iter = _consoles.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            var cons = entry.getValue();
            if (cons.getConsoleId() != _primaryConsoleId) {
                try {
                    cons.close();
                } catch (ConsoleException ex) {
                    LogManager.logError(LOG_SOURCE,
                                        "Cannot close console %012o:%s",
                                        cons.getConsoleId(),
                                        ex.getMessage());
                }
                iter.remove();
            } else {
                try {
                    cons.reset();
                } catch (ConsoleException ex) {
                    LogManager.logError(LOG_SOURCE,
                                        "Cannot reset console %012o:%s",
                                        cons.getConsoleId(),
                                        ex.getMessage());
                }
            }
        }

        _queuedReadOnlyMessages.clear();
        _queuedReadReplyMessages.clear();
    }

    public synchronized void dump(final PrintStream out,
                                  final String indent) {
        out.printf("%sConsoleManager ********************************\n", indent);

        out.println("%s  Queued Read-only messages:");
        for (var msg : _queuedReadOnlyMessages) {
            out.printf("%s    src:%s [%s] rte:%s %s\n",
                       indent,
                       msg.getSource().getRunId(),
                       msg.getRunId(),
                       msg.getRouting().toString(),
                       msg.getText());
        }

        out.println("%s  Queued Read-reply messages:");
        for (var msg : _queuedReadReplyMessages.values()) {
            out.printf("%s    src:%s [%s] rte:%s %s resp:%s cons:%s consIdx:%d cancel:%s\n",
                       indent,
                       msg.getSource().getRunId(),
                       msg.getRunId(),
                       msg.getRouting().toString(),
                       msg.getText(),
                       msg.getRouting(),
                       msg.getResponseConsoleId().toString(),
                       msg.getResponseConsoleMessageIndex(),
                       msg.isCanceled());
        }

        out.printf("%s  Primary console: %s (%s)\n",
                   indent,
                   _consoles.get(_primaryConsoleId).getConsoleId().toStringFromFieldata(),
                   _consoles.get(_primaryConsoleId).getConsoleId());

        var subIndent = indent + "  ";
        for (var console : _consoles.values()) {
            console.dump(out, subIndent);
        }
    }

    public void initialize() {
        _consoles.clear();
        var primary = new StandardConsole();
        _consoles.put(primary.getConsoleId(), primary);
        _primaryConsoleId = primary.getConsoleId();

        Exec.getInstance().getExecutor().scheduleWithFixedDelay(this,
                                                                THREAD_DELAY,
                                                                THREAD_DELAY,
                                                                TimeUnit.MILLISECONDS);
    }

    public void sendReadOnlyMessage(final ReadOnlyMessage message) {
        LogManager.logTrace(LOG_SOURCE,
                            "Queueing ReadOnly %s*%s",
                            message.getSource().getRunId(),
                            message.getText());
        if (!message.getSource().isExec()) {
            message.getSource().postToTailSheet(message.getText());
        }

        synchronized(this) {
            _queuedReadOnlyMessages.add(message);
        }
    }

    /**
     * sends a read-reply message, then waits until it is answered or canceled
     */
    public void sendReadReplyMessage(final ReadReplyMessage message) {
        LogManager.logTrace(LOG_SOURCE,
                            "Queueing ReadReply %s*%s",
                            message.getSource().getRunId(),
                            message.getText());
        if (!message.getSource().isExec()) {
            message.getSource().postToTailSheet(message.getText());
        }

        synchronized(this) {
            _queuedReadReplyMessages.put(message.getMessageId(), message);
        }

        while (!message.hasResponse() && !message.isCanceled()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // nothing to do
            }
        }

        if (message.isCanceled()) {
            LogManager.logWarning(LOG_SOURCE, "Read reply message %s canceled", message.getMessageId().toString());
        }
    }

    public synchronized void stop() {
        for (var rrMsg : _queuedReadReplyMessages.values()) {
            if (!rrMsg.isCanceled() && !rrMsg.hasResponse()) {
                rrMsg.setIsCanceled();
            }
        }
    }

    // If there are any queued read only messages...
    //   if there is routing
    //     if we can send it to the destination console, do so.
    //     else send it to the primary console.
    //   else send it to all the consoles.
    private synchronized boolean checkForReadOnlyMessage() throws KExecException {
        var dropList = new LinkedList<ConsoleId>();
        var iter = _queuedReadOnlyMessages.iterator();
        boolean didSomething = false;
        while (iter.hasNext() && !Exec.getInstance().isStopped()) {
            var roMsg = iter.next();
            if ((roMsg.getRouting().getW() == 0) && !dropList.contains(roMsg.getRouting())) {
                var cons = _consoles.get(roMsg.getRouting());
                var sent = false;
                if (cons != null) {
                    try {
                        cons.sendReadOnlyMessage(roMsg.getText());
                        sent = true;
                    } catch (ConsoleException ex) {
                        dropList.add(cons.getConsoleId());
                    }
                }

                if (!sent) {
                    try {
                        _consoles.get(_primaryConsoleId).sendReadOnlyMessage(roMsg.getText());
                    } catch (ConsoleException ex) {
                        // give up.
                        LogManager.logWarning(LOG_SOURCE,
                                              "Lost read-only message:%s", roMsg.getText());
                    }
                }
            } else {
                for (var cons : _consoles.values()) {
                    if (!dropList.contains(cons.getConsoleId())) {
                        try {
                            cons.sendReadOnlyMessage(roMsg.getText());
                        } catch (ConsoleException ex) {
                            dropList.add(cons.getConsoleId());
                        }
                    }
                }
            }

            iter.remove();
            didSomething = true;
        }

        for (var consId : dropList) {
            dropConsole(consId);
        }
        return didSomething;
    }

    private synchronized boolean checkForReadReplyMessage() throws KExecException {
        var dropList = new LinkedList<ConsoleId>();
        var iter = _queuedReadReplyMessages.entrySet().iterator();
        boolean didSomething = false;
        while (iter.hasNext() && !Exec.getInstance().isStopped()) {
            var entry = iter.next();
            var rrMsg = entry.getValue();
            if (!rrMsg.isAssignedToConsole()) {
                ConsoleId selectedConsoleId = null;
                Console selectedConsole = null;
                if (rrMsg.getRouting() != null
                    && !dropList.contains(rrMsg.getRouting())
                    && _consoles.containsKey(rrMsg.getRouting())) {
                    selectedConsoleId = rrMsg.getRouting();
                } else {
                    selectedConsoleId = _primaryConsoleId;
                }
                selectedConsole = _consoles.get(selectedConsoleId);

                try {
                    var msgIndex = selectedConsole.sendReadReplyMessage(rrMsg.getMessageId(),
                                                                        rrMsg.getText(),
                                                                        rrMsg.getMaxReplyLength());
                    rrMsg.setResponseConsoleId(selectedConsoleId);
                    rrMsg.setResponseConsoleMessageIndex(msgIndex);
                } catch (ConsoleException ex) {
                    dropList.add(rrMsg.getRouting());
                }

                didSomething = true;
            }
        }

        for (var consId : dropList) {
            dropConsole(consId);
        }
        return didSomething;
    }

    private synchronized boolean checkForSolicitedInput() throws KExecException {
        for (var cons : _consoles.values()) {
            try {
                var solInput = cons.pollSolicitedInput();
                if (solInput != null) {
                    if (!_queuedReadReplyMessages.containsKey(solInput.getMessageId())) {
                        LogManager.logWarning(LOG_SOURCE,
                                              "Received solicited input for unknown message %s",
                                              solInput.getMessageId().toString());
                        return true;
                    }

                    var rrMsg = _queuedReadReplyMessages.get(solInput.getMessageId());
                    if (rrMsg.hasResponse()) {
                        LogManager.logWarning(LOG_SOURCE,
                                              "Received solicited input for replied message %s",
                                              solInput.getMessageId().toString());
                        return true;
                    }

                    if (Exec.getInstance().getConfiguration().LogConsoleMessages && !rrMsg.doNotLogResponse()) {
                        LogManager.logInfo(LOG_SOURCE,
                                           "Msg:%s replyCons:%s %d-%s",
                                           rrMsg.getMessageId().toString(),
                                           rrMsg.getResponseConsoleId().toString(),
                                           rrMsg.getResponseConsoleMessageIndex(),
                                           rrMsg.getResponse());
                    }

                    _queuedReadReplyMessages.remove(rrMsg.getMessageId());
                    return true;
                }
            } catch (ConsoleException ex) {
                dropConsole(cons.getConsoleId());
                break;
            }

            if (Exec.getInstance().isStopped()) {
                break;
            }
        }

        return false;
    }

    private synchronized boolean checkForUnsolicitedInput() throws KExecException {
        for (var cons : _consoles.values()) {
            try {
                var input = cons.pollUnsolicitedInput();
                if (input != null) {
                    // send the raw input to the keyin manager
                    Exec.getInstance().getKeyinManager().postKeyin(cons.getConsoleId(), input);
                    return true;
                }
            } catch (ConsoleException ex) {
                dropConsole(cons.getConsoleId());
                break;
            }

            if (Exec.getInstance().isStopped()) {
                break;
            }
        }

        return false;
    }

    private void dropConsole(final ConsoleId consoleId) throws KExecException {
        // If we are asked to drop the primary console, we have to stop the exec
        if (consoleId == _primaryConsoleId) {
            var sc = StopCode.LastSystemConsoleDown;
            Exec.getInstance().stop(sc);
            throw new ExecStoppedException(sc);
        }
    }

    @Override
    public void run() {
        try {
            if (!Exec.getInstance().isStopped()) {
                boolean didSomething = false;
                synchronized (this) {
                    didSomething |= checkForReadOnlyMessage();
                    didSomething |= checkForReadReplyMessage();
                    didSomething |= checkForSolicitedInput();
                    didSomething |= checkForUnsolicitedInput();
                }

                if (didSomething && !Exec.getInstance().isStopped()) {
                    run();
                }
            }
        } catch (KExecException ex) {
            // do nothing
        }
    }
}
