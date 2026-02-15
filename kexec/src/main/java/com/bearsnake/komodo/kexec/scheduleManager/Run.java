/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exec.RunConditionWord;
import com.bearsnake.komodo.kexec.facilities.FacilitiesItemTable;
import com.bearsnake.komodo.kexec.tasks.Task;

import java.io.PrintStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Run {

    protected final RunCardInfo _runCardInfo;
    protected final String _actualRunId;
    protected final RunType _runType;
    protected final Instant _submissionTime;
    protected long _currentCardCount;
    protected long _currentCardLimit; // may be larger than the max in runCardInfo
    protected long _currentPageCount;
    protected long _currentPageLimit; // may be larger than the max in runCardInfo
    protected long _currentTimeCount; // in seconds
    protected long _currentTimeLimit; // in seconds - may be larger than the max in runCardInfo

    protected String _defaultQualifier;
    protected String _impliedQualifier;
    protected Character _processorPriority; // 'A' through 'Z'
    protected RunConditionWord _runConditionWord;
    protected Character _schedulingPriority; // 'A' through 'Z'

    protected boolean _facErrorFlag = false; // true if at least one CSI fac request errored - can only happen for batch or demand

    // The following indicate how many waits are in play for mass storage (waiting on file assign or similar)
    // and for peripherals (waiting on unit assign).
    protected final AtomicInteger _waitingForMassStorageCounter = new AtomicInteger(0);
    protected final AtomicInteger _waitingForPeripheralCounter = new AtomicInteger(0);

    protected final FacilitiesItemTable _facilitiesItemTable = new FacilitiesItemTable();
    protected Task _activeTask = null;

    // TODO privileges

    public Run(
        final RunType runType,
        final String actualRunId,
        final RunCardInfo runCardInfo
    ) {
        // Nothing here can rely on the Configuration - we go here when Exec is instantiated,
        // and that happens well before we have a Configuration object.
        // Or... if you do violate this, make sure you don't violate it for the Exec run.
        _runType = runType;
        _runCardInfo = runCardInfo;
        _actualRunId = actualRunId;

        // inject defaults into the RunCardInfo object
        if (_runCardInfo.getRunId() == null) {
            _runCardInfo.setRunId("RUN000");
        }

        if (_runCardInfo.getAccountId() == null) {
            _runCardInfo.setAccountId("000000");
        }

        if (_runCardInfo.getProjectId() == null) {
            _runCardInfo.setProjectId("Q$Q$Q$");
        }

        if (_runCardInfo.getSchedulingPriority() == null) {
            _runCardInfo.setSchedulingPriority('A');
        }

        if (_runCardInfo.getProcessorPriority() == null) {
            _runCardInfo.setProcessorPriority('A');
        }

        _runConditionWord = new RunConditionWord();
        _currentCardCount = 0;
        _currentPageCount = 0;
        _currentTimeCount = 0;

        _submissionTime = Instant.now();
    }

    public void decrementWaitingForMassStorage() { _waitingForMassStorageCounter.decrementAndGet(); }
    public void decrementWaitingForPeripheral() { _waitingForPeripheralCounter.decrementAndGet(); }
    public abstract String getAccountId();
    public final Task getActiveTask() { return _activeTask; }
    public final String getActualRunId() { return _actualRunId; }
    public final String getDefaultQualifier() { return _defaultQualifier; }
    public final FacilitiesItemTable getFacilitiesItemTable() { return _facilitiesItemTable; }
    public final String getImpliedQualifier() { return _impliedQualifier; }
    public final Long getMaxCards() { return _runCardInfo.getMaxCards(); }
    public final Long getMaxPages() { return _runCardInfo.getMaxPages(); }
    public final Long getMaxTime() { return _runCardInfo.getMaxTime(); }
    public final String getOriginalRunId() { return _runCardInfo.getRunId(); }
    public final Character getProcessorPriority() { return _runCardInfo.getProcessorPriority(); }
    public final String getProjectId() { return _runCardInfo.getProjectId(); }
    public final RunConditionWord getRunConditionWord() { return _runConditionWord; }
    public final RunType getRunType() { return _runType; }
    public final Character getSchedulingPriority() { return _runCardInfo.getSchedulingPriority(); }
    public final Instant getSubmissionTime() { return _submissionTime; }
    public final String getUserId() { return _runCardInfo.getUserId(); }
    public final boolean hasTask() { return _activeTask != null; }
    public void incrementWaitingForMassStorage() { _waitingForMassStorageCounter.incrementAndGet(); }
    public void incrementWaitingForPeripheral() { _waitingForPeripheralCounter.incrementAndGet(); }
    public abstract boolean isFinished();   // if true, then this exists only for output queue entries
    public abstract boolean isStarted();    // if false, then this is in backlog
    public abstract boolean isSuspended();
    public boolean isWaitingOnMassStorage() { return _waitingForMassStorageCounter.get() > 0; }
    public boolean isWaitingOnPeripheral() { return _waitingForPeripheralCounter.get() > 0; }
    public Run setDefaultQualifier(final String qualifier) { _defaultQualifier = qualifier; return this; }
    public Run setImpliedQualifier(final String qualifier) { _impliedQualifier = qualifier; return this; }
    public Run setProcessorPriority(final Character priority) { _processorPriority = priority; return this; }
    public Run setSchedulingPriority(final Character priority) { _schedulingPriority = priority; return this; }

    public void dump(final PrintStream out,
                     final String indent,
                     final boolean verbose) {
        out.printf("%s%s (%s) acct:%s user:%s proj:%s %s\n",
                   indent,
                   getActualRunId(),
                   getOriginalRunId(),
                   getAccountId(),
                   getUserId(),
                   getProjectId(),
                   _runType);
        if (verbose) {
            out.printf("%s  rcw:%s defQual:%s impQual:%s cards:%d/%d pages:%d/%d\n",
                       indent, _runConditionWord.toString(), _defaultQualifier, _impliedQualifier,
                       _currentCardCount, getMaxCards(), _currentPageCount, getMaxPages());

            // TODO privileges

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

    abstract public boolean isPrivileged();

    abstract public void postContingency(
        final int contingencyType,
        final int errorType,
        final int errorCode
    );

    abstract public void postContingency(
        final int contingencyType,
        final int errorType,
        final int errorCode,
        final long auxiliary
    );

    abstract public void postToPrint(final String text, final int lineSkip);
    abstract public void postToPunch(final String text);
    abstract public void postToTailSheet(final String message);

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
