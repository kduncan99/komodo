/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.FacilitiesItemTable;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.tasks.Task;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RunControlEntry {

    protected final String _accountId;
    protected String _defaultQualifier;
    protected String _impliedQualifier;
    protected final String _originalRunId;
    protected final String _projectId;
    protected Word36 _runConditionWord = new Word36();
    protected final String _runId;
    protected final RunType _runType;
    protected final String _userId;

    protected long _cardCount;
    protected long _cardLimit;
    protected long _pageCount;
    protected long _pageLimit;

    // The following indicate how many waits are in play for mass storage (waiting on file assign or similar)
    // and for peripherals (waiting on unit assign).
    protected final AtomicInteger _waitingForMassStorageCounter = new AtomicInteger(0);
    protected final AtomicInteger _waitingForPeripheralCounter = new AtomicInteger(0);

    protected final FacilitiesItemTable _facilitiesItems = new FacilitiesItemTable();
    protected Task _activeTask = null;

    // TODO privileges

    public RunControlEntry(final RunType runType,
                           final String runId,
                           final String originalRunId,
                           final String projectId,
                           final String accountId,
                           final String userId) {
        _runType = runType;
        _accountId = accountId;
        _originalRunId = originalRunId;
        _projectId = projectId;
        _runId = runId;
        _userId = userId;

        _cardLimit = Exec.getInstance().getConfiguration().getMaxCards();
        _pageLimit = Exec.getInstance().getConfiguration().getMaxPages();

        _defaultQualifier = projectId;
        _impliedQualifier = projectId;
    }

    public void decrementWaitingForMassStorage() { _waitingForMassStorageCounter.decrementAndGet(); }
    public void decrementWaitingForPeripheral() { _waitingForPeripheralCounter.decrementAndGet(); }
    public final String getAccountId() { return _accountId; }
    public final String getDefaultQualifier() { return _defaultQualifier; }
    public final FacilitiesItemTable getFacilityItemTable() { return _facilitiesItems; }
    public final String getImpliedQualifier() { return _impliedQualifier; }
    public final String getOriginalRunId() { return _originalRunId; }
    public final String getProjectId() { return _projectId; }
    public final Word36 getRunConditionWord() { return _runConditionWord; }
    public final String getRunId() { return _runId; }
    public final RunType getRunType() { return _runType; }
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
    public void setRunConditionWord(final long value) { _runConditionWord.setW(value); }
    public void setRunConditionWord(final Word36 value) { _runConditionWord = value; }

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
            _facilitiesItems.dump(out, indent + "    ", verbose);
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
