/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.FileCycleDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.FileSetDoesNotExistException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.logger.LogManager;

import java.util.LinkedList;
import java.util.Map;

import static com.bearsnake.komodo.baselib.Word36.A_OPTION;
import static com.bearsnake.komodo.baselib.Word36.B_OPTION;
import static com.bearsnake.komodo.baselib.Word36.C_OPTION;
import static com.bearsnake.komodo.baselib.Word36.E_OPTION;
import static com.bearsnake.komodo.baselib.Word36.G_OPTION;
import static com.bearsnake.komodo.baselib.Word36.H_OPTION;
import static com.bearsnake.komodo.baselib.Word36.J_OPTION;
import static com.bearsnake.komodo.baselib.Word36.L_OPTION;
import static com.bearsnake.komodo.baselib.Word36.M_OPTION;
import static com.bearsnake.komodo.baselib.Word36.O_OPTION;
import static com.bearsnake.komodo.baselib.Word36.P_OPTION;
import static com.bearsnake.komodo.baselib.Word36.R_OPTION;
import static com.bearsnake.komodo.baselib.Word36.S_OPTION;
import static com.bearsnake.komodo.baselib.Word36.T_OPTION;
import static com.bearsnake.komodo.baselib.Word36.U_OPTION;
import static com.bearsnake.komodo.baselib.Word36.V_OPTION;
import static com.bearsnake.komodo.baselib.Word36.W_OPTION;
import static com.bearsnake.komodo.baselib.Word36.Z_OPTION;

class CatHandler extends Handler {

    private static final long COMMON_OPTIONS = G_OPTION | P_OPTION | R_OPTION | S_OPTION | V_OPTION | W_OPTION | Z_OPTION;
    private static final long MASS_STORAGE_OPTIONS = COMMON_OPTIONS | B_OPTION;
    private static final long TAPE_OPTIONS = COMMON_OPTIONS | E_OPTION | H_OPTION | J_OPTION | L_OPTION | M_OPTION | O_OPTION;

    @Override
    public boolean allowCSF() {
        return true;
    }

    @Override
    public boolean allowCSI() {
        return true;
    }

    @Override
    public boolean allowTIP() {
        return true;
    }

    @Override
    public String getCommand() {
        return "CAT";
    }

