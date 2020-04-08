/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class ConsoleStatusMessage {
    @JsonProperty("identifier")             public Long _identifier;
    @JsonProperty("text")                   public String[] _text;

    public ConsoleStatusMessage(
        @JsonProperty("identifier") final Long identifier,
        @JsonProperty("text") final String[] text
    ) {
        _identifier = identifier;
        _text = Arrays.copyOf(text, text.length);
    }
}
