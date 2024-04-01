/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

class ReadReplyTracker {

    private final MessageId _messageId;
    private ConsoleId _replyConsole; // indicates the Console to which the message is assigned
    private int _messageIndex; // from the Console
    private boolean _hasReply;
    private boolean _isCanceled;
    private boolean _retryLater;

    public ReadReplyTracker(final MessageId messageId) {
        _messageId = messageId;
        _replyConsole = null;
        _messageIndex = 0;
        _hasReply = false;
        _isCanceled = false;
        _retryLater = false;
    }

    public MessageId getMessageId() { return _messageId; }
    public int getMessageIndex() { return _messageIndex; }
    public ConsoleId getReplyConsole() { return _replyConsole; }
    public boolean hasReplyConsole() { return _replyConsole != null; }
    public boolean isCanceled() { return _isCanceled; }
    public boolean retryLater() { return _retryLater; }
    public void setHasReply(final boolean flag) { _hasReply = flag; }
    public void setIsCanceled(final boolean flag) { _isCanceled = flag; }

    public void setReplyConsole(final ConsoleId consoleId,
                                final int messageIndex) {
        _replyConsole = consoleId;
        _messageIndex = messageIndex;
    }

    public void setRetryLater(final boolean flag) { _retryLater = flag; }
}