    // MassStorage:
    // @CAT[,options] filename
    //   [,type/reserve/granule/maximum,
    //   pack-id-1/.../pack-id-n,,,ACR-name]
    // options: B,G,P,R,S,V,W,Z
    //
    // Tape:
    // @CAT,options filename,
    //   type[/units/log/noise/processor/tape/format/data-converter/block-numbering/data-compression/buffered-write/expanded-buffer,
    //   reel-1/reel-2/.../reel-n,
    //   expiration-period/mmspec,,ACR-name,CTL-pool]
    // --> E:256333 Units and logical subfields not allowed on CAT image.
    // options: E,G,H,J,L,M,O,P,R,S,V,W,Z
    //
    // If you omit type, the Exec uses sector-addressable mass storage. Appendix D, Table D-1 shows the standard type entries supplied by Unisys.
    // If you are creating a new cycle for an existing file cycle set, and you omit the type, the type is taken from
    //   the latest existing cycle of the file. If all cycles of the file are in a to-be-cataloged or to-be-dropped state,
    //   then the type from the highest cycle of this type is used.
    @Override
    public void handle(
        final HandlerPacket hp
    ) throws ExecStoppedException {
        var e = Exec.getInstance();
        var rce = hp._runControlEntry;
        var fsResult = hp._statement._facStatusResult;

        // get the filename field
        var fnField = getSubField(hp, 0, 0);
        if (fnField == null) {
            fsResult.postMessage(FacStatusCode.FilenameIsRequired);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        FileSpecification fileSpec;
        try {
            fileSpec = FileSpecification.parse(new Parser(fnField), ".,/ ");
        } catch (FileSpecification.InvalidFileCycleException ex) {
            fsResult.postMessage(FacStatusCode.IllegalValueForFCycle);
            fsResult.mergeStatusBits(0_600000_00000L);
            return;
        } catch (FileSpecification.Exception ex) {
            fsResult.postMessage(FacStatusCode.SyntaxErrorInImage);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        if (fileSpec == null) {
            fsResult.postMessage(FacStatusCode.FilenameIsRequired);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        fileSpec = rce.getUseItemTable().resolveFileSpecification(fileSpec);
        fileSpec = rce.resolveQualifier(fileSpec);

        // Brief sanity check
        if (!checkMutuallyExclusiveOptions(hp, A_OPTION | C_OPTION | T_OPTION | U_OPTION)) {
            return;
        }

        var isGuarded = (hp._optionWord & G_OPTION) != 0;
        var isPrivate = (hp._optionWord & P_OPTION) == 0;
        var isReadOnly = (hp._optionWord & R_OPTION) != 0;
        var isWriteOnly = (hp._optionWord & W_OPTION) != 0;

        // If the type is specified, determine whether it is disk or tape and invoke the proper
        // version of cataloging via the fac manager.
        var typeField = getSubField(hp, 1, 0);
        if (typeField != null) {
            if (typeField.length() > 6) {
                fsResult.postMessage(FacStatusCode.AssignMnemonicTooLong);
                fsResult.mergeStatusBits(0_600000_000000L);
                return;
            }

            var mnemonicType = e.getConfiguration().getMnemonicType(typeField);
            if (mnemonicType == null) {
                fsResult.postMessage(FacStatusCode.MnemonicIsNotConfigured, new String[]{typeField});
                fsResult.mergeStatusBits(0_600000_000000L);
                return;
            }

            switch (mnemonicType) {
                case SECTOR_ADDRESSABLE_DISK, WORD_ADDRESSABLE_DISK -> {
                    if (!checkIllegalOptions(hp, MASS_STORAGE_OPTIONS)) {
                        return;
                    }
                    handleDisk(hp._optionWord,
                               hp._statement._operandFields,
                               hp._runControlEntry,
                               fileSpec,
                               typeField,
                               isGuarded,
                               isPrivate,
                               isReadOnly,
                               isWriteOnly,
                               fsResult);
                }
                case TAPE -> {
                    if (!checkIllegalOptions(hp, TAPE_OPTIONS)) {
                        return;
                    }
                    handleTape(hp._optionWord,
                               hp._statement._operandFields,
                               hp._runControlEntry,
                               fileSpec,
                               typeField,
                               isGuarded,
                               isPrivate,
                               isReadOnly,
                               isWriteOnly,
                               fsResult);
                }
            }
        } else {
            // Type is not specified.
            handleUnknown(hp._optionWord,
                          hp._statement._operandFields,
                          hp._runControlEntry,
                          fileSpec,
                          isGuarded,
                          isPrivate,
                          isReadOnly,
                          isWriteOnly,
                          fsResult);
        }
    }

    private void
    handleDisk(
        final long optionWord,
        final Map<SubfieldSpecifier, String> operands,
        final RunControlEntry rce,
        final FileSpecification fileSpecification,
        final String type,
        final boolean isGuarded,
        final boolean isPrivate,
        final boolean isReadOnly,
        final boolean isWriteOnly,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        var e = Exec.getInstance();
        var fm = e.getFacilitiesManager();

        var isUnloadInhibited = (optionWord & V_OPTION) != 0;
        var saveOnCheckpoint = (optionWord & B_OPTION) != 0;

        long initialGranules = 0;
        var initStr = operands.get(new SubfieldSpecifier(1, 1));
        if (initStr != null) {
            try {
                initialGranules = Long.parseLong(initStr);
                if ((initialGranules < 0) || (initialGranules > 0777777)) {
                    fsResult.postMessage(FacStatusCode.IllegalInitialReserve);
                    fsResult.mergeStatusBits(0_600000_000000L);
                    return;
                }
            } catch (NumberFormatException ex) {
                fsResult.postMessage(FacStatusCode.SyntaxErrorInImage);
                fsResult.mergeStatusBits(0_600000_000000L);
                return;
            }
        }

        Granularity granularity = null;
        var granStr = operands.get(new SubfieldSpecifier(1, 2));
        if (granStr != null) {
            if (granStr.equalsIgnoreCase("TRK")) {
                granularity = Granularity.Track;
            } else if (granStr.equalsIgnoreCase("POS")) {
                granularity = Granularity.Position;
            } else {
                fsResult.postMessage(FacStatusCode.IllegalValueForGranularity);
                fsResult.mergeStatusBits(0_600000_000000L);
                return;
            }
        }

        long maxGranules = 0;
        var maxStr = operands.get(new SubfieldSpecifier(1, 3));
        if (maxStr != null) {
            try {
                maxGranules = Long.parseLong(maxStr);
                if ((maxGranules < 0) || (maxGranules > 0777777)) {
                    fsResult.postMessage(FacStatusCode.IllegalMaxGranules);
                    fsResult.mergeStatusBits(0_600000_000000L);
                    return;
                }
            } catch (NumberFormatException ex) {
                fsResult.postMessage(FacStatusCode.SyntaxErrorInImage);
                fsResult.mergeStatusBits(0_600000_000000L);
                return;
            }
        }

        var placeStr = operands.get(new SubfieldSpecifier(1, 4));
        if (placeStr != null) {
            fsResult.postMessage(FacStatusCode.PlacementFieldNotAllowed);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        LinkedList<String> packIds = new LinkedList<>();
        for (var entry : operands.entrySet()) {
            var subSpec = entry.getKey();
            if (subSpec.getFieldIndex() == 2) {
                packIds.add(entry.getValue());
            }
        }

        fm.catalogDiskFile(fileSpecification,
                           type,
                           rce.getProjectId(),
                           rce.getAccountId(),
                           isGuarded,
                           isPrivate,
                           isUnloadInhibited,
                           isReadOnly,
                           isWriteOnly,
                           saveOnCheckpoint,
                           granularity,
                           initialGranules,
                           maxGranules,
                           packIds,
                           fsResult);

    }

    private void
    handleTape(
        final long optionWord,
        final Map<SubfieldSpecifier, String> operands,
        final RunControlEntry rce,
        final FileSpecification fileSpecification,
        final String type,
        final boolean isGuarded,
        final boolean isPrivate,
        final boolean isReadOnly,
        final boolean isWriteOnly,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        // TODO
    }

    private void
    handleUnknown(
        final long optionWord,
        final Map<SubfieldSpecifier, String> operands,
        final RunControlEntry rce,
        final FileSpecification fileSpecification,
        final boolean isGuarded,
        final boolean isPrivate,
        final boolean isReadOnly,
        final boolean isWriteOnly,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        // Type is not specified - if there is an existing fileset, we can figure out what type to use.
        // If not, then we assume sector-formatted mass storage.
        // This verification must be done with facilities locked.
        var e = Exec.getInstance();
        var fm = e.getFacilitiesManager();
        var mm = e.getMFDManager();
        synchronized (fm) {
            try {
                // If a fileset exists, we use the "latest" existing (i.e., not to-be) cycle for the equipment type.
                // If all cycles are to-be, use the highest cycle. For "latest" we use the highest cycle, not the newest cycle.
                var fsInfo = mm.getFileSetInfo(fileSpecification.getQualifier(), fileSpecification.getFilename());
                var fsci = fsInfo.getCycleInfo().getFirst();
                if (fsci.isToBeCataloged() || fsci.isToBeDropped()) {
                    for (var f : fsInfo.getCycleInfo()) {
                        if (!fsci.isToBeDropped() && !fsci.isToBeCataloged()) {
                            fsci = f;
                            break;
                        }
                    }
                }

                var fcInfo = mm.getFileCycleInfo(fileSpecification.getQualifier(),
                                                 fileSpecification.getFilename(),
                                                 fsci.getAbsoluteCycle());

                switch (fsInfo.getFileType()) {
                    case Fixed, Removable -> handleDisk(optionWord,
                                                        operands,
                                                        rce,
                                                        fileSpecification,
                                                        fcInfo.getAssignMnemonic(),
                                                        isGuarded,
                                                        isPrivate,
                                                        isReadOnly,
                                                        isWriteOnly,
                                                        fsResult);
                    case Tape -> handleTape(optionWord,
                                            operands,
                                            rce,
                                            fileSpecification,
                                            fcInfo.getAssignMnemonic(),
                                            isGuarded,
                                            isPrivate,
                                            isReadOnly,
                                            isWriteOnly,
                                            fsResult);
                }
            } catch (FileCycleDoesNotExistException ex) {
                // this happens if getFileCycleInfo() fails... which should never happen here
                LogManager.logFatal(getCommand(), "getFileCycleInfo() should not have failed");
                e.stop(StopCode.FacilitiesComplex);
                throw new ExecStoppedException();
            } catch (FileSetDoesNotExistException ex) {
                var type = e.getConfiguration().getMassStorageDefaultMnemonic();
                handleDisk(optionWord,
                           operands,
                           rce,
                           fileSpecification,
                           type,
                           isGuarded,
                           isPrivate,
                           isReadOnly,
                           isWriteOnly,
                           fsResult);
            }
        }
    }
}
