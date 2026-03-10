/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestProgramAddressRegister {

    @Test
    public void testGetProgramCounter() {
        long value = VirtualAddress.getCompositeValue(0, 0, 012345);
        ProgramAddressRegister par = new ProgramAddressRegister(value);
        assertEquals(012345, par.getProgramCounter());
    }

    @Test
    public void testDefaultConstructor() {
        ProgramAddressRegister par = new ProgramAddressRegister();
        assertEquals(0, par.getProgramCounter());
    }
}
