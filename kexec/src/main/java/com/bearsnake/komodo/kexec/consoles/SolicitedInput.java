/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

public class SolicitedInput {

    private final MessageId _messageId;
    private final int _messageIndex;
    private final String _text;

    public SolicitedInput(
        final MessageId messageId,
        final int messageIndex,
        final String text
    ) {
        _messageId = messageId;
        _messageIndex = messageIndex;
        _text = text;
    }

    public MessageId getMessageId() { return _messageId; }
    public int getMessageIndex() { return _messageIndex; }
    public String getText() { return _text; }
}
