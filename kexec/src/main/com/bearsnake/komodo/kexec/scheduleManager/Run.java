/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.RunConditionWord;
import com.bearsnake.komodo.kexec.facilities.FacilitiesItemTable;
import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.facilities.facItems.DiskFileFacilitiesItem;
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

    public boolean isPrivileged() {
        // check SYS$*DLOC$
        var facItem = _facilitiesItemTable.getFacilitiesItem("SYS$", "DLOC$");
        if (facItem != null) {
            var dfi = (DiskFileFacilitiesItem)facItem;
            if (dfi.isReadable() && dfi.isWriteable()) {
                return true;
            }
        }

        // Is the run privileged by way of Security?
        // That's a tricky one, there are various dimensions of privilege. This applies only to general all-encompassing privilege.
        // TODO
        return false;
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

    protected void postFinMessage() {
        var sb = new StringBuilder();
        sb.append(_actualRunId).append("      ").setLength(7);
        if (_facErrorFlag) {
            sb.append("FAC ERROR ");
        } else if (_runConditionWord.getMostRecentActivityAbort()) {
            sb.append("ABORT ");
        } else if (_runConditionWord.getAtLeastOnePriorTaskError() || _runConditionWord.getMostRecentTaskError()) {
            sb.append("ERROR ");
        }
        sb.append("FIN");

        var msg = sb.toString();
        Exec.getInstance().sendExecReadOnlyMessage(msg, ConsoleType.System);
        postToTailSheet(msg);
    }

    protected void postStartMessage() {
        var msg = String.format("%-6s START", _actualRunId);
        Exec.getInstance().sendExecReadOnlyMessage(msg, ConsoleType.System);
        postToTailSheet(msg);
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
