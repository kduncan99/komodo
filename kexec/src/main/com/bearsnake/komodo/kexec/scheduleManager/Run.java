/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.RunConditionWord;
import com.bearsnake.komodo.kexec.facilities.FacilitiesItemTable;
import com.bearsnake.komodo.baselib.FileSpecification;
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
    protected RunConditionWord _runConditionWord;

    private String _invalidRunReason; // if not null, this run will be removed from BL with this error message
    private boolean _isReady;         // false while the run is still being built (not ready to come out of backlog)

    // The following indicate how many waits are in play for mass storage (waiting on file assign or similar)
    // and for peripherals (waiting on unit assign).
    protected final AtomicInteger _waitingForMassStorageCounter = new AtomicInteger(0);
    protected final AtomicInteger _waitingForPeripheralCounter = new AtomicInteger(0);

    protected final FacilitiesItemTable _facilitiesItemTable = new FacilitiesItemTable();
    protected Task _activeTask = null;

    // TODO privileges

    public Run(final RunType runType,
               final String actualRunId,
               final RunCardInfo runCardInfo) {
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

        if (this instanceof Exec) {
            _currentCardLimit = 0;
            _currentPageLimit = 0;
            _currentTimeLimit = 0;
        } else {
            var exec = Exec.getInstance();
            var cfg = exec.getConfiguration();
            if (_runCardInfo.getMaxCards() == null) {
                _runCardInfo.setMaxCards(cfg.getIntegerValue(Tag.MAXCRD));
            }
            if (_runCardInfo.getMaxPages() == null) {
                _runCardInfo.setMaxPages(cfg.getIntegerValue(Tag.MAXPAG));
            }
            if (_runCardInfo.getMaxTime() == null) {
                _runCardInfo.setMaxTime(cfg.getIntegerValue(Tag.MAXTIM));
            }
        }

        _runConditionWord = new RunConditionWord();
        _defaultQualifier = _runCardInfo.getProjectId();
        _impliedQualifier = _runCardInfo.getProjectId();
        _currentCardCount = 0;
        _currentPageCount = 0;
        _currentTimeCount = 0;

        _submissionTime = Instant.now();
    }

    public void decrementWaitingForMassStorage() { _waitingForMassStorageCounter.decrementAndGet(); }
    public void decrementWaitingForPeripheral() { _waitingForPeripheralCounter.decrementAndGet(); }
    public String getAccountId() { return _runCardInfo.getAccountId(); }
    public final Task getActiveTask() { return _activeTask; }
    public final String getActualRunId() { return _actualRunId; }
    public final String getDefaultQualifier() { return _defaultQualifier; }
    public final FacilitiesItemTable getFacilitiesItemTable() { return _facilitiesItemTable; }
    public final String getImpliedQualifier() { return _impliedQualifier; }
    public final String getInvalidRunReason() { return _invalidRunReason; }
    public final Long getMaxCards() { return _runCardInfo.getMaxCards(); }
    public final Long getMaxPages() { return _runCardInfo.getMaxPages(); }
    public final Long getMaxTime() { return _runCardInfo.getMaxTime(); }
    public final String getOriginalRunId() { return _runCardInfo.getRunId(); }
    public final String getProjectId() { return _runCardInfo.getProjectId(); }
    public final RunConditionWord getRunConditionWord() { return _runConditionWord; }
    public final RunType getRunType() { return _runType; }
    public final Instant getSubmissionTime() { return _submissionTime; }
    public final String getUserId() { return _runCardInfo.getUserId(); }
    public final boolean hasTask() { return _activeTask != null; }
    public void incrementWaitingForMassStorage() { _waitingForMassStorageCounter.incrementAndGet(); }
    public void incrementWaitingForPeripheral() { _waitingForPeripheralCounter.incrementAndGet(); }
    public abstract boolean isFinished();   // if true, then this exists only for output queue entries
    public final boolean isInvalidRun() { return _invalidRunReason != null; }
    public abstract boolean isStarted();    // if false, then this is in backlog
    public abstract boolean isSuspended();
    public boolean isWaitingOnMassStorage() { return _waitingForMassStorageCounter.get() > 0; }
    public boolean isWaitingOnPeripheral() { return _waitingForPeripheralCounter.get() > 0; }
    public void setDefaultQualifier(final String qualifier) { _defaultQualifier = qualifier; }
    public void setImpliedQualifier(final String qualifier) { _impliedQualifier = qualifier; }
    public void setInvalidRunReason(final String reason) { _invalidRunReason = reason; }
    public void setIsReady() { _isReady = true; }

    public void dump(final PrintStream out,
                     final String indent,
                     final boolean verbose) {
        out.printf("%s%s (%s) proj:%s acct:%s user:%s %s\n",
                   indent,
                   getActualRunId(),
                   getOriginalRunId(),
                   getProjectId(),
                   getAccountId(),
                   getUserId(), _runType);
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
