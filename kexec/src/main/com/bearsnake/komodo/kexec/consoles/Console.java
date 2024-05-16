/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.exceptions.ConsoleException;
import java.io.PrintStream;
import java.util.Collection;

public interface Console {

    void clearReadReplyMessage(MessageId messageId) throws ConsoleException;
    void close() throws ConsoleException;
    void dump(PrintStream out, String indent);
    ConsoleId getConsoleId();
    Collection<ConsoleType> getConsoleTypes();
    SolicitedInput pollSolicitedInput() throws ConsoleException;
    String pollUnsolicitedInput() throws ConsoleException;
    boolean IsConnected();
    void reset() throws ConsoleException;
    void sendReadOnlyMessage(String text) throws ConsoleException;
    void sendSystemMessages(String text1, String text2) throws ConsoleException;
    int sendReadReplyMessage(MessageId messageId, String text, int maxReplyLength) throws ConsoleException;
    void consoleTypeClear(ConsoleType type);
    void consoleTypeSet(ConsoleType type);
}
