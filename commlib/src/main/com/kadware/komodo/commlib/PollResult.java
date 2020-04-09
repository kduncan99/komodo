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

    @JsonProperty("inputDelivered")             public final Boolean _inputDelivered;
    @JsonProperty("isMaster")                   public final Boolean _isMaster;
    @JsonProperty("jumpKeySettings")            public final Long _jumpKeySettings;
    @JsonProperty("latestStatusMessage")        public final ConsoleStatusMessage _latestStatusMessage;
    @JsonProperty("newLogEntries")              public final SystemLogEntry[] _newLogEntries;
    @JsonProperty("newReadOnlyMessages")        public final ConsoleReadOnlyMessage[] _newReadOnlyMessages;
    @JsonProperty("readReplyMessagesUpdated")   public final Boolean _readReplyMessagesUpdated;
    //  TODO hardware config
    //  TODO system config

    public PollResult(
        @JsonProperty("inputDelivered")             final Boolean inputDelivered,
        @JsonProperty("isMaster")                   final Boolean isMaster,
        @JsonProperty("jumpKeySettings")            final Long jumpKeySettings,
        @JsonProperty("latestStatusMessage")        final ConsoleStatusMessage latestStatusMessage,
        @JsonProperty("newLogEntries")              final SystemLogEntry[] newLogEntries,
        @JsonProperty("newReadOnlyMessages")        final ConsoleReadOnlyMessage[] newReadOnlyMessages,
        @JsonProperty("readReplyMessagesUpdated")   final Boolean readReplyMessagesUpdated
    ) {
        _inputDelivered = inputDelivered;
        _isMaster = isMaster;
        _jumpKeySettings = jumpKeySettings;
        _latestStatusMessage = latestStatusMessage;
        _newLogEntries = newLogEntries == null ? null :  Arrays.copyOf(newLogEntries, newLogEntries.length);
        _newReadOnlyMessages = newReadOnlyMessages == null ? null : Arrays.copyOf(newReadOnlyMessages, newReadOnlyMessages.length);
        _readReplyMessagesUpdated = readReplyMessagesUpdated;
    }
}
