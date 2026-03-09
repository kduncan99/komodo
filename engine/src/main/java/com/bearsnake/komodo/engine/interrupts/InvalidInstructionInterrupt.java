/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

/**
 * Represents a particular machine interrupt class
 * Occurs when we try to execute an instruction code which is not defined, or cannot be executed for some reason.
 */
public class InvalidInstructionInterrupt extends MachineInterrupt {

    public enum Reason {
        UndefinedFunctionCode(0),
        InvalidLinkageRegister(0),
        InvalidBaseRegister(0),
        InvalidProcessorPrivilege(1),
        InvalidTargetInstruction(3);

        private final short _code;

        Reason(
            final int code
        ) {
            _code = (short)code;
        }

        public short getCode() {
            return _code;
        }
    }

    public final Reason _reason;

    public InvalidInstructionInterrupt(
        final Reason reason
    ) {
        super(InterruptClass.InvalidInstruction,
              ConditionCategory.Fault,
              Synchrony.Synchronous,
              Deferrability.Exigent,
              InterruptPoint.IndirectExecute);
        _reason = reason;
    }

    @Override
    public byte getShortStatusField() {
        return (byte)_reason.getCode();
    }
}
