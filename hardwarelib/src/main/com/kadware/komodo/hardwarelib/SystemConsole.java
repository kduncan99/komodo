/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.*;
import java.io.BufferedWriter;


/**
 * Specifies the interfaces which must be implemented by any concrete system console class.
 */
@SuppressWarnings("Duplicates")
public interface SystemConsole {

    /**
     * For debugging - writes information specific to the implementor, to the log
     */
    void dump(
        final BufferedWriter writer
    );

    /**
     * Cancels a specific pending read-reply message.
     * Used when the message was successfully responded to, so that the client console knows that the message
     * is no longer outstanding.
     */
    void cancelReadReplyMessage(
        final int messageId
    );

    String getName();

    /**
     * Polls the console for the next available input message
     * @return text, or null if there is no message to be had
     */
    String pollInputMessage();

    /**
     * Posts a read-only message to the implementor
     */
    void postReadOnlyMessage(
        final String message,
        final Boolean rightJustified
    );

    /**
     * Posts a read-reply message to the implementor
     */
    void postReadReplyMessage(
        final int messageId,
        final String message,
        final int maxReplyLength
    );

    /**
     * Posts a particular set of status messages to the implementor
     */
    void postStatusMessages(
        final String[] messgaes
    );

    /**
     * Notifies the console that new system log entries are available - if it cares.
     */
    void postSystemLogEntries(
        final KomodoLoggingAppender.LogEntry[] logEntries
    );

    /**
     * Resets the implementor. What this means, is entirely dependent upon the implementor.
     */
    void reset();

    /**
     * Starts up the console, if that makes any sense.
     */
    boolean start();

    /**
     * Stops the console, if that makes any sense.
     */
    void stop();
}
