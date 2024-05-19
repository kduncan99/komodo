/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.PrepFactor;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.kexec.mfd.MFDManager;

class PREPKeyinHandler extends KeyinHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "PREP,[F|R] {device},{packname}",
        "Writes a label and an initial directory track to the pack currently mounted",
        "on the indicated device. The prep factor is determined from the geometry",
        "reported by the device for the mounted pack. The device must be reserved (RV)."
    };

    public static final String COMMAND = "PREP";

    private String _deviceName;
    private String _packName;

    public PREPKeyinHandler(final ConsoleId source,
                            final String options,
                            final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_options == null || _arguments == null) {
            return false;
        }

        if (_options.length() != 1) {
            return false;
        }

        var split = _arguments.split(",");
        if (split.length != 2) {
            return false;
        }

        _deviceName = split[0].toUpperCase();
        _packName = split[1].toUpperCase();
        return true;
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        return true;
    }

    private boolean checkExistingLabel(final Device device) {
        // TODO use facilities to read the volume label
        var label = new long[28];
        var labelSlice = new ArraySlice(label);
        var cw = new ChannelProgram.ControlWord().setBuffer(labelSlice)
                                                 .setDirection(ChannelProgram.Direction.Increment)
                                                 .setBufferOffset(0)
                                                 .setTransferCount(28);
        var cp = new ChannelProgram().setFunction(ChannelProgram.Function.Read)
                                     .setNodeIdentifier(device.getNodeIdentifier())
                                     .setBlockId(0)
                                     .addControlWord(cw);
        try {
            Exec.getInstance().getFacilitiesManager().routeIo(cp);
            if (cp.getIoStatus() == IoStatus.Complete) {
                if (Word36.toStringFromASCII(label[0]).equals("VOL1")) {
                    // pack already has a label - ask the operator if we really want to prep the pack
                    var existing = Word36.toStringFromASCII(label[1])
                        + Word36.toStringFromASCII(label[2]).substring(0, 2);
                    var msg = String.format("Pack on %s is already labeled", device.getNodeName());
                    Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
                    msg = String.format("Continue prepping pack %s as %s? Y/N",
                                            existing.trim(),
                                            _packName);
                    var allowed = new String[]{"Y", "N"};
                    var response = Exec.getInstance().sendExecRestrictedReadReplyMessage(msg, allowed, _source);
                    if (!response.equalsIgnoreCase("Y")) {
                        return false;
                    }
                }
            }
        } catch (KExecException ex) {
            // This is okay - it just means there is no VOL1 label
            // (or it could mean something worse, but we'll catch that later on the write)
        }

        // If we get here, it is okay to proceed with writing the label.
        return true;
    }

    @Override
    void process() {
        var fixed = _options.equalsIgnoreCase("F");
        var rem = _options.equalsIgnoreCase("R");
        if (!fixed && !rem) {
            var msg = "Invalid option on PREP keyin";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (!Exec.isValidNodeName(_deviceName)) {
            var msg = "Invalid device name on PREP keyin";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (!Exec.isValidPackName(_packName)) {
            var msg = "Invalid device name on PREP keyin";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        var nodeInfo = Exec.getInstance().getFacilitiesManager().getNodeInfo(_deviceName);
        if ((nodeInfo == null) || !(nodeInfo.getNode() instanceof DiskDevice device)) {
            var msg = _deviceName + " is not a configured disk device";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (nodeInfo.getNodeStatus() != NodeStatus.Reserved) {
            var msg = nodeInfo.getNode().getNodeName() + " is not reserved";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        var ddInfo = device.getInfo();

        if (!ddInfo.isReady()) {
            var msg = nodeInfo.getNode().getNodeName() + " is not ready";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (ddInfo.isWriteProtected()) {
            var msg = nodeInfo.getNode().getNodeName() + " is write protected";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        // TODO call facilities to @ASG the device, then pass the device name to the sub-methods
        // TODO see if there is already a label, and if so prompt the operator

        var prepFactor = PrepFactor.getPrepFactorFromBlockSize(ddInfo.getBlockSize());
        var blocksPerTrack = 1792 / prepFactor;
        var capacity = ddInfo.getMaxBlockCount();
        var firstDirectoryTrackAddress = 1;
        var directoryTrackBlockId = firstDirectoryTrackAddress * blocksPerTrack;

        if (!writeInitialDirectoryTrack(device,
                                        capacity,
                                        _packName,
                                        blocksPerTrack,
                                        prepFactor,
                                        directoryTrackBlockId,
                                        fixed)) {
            // TODO fail and @free the device
            return;
        }

        if (!writeLabel(device, _packName, prepFactor, capacity, firstDirectoryTrackAddress)) {
            // TODO fail and @free the device
            return;
        }

        var msg = String.format("Prep %s complete", device.getNodeName());
        Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
    }

    private boolean writeInitialDirectoryTrack(final Device device,
                                               final long capacity,
                                               final String packName,
                                               final int blocksPerTrack,
                                               final int prepFactor,
                                               final long directoryTrackBlockAddress,
                                               final boolean isFixed) {
        var dirTrack = new long[1792];
        var dirTrackSlice = new ArraySlice(dirTrack);
        var sector0 = new ArraySlice(dirTrackSlice, 0, 28);
        var sector1 = new ArraySlice(dirTrackSlice, 28, 28);

        // sector 0
        // no LDAT index (yet), so leave [0]H1 alone.
        // First two sectors are allocated, so note that in [1].
        // DAS links to tracks 1 through 8 are invalid, as is link to next DAS.
        sector0.set(01, 0_600000_000000L);
        for (int dx = 3; dx < 27; dx += 3) {
            sector0.set(dx, MFDManager.INVALID_LINK);
        }
        sector0.set(27, MFDManager.INVALID_LINK);

        // sector 1
        // Leave +0 and +1 alone (We aren't doing HMBT/SMBT)
        // Set +2 and +3 to available tracks, +4 to pack-id
        // +5,H1 Bit35 needs to be set to indicate fixed pack
        sector1.set(2, capacity);
        sector1.set(3, capacity);
        String packId = String.format("%-6s", packName);
        sector1.set(4, Word36.stringToWordFieldata(packId));
        sector1.set(5, 0_400000_000000L);

        // +010,T1 is blocks per track
        // +010,S3 is version (1)
        // +010,T3 is prepfactor
        sector1.setT1(010, blocksPerTrack);
        sector1.setS3(010, 1);
        sector1.setT3(010, prepFactor);

        var cw = new ChannelProgram.ControlWord().setBuffer(dirTrackSlice)
                                                 .setDirection(ChannelProgram.Direction.Increment)
                                                 .setBufferOffset(0)
                                                 .setTransferCount(1792);
        var cp = new ChannelProgram().setFunction(ChannelProgram.Function.Write)
                                     .setNodeIdentifier(device.getNodeIdentifier())
                                     .setBlockId(directoryTrackBlockAddress)
                                     .addControlWord(cw);

        boolean err = false;
        try {
            Exec.getInstance().getFacilitiesManager().routeIo(cp);
            err = cp.getIoStatus() != IoStatus.Complete;
        } catch (KExecException ex) {
            err = true;
        }

        if (err) {
            var msg = String.format("IO error writing directory track to %s", device.getNodeCategory());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return false;
        }

        return true;
    }

    private boolean writeLabel(final DiskDevice device,
                               final String packName,
                               final int prepFactor,
                               final long capacity,
                               final long firstDirectoryTrackAddress) {
        // We are potentially writing to space which has never yet been written to.
        // For this reason, we need to write an entire block.
        var blockSize = device.getInfo().getBlockSize();
        var label = new long[blockSize];
        label[0] = Word36.stringToWordASCII("VOL1");

        var paddedPack = String.format("%-8s", packName);
        label[1] = Word36.stringToWordASCII(paddedPack.substring(0, 4));
        label[2] = Word36.stringToWordASCII(paddedPack.substring(4, 8));
        label[2] = Word36.setH2(label[2], 0);

        label[3] = firstDirectoryTrackAddress * 1792; // convert to DRWA

        var recordsPerTrack = (long)(1792 / prepFactor);
        label[4] = Word36.setH1(label[4], recordsPerTrack);
        label[4] = Word36.setH2(label[4], prepFactor);

        label[014] = Word36.setS1(label[014], 040);
        label[014] = Word36.setS2(label[014], 1);

        label[016] = capacity;

        var cw = new ChannelProgram.ControlWord().setBuffer(new ArraySlice(label))
                                                 .setDirection(ChannelProgram.Direction.Increment)
                                                 .setBufferOffset(0)
                                                 .setTransferCount(PrepFactor.getPrepFactorFromBlockSize(blockSize));
        var cp = new ChannelProgram().setFunction(ChannelProgram.Function.Write)
                                     .setNodeIdentifier(device.getNodeIdentifier())
                                     .setBlockId(0)
                                     .addControlWord(cw);
        boolean err = false;
        try {
            Exec.getInstance().getFacilitiesManager().routeIo(cp);
            err = cp.getIoStatus() != IoStatus.Complete;
        } catch (KExecException ex) {
            err = true;
        }

        if (err) {
            var msg = String.format("IO error writing label to %s", device.getNodeCategory());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return false;
        }

        return true;
    }
}
