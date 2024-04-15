/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.kexec.mfd.MFDManager;
import com.bearsnake.komodo.logger.LogManager;

public class PREPKeyinHandler extends KeyinHandler implements Runnable {

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
    public void abort(){}

    @Override
    public boolean checkSyntax() {
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
    public String getCommand() { return COMMAND; }

    @Override
    public String[] getHelp() { return HELP_TEXT; }

    @Override
    public boolean isAllowed() {
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
            if (cp._ioStatus == IoStatus.Complete) {
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

    private void process() {
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
        var nodeId = device.getNodeIdentifier();

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

        var prepFactor = DiskDevice.getPrepFactorForBlockSize(ddInfo.getBlockSize());
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
        sector1.set(4, Word36.stringToWordFieldata(packId).getW());
        sector1.set(5, 0_400000_000000L);

        // +010,T1 is blocks per track
        // +010,S3 is version (1)
        // +010,T3 is prepfactor
        Word36 w36 = new Word36();
        w36.setT1(blocksPerTrack);
        w36.setS3(1);
        w36.setT3(prepFactor);
        sector1.set(010, w36.getW());

        // TODO use facilities to write the directory track
        var cw = new ChannelProgram.ControlWord().setBuffer(dirTrackSlice)
                                                 .setDirection(ChannelProgram.Direction.Increment)
                                                 .setBufferOffset(0)
                                                 .setTransferCount(28);
        var cp = new ChannelProgram().setFunction(ChannelProgram.Function.Write)
                                     .setNodeIdentifier(device.getNodeIdentifier())
                                     .setBlockId(directoryTrackBlockAddress)
                                     .addControlWord(cw);
        try {
            Exec.getInstance().getFacilitiesManager().routeIo(cp);
        } catch (KExecException ex) {
            var msg = String.format("IO error writing directory track to %s", device.getNodeCategory());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return false;
        }

        return true;
    }

    private boolean writeLabel(final Device device,
                               final String packName,
                               final int prepFactor,
                               final long capacity,
                               final long firstDirectoryTrackAddress) {
        var label = new long[28];
        label[0] = Word36.stringToWordASCII("VOL1").getW();

        var paddedPack = String.format("%-8s", packName);
        label[1] = Word36.stringToWordASCII(paddedPack.substring(0, 4)).getW();
        label[2] = Word36.stringToWordASCII(paddedPack.substring(4, 8)).getW();
        label[2] = Word36.setH2(label[2], 0);

        label[3] = firstDirectoryTrackAddress;

        Word36 w36 = new Word36();

        var recordsPerTrack = (long)(1792 / prepFactor);
        w36.setH1(recordsPerTrack);
        w36.setH2(prepFactor);
        label[4] = w36.getW();

        w36.setW(0);
        w36.setS1(040);
        w36.setS2(1);
        label[014] = w36.getW();

        label[016] = capacity;

        // TODO use facilities to write the label
        var cw = new ChannelProgram.ControlWord().setBuffer(new ArraySlice(label))
                                                 .setDirection(ChannelProgram.Direction.Increment)
                                                 .setBufferOffset(0)
                                                 .setTransferCount(28);
        var cp = new ChannelProgram().setFunction(ChannelProgram.Function.Write)
                                     .setNodeIdentifier(device.getNodeIdentifier())
                                     .setBlockId(0)
                                     .addControlWord(cw);
        try {
            Exec.getInstance().getFacilitiesManager().routeIo(cp);
        } catch (KExecException ex) {
            var msg = String.format("IO error writing label to %s", device.getNodeCategory());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        try {
            process();
        } catch (Throwable t) {
            LogManager.logCatching(COMMAND, t);
            Exec.getInstance().stop(StopCode.ExecContingencyHandler);
        }
        setFinished();
    }
}
