/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.exceptions.ConsoleException;

import com.bearsnake.komodo.logger.LogManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * StandardConsole implements a basic system console using the Java version of stdin and stdout
 */
public class StandardConsole implements Console {

    private static final long POLL_DELAY = 100; // how often do we run the thread in msecs
    private static final String LOG_SOURCE = "StdCons";

    private final LinkedList<ConsoleType> _consoleTypes = new LinkedList<>();
    private final ConsoleId _consoleId;
    private final ReadReplyInfo[] _activeReadReplyMessages = new ReadReplyInfo[10];
    private Integer _pendingReplyIndex = null;
    private String _pendingUnsolicitedInput = null;

    private Poller _poller;
    private Timer _timer = new Timer();
    private BufferedReader _reader = new BufferedReader(new InputStreamReader(System.in));

    public StandardConsole() {
        _consoleId = new ConsoleId(1);
        _consoleTypes.addAll(List.of(ConsoleType.values()));
        _poller = new Poller();
        _timer.schedule(_poller, POLL_DELAY, POLL_DELAY);
    }

    @Override
    public void clearReadReplyMessage(MessageId messageId) throws ConsoleException {
        LogManager.logTrace(LOG_SOURCE, "clearReadReplyMessage(%s)", messageId);
        for (int mx = 0; mx < 10; mx++) {
            if ((_activeReadReplyMessages[mx] != null)
                && (_activeReadReplyMessages[mx]._messageId.equals(messageId))) {
                _activeReadReplyMessages[mx] = null;
                LogManager.logTrace(LOG_SOURCE, "clearReadReplyMessage exit");
                return;
            }
        }

        LogManager.logTrace(LOG_SOURCE, "clearReadReplyMessage not found");
        throw new ConsoleException(String.format("Message %s not found", messageId));
    }

    @Override
    public void close() {
        LogManager.logTrace(LOG_SOURCE, "close()");
        _poller.cancel();
        _timer.cancel();
        _timer = null;
        System.out.println("** CONSOLE CLOSED **");
    }

    @Override
    public void consoleTypeClear(final ConsoleType type) { _consoleTypes.remove(type); }

    @Override
    public void consoleTypeSet(final ConsoleType type) { _consoleTypes.add(type); }

    @Override
    public void dump(PrintStream out, String indent) {
        out.printf("%sConsole %s(%s) StandardConsole\n", indent, _consoleId.toStringFromFieldata(), _consoleId);
        if (_pendingUnsolicitedInput != null) {
            out.printf("%spending input:%s\n", indent, _pendingUnsolicitedInput);
        }

        out.printf("%sactive read-reply messages:\n", indent);
        for (var ex = 0; ex < 10; ex++) {
            if (_activeReadReplyMessages[ex] != null) {
                var rrMsg = _activeReadReplyMessages[ex];
                out.printf("%s[%d] msgId:%s msg:%s maxLen:%d resp:%s%s\n",
                           indent,
                           ex,
                           rrMsg._messageId,
                           rrMsg._originalMessage,
                           rrMsg._maxReplyLength,
                           rrMsg._response,
                           _pendingReplyIndex != null && ex == _pendingReplyIndex ? "*PENDING*" : "");
            }
        }

    }

    @Override public ConsoleId getConsoleId() { return _consoleId; }

    @Override public Collection<ConsoleType> getConsoleTypes() { return _consoleTypes; }

    @Override
    public SolicitedInput pollSolicitedInput() throws ConsoleException {
        SolicitedInput result = null;
        if (_pendingReplyIndex != null) {
            synchronized (this) {
                var rrMsg = _activeReadReplyMessages[_pendingReplyIndex];
                result = new SolicitedInput(rrMsg._messageId, _pendingReplyIndex, rrMsg._response);

                _activeReadReplyMessages[_pendingReplyIndex] = null;
                _pendingReplyIndex = null;
            }
        }
        return result;
    }

    @Override
    public String pollUnsolicitedInput() throws ConsoleException {
        var result = _pendingUnsolicitedInput;
        _pendingUnsolicitedInput = null;
        return result;
    }

    @Override
    public boolean IsConnected() {
        return true;
    }

    @Override
    public void reset() throws ConsoleException {
        _poller.cancel();
        _timer.purge();
        _poller = new Poller();
        _timer.schedule(_poller, POLL_DELAY, POLL_DELAY);
        System.out.println("** CONSOLE RESET **");
    }

    @Override
    public void sendReadOnlyMessage(String text) throws ConsoleException {
        System.out.println(text);
    }

    @Override
    public void sendSystemMessages(String text1, String text2) throws ConsoleException {
        // we don't do anything with these
    }

    @Override
    public int sendReadReplyMessage(MessageId messageId,
                                    String text,
                                    int maxReplyLength) throws ConsoleException {
        synchronized (_activeReadReplyMessages) {
            for (int mx = 0; mx < 10; mx++) {
                if (_activeReadReplyMessages[mx] == null) {
                    _activeReadReplyMessages[mx] = new ReadReplyInfo(messageId,
                                                                     text,
                                                                     maxReplyLength);
                    System.out.printf("%d-%s\n", mx, text);
                    return mx;
                }
            }
        }

        // ran out of message indices
        throw new ConsoleException("StandardConsole is using all message indices");
    }

    /**
     * Async thread which handles interaction with stdin/stdout
     */
    private class Poller extends TimerTask {

        @Override
        public void run() {
            try {
                if (System.in.available() > 0) {
                    // read input if there is any to be read, but ignore it if the previous input is still pending.
                    var input = _reader.readLine();
                    if (_pendingReplyIndex != null || _pendingUnsolicitedInput != null) {
                        return;
                    }

                    // if the input is empty (all spaces is considered empty) ignore it.
                    input = input.trim();
                    if (input.isEmpty()) {
                        return;
                    }

                    // solicited input?
                    if (Character.isDigit(input.charAt(0))) {
                        var mx = Integer.parseInt(input.substring(0, 1));
                        if (_activeReadReplyMessages[mx] == null) {
                            System.out.printf("** MESSAGE %d DOES NOT EXIST **\n", mx);
                            return;
                        }

                        if (input.length() == 1) {
                            _activeReadReplyMessages[mx]._response = "";
                            _pendingReplyIndex = mx;
                            return;
                        }

                        if (input.charAt(1) != ' ') {
                            System.out.println("** INVALID INPUT **");
                            return;
                        }

                        var cx = 2;
                        while (input.charAt(cx) == ' ') {
                            cx++;
                        }

                        var response = input.substring(cx);
                        if (response.length() > _activeReadReplyMessages[mx]._maxReplyLength) {
                            System.out.println("** RESPONSE IS TOO LONG **");
                            return;
                        }

                        _activeReadReplyMessages[mx]._response = response;
                        _pendingReplyIndex = mx;
                        return;
                    }

                    // unsolicited input...
                    _pendingUnsolicitedInput = input;
                }
            } catch (Throwable t) {
                LogManager.logCatching(LOG_SOURCE, t);
                System.out.printf("--- %s ---\n", t);
            }
        }
    }
}
