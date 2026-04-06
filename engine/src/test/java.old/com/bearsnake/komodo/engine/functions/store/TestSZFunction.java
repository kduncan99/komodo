/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSZFunction extends TestConstantFunction {

    public TestSZFunction() {
        super(0, 0L);
    }

    @BeforeEach
    public void setup() {
        super.setup();
    }

    @Test
    public void testSZ_Simple_BM() throws MachineInterrupt {
        test_Simple_BM();
    }

    @Test
    public void testSZ_Simple_EM() throws MachineInterrupt {
        test_Simple_EM();
    }

    @Test
    public void testSZ_H_BM() throws MachineInterrupt {
        test_H_BM();
    }

    @Test
    public void testSZ_T_EM() throws MachineInterrupt {
        test_T_EM();
    }

    @Test
    public void testSZ_S_Indirect_Indexed_BM() throws MachineInterrupt {
        test_S_Indirect_Indexed_BM();
    }

    @Test
    public void testSZ_U_EM() throws MachineInterrupt {
        test_U_EM();
    }

    @Test
    public void testSZ_XU_BM() throws MachineInterrupt {
        test_XU_BM();
    }

    @Test
    public void testSZ_GRS_EM() throws MachineInterrupt {
        test_GRS_EM();
    }
}
