/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.Register;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.AddressingExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Bank and Jump function
 * (LBJ) Loads a bank (selected from B12:B15 for Basic Mode, B0 for Extended Mode)
 * and then jumps to the address in the U field.
 */

public abstract class LXJFunction extends Function {

    protected LXJFunction(
        final String mnemonic
    ) {
        super(mnemonic);

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }

    protected boolean executeCommon(
        final Engine engine,
        final int operand,
        final Register xaRegister,
        final short baseRegisterNumber
    ) throws MachineInterrupt {
        var xaValue = xaRegister.getW();
        var interfaceSpec = (short)((xaValue >> 30) & 03);
        short bankLevel = 0;
        int bankDescriptorIndex = 0;

        switch (interfaceSpec) {
            case 0, 1 -> {
                var execFlag = (int)(xaValue >> 35);
                var levelSpec = (int)(xaValue >> 32) & 01;
                bankLevel = (short) switch (((execFlag << 1) | levelSpec)) {
                    case 0b00 -> 4;
                    case 0b01 -> 6;
                    case 0b10 -> 2;
                    case 0b11 -> 0;
                    default -> throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidLinkageRegister);
                };
                bankDescriptorIndex = (int)(xaValue >> 18) & 0_077777;
            }
            case 2 -> {
            } case 3 -> throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidISValue, 0, 0);
        }

        var oldAddress = engine.getProgramAddressRegister().getCompositeValue();
        engine.bankManipulation(this, interfaceSpec, baseRegisterNumber, bankLevel, bankDescriptorIndex, xaRegister, operand, null);
        engine.createJumpHistoryEntry(oldAddress);
        return true;
    }
}
