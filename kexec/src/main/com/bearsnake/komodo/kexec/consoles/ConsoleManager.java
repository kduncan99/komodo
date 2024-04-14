/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ConsoleException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.RunType;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class ConsoleManager implements Manager, Runnable {

    private static final String LOG_SOURCE = "ConsMgr";
    private static final long THREAD_DELAY = 50; // how often do we run the thread in msecs

    private final ConcurrentHashMap<ConsoleId, Console> _consoles = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ConsoleId> _dropConsoleList = new ConcurrentLinkedQueue<>();
    private ConsoleId _primaryConsoleId;
    private final ConcurrentLinkedQueue<ReadOnlyMessage> _queuedReadOnlyMessages = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<MessageId, ReadReplyMessage> _queuedReadReplyMessages = new ConcurrentHashMap<>();

    public ConsoleManager() {
        Exec.getInstance().managerRegister(this);
    }

    @Override
    public void boot(final boolean recoveryBoot) {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);
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
        out.printf("%sConsoleManager ********************************\n", indent);

        out.printf("%s  Queued Read-only messages:\n", indent);
        for (var msg : _queuedReadOnlyMessages) {
            out.printf("%s    src:%s [%s] rte:%s %s\n",
                       indent,
                       msg.getSource().getRunId(),
                       msg.getRunId(),
                       msg.getRouting() == null ? "<no routing>" : msg.getRouting().toString(),
                       msg.getText());
        }

        out.printf("%s  Queued Read-reply messages:\n", indent);
        for (var msg : _queuedReadReplyMessages.values()) {
            out.printf("%s    src:%s [%s] rte:%s %s resp:%s cons:%s consIdx:%d cancel:%s\n",
                       indent,
                       msg.getSource().getRunId(),
                       msg.getRunId(),
                       msg.getRouting() == null ? "<none>" : msg.getRouting().toString(),
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

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");
        _consoles.clear();
        var primary = new StandardConsole();
        _consoles.put(primary.getConsoleId(), primary);
        _primaryConsoleId = primary.getConsoleId();
    }

    public void sendReadOnlyMessage(final ReadOnlyMessage message) {
        LogManager.logTrace(LOG_SOURCE,
                            "Queueing ReadOnly %s*%s",
                            message.getSource().getRunId(),
                            message.getText());
        if (message.getSource().getRunType() != RunType.Exec) {
            message.getSource().postToTailSheet(message.getText());
        }

        _queuedReadOnlyMessages.add(message);
    }

    /**
     * sends a read-reply message, then waits until it is answered or canceled
     */
    public void sendReadReplyMessage(final ReadReplyMessage message) {
        LogManager.logTrace(LOG_SOURCE,
                            "Queueing ReadReply %s*%s",
                            message.getSource().getRunId(),
                            message.getText());
        if (message.getSource().getRunType() != RunType.Exec) {
            message.getSource().postToTailSheet(message.getText());
        }

        _queuedReadReplyMessages.put(message.getMessageId(), message);
        while (!message.hasResponse() && !message.isCanceled()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // nothing to do
            }
        }

        if (message.isCanceled()) {
            LogManager.logWarning(LOG_SOURCE, "Read reply message %s canceled", message.getMessageId().toString());
        }
    }

    @Override
    public void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
        synchronized (_queuedReadReplyMessages) {
            _queuedReadReplyMessages.values()
                                    .stream()
                                    .filter(rrMsg -> !rrMsg.isCanceled() && !rrMsg.hasResponse())
                                    .forEach(ReadReplyMessage::setIsCanceled);
        }
    }

    // If there are any queued read only messages...
    //   if there is routing
    //     if we can send it to the destination console, do so.
    //     else send it to the primary console.
    //   else send it to all the consoles.
    private void checkForReadOnlyMessage() throws KExecException {
        while (true) {
            ReadOnlyMessage roMsg = null;
            synchronized (_queuedReadOnlyMessages) {
                roMsg = _queuedReadOnlyMessages.poll();
            }
            if (roMsg == null) {
                return;
            }

            if (roMsg.getRouting() == null) {
                // no routing was specified for the message - send it to all consoles.
                for (Console cons : _consoles.values()) {
                    try {
                        cons.sendReadOnlyMessage(roMsg.getText());
                    } catch (ConsoleException ex) {
                        _dropConsoleList.add(cons.getConsoleId());
                    }
                }
            } else {
                // routing was specified - send it to the indicated console if possible,
                // else send it to the primary console.
                var sent = false;
                var consId = roMsg.getRouting();
                var cons = _consoles.get(consId);
                if (cons != null && !_dropConsoleList.contains(consId)) {
                    try {
                        cons.sendReadOnlyMessage(roMsg.getText());
                        sent = true;
                    } catch (ConsoleException ex) {
                        _dropConsoleList.add(consId);
                    }
                }

                if (!sent) {
                    cons = _consoles.get(_primaryConsoleId);
                    try {
                        cons.sendReadOnlyMessage(roMsg.getText());
                    } catch (ConsoleException ex) {
                        _dropConsoleList.add(_primaryConsoleId);
                    }
                }
            }
        }
    }

    // Iterate over the queued RR messages.
    // For any message which has not been assigned to a particular console:
    //   if there is routing:
    //     if we can assign and send it to the destination console, do so,
    //       if that fails, assign and send it to the primary console.
    //     else assign and send it to the primary console.
    //   otherwise (no routing):
    //     send it to all consoles (other than primary) as read-only,
    //       and assign and sent it to the primary console.
    private void checkForReadReplyMessage() throws KExecException {
        var iter = _queuedReadReplyMessages.entrySet().iterator();
        while (iter.hasNext() && !Exec.getInstance().isStopped()) {
            var entry = iter.next();
            var rrMsg = entry.getValue();
            if (!rrMsg.isAssignedToConsole()) {
                ConsoleId selectedConsoleId;
                Console selectedConsole;
                if (rrMsg.getRouting() != null
                    && !_dropConsoleList.contains(rrMsg.getRouting())
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
                    _dropConsoleList.add(rrMsg.getRouting());
                }
            }
        }
    }

    // iterates over the known consoles, polling for a response to a particular read-reply message.
    private void checkForSolicitedInput() throws KExecException {
        for (var cons : _consoles.values()) {
            try {
                var solInput = cons.pollSolicitedInput();
                if (solInput != null) {
                    if (!_queuedReadReplyMessages.containsKey(solInput.getMessageId())) {
                        LogManager.logWarning(LOG_SOURCE,
                                              "Received solicited input for unknown message %s",
                                              solInput.getMessageId().toString());
                        return;
                    }

                    var rrMsg = _queuedReadReplyMessages.get(solInput.getMessageId());
                    if (rrMsg.hasResponse()) {
                        LogManager.logWarning(LOG_SOURCE,
                                              "Received solicited input for replied message %s",
                                              solInput.getMessageId().toString());
                        return;
                    }

                    rrMsg.setResponse(solInput.getText());
                    if (Exec.getInstance().getConfiguration().getLogConsoleMessages() && !rrMsg.doNotLogResponse()) {
                        LogManager.logInfo(LOG_SOURCE,
                                           "Msg:%s replyCons:%s %d-%s",
                                           rrMsg.getMessageId().toString(),
                                           rrMsg.getResponseConsoleId().toString(),
                                           rrMsg.getResponseConsoleMessageIndex(),
                                           rrMsg.getResponse());
                    }

                    _queuedReadReplyMessages.remove(rrMsg.getMessageId());
                    return;
                }
            } catch (ConsoleException ex) {
                _dropConsoleList.add(cons.getConsoleId());
            }

            if (Exec.getInstance().isStopped()) {
                break;
            }
        }
    }

    // iterates over the known consoles looking for unsolicited input
    private void checkForUnsolicitedInput() throws KExecException {
        for (var cons : _consoles.values()) {
            try {
                var input = cons.pollUnsolicitedInput();
                if (input != null) {
                    Exec.getInstance().getKeyinManager().postKeyin(cons.getConsoleId(), input);
                    return;
                }
            } catch (ConsoleException ex) {
                _dropConsoleList.add(cons.getConsoleId());
            }

            if (Exec.getInstance().isStopped()) {
                break;
            }
        }
    }

    private void dropConsoles() throws KExecException {
        // Process the drop console list.
        // If we are asked to drop the primary console, we have to stop the exec
        while (!_dropConsoleList.isEmpty()) {
            var consoleId = _dropConsoleList.poll();
            if (consoleId == _primaryConsoleId) {
                var sc = StopCode.LastSystemConsoleDown;
                Exec.getInstance().stop(sc);
                throw new ExecStoppedException();
            }

            var cons = _consoles.remove(consoleId);
            cons.reset();
            LogManager.logWarning(LOG_SOURCE, "Dropping console %s", consoleId.toString());
        }
    }

    @Override
    public void run() {
        try {
            checkForReadOnlyMessage();
            checkForReadReplyMessage();
            checkForSolicitedInput();
            checkForUnsolicitedInput();
            dropConsoles();
        } catch (KExecException ex) {
            // trap door - exec is stopped.
            // we don't need to do anything here, but we do need to avoid throwing the exception.
        } catch (Throwable t) {
            // Something very unexpected went wrong.
            // We need to avoid throwing the exception (which would cause all sorts of shenanigans),
            // but we do need to bring the exec to a screeching halt.
            LogManager.logCatching(LOG_SOURCE, t);
            Exec.getInstance().stop(StopCode.ExecContingencyHandler);
        }
    }
}
