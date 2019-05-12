/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.instructionProcessor.test;

import com.kadware.em2200.hardwarelib.*;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_Basic extends Test_InstructionProcessor {

    @Test
    public void canConnect(
    ) {
        InstructionProcessor ip = new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        assertFalse(ip.canConnect(ip));
    }
}
