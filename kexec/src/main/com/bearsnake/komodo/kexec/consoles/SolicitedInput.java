/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

public class SolicitedInput {

    private final MessageId _messageId;
    private final String _text;

    public SolicitedInput(
        final MessageId messageId,
        final String text
    ) {
        _messageId = messageId;
        _text = text;
    }

    public MessageId getMessageId() { return _messageId; }
    public String getText() { return _text; }
}
