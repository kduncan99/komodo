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
    class InputMessage {

        public final long _identifier;      //  console-assigned unique identifier for this message
        public final String _text;          //  text of the message

        InputMessage(
            final long identifier,
            final String text
        ) {
            _identifier = identifier;
            _text = text;
        }
    }

    /**
     * Represents output from the operating system to the console device
     */
    class OutputMessage {

        public final long _identifier;      //  unique identifier for this message
        public final boolean _pinned;       //  if true, this is a message which should not scroll off the display
        public final String[] _text;

        OutputMessage(
            final long identifier,
            final boolean pinned,
            final String text
        ) {
            _identifier = identifier;
            _pinned = pinned;
            String[] temp = { text };
            _text = temp;
        }

        OutputMessage(
            final long identifier,
            final boolean pinned,
            final String[] text
        ) {
            _identifier = identifier;
            _pinned = pinned;
            _text = Arrays.copyOf(text, text.length);
        }
    }

    /**
     * Represents a multi-line status message indicating some basic important (or not-so-important) tallies, rates, etc.
     * Only the most recently-posted set of status messages is expected to be relevant.
     */
    class StatusMessage {

        public final long _identifier;      //  unique identifier for this message
        public final String[] _text;

        StatusMessage(
            final long identifier,
            final String[] text
        ) {
            _identifier = identifier;
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
     * @return InputMessage object, or null if there is no message to be had
     */
    InputMessage pollInputMessage();

    /**
     * Posts a particular OutputMessage to the implementor.
     */
    void postOutputMessage(
        final OutputMessage message
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
