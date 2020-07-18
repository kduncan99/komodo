/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_SystemControlInstructions extends BaseFunctions {

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    @Test
    public void sysc_badSubfunction(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(0)",
            "PACKET",
            "          SYSC$FORM 077,0,0,0",
            "",
            "$(1)",
            "          LBU       B2,(LBDIREF$+PACKET, 0)",
            "          SYSC      PACKET,,B2",
            "          HALT      077"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(526, _instructionProcessor.getLatestStopDetail());
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  SYSC memory management
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Subfunction 020: Create dynamic memory block
    //      U+0,S1:     020
    //      U+0,S2:     Status
    //      U+0,S3:     UPI of target MSP
    //      U+1,W:      Newly-assigned segment index if status is zero
    //      U+2,W:      Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)

    @Test
    public void sysc_create_good(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "PACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 32768",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+PACKET, 0)",
            "          SYSC      PACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,PACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bank = getBankByBaseRegister(3);
        int segment = (int) bank[1];
        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
        assertEquals(32768, slice._length);
    }

    @Test
    public void sysc_create_badUPI(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "PACKET",
            "          SYSC$FORM SYSC$CREATE,0,077,0",
            "          + 0",
            "          + 32768",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+PACKET, 0)",
            "          SYSC      PACKET,,B3",
            "          LA,U      A5,SYSC$BADUPI",
            "          TE        A5,PACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void sysc_create_badSize(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "PACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          - 1",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+PACKET, 0)",
            "          SYSC      PACKET,,B3",
            "          LA,U      A5,SYSC$INVSIZE",
            "          TE        A5,PACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  Subfunction 021: Release dynamic memory block
    //      U+0,S1:     021
    //      U+0,S2:     Status
    //      U+0,S3:     UPI of target MSP
    //      U+1,W:      Segment index of block to be released

    @Test (expected = AddressingExceptionInterrupt.class)
    public void sysc_delete_good(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "ALLOCPACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 32768",
            "",
            "FREEPACKET",
            "          SYSC$FORM SYSC$DELETE,0,DEFAULT$MSP,0",
            "          + 0",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+ALLOCPACKET, 0)",
            ".",
            "          SYSC      ALLOCPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,ALLOCPACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+SYSC$MEMSEG,,B3",
            "          SA        A5,FREEPACKET+SYSC$MEMSEG,,B3",
            "          SYSC      FREEPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,FREEPACKET+SYSC$STATUS,,B3",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bank = getBankByBaseRegister(3);
        //  make sure assigned segment is gone
        int segment = (int) bank[1];
        _mainStorageProcessor.getStorage(segment);
    }

    @Test
    public void sysc_delete_badUPI(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "ALLOCPACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 32768",
            "",
            "FREEPACKET",
            "          SYSC$FORM SYSC$DELETE,0,077,0",
            "          + 0",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+ALLOCPACKET, 0)",
            ".",
            "          SYSC      ALLOCPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,ALLOCPACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+SYSC$MEMSEG,,B3",
            "          SA        A5,FREEPACKET+SYSC$MEMSEG,,B3",
            "          SYSC      FREEPACKET,,B3",
            "          LA,U      A5,SYSC$BADUPI",
            "          TE        A5,FREEPACKET+SYSC$STATUS,,B3",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bank = getBankByBaseRegister(3);
        int segment = (int) bank[1];    // assigned segment number - make sure it still exists
        _mainStorageProcessor.getStorage(segment);
    }

    @Test
    public void sysc_delete_badSegment(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "ALLOCPACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "FREEPACKET",
            "          + 0210001000000",
            "          + 0",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+ALLOCPACKET, 0)",
            ".",
            "          SYSC      ALLOCPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,ALLOCPACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+SYSC$MEMSEG,,B3",
            "          LSSL      A5,1",
            "          SA        A5,FREEPACKET+SYSC$MEMSEG,,B3",
            "          SYSC      FREEPACKET,,B3",
            "          LA,U      A5,SYSC$BADSEG",
            "          TE        A5,FREEPACKET+SYSC$STATUS,,B3",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bank = getBankByBaseRegister(3);
        int segment = (int) bank[1];    // assigned segment number - make sure it still exists
        _mainStorageProcessor.getStorage(segment);
    }

    //  Subfunction 022: Resize dynamic memory block
    //      U+0,S1:     022
    //      U+0,S2:     Status
    //      U+0,S3:     UPI of target MSP
    //      U+1,W:      Segment index of block to be resized
    //      U+2,W:      Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)

    @Test
    public void sysc_resize_good(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "ALLOCPACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 32768",
            "",
            "RESIZEPACKET",
            "          SYSC$FORM SYSC$RESIZE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 65536",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+ALLOCPACKET, 0)",
            ".",
            "          SYSC      ALLOCPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,ALLOCPACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+SYSC$MEMSEG,,B3",
            "          SA        A5,RESIZEPACKET+SYSC$MEMSEG,,B3",
            "          SYSC      RESIZEPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,RESIZEPACKET+SYSC$STATUS,,B3",
            "          HALT      076",
            ".",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bank = getBankByBaseRegister(3);
        int segment = (int) bank[1];
        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
        assertEquals(65536, slice._length);
    }

    @Test
    public void sysc_resize_badUPI(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "ALLOCPACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 32768",
            "",
            "RESIZEPACKET",
            "          SYSC$FORM SYSC$RESIZE,0,077,0",
            "          + 0",
            "          + 65536",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+ALLOCPACKET, 0)",
            ".",
            "          SYSC      ALLOCPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,ALLOCPACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+SYSC$MEMSEG,,B3",
            "          SA        A5,RESIZEPACKET+SYSC$MEMSEG,,B3",
            "          SYSC      RESIZEPACKET,,B3",
            "          LA,U      A5,SYSC$BADUPI",
            "          TE        A5,RESIZEPACKET+SYSC$STATUS,,B3",
            "          HALT      076",
            ".",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bank = getBankByBaseRegister(3);
        int segment = (int) bank[1];
        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
        assertEquals(32768, slice._length);
    }

    @Test
    public void sysc_resize_badSegment(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "ALLOCPACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 32768",
            "",
            "RESIZEPACKET",
            "          SYSC$FORM SYSC$RESIZE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 65536",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+ALLOCPACKET, 0)",
            ".",
            "          SYSC      ALLOCPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,ALLOCPACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+SYSC$MEMSEG,,B3",
            "          LSSL      A5,2",
            "          SA        A5,RESIZEPACKET+SYSC$MEMSEG,,B3",
            "          SYSC      RESIZEPACKET,,B3",
            "          LA,U      A5,SYSC$BADSEG",
            "          TE        A5,RESIZEPACKET+SYSC$STATUS,,B3",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bank = getBankByBaseRegister(3);
        int segment = (int) bank[1];
        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
        assertEquals(32768, slice._length);
    }

    @Test
    public void sysc_resize_badSize(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'SYSC$DEFS'",
            "",
            "$(4)",
            "ALLOCPACKET",
            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
            "          + 0",
            "          + 32768",
            "",
            "RESIZEPACKET",
            "          SYSC$FORM SYSC$RESIZE,0,DEFAULT$MSP,0",
            "          + 0",
            "          - 1",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+ALLOCPACKET, 0)",
            ".",
            "          SYSC      ALLOCPACKET,,B3",
            "          LA,U      A5,SYSC$OK",
            "          TE        A5,ALLOCPACKET+SYSC$STATUS,,B3",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+SYSC$MEMSEG,,B3",
            "          SA        A5,RESIZEPACKET+SYSC$MEMSEG,,B3",
            "          SYSC      RESIZEPACKET,,B3",
            "          LA,U      A5,SYSC$INVSIZE",
            "          TE        A5,RESIZEPACKET+SYSC$STATUS,,B3",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bank = getBankByBaseRegister(3);
        int segment = (int) bank[1];
        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
        assertEquals(32768, slice._length);
    }
}
