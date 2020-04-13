/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.net;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsoleInputMessage {

    @JsonProperty("messageId")          public Integer _messageId;  //  only applies to read-reply responses
    @JsonProperty("text")               public String _text;        //  the text of this message

    public ConsoleInputMessage(
        @JsonProperty("messageId")      final Integer messageId,    //  null for unsolicited input
        @JsonProperty("text")           final String text           //  text of the input
    ) {
        _messageId = messageId;
        _text = text;
    }
}
