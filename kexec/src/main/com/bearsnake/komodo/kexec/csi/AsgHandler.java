/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DeviceType;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.TapeDevice;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.FileCycleDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.FileSetDoesNotExistException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.kexec.facilities.FacilitiesManager;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.kexec.mfd.FileSetInfo;
import com.bearsnake.komodo.logger.LogManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static com.bearsnake.komodo.baselib.Word36.A_OPTION;
import static com.bearsnake.komodo.baselib.Word36.C_OPTION;
import static com.bearsnake.komodo.baselib.Word36.D_OPTION;
import static com.bearsnake.komodo.baselib.Word36.E_OPTION;
import static com.bearsnake.komodo.baselib.Word36.I_OPTION;
import static com.bearsnake.komodo.baselib.Word36.K_OPTION;
import static com.bearsnake.komodo.baselib.Word36.M_OPTION;
import static com.bearsnake.komodo.baselib.Word36.Q_OPTION;
import static com.bearsnake.komodo.baselib.Word36.R_OPTION;
import static com.bearsnake.komodo.baselib.Word36.T_OPTION;
import static com.bearsnake.komodo.baselib.Word36.U_OPTION;
import static com.bearsnake.komodo.baselib.Word36.X_OPTION;
import static com.bearsnake.komodo.baselib.Word36.Y_OPTION;
import static com.bearsnake.komodo.baselib.Word36.Z_OPTION;

// TODO NOTE:
//  To create a file on a fixed pack, use the @ASG or @CAT statement without including any pack-id field.
//  To create a file on one or more removable packs, include one or more device identifiers on the pack-id field.
//  This applies to ASG and CAT.

class AsgHandler extends Handler {

