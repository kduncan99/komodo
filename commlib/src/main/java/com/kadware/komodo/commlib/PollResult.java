/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object encapsulating certain other objects.
 * Client issues a GET on the /poll subdirectory, and we respond with all the updated information.
 * An entity will be null if that entity has not been updated in the interim.
 */
public class PollResult {

    @JsonProperty("jumpKeySettings")            public Long _jumpKeySettings;
    @JsonProperty("newLogEntries")              public SystemLogEntry[] _newLogEntries;
    @JsonProperty("latestStatusMessage")        public ConsoleStatusMessage _latestStatusMessage;
    @JsonProperty("newOutputMessages")          public ConsoleOutputMessage[] _newOutputMessages;
    //  TODO hardware config
    //  TODO system config
}
