/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsoleInputMessage {
    @JsonProperty("text")                   public String _text;        //  the text of this message

    public ConsoleInputMessage(
        @JsonProperty("text") final String text
    ) {
        _text = text;
    }
}
