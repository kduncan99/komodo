/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for Processor class
 */
public class Test_Processor {

    private static class TestProcessor extends Processor {

        public TestProcessor(
            final String name,
            final int upi
        ) {
            super(ProcessorType.InstructionProcessor, name, upi);
        }

        @Override
        public boolean canConnect(
            final Node ancestor
        ) {
            return false;
        }

        @Override
        public void initialize(){}

        @Override
        public void upiHandleInterrupt(
            final Processor source,
            final boolean broadcast
        ){
        }

        @Override
        public void terminate(){}
    }

    @Test
    public void create(
    ) {
        TestProcessor p = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX);
        assertEquals(NodeCategory.Processor, p._category);
        assertEquals("IP0", p._name);
        assertEquals(ProcessorType.InstructionProcessor, p._processorType);
    }
}
