/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

class ReadReplyInfo {

    final MessageId _messageId;
    final String _originalMessage;
    final int _maxReplyLength;
    String _response;

    ReadReplyInfo(
        final MessageId messageId,
        final String originalMessage,
        final int maxReplyLength
    ) {
        _messageId = messageId;
        _originalMessage = originalMessage;
        _maxReplyLength = maxReplyLength;
        _response = null;
    }
}
