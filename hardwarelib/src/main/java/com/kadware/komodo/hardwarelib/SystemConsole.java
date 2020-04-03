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

    abstract class InputMessage {

        public final long _identifier;      //  console-assigned unique identifier for this message

        InputMessage(
            final long identifier
        ) {
            _identifier = identifier;
        }
    }

    class ResponseMessage extends InputMessage {

        public final long _promptMessageIdentifier;
        public final String _text;

        ResponseMessage(
            final long identifier,
            final long promptMessageIdentifier,
            final String text
        ) {
            super(identifier);
            _promptMessageIdentifier = promptMessageIdentifier;
            _text = text;
        }
    }

    class UnsolicitedInputMessage extends InputMessage {

        public final String _text;

        UnsolicitedInputMessage(
            final long identifier,
            final String text
        ) {
            super(identifier);
            _text = text;
        }
    }

    abstract class OutputMessage {

        public final long _identifier;      //  our internal unique identifier for this message
        public final long _osIdentifier;    //  operating system identifier for this message (if any)

        OutputMessage(
            final long identifier,
            final long osIdentifier
        ) {
            _identifier = identifier;
            _osIdentifier = osIdentifier;
        }
    }

    /**
     * Represents a single- or multi-line message representing a notification generated either by the OS or by one of
     * the applications, intended to be displayed upon the system console. Usually messages are presented within a single
     * line - however some of the more verbose messages may require several lines of output.
     * ...
     * It is unspecified whether the console device is able to display the entire message, nor what the console does in the
     * event a message is too long to be displayed. A console may truncate the message, or it may break up the message at a
     * convenient point and display it as two or more individual lines. The only restriction is that a console may not
     * reject the message for any reason.
     */
    class ReadOnlyMessage extends OutputMessage {

        public final String[] _text;

        ReadOnlyMessage(
            final long identifier,
            final long osIdentifier,
            final String text
        ) {
            super(identifier, osIdentifier);
            String[] temp = { text };
            _text = temp;
        }

        ReadOnlyMessage(
            final long identifier,
            final long osIdentifier,
            final String[] text
        ) {
            super(identifier, osIdentifier);
            _text = Arrays.copyOf(text, text.length);
        }
    }

    /**
     * Represents a single- or multi-line message representing a notification generated either by the OS or by one of
     * the applications, intended to be displayed upon the system console. Usually messages are presented within a single
     * line - however some of the more verbose messages may require several lines of output.
     * ...
     * It is unspecified whether the console device is able to display the entire message, nor what the console does in the
     * event a message is too long to be displayed. A console may truncate the message, or it may break up the message at a
     * convenient point and display it as two or more individual lines. If the console is display-only, it should reject the
     * message.
     * ...
     * It is unspecified whether the console is required to support the maximum size of response. A console may imply a
     * shorter restriction on the response length if so dictated by the console hardware. A console should be designed such
     * that responses of at least 12 characters are possible; a minimum of 64 characters are suggested.
     * ...
     * Multi-line responses are not supported.
     * ...
     * Consoles must be able to manage up to MAX_OUTSTANDING_READ_REPLY_MESSAGES concurrent messages.
     */
    class ReadReplyMessage extends OutputMessage {

        public final int _maxResponseLength;
        public final String[] _prompt;

        ReadReplyMessage(
            final long identifier,
            final long osIdentifier,
            final String prompt,
            final int maxResponseLength
        ) {
            super(identifier, osIdentifier);
            _maxResponseLength = maxResponseLength;
            String[] temp = { prompt };
            _prompt = temp;
        }

        ReadReplyMessage(
            final long identifier,
            final long osIdentifier,
            final String[] prompt,
            final int maxResponseLength
        ) {
            super(identifier, osIdentifier);
            _maxResponseLength = maxResponseLength;
            _prompt = Arrays.copyOf(prompt, prompt.length);
        }
    }

    /**
     * Represents a multi-line status message indicating some basic important (or not-so-important) tallies, rates, etc.
     * Only the most recently-posted set of status messages is expected to be relevant.
     */
    class StatusMessage extends OutputMessage {

        public final String[] _text;

        StatusMessage(
            final long identifier,
            final long osIdentifier,
            final String[] text
        ) {
            super(identifier, osIdentifier);
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
     * Required in the construction of any OutputMessage - asks the implementor to produce a unique
     * integer identifier for subsequently identifying the particular message being constructed.
     */
    long getNextMessageIdentifier();

    /**
     * Polls the console for the next available input message
     * @return InputMessage subclass object, or null if there is no message to be had
     */
    InputMessage pollMessage();

    /**
     * Posts a particular OutputMessage to the implementor.
     * There are only rare cases where the implementor is allowed to reject the message; if it does so, it will return false.
     */
    boolean postMessage(
        final OutputMessage message
    );

    /**
     * Notifies the console that new system log entries are available - if it cares.
     */
    void postSystemLogEntries(
        final KomodoAppender.LogEntry[] logEntries
    );

    /**
     * Used primarily for indicating that a previously-posted ReadReplyMessage is no longer active, presumably
     * because it has been responded to by some other entity. Generally, this shouldn't happen as the operating system
     * is supposed to be smart enough not to post a particular read-reply message to multiple consoles.
     * Implementor returns true if the message has been found and dealt with.
     */
    boolean revokeMessage(
        final OutputMessage message
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
