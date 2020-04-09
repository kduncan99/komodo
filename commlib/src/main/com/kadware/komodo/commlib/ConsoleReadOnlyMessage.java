/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class ConsoleReadOnlyMessage {
    @JsonProperty("text")           public String _text;      //  text of the message

    public ConsoleReadOnlyMessage(
        @JsonProperty("text")       final String text
    ) {
        _text = text;
    }
}
