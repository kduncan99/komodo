/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.exec.Exec;

import java.io.PrintStream;

/**
 * StandardConsole implements a basic system console using the Java version of stdin and stdout
 */
public class StandardConsole extends Console implements Runnable {

    private static class ReadReplyInfo {

        final MessageId _messageId;
        final String _originalMessage;
        final int _maxReplyLength;
        String _response;

        ReadReplyInfo(
            final MessageId messageId,
            final String originalMessage,
            final int maxReplyLength
        ) {
            _messageId = messageId;
            _originalMessage = originalMessage;
            _maxReplyLength = maxReplyLength;
            _response = null;
        }
    }

    private final ReadReplyInfo[] _activeReadReplyMessages = new ReadReplyInfo[10];
    boolean _terminate;

    public StandardConsole() {
        _terminate = true;
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
    public void close() throws ConsoleException {
        _terminate = true
    }

    @Override
    public void dump(PrintStream out, String indent) {

    }

    @Override
    public SolicitedInput pollSolicitedInput() throws ConsoleException {
        return null;
    }

    @Override
    public String pollUnsolicitedInput() throws ConsoleException {
        return null;
    }

    @Override
    public boolean IsConnected() {
        return false;
    }

    @Override
    public void reset() throws ConsoleException {

    }

    @Override
    public void sendReadOnlyMessage(String text) throws ConsoleException {

    }

    @Override
    public void sendSystemMessages(String text1, String text2) throws ConsoleException {

    }

    @Override
    public void sendReadReplyMessage(MessageId messageId, String text, int maxReplyLength) {

    }

    /**
     * Async thread which handles interaction with stdin/stdout
     */
    @Override
    public void run() {
        while (!_terminate) {
            // TODO
        }
    }
}
