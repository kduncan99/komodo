/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for BatchRun and DemandRun
 * Contains most of the functional aspects of those two, leaving only the minor differences to be implemented in the subclasses.
 */
public abstract class ControlStatementRun extends NonExecRun {

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

    protected final Set<HoldCondition> _holdConditions = new HashSet<>();

    public ControlStatementRun(
        final RunType runType,
        final String actualRunId,
        final RunCardInfo runCardInfo
    ) {
        super(runType, actualRunId, runCardInfo);
        _inputQueueAddress = 0;
    }

    public final void clearHoldCondition(final HoldCondition condition) { _holdConditions.remove(condition); }
    public final boolean isHeld() { return !_holdConditions.isEmpty(); }
    public final boolean isHeldFor(final HoldCondition condition) { return _holdConditions.contains(condition); }

    @Override public ControlStatementRun setDefaultQualifier(final String qualifier) { super.setDefaultQualifier(qualifier); return this; }
    public ControlStatementRun setHoldCondition(final HoldCondition condition) { _holdConditions.add(condition); return this; }
    @Override public ControlStatementRun setImpliedQualifier(final String qualifier) { super.setImpliedQualifier(qualifier); return this; }
    public ControlStatementRun setInputQueueAddress(final int address) { _inputQueueAddress = address; return this; }
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

    protected void postFinMessageToConsole() {
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

    protected void postStartMessageToConsole() {
        var msg = String.format("%-6s START", _actualRunId);
        Exec.getInstance().sendExecReadOnlyMessage(msg, ConsoleType.System);
        postToTailSheet(msg);
    }
}
