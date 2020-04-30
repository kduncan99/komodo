/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.*;
import java.io.BufferedWriter;


/**
 * Specifies the interfaces which must be implemented by any concrete system console class.
 * Referred to fondly as the SCIF.
 */
public interface SystemConsoleInterface {

    class ConsoleInputMessage {
        public final int _consoleIdentifier;
        public final String _text;

        public ConsoleInputMessage(
            final int consoleIdentifier,
            final String text
        ) {
            _consoleIdentifier = consoleIdentifier;
            _text = text;
        }

        @Override
        public String toString() {
            return String.format("%d:%s", _consoleIdentifier, _text);
        }
    }

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
     * @param consoleId console id of a specific console; zero for all consoles
     * @param messageId the messgae identifier of the read-reply message we wish to cancel
     * @param replacementText text to be written over the original read-reply message
     */
    void cancelReadReplyMessage(
        final int consoleId,
        final int messageId,
        final String replacementText
    );

    String getName();

    /**
     * Polls the console for the next available input message
     * @return InputMessage object, or null if there is no message to be had
     */
    ConsoleInputMessage pollInputMessage();

    /**
     * Alternate method of polling the console.
     * Waits until input is available, then returns - if the wait period is exceeded, returns with null
     */
    ConsoleInputMessage pollInputMessage(long timeoutMillis);

    /**
     * Posts a read-only message to the implementor
     * @param consoleId console id of a specific console; zero for all consoles
     * @param message the message to be displayed
     * @param rightJustified true if this message should be aligned against the right side of the output
     *                       this is usually true for time-of-day messagees
     * @param cached true if this message should be cached for future new sessions
     *               this should be false for time-of-day messages
     */
    void postReadOnlyMessage(
        final int consoleId,
        final String message,
        final Boolean rightJustified,
        final Boolean cached
    );

    /**
     * Posts a read-reply message to the implementor
     * @param consoleId console id of a specific console; zero for all consoles
     * @param messageId message identifier which can be used for canceling this message later
     * @param message text of the message
     * @param maxReplyLength maximum number of characters allowed in the reply
     */
    void postReadReplyMessage(
        final int consoleId,
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
