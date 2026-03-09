/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.baselib.Word36;

/**
 * Sufficiently interesting to merit its own class
 * The various bits and subfields are as follows:
 * T1: System condition bits - set only by exec
 *  Bit 3:  Inhibit page ejects between processor calls
 *  Bit 4:  Run rescheduled after recovery boot
 *  Bit 5:  Inhibits run termination when a task error-terminates
 *  Bit 7:  At least one task prior to the most recently-executed task registered an error
 *  Bit 8:  The most recently-executed task registered an error
 *  Bit 9:  Most recent activity termination was an abort
 *  Bit 10: Most recent activity termination was an error
 *  Bit 11: One or more previous activity terminations of the current task,
 *              or previous task if between executions, was an error termination
 *  -- bits 9 through 11 are cleared with the next task execution other than @PMD --
 * T2: initialized to zero, may subsequently be set by the @SETC statement
 * T3: initialized to zero, may subsequently be set by ER SETC$
 */
public class RunConditionWord {

    private static final long MASK_INHIBIT_PAGE_EJECTS = Word36.MASK_B3;
    private static final long MASK_RUN_RESCHEDULED_AFTER_BOOT = Word36.MASK_B4;
    private static final long MASK_INHIBIT_RUN_TERMINATION_ON_TASK_ERROR = Word36.MASK_B5;
    private static final long MASK_AT_LEAST_ONE_PRIOR_TASK_ERROR = Word36.MASK_B7;
    private static final long MASK_MOST_RECENT_TASK_ERROR = Word36.MASK_B8;
    private static final long MASK_MOST_RECENT_ACTIVITY_ABORT = Word36.MASK_B9;
    private static final long MASK_MOST_RECENT_ACTIVITY_ERROR = Word36.MASK_B10;
    private static final long MASK_CURRENT_TASK_ACTIVITY_ERROR = Word36.MASK_B11;

    private boolean _inhibitPageEjects;
    private boolean _runRescheduledAfterBoot;
    private boolean _inhibitRunTerminationOnTaskError;
    private boolean _atLeastOnePriorTaskError;
    private boolean _mostRecentTaskError;
    private boolean _mostRecentActivityAbort;
    private boolean _mostRecentActivityError;
    private boolean _currentTaskActivityError;
    private long _csiSetCValue;
    private long _erSetCValue;

    public RunConditionWord() {}

    public long getWord36() {
        long result = 0;
        if (_inhibitPageEjects) result |= MASK_INHIBIT_PAGE_EJECTS;
        if (_runRescheduledAfterBoot) result |= MASK_RUN_RESCHEDULED_AFTER_BOOT;
        if (_inhibitRunTerminationOnTaskError) result |= MASK_INHIBIT_RUN_TERMINATION_ON_TASK_ERROR;
        if (_atLeastOnePriorTaskError) result |= MASK_AT_LEAST_ONE_PRIOR_TASK_ERROR;
        if (_mostRecentTaskError) result |= MASK_MOST_RECENT_TASK_ERROR;
        if (_mostRecentActivityAbort) result |= MASK_MOST_RECENT_ACTIVITY_ABORT;
        if (_mostRecentActivityError) result |= MASK_MOST_RECENT_ACTIVITY_ERROR;
        if (_currentTaskActivityError) result |= MASK_CURRENT_TASK_ACTIVITY_ERROR;
        result = Word36.setT2(result, _csiSetCValue);
        result = Word36.setT3(result, _erSetCValue);
        return result;
    }

    public RunConditionWord setWord36(final long value) {
        _inhibitPageEjects = (value & MASK_INHIBIT_PAGE_EJECTS) != 0;
        _runRescheduledAfterBoot = (value & MASK_RUN_RESCHEDULED_AFTER_BOOT) != 0;
        _inhibitRunTerminationOnTaskError = (value & MASK_INHIBIT_RUN_TERMINATION_ON_TASK_ERROR) != 0;
        _atLeastOnePriorTaskError = (value & MASK_AT_LEAST_ONE_PRIOR_TASK_ERROR) != 0;
        _mostRecentTaskError = (value & MASK_MOST_RECENT_TASK_ERROR) != 0;
        _mostRecentActivityAbort = (value & MASK_MOST_RECENT_ACTIVITY_ABORT) != 0;
        _mostRecentActivityError = (value & MASK_MOST_RECENT_ACTIVITY_ERROR) != 0;
        _currentTaskActivityError = (value & MASK_CURRENT_TASK_ACTIVITY_ERROR) != 0;
        _csiSetCValue = Word36.getT2(value);
        _erSetCValue = Word36.getT3(value);
        return this;
    }

    public boolean getInhibitPageEjects() { return _inhibitPageEjects; }
    public boolean getRunRescheduledAfterBoot() { return _runRescheduledAfterBoot; }
    public boolean getInhibitRunTerminationOnTaskError() { return _inhibitRunTerminationOnTaskError; }
    public boolean getAtLeastOnePriorTaskError() { return _atLeastOnePriorTaskError; }
    public boolean getMostRecentTaskError() { return _mostRecentTaskError; }
    public boolean getMostRecentActivityAbort() { return _mostRecentActivityAbort; }
    public boolean getMostRecentActivityError() { return _mostRecentActivityError; }
    public boolean getCurrentTaskActivityError() { return _currentTaskActivityError; }

    public long getCSISetCValue() { return _csiSetCValue; }
    public long getERSetCValue() { return _erSetCValue; }

    public RunConditionWord setInhibitPageEjects(final boolean flag) {
        _inhibitPageEjects = flag;
        return this;
    }

    public RunConditionWord setRunRescheduledAfterBoot(final boolean flag) {
        _runRescheduledAfterBoot = flag;
        return this;
    }

    public RunConditionWord setInhibitRunTerminationOnTaskError(final boolean flag) {
        _inhibitRunTerminationOnTaskError = flag;
        return this;
    }

    public RunConditionWord setAtLeastOnePriorTaskError(final boolean flag) {
        _atLeastOnePriorTaskError = flag;
        return this;
    }

    public RunConditionWord setMostRecentTaskError(final boolean flag) {
        _mostRecentTaskError = flag;
        return this;
    }

    public RunConditionWord setMostRecentActivityAbort(final boolean flag) {
        _mostRecentActivityAbort = flag;
        return this;
    }

    public RunConditionWord setMostRecentActivityError(final boolean flag) {
        _mostRecentActivityError = flag;
        return this;
    }

    public RunConditionWord setCurrentTaskActivityError(final boolean flag) {
        _currentTaskActivityError = flag;
        return this;
    }

    public RunConditionWord setCSISetCValue(final long value) {
        _csiSetCValue = value & 07777;
        return this;
    }

    public RunConditionWord setERSetCValue(final long value) {
        _erSetCValue = value & 07777;
        return this;
    }

    public void taskStarted() {
        _mostRecentActivityAbort = false;
        _mostRecentActivityError = false;
        _currentTaskActivityError = false;
    }

    @Override
    public String toString() {
        return Word36.toOctal(getWord36());
    }
}
