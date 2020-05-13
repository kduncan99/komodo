/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.kex.kasm.AbsoluteModule;
import com.kadware.komodo.baselib.GeneralRegisterSet;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_ShiftInstructions extends BaseFunctions {

    @Test
    public void singleShiftAlgebraic(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0777777771352",
            "",
            "$(1),START$*",
            "          LA        A5,DATA,,B2",
            "          SSA       A5,6",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_777777_777713L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void doubleShiftAlgebraic(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0333444555666",
            "          + 0777000111222",
            "",
            "$(1),START$*",
            "          LA        A2,DATA,,B2",
            "          LA        A3,DATA+1,,B2",
            "          DSA       A2,12",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_000033_344455L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_566677_700011L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void singleShiftCircular(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A6,012345",
            "          SSC       A6,014",
            "          LA,U      A0,017",
            "          SSC       A6,3,A0",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_000001_234500L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A6).getW());
    }

    @Test
    public void doubleShiftCircular(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0",
            "          + 0123456,0765432",
            "",
            "$(1),START$*",
            "          LA        A2,DATA,,B2",
            "          LA        A3,DATA+1,,B2",
            "          DSC       A2,024",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_575306_400000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_000000_024713L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void singleShiftLogical(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0123,0366157",
            "",
            "$(1),START$*",
            "          LA        A5,DATA,,B2",
            "          SSL       A5,2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_000024_675433L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void doubleShiftLogical(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0777666555444",
            "          + 0333222112345",
            "",
            "$(1),START$*",
            "          LA        A2,DATA,,B2",
            "          LA        A3,DATA+1,,B2",
            "          DSL       A2,9",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_000777_666555L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_444333_222112L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void loadShiftAndCount(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0400000,0",
            "          + 0377777,0777777",
            "          + 0",
            "          + 0777777,0777777",
            "          + 0001111,0111111",
            "",
            "$(1),START$*",
            "          LSC       A0,DATA,,B2",
            "          LSC       A2,DATA+1,,B2",
            "          LSC       A4,DATA+2,,B2",
            "          LSC       A6,DATA+3,,B2",
            "          LSC       A8,DATA+4,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_400000_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_377777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_000000_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(35, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(0_777777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(35, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(0_222222_222200L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(7, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A9).getW());
    }

    @Test
    public void doubleLoadShiftAndCount(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0",
            "          + 05432107",
            "          + 0377777,0777777",
            "          + 0777777,0777777",
            "          + 0",
            "          + 0",
            "          + 0777777,0777777",
            "          + 0777777,0777777",
            "          + 0",
            "          + 0333444,0555666",
            "",
            "$(1),START$*",
            "          DLSC      A0,DATA,,B2",
            "          DLSC      A3,DATA+2,,B2",
            "          DLSC      A6,DATA+4,,B2",
            "          DLSC      A9,DATA+6,,B2",
            "          DLSC      A12,DATA+8,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0,processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_261504_340000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(062, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_377777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_777777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(0_000000_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(0_000000_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(71, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(0_777777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A9).getW());
        assertEquals(0_777777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A10).getW());
        assertEquals(71, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A11).getW());
        assertEquals(0_333444_555666L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A12).getW());
        assertEquals(0_000000_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A13).getW());
        assertEquals(36, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A14).getW());
    }

    @Test
    public void leftSingleShiftCircular(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0776655443322",
            "",
            "$(1),START$*",
            "          LA        A5,DATA,,B2",
            "          LSSC      A5,15",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_544332_277665L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void leftDoubleShiftCircular(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0777777600000",
            "          + 0666666666666",
            "",
            "$(1),START$*",
            "          DL        A2,DATA,,B2",
            "          LDSC      A2,20",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_000003_333333L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_333333_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void leftSingleShiftLogical(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0000123366157",
            "",
            "$(1),START$*",
            "          LA        A5,DATA,,B2",
            "          LSSL      A5,3",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_001233_661570L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void leftDoubleShiftLogical(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA",
            "          + 0777666,0555444",
            "          + 0333222,0112345",
            "",
            "$(1),START$*",
            "          DL        A2,DATA,,B2",
            "          LDSL      A2,9",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0_666555_444333L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_222112_345000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }
}
