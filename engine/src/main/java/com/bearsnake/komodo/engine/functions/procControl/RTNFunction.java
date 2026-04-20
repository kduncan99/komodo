/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.AccessKey;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.AddressingExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.TerminalAddressingExceptionInterrupt;

/**
 * Return function
 * (RTN) uses the RCS to determine how to re-establish a previously saved environment,
 * loading the appropriate bank and then jumping to the address in the U field.
 */
public class RTNFunction extends Function {

    public static final RTNFunction INSTANCE = new RTNFunction();

    private RTNFunction() {
        super("RTN");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_17).setAField(0_03));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        // No operands, extended mode only, F.x,h,i,b,d are all ignored.

        // Find RCS frame
        var frameContent = new long[2];
        engine.releaseRCSFrame(frameContent);

        // Determine source L, BDI
        var sourceLevel = (int)(frameContent[0] >> 33);
        var sourceBDI = (int)(frameContent[0] >> 18) & 0_077777;
        var sourceOffset = (int)(frameContent[0] & 0_777777L);

        // Are we doing mixed-mode transfer?
        var rcsB = (int)(frameContent[1] >> 24) & 0_3;
        var rcsDB12 = (int)(frameContent[1] >> 18) & 0_77;
        var rcsAccessKey = new AccessKey(frameContent[1] & 0_777777);

        // This is a mixed mode transfer if DB16 (basic mode) in the RCS is set.
        var mixedModeTransfer = (rcsDB12 & 02) != 0;
        var rcsBankToBeLoaded = mixedModeTransfer ? (rcsB + 12) : 0;

        // Check validity of L,BDI.
        // If L,BDI is 0,0, we're going to load a void bank.
        if ((sourceLevel == 0) && (sourceBDI >= 1) && (sourceBDI <= 31)) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                   sourceLevel,
                                                   sourceBDI);
        }
        var loadVoid = (sourceLevel == 0) && (sourceBDI == 0);
        if (!mixedModeTransfer && loadVoid) {
            // Cannot load void bank on EM->EM transfer.
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                   sourceLevel,
                                                   sourceBDI);
        }

        // Find the storage and offset for the BD described by L,BDI
        var sourceBDStorage = engine.getBaseRegister(sourceLevel + 16).getStorage();
        var sourceBDOffset = 8 * sourceBDI;
        if ((sourceBDOffset + 7) > sourceBDStorage.getSize()) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                   sourceLevel,
                                                   sourceBDI);
        }


        var sourceBankType = BankDescriptor.getBankType(sourceBDStorage, sourceBDOffset);
        var generalFault = BankDescriptor.isGeneralFault(sourceBDStorage, sourceBDOffset);
        switch (sourceBankType) {
           case ExtendedMode -> {
               // this is okay - just drop through
           }
           case BasicMode, Gate -> {
               // EM->EM addressing exception, EM->BM is okay
               if (!mixedModeTransfer) {
                   throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                        sourceLevel,
                                                        sourceBDI);
               }
            }
            default -> throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                             sourceLevel,
                                                             sourceBDI);
        }

        var targetBReg = engine.getBaseRegister(rcsBankToBeLoaded);
        var par = engine.getProgramAddressRegister();
        var ikr = engine.getActivityStatePacket().getIndicatorKeyRegister();
        var dr = engine.getDesignatorRegister();

        ikr.getAccessKey().set(rcsAccessKey);
        var drBits = dr.getCompositeValue() & 0_777700_777777L;

        if (mixedModeTransfer) {
            targetBReg.setIsVoid(true);
            par.setBankLevel((short)0).setBankDescriptorIndex(0).setOffset(sourceOffset);
            dr.setWord36(drBits | (long)rcsDB12 << 18);
            var abte = engine.getActiveBaseTableEntry(rcsBankToBeLoaded);
            abte.setBankLevel((short)sourceLevel).setBankDescriptorIndex(sourceBDI).setSubsetSpecification(0);
            engine.getDesignatorRegister().setBasicModeBaseRegisterSelection(rcsBankToBeLoaded == 13 || rcsBankToBeLoaded == 15);
            engine.spSetBasicModeCachedBaseRegisterIndex(rcsBankToBeLoaded);
            targetBReg.checkAccessLimits(sourceOffset, true);
        } else {
            par.setBankLevel((short)sourceLevel).setBankDescriptorIndex(sourceBDI).setOffset(sourceOffset);
        }

        if (generalFault) {
            throw new TerminalAddressingExceptionInterrupt(TerminalAddressingExceptionInterrupt.Reason.GBitSetInTargetBD,
                                                           sourceLevel,
                                                           sourceBDI);
        }

        engine.spSetPreventProgramCounterUpdate(true);
        return true;
    }
}
