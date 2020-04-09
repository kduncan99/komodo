/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class ConsoleStatusMessage {
    @JsonProperty("text")                   public String[] _text;

    public ConsoleStatusMessage(
        @JsonProperty("text") final String[] text
    ) {
        _text = Arrays.copyOf(text, text.length);
    }
}
