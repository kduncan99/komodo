/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.baselib.Word36;

public abstract class RunControlEntry {

    protected final String _accountId;
    protected String _defaultQualifier;
    protected String _impliedQualifier;
    protected final String _originalRunId;
    protected final String _projectId;
    protected Word36 _runConditionWord;
    protected final String _runId;
    protected final String _userId;
    // TODO privileges
    // TODO facility items

    public RunControlEntry(final String runId,
                           final String originalRunId,
                           final String projectId,
                           final String accountId,
                           final String userId) {
        _accountId = accountId;
        _originalRunId = originalRunId;
        _projectId = projectId;
        _runId = runId;
        _userId = userId;

        _defaultQualifier = projectId;
        _impliedQualifier = projectId;
    }

    public final String getAccountId() { return _accountId; }
    public final String getDefaultQualifier() { return _defaultQualifier; }
    public final String getImpliedQualifier() { return _impliedQualifier; }
    public final String getOriginalRunId() { return _originalRunId; }
    public final String getProjectId() { return _projectId; }
    public final Word36 getRunConditionWord() { return _runConditionWord; }
    public final String getRunId() { return _runId; }
    public final String getUserId() { return _userId; }
    public abstract boolean isBatch();
    public abstract boolean isDemand();
    public abstract boolean isExec();
    public abstract boolean isFinished();   // if true, then this exists only for output queue entries
    public abstract boolean isStarted();    // if false, then this is in backlog
    public abstract boolean isTIP();
    public void setDefaultQualifier(final String qualifier) { _defaultQualifier = qualifier; }
    public void setImpliedQualifier(final String qualifier) { _impliedQualifier = qualifier; }
    public void setRunConditionWord(final long value) { _runConditionWord.setW(value); }
    public void setRunConditionWord(final Word36 value) { _runConditionWord = value; }

    public void postToTailSheet(final String message) {
        // TODO
    }
}
