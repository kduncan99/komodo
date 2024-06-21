/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.facilities.FacilitiesItemTable;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.tasks.Task;

import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Run {

    protected final RunCardInfo _runCardInfo;
    protected final String _actualRunId;
    protected final RunType _runType;
    protected final Instant _submissionTime;
    protected final Instant _actualDeadlineTime;
    protected final Instant _actualStartTime;

    protected long _cardCount;
    protected long _pageCount;
    protected long _timeCount;    // in seconds

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

        if (!(this instanceof Exec)) {
            var exec = Exec.getInstance();
            var cfg = exec.getConfiguration();
            if (_runCardInfo.getMaxCards() == null) {
                _runCardInfo.setMaxCards(cfg.getMaxCards());
            }
            if (_runCardInfo.getMaxPages() == null) {
                _runCardInfo.setMaxPages(cfg.getMaxPages());
            }
            if (_runCardInfo.getMaxTime() == null) {
                _runCardInfo.setMaxTime(cfg.getMaxTime());
            }
        }

        _runConditionWord = new RunConditionWord();
        _defaultQualifier = _runCardInfo.getProjectId();
        _impliedQualifier = _runCardInfo.getProjectId();

        _submissionTime = Instant.now();
        _actualDeadlineTime = getActualTimeValue(_submissionTime, _runCardInfo.getDeadlineTime());
        _actualStartTime = getActualTimeValue(_submissionTime, _runCardInfo.getStartTime());
    }

    private Instant getActualTimeValue(
        final Instant baseTime,
        final RunCardInfo.TimeSpecification timeSpecification
    ) {
        Instant result = null;
        if (timeSpecification != null) {
            if (timeSpecification._isClockTime) {
                result = Instant.now()
                                .atZone(ZoneOffset.UTC)
                                .withHour(timeSpecification._hourSpec)
                                .withMinute(timeSpecification._minuteSpec)
                                .withSecond(0)
                                .withNano(0)
                                .toInstant();
            } else {
                long delta = (timeSpecification._hourSpec * 3600L) + (timeSpecification._minuteSpec * 60L);
                result = baseTime.plusSeconds(delta);
            }
        }
        return result;
    }

    public void decrementWaitingForMassStorage() { _waitingForMassStorageCounter.decrementAndGet(); }
    public void decrementWaitingForPeripheral() { _waitingForPeripheralCounter.decrementAndGet(); }
    public String getAccountId() { return _runCardInfo.getAccountId(); }
    public final String getActualRunId() { return _actualRunId; }
    public final String getDefaultQualifier() { return _defaultQualifier; }
    public final FacilitiesItemTable getFacilitiesItemTable() { return _facilitiesItemTable; }
    public final String getImpliedQualifier() { return _impliedQualifier; }
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
                   indent,
                   getActualRunId(),
                   getOriginalRunId(),
                   getProjectId(),
                   getAccountId(),
                   getUserId(), _runType);
        if (verbose) {
            out.printf("%s  rcw:%s defQual:%s impQual:%s cards:%d/%d pages:%d/%d\n",
                       indent, _runConditionWord.toString(), _defaultQualifier, _impliedQualifier,
                       _cardCount, getMaxCards(), _pageCount, getMaxPages());

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
