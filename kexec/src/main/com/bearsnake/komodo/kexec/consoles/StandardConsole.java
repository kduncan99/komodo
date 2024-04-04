/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.exceptions.ConsoleException;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * StandardConsole implements a basic system console using the Java version of stdin and stdout
 */
public class StandardConsole implements Console, Runnable {

    private static final long THREAD_DELAY = 100; // how often do we run the thread in msecs

    private final ConsoleId _consoleId;
    private final ReadReplyInfo[] _activeReadReplyMessages = new ReadReplyInfo[10];
    private Integer _pendingReplyIndex = null;
    private String _pendingUnsolicitedInput = null;
    private final Scanner _scanner = new Scanner(System.in);

    public StandardConsole() {
        _consoleId = new ConsoleId(1);
    }

    @Override
    public synchronized void clearReadReplyMessage(MessageId messageId) throws ConsoleException {
        for (int mx = 0; mx < 10; mx++) {
            if ((_activeReadReplyMessages[mx] != null)
                && (_activeReadReplyMessages[mx]._messageId.equals(messageId))) {
                _activeReadReplyMessages[mx] = null;
                return;
            }
        }

        throw new ConsoleException(String.format("Message %s not found", messageId));
    }

    @Override
    public void close() {
        System.out.println("** CONSOLE CLOSED **");
    }

    @Override
    public synchronized void dump(PrintStream out, String indent) {
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

    @Override
    public synchronized SolicitedInput pollSolicitedInput() throws ConsoleException {
        SolicitedInput result = null;
        if (_pendingReplyIndex != null) {
            var rrMsg = _activeReadReplyMessages[_pendingReplyIndex];
            System.out.printf("  %s\n", rrMsg._originalMessage);
            result = new SolicitedInput(rrMsg._messageId, _pendingReplyIndex, rrMsg._response);

            _activeReadReplyMessages[_pendingReplyIndex] = null;
            _pendingReplyIndex = null;
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
        System.out.println("** CONSOLE RESET **");
        Exec.getInstance().getExecutor().scheduleWithFixedDelay(this,
                                                                THREAD_DELAY,
                                                                THREAD_DELAY,
                                                                TimeUnit.MILLISECONDS);
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
    public synchronized int sendReadReplyMessage(MessageId messageId,
                                                 String text,
                                                 int maxReplyLength) throws ConsoleException {
        for (int mx = 0; mx < 10; mx++) {
            if (_activeReadReplyMessages[mx] == null) {
                _activeReadReplyMessages[mx] = new ReadReplyInfo(messageId,
                                                                 text,
                                                                 maxReplyLength);
                System.out.printf("%d-%s\n", mx, text);
                return mx;
            }
        }

        // ran out of message indices
        throw new ConsoleException("StandardConsole is using all message indices");
    }

    /**
     * Async thread which handles interaction with stdin/stdout
     */
    @Override
    public synchronized void run() {
        if (_scanner.hasNext()) {
            // read input if there is any to be read, but ignore it if the previous input is still pending.
            var input = _scanner.nextLine();
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
    }
}
