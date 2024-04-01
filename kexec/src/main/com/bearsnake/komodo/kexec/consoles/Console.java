/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import java.io.PrintStream;

public abstract class Console {

    public abstract void clearReadReplyMessage(MessageId messageId) throws ConsoleException;
    public abstract void close() throws ConsoleException;
    public abstract void dump(PrintStream out, String indent);
    public abstract SolicitedInput pollSolicitedInput() throws ConsoleException;
    public abstract String pollUnsolicitedInput() throws ConsoleException;
    public abstract boolean IsConnected();
    public abstract void reset() throws ConsoleException;
    public abstract void sendReadOnlyMessage(String text) throws ConsoleException;
    public abstract void sendSystemMessages(String text1, String text2) throws ConsoleException;
    public abstract void sendReadReplyMessage(MessageId messageId, String text, int maxReplyLength);
}
