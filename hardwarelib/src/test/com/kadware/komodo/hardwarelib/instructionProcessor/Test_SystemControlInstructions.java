/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.AssemblerOption;
import com.kadware.komodo.kex.kasm.AssemblerResult;
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_SystemControlInstructions extends BaseFunctions {

    private static Map<String, Dictionary> definitionSets = null;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  primitives
    //  ----------------------------------------------------------------------------------------------------------------------------

    @BeforeClass
    public static void beforeClass() {
        if (definitionSets == null) {
            String[] source = {
                "SYSC$CREATE*  $EQU 020",
                "SYSC$DELETE*  $EQU 021",
                "SYSC$RESIZE*  $EQU 022",
                "",
                "SYSC$CNSTAT*  $EQU 030",
                "SYSC$CNREAD*  $EQU 031",
                "SYSC$CNRDREP* $EQU 032",
                "SYSC$CNPOLL*  $EQU 033",
                "SYSC$CNRESET* $EQU 034",
                "",
                "SYSC$OK*      $EQU 0    . Request successful",
                "SYSC$BADUPI*  $EQU 01   . Given UPI is not a main storage processor",
                "SYSC$BADSEG*  $EQU 02   . Given segment index is unknown to the given MSP",
                "SYSC$INVADDR* $EQU 03   . Given address is invalid or does not exist",
                "SYSC$INVSIZE* $EQU 04   . Requested size is out of range or invalid",
                "SYSC$NACCESS* $EQU 05   . Access denied",
                "",
                "SYSC$FORM*    $FORM 6,6,6,18",
                "SYSC$SUBFUNC* $EQUF 0,,S1",
                "SYSC$STATUS*  $EQUF 0,,S2",
                "SYSC$MSPUPI*  $EQUF 0,,S3",
                "SYSC$MEMSEG*  $EQUF 1,,W",
                "SYSC$MEMSIZE* $EQUF 2,,W",
                //  U+0,S1          Subfunction
                //  U+0,S2:         Status
                //  U+0,S3:         UPI of target MSP
                //  U+1,W:          Newly-assigned segment index if status is zero
                //  U+2,W:          Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)
                "",
                "DEFAULT$MSP*  $EQU 01",
                "",
                "             $END"
            };

            AssemblerOption[] options = {
                AssemblerOption.EMIT_DICTIONARY,
                AssemblerOption.EMIT_SOURCE,
                AssemblerOption.DEFINITION_MODE
            };

            Assembler asm = new Assembler.Builder().setOptions(options)
                                                   .setModuleName("Definitions")
                                                   .setSource(source)
                                                   .build();
            AssemblerResult result = asm.assemble();
            assertFalse(result._diagnostics.hasError());
            assertNotNull(result._definitions);
            definitionSets = new HashMap<>();
            definitionSets.put("DEFINITIONS", result._definitions);
        }
    }

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  SYSC general stuff
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void sysc_badSubfunction(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'DEFINITIONS'",
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

        buildMultiBank(wrapForExtendedMode(source), true, false, definitionSets);
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
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'DEFINITIONS'",
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

        buildMultiBank(wrapForExtendedMode(source), true, false, definitionSets);
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
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'DEFINITIONS'",
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

        buildMultiBank(wrapForExtendedMode(source), true, false, definitionSets);
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void sysc_create_badSize(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'DEFINITIONS'",
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

        buildMultiBank(wrapForExtendedMode(source), true, false, definitionSets);
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
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'DEFINITIONS'",
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

        buildMultiBank(wrapForExtendedMode(source), true, false, definitionSets);
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
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'DEFINITIONS'",
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

        buildMultiBank(wrapForExtendedMode(source), true, false, definitionSets);
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
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'DEFINITIONS'",
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

        buildMultiBank(wrapForExtendedMode(source), true, false, definitionSets);
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

    //TODO
//    @Test
//    public void sysc_resize_good(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source1 = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//        };
//
//        String[] source2 = {
//            "$(0)      .",
//            "ALLOCPACKET",
//            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
//            "          + 0",
//            "          + 32768",
//            "",
//            "RESIZEPACKET",
//            "          SYSC$FORM SYSC$RESIZE,0,DEFAULT$MSP,0",
//            "          + 0",
//            "          + 65536",
//            "",
//            "$(1),START$*",
//            "          SYSC      ALLOCPACKET,,B2",
//            "          LA,U      A5,SYSC$OK",
//            "          TE,S2     A5,ALLOCPACKET,,B2",
//            "          HALT      077",
//            ".",
//            "          LA        A5,ALLOCPACKET+1,,B2",
//            "          SA        A5,RESIZEPACKET+1,,B2",
//            "          SYSC      RESIZEPACKET,,B2",
//            "          LA,U      A5,SYSC$OK",
//            "          TE,S2     A5,RESIZEPACKET,,B2",
//            "          HALT      076",
//            ".",
//            "          HALT      0",
//        };
//
//        String[][] sourceSet = {
//            source1,
//            definitions,
//            source2
//        };
//
//        String[] source = compose(sourceSet);
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        _instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        long[] bank = getBank(_instructionProcessor, 2);
//        int segment = (int) bank[1];
//        assertEquals(1, segment);
//
//        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
//        assertEquals(65536, slice._length);
//    }
//
    //TODO
//    @Test
//    public void sysc_resize_badUPI(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source1 = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//        };
//
//        String[] source2 = {
//            "$(0)      .",
//            "ALLOCPACKET",
//            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
//            "          + 0",
//            "          + 32768",
//            "",
//            "RESIZEPACKET",
//            "          SYSC$FORM SYSC$RESIZE,0,077,0",
//            "          + 0",
//            "          + 65536",
//            "",
//            "$(1),START$*",
//            "          SYSC      ALLOCPACKET,,B2",
//            "          LA,U      A5,SYSC$OK",
//            "          TE,S2     A5,ALLOCPACKET,,B2",
//            "          HALT      077",
//            ".",
//            "          LA        A5,ALLOCPACKET+1,,B2",
//            "          SA        A5,RESIZEPACKET+1,,B2",
//            "          SYSC      RESIZEPACKET,,B2",
//            "          LA,U      A5,SYSC$BADUPI",
//            "          TE,S2     A5,RESIZEPACKET,,B2",
//            "          HALT      076",
//            ".",
//            "          HALT      0",
//        };
//
//        String[][] sourceSet = {
//            source1,
//            definitions,
//            source2
//        };
//
//        String[] source = compose(sourceSet);
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        _instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        long[] bank = getBank(_instructionProcessor, 2);
//        int segment = (int) bank[1];
//        assertEquals(1, segment);
//
//        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
//        assertEquals(32768, slice._length);
//    }
//
    //TODO
//    @Test
//    public void sysc_resize_badSegment(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source1 = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//        };
//
//        String[] source2 = {
//            "$(0)      .",
//            "ALLOCPACKET",
//            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
//            "          + 0",
//            "          + 32768",
//            "",
//            "RESIZEPACKET",
//            "          SYSC$FORM SYSC$RESIZE,0,DEFAULT$MSP,0",
//            "          + 0",
//            "          + 65536",
//            "",
//            "$(1),START$*",
//            "          SYSC      ALLOCPACKET,,B2",
//            "          LA,U      A5,SYSC$OK",
//            "          TE,S2     A5,ALLOCPACKET,,B2",
//            "          HALT      077",
//            ".",
//            "          LA        A5,ALLOCPACKET+1,,B2",
//            "          LSSL      A5,2",
//            "          SA        A5,RESIZEPACKET+1,,B2",
//            "          SYSC      RESIZEPACKET,,B2",
//            "          LA,U      A5,SYSC$BADSEG",
//            "          TE,S2     A5,RESIZEPACKET,,B2",
//            "          HALT      076",
//            ".",
//            "          HALT      0",
//        };
//
//        String[][] sourceSet = {
//            source1,
//            definitions,
//            source2
//        };
//
//        String[] source = compose(sourceSet);
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        _instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        long[] bank = getBank(_instructionProcessor, 2);
//        int segment = (int) bank[1];
//        assertEquals(1, segment);
//
//        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
//        assertEquals(32768, slice._length);
//    }
//
    //TODO
//    @Test
//    public void sysc_resize_badSize(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source1 = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//        };
//
//        String[] source2 = {
//            "$(0)      .",
//            "ALLOCPACKET",
//            "          SYSC$FORM SYSC$CREATE,0,DEFAULT$MSP,0",
//            "          + 0",
//            "          + 32768",
//            "",
//            "RESIZEPACKET",
//            "          SYSC$FORM SYSC$RESIZE,0,DEFAULT$MSP,0",
//            "          + 0",
//            "          - 1",
//            "",
//            "$(1),START$*",
//            "          SYSC      ALLOCPACKET,,B2",
//            "          LA,U      A5,SYSC$OK",
//            "          TE,S2     A5,ALLOCPACKET,,B2",
//            "          HALT      077",
//            ".",
//            "          LA        A5,ALLOCPACKET+1,,B2",
//            "          SA        A5,RESIZEPACKET+1,,B2",
//            "          SYSC      RESIZEPACKET,,B2",
//            "          LA,U      A5,SYSC$INVSIZE",
//            "          TE,S2     A5,RESIZEPACKET,,B2",
//            "          HALT      076",
//            ".",
//            "          HALT      0",
//        };
//
//        String[][] sourceSet = {
//            source1,
//            definitions,
//            source2
//        };
//
//        String[] source = compose(sourceSet);
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        _instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        long[] bank = getBank(_instructionProcessor, 2);
//        int segment = (int) bank[1];
//        assertEquals(1, segment);
//
//        ArraySlice slice = _mainStorageProcessor.getStorage(segment);
//        assertEquals(32768, slice._length);
//    }
}
