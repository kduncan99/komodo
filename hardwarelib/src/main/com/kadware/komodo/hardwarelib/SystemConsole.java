/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.*;
import java.io.BufferedWriter;
import java.util.Arrays;


/**
 * Specifies the interfaces which must be implemented by any concrete system console class.
 */
@SuppressWarnings("Duplicates")
interface SystemConsole {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Primitive objects representing console traffic in either direction
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Represents input to the operating system from the console device
     */
    abstract class InputMessage {

        public final String _text;

        InputMessage(
            final String text
        ) {
            _text = text;
        }
    }

    /**
     * Represents unsolicited input to the operating system from the console device
     */
    class UnsolicitedInputMessage extends InputMessage {

        UnsolicitedInputMessage(
            final String text
        ) {
            super(text);
        }
    }

    /**
     * Represents input to the operating system from the console device, in response to a previous read-reply message
     */
    class ReadReplyInputMessage extends InputMessage {

        public final int _messageId;        //  from the associated ReadReplyOutputMessage

        ReadReplyInputMessage(
            final int messageId,
            final String text
        ) {
            super(text);
            _messageId = messageId;
        }
    }

    /**
     * Represents output from the operating system to the console device
     */
    abstract class OutputMessage {}

    /**
     * Represents output from the operating system to the console device, which does not require a response
     */
    class ReadOnlyMessage extends OutputMessage {

        public final String _text;

        ReadOnlyMessage(
            final String text
        ) {
            _text = text;
        }
    }

    /**
     * Represents output from the operating system to the console device, which does require a response
     */
    class ReadReplyMessage extends OutputMessage {

        public final int _maxReplyLength;   //  max accepted reply length in characters
        public final int _messageId;
        public final String _text;

        ReadReplyMessage(
            final int messageId,
            final String text,
            final int maxReplyLength
        ) {
            _messageId = messageId;
            _text = text;
            _maxReplyLength = maxReplyLength;
        }
    }

    /**
     * Represents output from the operating system to the console device
     */
    class StatusMessage extends OutputMessage {

        public final String[] _text;    //  One or more lines of text comprising the reported status

        StatusMessage(
            final String[] text
        ) {
            _text = Arrays.copyOf(text, text.length);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Methods which must be implemented by the subclass
    //  ----------------------------------------------------------------------------------------------------------------------------

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

    /**
     * Polls the console for the next available input message
     * @return InputMessage object, or null if there is no message to be had
     */
    InputMessage pollInputMessage();

    /**
     * Posts a particular ReadOnlyMessage to the implementor.
     */
    void postReadOnlyMessage(
        final ReadOnlyMessage message
    );

    /**
     * Posts a particular ReadReplyMessage to the implementor.
     */
    void postReadReplyMessage(
        final ReadReplyMessage message
    );

    /**
     * Posts a particular OutputMessage to the implementor.
     * If the identifier is the same as a message already known to the console, it overwrites that message.
     * This is the method used for 'unpinning' a previously pinned message.
     */
    void postStatusMessage(
        final StatusMessage message
    );

    /**
     * Notifies the console that new system log entries are available - if it cares.
     */
    void postSystemLogEntries(
        final KomodoAppender.LogEntry[] logEntries
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
