/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSP1Function extends TestConstantFunction {

    public TestSP1Function() {
        super(2, 01L);
    }

    @BeforeEach
    public void setup() {
        super.setup();
    }

    @Test
    public void testSP1_Simple_BM() throws MachineInterrupt {
        test_Simple_BM();
    }

    @Test
    public void testSP1_Simple_EM() throws MachineInterrupt {
        test_Simple_EM();
    }

    @Test
    public void testSP1_H_BM() throws MachineInterrupt {
        test_H_BM();
    }

    @Test
    public void testSP1_T_EM() throws MachineInterrupt {
        test_T_EM();
    }

    @Test
    public void testSP1_S_Indirect_Indexed_BM() throws MachineInterrupt {
        test_S_Indirect_Indexed_BM();
    }

    @Test
    public void testSP1_U_EM() throws MachineInterrupt {
        test_U_EM();
    }

    @Test
    public void testSP1_XU_BM() throws MachineInterrupt {
        test_XU_BM();
    }

    @Test
    public void testSP1_GRS_EM() throws MachineInterrupt {
        test_GRS_EM();
    }
}
