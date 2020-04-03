/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemLogEntry {
    @JsonProperty("timestamp")              public Long _timestamp;     //  system milliseconds
    @JsonProperty("entity")                 public String _entity;      //  reporting entity
    @JsonProperty("message")                public String _message;
}
