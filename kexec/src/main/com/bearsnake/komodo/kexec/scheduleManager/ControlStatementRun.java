/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.csi.RunCardInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for BatchRun and DemandRun
 * Contains most of the functional aspects of those two, leaving only the minor differences to be implemented in the subclasses.
 */
public abstract class ControlStatementRun extends Run {

    public static enum HoldCondition {
        ArbitraryDeviceWait(7, "AD"),
        CatalogedFileWait(5, "CT"),
        FinStatementHold(0, "F"),
        MassStorageAvailability(4, "MS"),
        IndividualHold(1, "O"),
        OtherFacilityHold(8, "OH"),
        SOptionHold(2, "S"),
        StartTimePending(3, "ST"),
        TapeHold(6, "TP");

        private final String _code;
        private final int _priority; // lower value is higher priority

        HoldCondition(final int priority, final String code) {
            _priority = priority;
            _code = code;
        }

        public String getCode() { return _code; }
        public int getPriority() { return _priority; }
    }

    protected final Set<BatchRun.HoldCondition> _holdConditions = new HashSet<>();

    // Sector address in GENF$ of the input queue item for this job if it isn't open yet
    // Will be set when the input queue item is created; will be set to zero once the input queue item is removed.
    protected long _inputQueueAddress;

    // True only if we have come out of backlog for batch, or are DEMAND, and have subsequently finished.
    // We *might* have queue items, but if we don't, we will very quickly be removed from ScheduleManager.
    protected boolean _isFinished = false;

    // False if we never left backlog; true if we are running, or we have finished.
    // Always true for DEMAND.
    // Usually there are queue items, but this might not be true for jobs which have *just* finished with no
    // queue items and have not yet been removed from ScheduleManager, or for jobs which *did* have queue items
    // but those queue items have just been removed, and we have not yet been removed from ScheduleManager.
    protected boolean _isStarted = false;

    // Indicates whether this run is currently suspended
    protected boolean _isSuspended = false;

    // Indicates the symbiont of site-id which produced the run. null if started from the console.
    protected String _siteId;

    // Thread which is running this runnable, if we are started
    protected Thread _thread;

    public ControlStatementRun(
        final RunType runType,
        final String actualRunId,
        final RunCardInfo runCardInfo
    ) {
        super(runType, actualRunId, runCardInfo);
        _inputQueueAddress = 0;
    }

    public final void clearHoldCondition(final HoldCondition condition) { _holdConditions.remove(condition); }
    public final void clearInputQueueAddress() { _inputQueueAddress = 0; }
    public final long getInputQueueAddress() { return _inputQueueAddress; }
    public final String getSiteId() { return _siteId; }
    @Override public final boolean isFinished() { return _isFinished; }
    public final boolean isHeld() { return !_holdConditions.isEmpty(); }
    public final boolean isHeldFor(final HoldCondition condition) { return _holdConditions.contains(condition); }
    @Override public final boolean isStarted() { return _isStarted; }
    @Override public final boolean isSuspended() { return _isSuspended; }

    @Override public ControlStatementRun setDefaultQualifier(final String qualifier) { super.setDefaultQualifier(qualifier); return this; }
    public ControlStatementRun setHoldCondition(final HoldCondition condition) { _holdConditions.add(condition); return this; }
    @Override public ControlStatementRun setImpliedQualifier(final String qualifier) { super.setImpliedQualifier(qualifier); return this; }
    public ControlStatementRun setInputQueueAddress(final long address) { _inputQueueAddress = address; return this; }
    public ControlStatementRun setIsFinished(final boolean isFinished) { _isFinished = isFinished; return this; }
    public ControlStatementRun setIsStarted(final boolean isStarted) { _isStarted = isStarted; return this; }
    @Override public ControlStatementRun setProcessorPriority(final Character priority) { super.setProcessorPriority(priority); return this; }
    @Override public ControlStatementRun setSchedulingPriority(final Character priority) { super.setSchedulingPriority(priority); return this; }
    public ControlStatementRun setSiteId(final String siteId) { _siteId = siteId; return this; }

    public HoldCondition getHighestPriorityHoldCondition() {
        HoldCondition result = null;
        for (var condition : _holdConditions) {
            if (result == null || (condition.getPriority() < result.getPriority())) {
                result = condition;
            }
        }
        return result;
    }
}
