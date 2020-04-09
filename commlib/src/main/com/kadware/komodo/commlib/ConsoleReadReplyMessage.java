/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class ConsoleReadReplyMessage {
    @JsonProperty("messageId")              public int _messageId;      //  identifier of this message
    @JsonProperty("maxReplyLength")         public int _maxReplyLength; //  max length of the reply to this message
    @JsonProperty("text")                   public String[] _text;      //  text of the message

    public ConsoleReadReplyMessage(
        @JsonProperty("messageId")      final int messageId,
        @JsonProperty("maxReplyLength") final int maxReplyLength,
        @JsonProperty("text")           final String[] text
    ) {
        _maxReplyLength = maxReplyLength;
        _messageId = messageId;
        _text = Arrays.copyOf(text, text.length);
    }
}
