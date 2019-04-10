/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.hardwarelib.*;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for Processor class
 */
public class Test_Processor {

    private static class TestProcessor extends Processor {

        public TestProcessor(
            final String name,
            final short upi
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
        public void signal(
            final Node source
        ){
        }

        @Override
        public void terminate(){}
    }

    @Test
    public void create(
    ) {
        TestProcessor p = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        assertEquals(Node.Category.Processor, p.getCategory());
        assertEquals("IP0", p.getName());
        assertEquals(Processor.ProcessorType.InstructionProcessor, p.getProcessorType());
    }
}
