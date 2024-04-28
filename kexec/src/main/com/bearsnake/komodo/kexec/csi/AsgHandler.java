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
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;

// TODO NOTE:
//  To create a file on a fixed pack, use the @ASG or @CAT statement without including any pack-id field.
//  To create a file on one or more removable packs, include one or more device identifiers on the pack-id field.
//  This applies to ASG and CAT.


class AsgHandler extends Handler {

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
        var fnField = getSubField(hp._statement._operandFields, 0, 0);
        if (fnField == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.FilenameIsRequired);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        FileSpecification fileSpec;
        try {
            fileSpec = FileSpecification.parse(new Parser(fnField), ".,/ ");
        } catch (Parser.SyntaxException ex) {
            // TODO we need to be more specific here than we currently can...
            //   e.g., file cycle specification error is a different message.
            hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        if (fileSpec == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.FilenameIsRequired);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        // get the type field.
        var typeField = getSubField(hp._statement._operandFields, 1, 0);
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

    // TODO might use some of these...
    //E:200533 Device not available on control unit unit-Name.
    //E:200733 Insufficient number of units available on control unit unit-Name.
    //E:201033 asg-mnem is not a configured assign mnemonic.
    //E:201133 device-Name is not a configured Name.
    //E:201333 The operator does not allow absolute assignment of pack pack-id.
    //E:202233 Pack pack-id is not a removable pack.
    //E:202533 reel nnnnnn already in use by this run.
    //E:203133 unit-Name is not in the reserved state.
    //E:203233 device-Name is not up.
    //E:203333 Name is not in the up or reserved state.
    //E:240133 Absolute device specification is not allowed on cataloged file.
    //E:240233 Absolute assignment of tape not allowed with CY or UY options.
    //E:240333 Illegal options on absolute assignment.
    //E:245433 Insufficient number of units available.
    //E:246733 Logical channel not allowed with absolute device assign.
    //E:247633 Maximum number of packids exceeded.
    //E:247733 Maximum number of reelids exceeded.
    //E:250233 Number of units not allowed with absolute device assign.
    //E:251633 Packid is required.
    //E:252633 Quota set does not allow absolute device assignment.
    //E:254433 Security does not allow absolute device assignment.
    //E:255733 Image contains an undefined field or subfield.
    //E:256033 Requested unit(s) not available.

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
            handleAbsoluteDiskDevice(hp, fs, nodeInfo, (DiskDevice) device);
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
     * @param nodeInfo nodeInfo describing the exec's knowledge of the device
     * @param device disk device
     */
    private void handleAbsoluteDiskDevice(final HandlerPacket hp,
                                          final FileSpecification fs,
                                          final NodeInfo nodeInfo,
                                          final DiskDevice device) throws ExecStoppedException {
        long allowed = Word36.H_OPTION | Word36.I_OPTION | Word36.T_OPTION | Word36.Z_OPTION;
        if (!checkIllegalOptions(hp, allowed)) {
            return;
        }

        // A thought... if the T option is *not* specified, algorithms are supposed to go see if a file exists in
        // the MFD given the file specification, and if so, use A semantics. We cannot use A semantics for a
        // disk unit, so we are assuming a T option exists if a disk unit is specified. Is this correct?

        if (hp._statement._operandFields.get(1).size() != 1) {
            // contains undefined subfield
            hp._statement._facStatusResult.postMessage(FacStatusCode.UndefinedFieldOrSubfield);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        var packName = getSubField(hp._statement._operandFields, 2, 0);
        if (packName == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.PackIdIsRequired);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        } else if (hp._statement._operandFields.get(2).size() > 1) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.MaximumNumberOfPackIdsExceeded);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        var iOpt = (hp._optionWord & Word36.I_OPTION) != 0;
        var zOpt = (hp._optionWord & Word36.Z_OPTION) != 0;
        Exec.getInstance().getFacilitiesManager().assignDiskUnitToRun(hp._runControlEntry,
                                                                      fs,
                                                                      device.getNodeIdentifier(),
                                                                      packName,
                                                                      iOpt,
                                                                      zOpt,
                                                                      hp._statement._facStatusResult);
        postComplete(hp);
    }

    private void handleAbsoluteTapeDevice(final HandlerPacket hp,
                                          final FileSpecification fs,
                                          final NodeInfo nodeInfo,
                                          final TapeDevice device) {
        // TODO - lots of stuff to be done

        // add fac item to the run
        // TODO

        postComplete(hp);
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