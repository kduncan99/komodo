/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

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
        public void run() {
            while (!_workerTerminate) {
                try {
                    synchronized (_workerThread) { _workerThread.wait(1000); }
                } catch (InterruptedException ex) {
                    System.out.println("Caught " + ex.getMessage());
                }
            }

            synchronized (this) { notify(); }
        }
    }

    @Test
    public void create(
    ) {
        TestProcessor p = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX);
        assertEquals(Node.NodeCategory.Processor, p._category);
        assertEquals("IP0", p._name);
        assertEquals(Processor.ProcessorType.InstructionProcessor, p._Type);
    }
}
