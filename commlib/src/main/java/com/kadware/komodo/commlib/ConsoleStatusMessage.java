/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsoleStatusMessage {
    @JsonProperty("identifier")             public Long _identifier;
    @JsonProperty("text")                   public String[] _text;
}