    // filename,
    // type/reserve/granule/maximum/placement,
    // pack-id-1/.../pack-id-n,
    // ,,ACR-name
    private static final Set<SubfieldSpecifier> CATALOGED_DISK_SUBFIELD_SPECS = new HashSet<>();
    private static final Set<Integer> CATALOGED_DISK_UNBOUND_SUBFIELDS = new HashSet<>();
    static {
        CATALOGED_DISK_SUBFIELD_SPECS.add(new SubfieldSpecifier(0, 0)); // filename
        CATALOGED_DISK_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 0)); // type
        CATALOGED_DISK_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 1)); // reserve
        CATALOGED_DISK_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 2)); // granularity
        CATALOGED_DISK_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 3)); // maximum
        CATALOGED_DISK_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 4)); // placement
        CATALOGED_DISK_SUBFIELD_SPECS.add(new SubfieldSpecifier(5, 0)); // ACR-name

        CATALOGED_DISK_UNBOUND_SUBFIELDS.add(2); // pack-ids
    }

    // filename,*name,pack-id
    private static final Set<SubfieldSpecifier> ABSOLUTE_MASS_STORAGE_SUBFIELD_SPECS = new HashSet<>();
    static {
        ABSOLUTE_MASS_STORAGE_SUBFIELD_SPECS.add(new SubfieldSpecifier(0, 0)); // filename
        ABSOLUTE_MASS_STORAGE_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 0)); // name
        ABSOLUTE_MASS_STORAGE_SUBFIELD_SPECS.add(new SubfieldSpecifier(2, 0)); // pack-id
    }

    // filename,
    // *type/*units/noise/processor/format,
    // data-converter/block-numbering/data-compression/buffered-write,
    // reel-1/.../reel-n,
    // expiration-period,
    // ring-indicator,
    // ACR-name,
    // CTL-pool
    private static final Set<SubfieldSpecifier> ABSOLUTE_TAPE_SUBFIELD_SPECS = new HashSet<>();
    private static final Set<Integer> ABSOLUTE_TAPE_UNBOUND_SUBFIELDS = new HashSet<>();
    static {
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(0, 0)); // filename
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 0)); // type
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 1)); // units
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 2)); // noise
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 3)); // processor
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 4)); // format
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(3, 0)); // expiration
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(4, 0)); // ring-indicator
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(5, 0)); // ACR-name
        ABSOLUTE_TAPE_SUBFIELD_SPECS.add(new SubfieldSpecifier(6, 0)); // CTL-pool

        ABSOLUTE_TAPE_UNBOUND_SUBFIELDS.add(2); // reel-ids
    }

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return true; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public String getCommand() { return "ASG"; }

    @Override
    public void handle(final HandlerPacket hp) throws ExecStoppedException {
        if (cleanOptions(hp)) {
            postSyntaxError(hp);
            return;
        }

        // get the filename field
        var fnField = getSubField(hp, 0, 0);
        if (fnField == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.FilenameIsRequired);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        FileSpecification fileSpec;
        try {
            fileSpec = FileSpecification.parse(new Parser(fnField), ".,/ ");
        } catch (FileSpecification.InvalidFileCycleException ex) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalValueForFCycle);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_00000L);
            return;
        } catch (FileSpecification.Exception ex) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        if (fileSpec == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.FilenameIsRequired);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        fileSpec = hp._runControlEntry.getUseItemTable().resolveFileSpecification(fileSpec);
        fileSpec = hp._runControlEntry.resolveQualifier(fileSpec);

        // Brief sanity check
        if (!checkMutuallyExclusiveOptions(hp, A_OPTION | C_OPTION | T_OPTION | U_OPTION)) {
            return;
        }

        /* TODO
            The options field, if left blank, defaults to the A option unless the file is already assigned,
            in which case the options on the previous assign are used. If the file specified is not cataloged,
            the blank options field defaults to a T option, thus creating a temporary file.
        */

        // If A option is set, we are going to try to assign an existing cataloged file.
        // In this case, go look for the file first.
        if ((hp._optionWord & A_OPTION) != 0) {
            handleCatalogedFile(hp, fileSpec);
        }

        // get the type field.
        var typeField = getSubField(hp, 1, 0);
        if (typeField != null) {
            if (typeField.length() > 6) {
                hp._statement._facStatusResult.postMessage(FacStatusCode.AssignMnemonicTooLong);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
                return;
            }

            if (typeField.startsWith("*")) {
                handleAbsolute(hp, fileSpec, typeField.substring(1));
                return;
            }
            // TODO
        }

        // TODO
    }

    private void handleAbsolute(final HandlerPacket hp,
                                final FileSpecification fs,
                                final String unitName) throws ExecStoppedException {
        // Find the device or channel corresponding to unitName
        var nodeInfo = Exec.getInstance().getFacilitiesManager().getNodeInfo(unitName);
        if (nodeInfo == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.UnitNameIsNotConfigured, new String[]{ unitName });
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        if (nodeInfo.getNodeStatus() == NodeStatus.Down) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.UnitIsNotUpOrReserved, new String[]{ unitName });
            hp._statement._facStatusResult.mergeStatusBits(0_400000_001000L);
            return;
        }

        switch (nodeInfo.getNode().getNodeCategory()) {
            case Channel -> handleAbsoluteChannel(hp, fs, nodeInfo);
            case Device -> handleAbsoluteDevice(hp, fs, nodeInfo);
        }
    }

    private void handleAbsoluteChannel(final HandlerPacket hp,
                                       final FileSpecification fs,
                                       final NodeInfo nodeInfo) {
        // TODO
    }

    private void handleAbsoluteDevice(final HandlerPacket hp,
                                      final FileSpecification fs,
                                      final NodeInfo nodeInfo) throws ExecStoppedException {
        var device = (Device)nodeInfo.getNode();
        if (device.getDeviceType() == DeviceType.DiskDevice) {
            handleAbsoluteDiskDevice(hp, fs, (DiskDevice) device);
        } else if (device.getDeviceType() == DeviceType.TapeDevice) {
            handleAbsoluteTapeDevice(hp, fs, nodeInfo, (TapeDevice) device);
        } else {
            // neither disk nor tape...
            // assign device to the run (do we wait for it?) and create a fac item
            // TODO

            postComplete(hp);
        }
    }

    /**
     * Absolute disk device assignment.
     *  options may include:
     *      H - indicates cache control commands will be sent for cache control units
     *      I - free's the file at the next task termination
     *      T - required (or we wouldn't be here)
     *      Z - assignment cannot cause the run to be held
     *  field[0][0] is the external file name
     *  field[1][0] contains an asterisk followed by either a device name or a channel name
     *                  for devices, the device must be RV
     *                  for channels, at least one device accessible via the channel must be RV
     *  field[2][0] contains the pack-id we want to access
     * @param hp handler packet
     * @param device disk device
     */
    private void handleAbsoluteDiskDevice(
        final HandlerPacket hp,
        final FileSpecification fs,
        final DiskDevice device
    ) throws ExecStoppedException {
        long allowed = Word36.H_OPTION | I_OPTION | Word36.T_OPTION | Z_OPTION;
        if (!checkIllegalOptions(hp, allowed)) {
            return;
        }

        // A thought... if the T option is *not* specified, algorithms are supposed to go see if a file exists in
        // the MFD given the file specification, and if so, use A semantics. We cannot use A semantics for a
        // disk unit, so we are assuming a T option exists if a disk unit is specified. Is this correct?

        if (!checkIllegalFieldsAndSubfields(hp, ABSOLUTE_MASS_STORAGE_SUBFIELD_SPECS, Collections.emptySet())) {
            return;
        }

        var packName = getSubField(hp, 2, 0);
        if (packName == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.PackIdIsRequired);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        var releaseOnTaskEnd = (hp._optionWord & I_OPTION) != 0;
        var doNotHoldRun = (hp._optionWord & Z_OPTION) != 0;

        var fm = Exec.getInstance().getFacilitiesManager();
        if (fm.assignDiskUnitToRun(hp._runControlEntry,
                                   fs,
                                   device.getNodeIdentifier(),
                                   packName,
                                   hp._optionWord,
                                   releaseOnTaskEnd,
                                   doNotHoldRun,
                                   hp._statement._facStatusResult)) {
            postComplete(hp);
        }
    }

    private void handleAbsoluteTapeDevice(
        final HandlerPacket hp,
        final FileSpecification fs,
        final NodeInfo nodeInfo,
        final TapeDevice device
    ) {
        // TODO - lots of stuff to be done

        // add fac item to the run
        // TODO

        postComplete(hp);
    }

    /**
     * 'A' option was specified. Check equipment field to decide whether to use disk or tape.
     * If neither is specified, we have to do an early run to MFD to find out what to use.
     */
    private void handleCatalogedFile(
        final HandlerPacket hp,
        final FileSpecification fileSpecification
    ) throws ExecStoppedException {
        var e = Exec.getInstance();
        var cfg = e.getConfiguration();

        var equip = getSubField(hp, 1, 0);
        if (equip == null) {
            var mm = Exec.getInstance().getMFDManager();
            FileSetInfo fsInfo = null;
            try {
                fsInfo = mm.getFileSetInfo(fileSpecification.getQualifier(), fileSpecification.getFilename());
                for (var fsci : fsInfo.getCycleInfo()) {
                    if (!fsci.isToBeCataloged()) {
                        var fcInfo = mm.getFileCycleInfo(fileSpecification.getQualifier(),
                                                         fileSpecification.getFilename(),
                                                         fsci.getAbsoluteCycle());
                        equip = fcInfo.getAssignMnemonic();
                    }
                }
            } catch (FileCycleDoesNotExistException ex) {
                LogManager.logFatal(getCommand(),
                                    "fsInfo for %s*%s references abs cycle which MFD cannot find",
                                    fsInfo.getQualifier(),
                                    fsInfo.getFilename());
                e.stop(StopCode.FacilitiesComplex);
                throw new ExecStoppedException();
            } catch (FileSetDoesNotExistException ex) {
                hp._statement._facStatusResult.postMessage(FacStatusCode.FileIsNotCataloged);
                hp._statement._facStatusResult.mergeStatusBits(0_400010_000000L);
                return;
            }
        } else {
            if (equip.length() > 6) {
                hp._statement._facStatusResult.postMessage(FacStatusCode.AssignMnemonicTooLong);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
                return;
            }
        }

        switch (cfg.getMnemonicType(equip)) {
            case SECTOR_ADDRESSABLE_DISK,
                WORD_ADDRESSABLE_DISK -> handleCatalogedDiskFile(hp, fileSpecification, equip);
            case TAPE -> handleCatalogedTapeFile(hp, fileSpecification);
            default -> {
                hp._statement._facStatusResult.postMessage(FacStatusCode.MnemonicIsNotConfigured);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            }
        }
    }

    /**
     * When we get here, there's an A option on the statement, and the type refers to mass storage
     */
    private void handleCatalogedDiskFile(
        final HandlerPacket hp,
        final FileSpecification fileSpecification,
        final String equip
    ) throws ExecStoppedException {
        // Validate options and subfields
        long allowedOpts = A_OPTION | I_OPTION | Z_OPTION
            | D_OPTION | E_OPTION | K_OPTION | M_OPTION | Q_OPTION | R_OPTION | X_OPTION | Y_OPTION;
        long mutexOpts1 = D_OPTION | K_OPTION;
        long mutexOpts2 = E_OPTION | Y_OPTION;
        if (!checkIllegalOptions(hp, allowedOpts)
            || !checkMutuallyExclusiveOptions(hp, mutexOpts1)
            || !checkMutuallyExclusiveOptions(hp, mutexOpts2)
            || !checkIllegalFieldsAndSubfields(hp,
                                               CATALOGED_DISK_SUBFIELD_SPECS,
                                               CATALOGED_DISK_UNBOUND_SUBFIELDS)) {
            return;
        }

        if (hp._statement._operandFields.get(new SubfieldSpecifier(1, 4)) != null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.PlacementFieldIgnored);
        }

        Integer iReserve = null;
        try {
            var reserve = getSubField(hp, 1, 1);
            iReserve = Integer.parseInt(reserve);
            if (iReserve < 0) {
                hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalInitialReserve);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
                return;
            }
        } catch (NumberFormatException ex) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalInitialReserve);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        var granularity = getSubField(hp, 1, 2);
        Granularity eGranularity = null;
        if (granularity != null) {
            if (granularity.equalsIgnoreCase("TRK")) {
                eGranularity = Granularity.Track;
            } else if (granularity.equalsIgnoreCase("POS")) {
                eGranularity = Granularity.Position;
            } else {
                hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalValueForGranularity);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
                return;
            }
        }

        Integer iMaximum = null;
        try {
            var maximum = getSubField(hp, 1, 3);
            iMaximum = Integer.parseInt(maximum);
            if (iMaximum < 0) {
                hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalMaxGranules);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
                return;
            }
        } catch (NumberFormatException ex) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalMaxGranules);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        var placement = hp._statement._operandFields.get(new SubfieldSpecifier(1, 4));

        var packIds = new LinkedList<String>();
        for (var entry : hp._statement._operandFields.entrySet()) {
            if (entry.getKey().getFieldIndex() == 2) {
                var packId = entry.getValue();
                if (packIds.contains(packId)) {
                    hp._statement._facStatusResult.postMessage(FacStatusCode.DuplicateMediaIdsAreNotAllowed);
                    hp._statement._facStatusResult.mergeStatusBits(0_400004_000000L);
                    return;
                }
                packIds.add(packId);
            }
        }
        if (packIds.size() > 510) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.MaximumNumberOfPackIdsExceeded);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        FacilitiesManager.DeleteBehavior deleteBehavior;
        if ((hp._optionWord & D_OPTION) != 0) {
            deleteBehavior = FacilitiesManager.DeleteBehavior.DeleteOnNormalRunTermination;
        } else if ((hp._optionWord & K_OPTION) != 0) {
            deleteBehavior = FacilitiesManager.DeleteBehavior.DeleteOnAnyRunTermination;
        } else {
            deleteBehavior = FacilitiesManager.DeleteBehavior.None;
        }

        FacilitiesManager.DirectoryOnlyBehavior directoryOnlyBehavior;
        if ((hp._optionWord & E_OPTION) != 0) {
            directoryOnlyBehavior = FacilitiesManager.DirectoryOnlyBehavior.DirectoryOnlyMountPacks;
        } else if ((hp._optionWord & Y_OPTION) != 0) {
            directoryOnlyBehavior = FacilitiesManager.DirectoryOnlyBehavior.DirectoryOnlyDoNotMountPacks;
        } else {
            directoryOnlyBehavior = FacilitiesManager.DirectoryOnlyBehavior.None;
        }

        var saveOnCheckpoint = (hp._optionWord & M_OPTION) != 0;
        var assignIfDisabled = (hp._optionWord & Q_OPTION) != 0;
        var readOnly = (hp._optionWord & R_OPTION) != 0;
        var releaseOnTaskEnd = (hp._optionWord & I_OPTION) != 0;
        var exclusiveUse = (hp._optionWord & X_OPTION) != 0;
        var doNotHoldRun = (hp._optionWord & Z_OPTION) != 0;

        var fm = Exec.getInstance().getFacilitiesManager();
        if (fm.assignCatalogedDiskFileToRun(hp._runControlEntry,
                                            fileSpecification,
                                            hp._optionWord,
                                            equip,
                                            iReserve,
                                            eGranularity,
                                            iMaximum,
                                            placement,
                                            packIds,
                                            deleteBehavior,
                                            directoryOnlyBehavior,
                                            saveOnCheckpoint,
                                            assignIfDisabled,
                                            readOnly,
                                            exclusiveUse,
                                            releaseOnTaskEnd,
                                            doNotHoldRun,
                                            hp._statement._facStatusResult)) {
            postComplete(hp);
        }
    }

    private void handleCatalogedTapeFile(
        final HandlerPacket hp,
        final FileSpecification fileSpecification
    ) {
        // TODO
    }

    /**
     * Handles temporary assignment of a file or an absolute device or controller.
     * For our purposes, a channel is a controller.
     * --------------------------------------
     * For tape unit or controller assignments:
     *  options may include:
     *      B - tape identifier is non-unique on system/run basis
     *      E - even parity - only for 7-track, which we do not have
     *      I - free's the file at the next task termination
     *      N - do not automatically assign reel to be mounted
     *      R - read-only
     *      T - required (or we wouldn't be here)
     *      W - write-only
     *      X - Do not inhibit IO after no-EOF or block-count EOF1/EOV1 does not match actual count
     *      Z - assignment cannot cause the run to be held
     *  field[0][0] is the external file name
     *  field[1][0] and [1][1] are either
     *      '*' device1 name
     *    and
     *      '*' device2 name (optional)
     *    or
     *      '*' channel name
     *    and
     *      '1' or '2' - number of units (defaults to 1)
     *  field[1] is noise/processor/format/data-converter/block-numbering/data-compression/buffered-write
     *  field[2] is optional list of reels
     *  field[3][0] is expiration-period
     *  field[4][0] is ring-indicator
     *  field[5][0] would be ACR-name, but is not allowed for T option
     *  field[6][0] is CTL-pool
     * It is not necessary to have a tape device in a reserve state for an absolute assign. It can be UP.
     * --------------------------------------
     * For any other device or controller absolute assignment:
     *  options may include:
     *      I - free's the file at the next task termination
     *      T - required (or we wouldn't be here)
     *      Z - assignment cannot cause the run to be held
     *  field[0][0] is the external file name
     *  field[1][0] contains an asterisk followed by either a device name or a channel name
     *                  for devices, the device must be RV
     *                  for channels, at least one device accessible via the channel must be RV
     * There is no indication that the device must be RV (it should not, however, be DN).
     * @param hp
     * @return
     */
}
