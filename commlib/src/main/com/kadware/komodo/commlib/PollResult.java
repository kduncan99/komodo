/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

/**
 * Object encapsulating certain other objects.
 * Client issues a GET on the /poll subdirectory, and we respond with all the updated information.
 * An entity will be null if that entity has not been updated in the interim.
 */
public class PollResult {

    @JsonProperty("jumpKeySettings")            public final Long _jumpKeySettings;
    @JsonProperty("newLogEntries")              public final SystemLogEntry[] _newLogEntries;
    @JsonProperty("latestStatusMessage")        public final ConsoleStatusMessage _latestStatusMessage;
    @JsonProperty("newOutputMessages")          public final ConsoleOutputMessage[] _newOutputMessages;
    //  TODO hardware config
    //  TODO system config

    public PollResult(
        @JsonProperty("jumpKeySettings")        final Long jumpKeySettings,
        @JsonProperty("newLogEntries")          final SystemLogEntry[] newLogEntries,
        @JsonProperty("latestStatusMessage")    final ConsoleStatusMessage latestStatusMessage,
        @JsonProperty("newOutputMessages")      final ConsoleOutputMessage[] newOutputMessages
    ) {
        _jumpKeySettings = jumpKeySettings;
        _newLogEntries = newLogEntries == null ? null :  Arrays.copyOf(newLogEntries, newLogEntries.length);
        _latestStatusMessage = latestStatusMessage;
        _newOutputMessages = newOutputMessages == null ? null : Arrays.copyOf(newOutputMessages, newOutputMessages.length);
    }
}
