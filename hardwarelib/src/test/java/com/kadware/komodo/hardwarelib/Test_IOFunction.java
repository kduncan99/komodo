/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for IOFunction class
 */
public class Test_IOFunction {

    @Test
    public void IOFunction_isRead(
    ) {
        assertTrue(IOFunction.GetInfo.isReadFunction());
        assertFalse(IOFunction.MoveBlock.isReadFunction());
        assertFalse(IOFunction.MoveBlockBackward.isReadFunction());
        assertFalse(IOFunction.MoveFile.isReadFunction());
        assertFalse(IOFunction.MoveFileBackward.isReadFunction());
        assertTrue(IOFunction.Read.isReadFunction());
        assertTrue(IOFunction.ReadBackward.isReadFunction());
        assertFalse(IOFunction.Reset.isReadFunction());
        assertFalse(IOFunction.Rewind.isReadFunction());
        assertFalse(IOFunction.RewindInterlock.isReadFunction());
        assertFalse(IOFunction.Write.isReadFunction());
        assertFalse(IOFunction.WriteEndOfFile.isReadFunction());
    }

    @Test
    public void IOFunction_isWrite(
    ) {
        assertFalse(IOFunction.GetInfo.isWriteFunction());
        assertFalse(IOFunction.MoveBlock.isWriteFunction());
        assertFalse(IOFunction.MoveBlockBackward.isWriteFunction());
        assertFalse(IOFunction.MoveFile.isWriteFunction());
        assertFalse(IOFunction.MoveFileBackward.isWriteFunction());
        assertFalse(IOFunction.Read.isWriteFunction());
        assertFalse(IOFunction.ReadBackward.isWriteFunction());
        assertFalse(IOFunction.Reset.isWriteFunction());
        assertFalse(IOFunction.Rewind.isWriteFunction());
        assertFalse(IOFunction.RewindInterlock.isWriteFunction());
        assertTrue(IOFunction.Write.isWriteFunction());
        assertTrue(IOFunction.WriteEndOfFile.isWriteFunction());
    }
}
