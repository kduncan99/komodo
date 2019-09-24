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
        assertTrue(Device.IOFunction.GetInfo.isReadFunction());
        assertFalse(Device.IOFunction.MoveBlock.isReadFunction());
        assertFalse(Device.IOFunction.MoveBlockBackward.isReadFunction());
        assertFalse(Device.IOFunction.MoveFile.isReadFunction());
        assertFalse(Device.IOFunction.MoveFileBackward.isReadFunction());
        assertTrue(Device.IOFunction.Read.isReadFunction());
        assertTrue(Device.IOFunction.ReadBackward.isReadFunction());
        assertFalse(Device.IOFunction.Reset.isReadFunction());
        assertFalse(Device.IOFunction.Rewind.isReadFunction());
        assertFalse(Device.IOFunction.RewindInterlock.isReadFunction());
        assertFalse(Device.IOFunction.Write.isReadFunction());
        assertFalse(Device.IOFunction.WriteEndOfFile.isReadFunction());
    }

    @Test
    public void IOFunction_isWrite(
    ) {
        assertFalse(Device.IOFunction.GetInfo.isWriteFunction());
        assertFalse(Device.IOFunction.MoveBlock.isWriteFunction());
        assertFalse(Device.IOFunction.MoveBlockBackward.isWriteFunction());
        assertFalse(Device.IOFunction.MoveFile.isWriteFunction());
        assertFalse(Device.IOFunction.MoveFileBackward.isWriteFunction());
        assertFalse(Device.IOFunction.Read.isWriteFunction());
        assertFalse(Device.IOFunction.ReadBackward.isWriteFunction());
        assertFalse(Device.IOFunction.Reset.isWriteFunction());
        assertFalse(Device.IOFunction.Rewind.isWriteFunction());
        assertFalse(Device.IOFunction.RewindInterlock.isWriteFunction());
        assertTrue(Device.IOFunction.Write.isWriteFunction());
        assertTrue(Device.IOFunction.WriteEndOfFile.isWriteFunction());
    }
}
