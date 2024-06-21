/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.kexec.facilities.FacilitiesItemTable;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.tasks.Task;

import java.io.PrintStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Run {

    protected final String _accountId;
    protected final String _originalRunId;
    protected final String _projectId;
    protected final String _runId;
    protected final RunType _runType;
    protected final Instant _submissionTime;
    protected final String _userId;

    protected char _schedulingPriority;
    protected char _processingPriority;
    protected long _optionsWord;
    protected long _cardCount;
    protected long _cardLimit;
    protected long _pageCount;
    protected long _pageLimit;
    protected long _timeCount;    // in seconds
    protected long _maxTimeLimit; // in seconds

    protected String _defaultQualifier;
    protected String _impliedQualifier;
    protected RunConditionWord _runConditionWord;

    // The following indicate how many waits are in play for mass storage (waiting on file assign or similar)
    // and for peripherals (waiting on unit assign).
    protected final AtomicInteger _waitingForMassStorageCounter = new AtomicInteger(0);
    protected final AtomicInteger _waitingForPeripheralCounter = new AtomicInteger(0);

    protected final FacilitiesItemTable _facilitiesItemTable = new FacilitiesItemTable();
    protected Task _activeTask = null;

    // TODO privileges

    public Run(final RunType runType,
               final String runId,
               final String originalRunId,
               final String projectId,
               final String accountId,
               final String userId) {
        // Nothing here can rely on the Configuration - we go here when Exec is instantiated,
        // and that happens well before we have a Configuration object.
        _runType = runType;
        _accountId = accountId;
        _originalRunId = originalRunId;
        _projectId = projectId;
        _runId = runId;
        _userId = userId;
        _runConditionWord = new RunConditionWord();
        _defaultQualifier = projectId;
        _impliedQualifier = projectId;

        _submissionTime = Instant.now();
    }

    public void decrementWaitingForMassStorage() { _waitingForMassStorageCounter.decrementAndGet(); }
    public void decrementWaitingForPeripheral() { _waitingForPeripheralCounter.decrementAndGet(); }
    public String getAccountId() { return _accountId; }
    public final String getDefaultQualifier() { return _defaultQualifier; }
    public final FacilitiesItemTable getFacilitiesItemTable() { return _facilitiesItemTable; }
    public final String getImpliedQualifier() { return _impliedQualifier; }
    public final String getOriginalRunId() { return _originalRunId; }
    public final String getProjectId() { return _projectId; }
    public final RunConditionWord getRunConditionWord() { return _runConditionWord; }
    public final String getRunId() { return _runId; }
    public final RunType getRunType() { return _runType; }
    public final Instant getSubmissionTime() { return _submissionTime; }
    public final String getUserId() { return _userId; }
    public final boolean hasTask() { return _activeTask != null; }
    public void incrementWaitingForMassStorage() { _waitingForMassStorageCounter.incrementAndGet(); }
    public void incrementWaitingForPeripheral() { _waitingForPeripheralCounter.incrementAndGet(); }
    public abstract boolean isFinished();   // if true, then this exists only for output queue entries
    public abstract boolean isStarted();    // if false, then this is in backlog
    public abstract boolean isSuspended();
    public boolean isWaitingOnMassStorage() { return _waitingForMassStorageCounter.get() > 0; }
    public boolean isWaitingOnPeripheral() { return _waitingForPeripheralCounter.get() > 0; }
    public void setDefaultQualifier(final String qualifier) { _defaultQualifier = qualifier; }
    public void setImpliedQualifier(final String qualifier) { _impliedQualifier = qualifier; }

    public void dump(final PrintStream out,
                     final String indent,
                     final boolean verbose) {
        out.printf("%s%s (%s) proj:%s acct:%s user:%s %s\n",
                   indent, _runId, _originalRunId, _projectId, _accountId, _userId, _runType);
        if (verbose) {
            out.printf("%s  rcw:%s defQual:%s impQual:%s cards:%d/%d pages:%d/%d\n",
                       indent, _runConditionWord.toString(), _defaultQualifier, _impliedQualifier,
                       _cardCount, _cardLimit, _pageCount, _pageLimit);

            // TODO privileges

            out.printf("%s  Facility Items:\n", indent);
            _facilitiesItemTable.dump(out, indent + "    ", verbose);
        }
    }

    public String getEffectiveQualifier(
        final FileSpecification fileSpec
    ) {
        if (!fileSpec.hasQualifier()) {
            return _defaultQualifier;
        } else if (fileSpec.getQualifier().isEmpty()) {
            return _impliedQualifier;
        } else {
            return fileSpec.getQualifier();
        }
    }

    public boolean isPrivileged() {
        return false; // TODO
    }

    public void postContingency(
        final int contingencyType,
        final int errorType,
        final int errorCode
    ) {
        // TODO
    }

    public void postContingency(
        final int contingencyType,
        final int errorType,
        final int errorCode,
        final long auxiliary
    ) {
        // TODO
    }

    public void postToPrint(
        final String text,
        final int lineSkip
    ) {
        // TODO
    }

    public void postToPunch(
        final String text
    ) {
        // TODO
    }

    public void postToTailSheet(
        final String message
    ) {
        // TODO
    }

    public FileSpecification resolveQualifier(
        final FileSpecification initialSpec
    ) {
        return new FileSpecification(getEffectiveQualifier(initialSpec),
                                     initialSpec.getFilename(),
                                     initialSpec.getFileCycleSpecification(),
                                     initialSpec.getReadKey(),
                                     initialSpec.getWriteKey());
    }
}
