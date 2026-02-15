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
public class RunConditionWord extends Word36 {

    // TODO other bit-specific things

    public boolean getInhibitPageEjects() { return (_value & Word36.MASK_B3) != 0; }
    public boolean getRunRescheduledAfterBoot() { return (_value & Word36.MASK_B4) != 0; }
    public boolean getInhibitRunTerminationOnTaskError() { return (_value & Word36.MASK_B5) != 0; }
    public boolean getAtLeastOnePriorTaskError() { return (_value & Word36.MASK_B7) != 0; }
    public boolean getMostRecentTaskError() { return (_value & Word36.MASK_B8) != 0; }
    public boolean getMostRecentActivityAbort() { return (_value & Word36.MASK_B9) != 0; }
    public boolean getMostRecentActivityError() { return (_value & Word36.MASK_B10) != 0; }
    public boolean getCurrentTaskActivityError() { return (_value & Word36.MASK_B11) != 0; }

    public void setInhibitPageEjects(
        final boolean flag
    ) {
        _value &= 0_7377_7777_7777L | (flag ? 0_0400_0000_0000L : 0);
    }

    public void setRunRescheduledAfterBoot(
        final boolean flag
    ) {
        _value &= 0_7577_7777_7777L | (flag ? 0_0200_0000_0000L : 0);
    }

    public void setInhibitRunTerminationOnTaskError(
        final boolean flag
    ) {
        _value &= 0_7677_7777_7777L | (flag ? 0_0100_0000_0000L : 0);
    }

    public void setCSISetCValue(
        final long value
    ) {
        setT2(value);
    }

    public void setERSetCValue(
        final long value
    ) {
        setT3(value);
    }

    public void taskStarted() { _value &= 0_7770_7777_7777L; }
}
