/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.system;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.HardwareCheckInterrupt;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Instruction Processor Control instruction
 * (IPC) invokes various processor control functions.
 */
public class IPCFunction extends Function {

    public static final IPCFunction INSTANCE = new IPCFunction();

    private IPCFunction() {
        super("IPC");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_17).setAField(0_10).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, false, false, false, false);
        switch ((int) (operand >> 30)) {
            case 0 -> engine.clearReset();
            case 1 -> engine.enableJumpHistoryInterrupt(true);
            case 2 -> engine.enableJumpHistoryInterrupt(false);
            case 4 -> engine.setBroadcastInterruptEligible(false);
            case 5 -> engine.setBroadcastInterruptEligible(true);
            case 070 -> {
                // allocate a bank (does NOT update bank descriptors)
                // +00:S1   070
                // +00:S2   where we return status, 0=success, 040=failure
                // +01:0-4  reserved
                // +01:5-35 requested size of bank
                // +02,03   where we return absolute address of bank if successful
                var operands = engine.getConsecutiveOperands(true, 4);
                try {
                    int segId = engine.getStorageManager().allocateSegment((int) operands[1] & 0x7FFFFFFF);
                    AbsoluteAddress.encodeToStorage(segId, 0, operands, 2);
                    operands[0] = Word36.setS2(operands[0], 0);
                } catch (HardwareCheckInterrupt e) {
                    operands[0] = Word36.setS2(operands[0], 040);
                }
                engine.storeConsecutiveOperandsToCachedAddress(operands);
            }
            case 071 -> {
                // resize a bank (does NOT update bank descriptors)
                // +00:S1   071
                // +00:S2   where we return status, 0=success, 040=failure
                // +01:0-4  reserved
                // +01:5-35 new requested size of bank
                // +02,03   absolute address of bank
                var operands = engine.getConsecutiveOperands(true, 4);
                try {
                    engine.getStorageManager().resizeSegment(AbsoluteAddress.extractSegmentFromStorage(operands, 2),
                                                             (int) operands[1] & 0x7FFFFFFF);
                    operands[0] = Word36.setS2(operands[0], 0);
                } catch (HardwareCheckInterrupt e) {
                    operands[0] = Word36.setS2(operands[0], 040);
                }
                engine.storeConsecutiveOperandsToCachedAddress(operands);
            }
            case 072 -> {
                // release a bank (does NOT update bank descriptors)
                // +00:S1   071
                // +00:S2   where we return status, 0=success, 040=failure
                // +01:W    reserved
                // +02,03   absolute address of bank
                var operands = engine.getConsecutiveOperands(true, 4);
                try {
                    engine.getStorageManager().releaseSegment(AbsoluteAddress.extractSegmentFromStorage(operands, 2));
                    operands[0] = Word36.setS2(operands[0], 0);
                } catch (HardwareCheckInterrupt e) {
                    operands[0] = Word36.setS2(operands[0], 040);
                }
                engine.storeConsecutiveOperandsToCachedAddress(operands);
            }
            default -> throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
        }

        return true;
    }
}
