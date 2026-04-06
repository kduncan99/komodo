/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSASFunction extends TestConstantFunction {

    public TestSASFunction() {
        super(06, 0_040040_040040L);
    }

    @BeforeEach
    public void setup() {
        super.setup();
    }

    @Test
    public void testSAS_Simple_BM() throws MachineInterrupt {
        test_Simple_BM();
    }

    @Test
    public void testSAS_Simple_EM() throws MachineInterrupt {
        test_Simple_EM();
    }

    @Test
    public void testSAS_H_BM() throws MachineInterrupt {
        test_H_BM();
    }

    @Test
    public void testSAS_T_EM() throws MachineInterrupt {
        test_T_EM();
    }

    @Test
    public void testSAS_S_Indirect_Indexed_BM() throws MachineInterrupt {
        test_S_Indirect_Indexed_BM();
    }

    @Test
    public void testSAS_U_EM() throws MachineInterrupt {
        test_U_EM();
    }

    @Test
    public void testSAS_XU_BM() throws MachineInterrupt {
        test_XU_BM();
    }

    @Test
    public void testSAS_GRS_EM() throws MachineInterrupt {
        test_GRS_EM();
    }
}
