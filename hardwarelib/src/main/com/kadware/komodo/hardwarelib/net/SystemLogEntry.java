/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.net;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemLogEntry {
    @JsonProperty("timestamp")              public Long _timestamp;     //  system milliseconds
    @JsonProperty("category")               public String _category;    //  ERROR, TRACE, etc
    @JsonProperty("entity")                 public String _entity;      //  reporting entity
    @JsonProperty("message")                public String _message;

    public SystemLogEntry (
        @JsonProperty("timestamp")      final Long timestamp,
        @JsonProperty("category")       final String category,
        @JsonProperty("entity")         final String entity,
        @JsonProperty("message")        final String message
    ) {
        _timestamp = timestamp;
        _category = category;
        _entity = entity;
        _message = message;
    }
}
