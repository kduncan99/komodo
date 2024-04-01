/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.exec.RunControlEntry;

public class ReadReplyMessage {
    private final MessageId _messageId;
    private final RunControlEntry _source;
    private final ConsoleId _routing; // may be null
    private final String _runId;   // for logging purposes - may not match RCE of instigator, and may be null
    private final String _text;
    private final boolean _doNotEmitRunId;
    private final boolean _doNotLogResponse;
    private final int _maxReplyLength;

    private ConsoleId _responseConsoleId; // ID of console to which this is currently assigned for reply
    private int _responseConsoleMessageIndex;
    private boolean _isCanceled;

    private String _response;

    public ReadReplyMessage(final RunControlEntry source,
                            final MessageId messageId,
                            final ConsoleId routing,
                            final String runId,
                            final String text,
                            final boolean doNotEmitRunId,
                            final boolean doNotLogResponse,
                            final int maxReplyLength) {
        _messageId = messageId;
        _source = source;
        _routing = routing;
        _runId = runId;
        _text = text;
        _doNotEmitRunId = doNotEmitRunId;
        _doNotLogResponse = doNotLogResponse;
        _maxReplyLength = maxReplyLength;

        _responseConsoleId = null;
        _responseConsoleMessageIndex = 0;
        _isCanceled = false;
        _response = null;
    }

    public final void clearResponseConsoleId() {
        _responseConsoleId = null;
        _responseConsoleMessageIndex = 0;
    }

    public final boolean doNotEmitRunId() { return _doNotEmitRunId; }
    public final boolean doNotLogResponse() { return _doNotLogResponse; }
    public final int getMaxReplyLength() { return _maxReplyLength; }
    public final MessageId getMessageId() { return _messageId; }
    public final String getResponse() { return _response; }
    public final ConsoleId getResponseConsoleId() { return _responseConsoleId; }
    public final int getResponseConsoleMessageIndex() { return _responseConsoleMessageIndex; }
    public final ConsoleId getRouting() { return _routing; }
    public final String getRunId() { return _runId; }
    public final RunControlEntry getSource() { return _source; }
    public final String getText() { return _text; }
    public final boolean hasResponse() { return _response != null; }
    public final boolean isAssignedToConsole() { return _responseConsoleId != null; }
    public final boolean isCanceled() { return _isCanceled; }
    public final void setIsCanceled() { _isCanceled = true; }
    public final void setResponse(final String value) { _response = value; }
    public final void setResponseConsoleId(final ConsoleId value) { _responseConsoleId = value; }
    public final void setResponseConsoleMessageIndex(final int value) { _responseConsoleMessageIndex = value; }
}
