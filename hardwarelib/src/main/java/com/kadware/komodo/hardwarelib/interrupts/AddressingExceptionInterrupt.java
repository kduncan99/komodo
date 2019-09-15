/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.interrupts;

import com.kadware.komodo.baselib.Word36;

/**
 * Represents a particular machine interrupt class
 */
public class AddressingExceptionInterrupt extends MachineInterrupt {

    public enum Reason {
        FatalAddressingException(0),
        GBitSetGate(1),
        EnterAccessDenied(2),
        InvalidSourceLevelBDI(3),
        GateBankBoundaryViolation(4),
        InvalidISValue(5),
        GoToInhibitSet(6),
        GeneralQueuingViolation(7),
        MaxCountExceeded(010),          //  ENQ/ENQF
        GBitSetIndirect(011),           //  BD.G = 1 in Indirecct Bank_Descriptor
        InactiveQueueBDListEmpty(013),  //  on DEQ/DEQW
        UpdateInProgress(014),          //  in queue structure
        QueueBankRepositoryFull(015),
        BDTypeInvalid(016),
        QBRIndexInvalid(024);

        private final short _code;

        Reason(int code) { _code = (short)code; }
        public short getCode() { return _code; }
    }

    private final short _bankDescriptorIndex;   // 15 bits significant
    private final byte _bankLevel;              // 3 bits significant
    private final Reason _reason;

    public AddressingExceptionInterrupt(
        final Reason reason,
        final int bankLevel,
        final int bankDescriptorIndex
    ) {
        super(InterruptClass.AddressingException, ConditionCategory.Fault, Synchrony.Pended, Deferrability.Exigent, InterruptPoint.MidExecution);
        _bankLevel = (byte)(bankLevel & 07);
        _bankDescriptorIndex = (short)(bankDescriptorIndex & 077777);
        _reason = reason;
    }

    public short getBankDescriptorIndex() { return _bankDescriptorIndex; }
    public byte getBankLevel() { return _bankLevel; }
    public Reason getReason() { return _reason; }

    @Override
    public Word36 getInterruptStatusWord1(
    ) {
        long levelBDI = (_bankLevel << 15) | _bankDescriptorIndex;
        Word36 result = new Word36();
        result.setH1(levelBDI);
        return result;
    }

    @Override public byte getShortStatusField() { return (byte)_reason.getCode(); }
}
