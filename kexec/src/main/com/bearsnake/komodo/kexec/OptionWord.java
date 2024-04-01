/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.baselib.Word36;

public class OptionWord extends Word36 {

    public final static long AOption = 0_000200_000000L;
    public final static long BOption = 0_000100_000000L;
    public final static long COption = 0_000040_000000L;
    public final static long DOption = 0_000020_000000L;
    public final static long EOption = 0_000010_000000L;
    public final static long FOption = 0_000004_000000L;
    public final static long GOption = 0_000002_000000L;
    public final static long HOption = 0_000001_000000L;
    public final static long IOption = 0_000000_400000L;
    public final static long JOption = 0_000000_200000L;
    public final static long KOption = 0_000000_100000L;
    public final static long LOption = 0_000000_040000L;
    public final static long MOption = 0_000000_020000L;
    public final static long NOption = 0_000000_010000L;
    public final static long OOption = 0_000000_004000L;
    public final static long POption = 0_000000_002000L;
    public final static long QOption = 0_000000_001000L;
    public final static long ROption = 0_000000_000400L;
    public final static long SOption = 0_000000_000200L;
    public final static long TOption = 0_000000_000100L;
    public final static long UOption = 0_000000_000040L;
    public final static long VOption = 0_000000_000020L;
    public final static long WOption = 0_000000_000010L;
    public final static long XOption = 0_000000_000004L;
    public final static long YOption = 0_000000_000002L;
    public final static long ZOption = 0_000000_000001L;

    public final boolean IsAOptionSet() { return (_value & AOption) != 0; }
    public final boolean IsBOptionSet() { return (_value & BOption) != 0; }
    public final boolean IsCOptionSet() { return (_value & COption) != 0; }
    public final boolean IsDOptionSet() { return (_value & DOption) != 0; }
    public final boolean IsEOptionSet() { return (_value & EOption) != 0; }
    public final boolean IsFOptionSet() { return (_value & FOption) != 0; }
    public final boolean IsGOptionSet() { return (_value & GOption) != 0; }
    public final boolean IsHOptionSet() { return (_value & HOption) != 0; }
    public final boolean IsIOptionSet() { return (_value & IOption) != 0; }
    public final boolean IsJOptionSet() { return (_value & JOption) != 0; }
    public final boolean IsKOptionSet() { return (_value & KOption) != 0; }
    public final boolean IsLOptionSet() { return (_value & LOption) != 0; }
    public final boolean IsMOptionSet() { return (_value & MOption) != 0; }
    public final boolean IsNOptionSet() { return (_value & NOption) != 0; }
    public final boolean IsOOptionSet() { return (_value & OOption) != 0; }
    public final boolean IsPOptionSet() { return (_value & POption) != 0; }
    public final boolean IsQOptionSet() { return (_value & QOption) != 0; }
    public final boolean IsROptionSet() { return (_value & ROption) != 0; }
    public final boolean IsSOptionSet() { return (_value & SOption) != 0; }
    public final boolean IsTOptionSet() { return (_value & TOption) != 0; }
    public final boolean IsUOptionSet() { return (_value & UOption) != 0; }
    public final boolean IsVOptionSet() { return (_value & VOption) != 0; }
    public final boolean IsWOptionSet() { return (_value & WOption) != 0; }
    public final boolean IsXOptionSet() { return (_value & XOption) != 0; }
    public final boolean IsYOptionSet() { return (_value & YOption) != 0; }
    public final boolean IsZOptionSet() { return (_value & ZOption) != 0; }
}
