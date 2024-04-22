/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.Channel;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DeviceType;
import com.bearsnake.komodo.hardwarelib.DiskChannel;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.FileSystemDiskDevice;
import com.bearsnake.komodo.hardwarelib.FileSystemTapeDevice;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.NodeCategory;
import com.bearsnake.komodo.hardwarelib.TapeChannel;
import com.bearsnake.komodo.hardwarelib.TapeDevice;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.HardwareTrackId;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;
import com.bearsnake.komodo.kexec.exec.RunType;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.facItems.AbsoluteDiskItem;
import com.bearsnake.komodo.kexec.mfd.FileAllocationSet;
import com.bearsnake.komodo.kexec.mfd.MFDRelativeAddress;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class FacilitiesManager implements Manager {

    static final String LOG_SOURCE = "FacMgr";

    // All assigned disk files are recorded here so that we can easily access and manage the file allocations.
    final HashMap<MFDRelativeAddress, FileAllocationSet> _acceleratedFileAllocations = new HashMap<>();

    // Inventory of all the hardware nodes, keyed by node identifier.
    // It is loaded at initialization(), and will remain unchanged during the application existence.
    final HashMap<Integer, NodeInfo> _nodeGraph = new HashMap<>();

    public FacilitiesManager() {
        Exec.getInstance().managerRegister(this);
    }

    // -------------------------------------------------------------------------
    // Manager interface
    // -------------------------------------------------------------------------

    @Override
    public void boot(final boolean recoveryBoot) {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);

        // update verbosity of nodes
        for (var ni : _nodeGraph.values()) {
            ni._node.setLogIos(Exec.getInstance().getConfiguration().getLogIos());
        }

        // clear cached disk labels
        for (var ni : _nodeGraph.values()) {
            ni._mediaInfo = null;
        }

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    @Override
    public synchronized void dump(final PrintStream out,
                                  final String indent,
                                  final boolean verbose) {
        out.printf("%sFacilitiesManager ********************************\n", indent);

        // TODO

        out.printf("%s  Node Graph:\n", indent);
        var subIndent = indent + "    ";
        for (var ni : _nodeGraph.values()) {
            out.printf("%s%s\n", subIndent, ni.toString());
        }

        if (verbose) {
            // Accelerated file information
            out.printf("%s  Accelerated files:\n", indent);
            for (var e : _acceleratedFileAllocations.entrySet()) {
                var prefix = e.getKey().toString();
                for (var fa : e.getValue().getFileAllocations()) {
                    out.printf("%s    %s: %s\n", indent, prefix, fa.toString());
                    prefix = "              ";
                }
            }
        }
    }

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");

        loadNodeGraph();

        // set up routes
        for (var ni : _nodeGraph.values()) {
            if (ni instanceof ChannelNodeInfo) {
                var chNode = (Channel) ni._node;
                for (var devNode : chNode.getDevices()) {
                    var dni = (DeviceNodeInfo) _nodeGraph.get(devNode.getNodeIdentifier());
                    dni._routes.add(chNode);
                }
            }
        }
    }

    @Override
    public synchronized void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
    }

    // -------------------------------------------------------------------------
    // Services interface
    // -------------------------------------------------------------------------

    /**
     * For assigning a (reserved) disk to a run.
     * This assignment can only be temporary, and the device must be reserved.
     * @param runControlEntry describes the run
     * @param fileSpecification describes the file name
     * @param nodeIdentifier node identifier of the device
     * @param packName pack name requested for the device
     * @param releaseOnTaskEnd I-option on assign
     * @param doNotHoldRun Z-option on assign
     * @param fsResult fac status result
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public boolean assignDiskUnitToRun(
        final RunControlEntry runControlEntry,
        final FileSpecification fileSpecification,
        final int nodeIdentifier,
        final String packName,
        final boolean releaseOnTaskEnd,
        final boolean doNotHoldRun,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        var nodeInfo = _nodeGraph.get(nodeIdentifier);
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToRun() Cannot find node %012o", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if ((node.getNodeCategory() != NodeCategory.Device) || ((Device)node).getDeviceType() != DeviceType.DiskDevice) {
            LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToRun() Node %012o is not a disk device", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var disk = (DiskDevice)node;
        if (nodeInfo.getNodeStatus() != NodeStatus.Reserved) {
            var params = new String[]{ nodeInfo.getNode().getNodeName() };
            fsResult.postMessage(FacStatusCode.UnitIsNotReserved, params );
            fsResult.mergeStatusBits(0_600000_000000L);
            return false;
        }

        // TODO check requested pack name - if it is already assigned to this run, reject the request

        // Create an effective file specification based on the given specification and
        // the qualifier specs in the run control entry.
        var effectiveFileSpec = resolveQualifier(fileSpecification, runControlEntry);
        var facItem = new AbsoluteDiskItem(node, packName);
        facItem.setQualifier(effectiveFileSpec.getQualifier())
               .setFilename(effectiveFileSpec.getFilename())
               .setIsTemporary(true)
               .setReleaseOnTaskEnd(releaseOnTaskEnd);
        if (effectiveFileSpec.hasFileCycleSpecification()) {
            var fcSpec = effectiveFileSpec.getFileCycleSpecification();
            if (fcSpec.isAbsolute()) {
                facItem.setAbsoluteCycle(fcSpec.getCycle());
            } else if (fcSpec.isRelative()) {
                facItem.setRelativeCycle(fcSpec.getCycle());
            }
        }

        // If there is a facilities item in the rce which matches the file specification which does not refer
        //  to an absolute assign of this same unit, fail.
        // If there is any facilities item in the rce which refers to this unit, fail (already in use by this run).
        // If the filename portion of the new facilities item is not unique to the run, post a warning
        //  (filename not unique)
        var filenameNotUnique = false;
        var facItems = runControlEntry.getFacilityItemTable();
        synchronized (facItems) {
            for (var fi : facItems.getFacilitiesItems()) {
                var facTable = runControlEntry.getFacilityItemTable();
                synchronized (facTable) {
                    if (facTable.getExactFacilitiesItem(effectiveFileSpec) != null) {
                        fsResult.postMessage(FacStatusCode.IllegalAttemptToChangeAssignmentType);
                        fsResult.mergeStatusBits(0_400000_000000L);
                        return false;
                    }
                }

                if (fi instanceof AbsoluteDiskItem adi) {
                    if (adi._node == node) {
                        var params = new String[]{ node.getNodeName() };
                        fsResult.postMessage(FacStatusCode.DeviceAlreadyInUse, params);
                        fsResult.mergeStatusBits(0_400000_000000L);
                        return false;
                    }
                }

                if (fi.getFilename().equals(facItem.getFilename())) {
                    filenameNotUnique = true;
                }
            }

            facItems.addFacilitiesItem(facItem);
        }

        if (filenameNotUnique) {
            fsResult.postMessage(FacStatusCode.FilenameNotUnique);
            fsResult.mergeStatusBits(0_004000_000000L);
        }

        // Wait for the unit if necessary...
        var startTime = Instant.now();
        var nextMessageTime = startTime.plusSeconds(120);
        if (!doNotHoldRun) {
            while (true) {
                if (!Exec.getInstance().isRunning()) {
                    throw new ExecStoppedException();
                }
                runControlEntry.incrementWaitingForPeripheral();
                synchronized (nodeInfo) {
                    if (nodeInfo._assignedTo == null) {
                        nodeInfo._assignedTo = runControlEntry;
                        facItem.setIsAssigned(true);
                        runControlEntry.decrementWaitingForPeripheral();
                        break;
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // do nothing
                }

                var now = Instant.now();
                if (now.isAfter(nextMessageTime)) {
                    nextMessageTime = nextMessageTime.plusSeconds(120);
                    if (!runControlEntry.hasTask()
                        && ((runControlEntry.getRunType() == RunType.Batch) || (runControlEntry.getRunType() == RunType.Demand))) {
                        long minutes = Duration.between(now, startTime).getSeconds() / 60;
                        var params = new Object[]{runControlEntry.getRunId(), minutes};
                        var facMsg = new FacStatusMessageInstance(FacStatusCode.RunHeldForDiskUnitAvailability, params);
                        runControlEntry.postToPrint(facMsg.toString(), 1);
                    }
                }
            }
        }

        if (!facItem.isAssigned()) {
            // z-option bail-out
            fsResult.postMessage(FacStatusCode.HoldForDiskUnitRejected);
            fsResult.mergeStatusBits(0_400001_000000L);
        }

        if (!promptLoadPack(runControlEntry, nodeInfo, disk, packName)) {
            facItems.removeFacilitiesItem(facItem);
            nodeInfo._assignedTo = null;
            var params = new String[]{packName};
            fsResult.postMessage(FacStatusCode.OperatorDoesNotAllowAbsoluteAssign, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            return false;
        }

        var packInfo = (PackInfo)nodeInfo._mediaInfo;
        if (packInfo.isFixed()) {
            facItems.removeFacilitiesItem(facItem);
            nodeInfo._assignedTo = null;
            var params = new String[]{packName};
            fsResult.postMessage(FacStatusCode.DeviceIsFixed, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            return false;
        }

        return true;
    }

    /**
     * Returns the facilities NodeInfo for the given node.
     * @param nodeName name of the node - we'd prefer it to be uppercase, but whatever...
     * @return NodeInfo of the node if it is configured, else null
     */
    public NodeInfo getNodeInfo(final String nodeName) {
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> ni._node.getNodeName().equalsIgnoreCase(nodeName))
                         .findFirst()
                         .orElse(null);

    }

    /**
     * Returns a collection of NodeInfo objects of all the nodes of a particular category
     * @param category category of interest
     * @return collection
     */
    public Collection<NodeInfo> getNodeInfos(final NodeCategory category) {
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> ni._node.getNodeCategory() == category)
                         .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Returns a collection of NodeInfo objects of all the device nodes of a particular type
     * @param deviceType device type of interest
     * @return collection
     */
    public Collection<NodeInfo> getNodeInfos(final DeviceType deviceType) {
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> (ni.getNode() instanceof Device dev) && (dev.getDeviceType() == deviceType))
                         .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Returns a collection of the NodeInfo objects associated with the nodes
     * which are accessible via the channel indicated by channelInfo.
     * @param channelInfo indicates the channel of interest
     * @return list of NodeInfo objects
     */
    public Collection<NodeInfo> getNodeInfosForChannel(final ChannelNodeInfo channelInfo) {
        LinkedList<DeviceNodeInfo> list;
        var chan = (Channel)channelInfo.getNode();
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> (ni instanceof DeviceNodeInfo dni) && dni._routes.contains(chan))
                         .map(ni -> (DeviceNodeInfo) ni)
                         .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Produces a string suitable for display node status upon the console
     * @param nodeIdentifier nodeIdentifier of the node we are interested in
     * @return string
     * @throws ExecStoppedException
     */
    public String getNodeStatusString(final int nodeIdentifier) throws ExecStoppedException {
        var ni = _nodeGraph.get(nodeIdentifier);
        if (ni == null) {
            LogManager.logFatal(LOG_SOURCE, "getNodeStatusString nodeIdentifier %d not found", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var sb = new StringBuilder();
        sb.append(ni._node.getNodeName()).append("     ").setLength(6);
        sb.append(" ").append(ni._nodeStatus.getDisplayString());
        sb.append(isDeviceAccessible(nodeIdentifier) ? "   " : " NA");

        if (ni._node instanceof DiskDevice) {
            // [[*] [R|F] PACKID pack-id]
            sb.append(ni._assignedTo == null ? "  " : " *");
            if (ni._mediaInfo instanceof PackInfo pi) {
                if (pi.isFixed()) {
                    sb.append(" F");
                } else if (pi.isRemovable()) {
                    sb.append(" R");
                } else {
                    sb.append("  ");
                }

                sb.append(" PACKID ").append(pi.getMediaName());
            }
        } else if (ni._node instanceof TapeDevice) {
            // [* RUNID run-id REEL reel [RING|NORING] [POS [*]ffff[+|-][*]bbbbbb | POS LOST]]
            // TODO
        }

        return sb.toString();
    }

    /**
     * Indicates whether the device with the given identifier is accessible
     * (it has at least one channel path for which the channel is not DN).
     * @param nodeIdentifier identifier of the device
     * @return true if the device is accessible
     */
    public boolean isDeviceAccessible(final int nodeIdentifier) {
        var ni = _nodeGraph.get(nodeIdentifier);
        if (ni instanceof DeviceNodeInfo dni) {
            for (var chan : dni._routes) {
                var cni = _nodeGraph.get(chan.getNodeIdentifier());
                if (cni != null && cni._nodeStatus == NodeStatus.Up) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Routes an IO described by a channel program.
     * For the case where some portion of the Exec needs to do device-specific IO.
     * @param channelProgram IO description
     */
    public void routeIo(final ChannelProgram channelProgram) throws ExecStoppedException, NoRouteForIOException {
        var nodeInfo = _nodeGraph.get(channelProgram.getNodeIdentifier());
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE,
                                "Node %d from channel program is not configured",
                                channelProgram.getNodeIdentifier());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if (node.getNodeCategory() != NodeCategory.Device) {
            LogManager.logFatal(LOG_SOURCE,
                                "Node %d from channel program is not a device",
                                channelProgram.getNodeIdentifier());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        selectRoute((Device) node).routeIo(channelProgram);
    }

    /**
     * Invoked by Exec after boot() has been called for all managers.
     */
    public void startup() {
        LogManager.logTrace(LOG_SOURCE, "startup()");

        // read disk labels
        var diskLabel = new ArraySlice(new long[28]);
        var cw = new ChannelProgram.ControlWord().setBuffer(diskLabel)
                                                 .setBufferOffset(0)
                                                 .setTransferCount(28)
                                                 .setDirection(ChannelProgram.Direction.Increment);
        var cp = new ChannelProgram().setFunction(ChannelProgram.Function.Read)
                                     .setBlockId(0)
                                     .addControlWord(cw);
        for (var ni : _nodeGraph.values()) {
            if ((ni._nodeStatus == NodeStatus.Up)
                || (ni._nodeStatus == NodeStatus.Suspended)
                || ni._nodeStatus == NodeStatus.Reserved) {
                if ((ni instanceof DeviceNodeInfo dni) && (ni._node instanceof DiskDevice dd)) {
                    try {
                        var chan = selectRoute(dd);
                        cp.setNodeIdentifier(dd.getNodeIdentifier());
                        chan.routeIo(cp);
                        if (cp.getIoStatus() != IoStatus.Complete) {
                            var msg = String.format("IO Error reading pack label on device %s", dd.getNodeName());
                            Exec.getInstance().sendExecReadOnlyMessage(msg, null);

                            LogManager.logInfo(LOG_SOURCE, "IO error device %s:%s",
                                               dd.getNodeName(),
                                               cp.getIoStatus());

                            dni._nodeStatus = NodeStatus.Down;
                            msg = getNodeStatusString(dd.getNodeIdentifier());
                            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
                            continue;
                        }

                        var pi = loadDiskPackInfo(dni, diskLabel);
                        if (!pi.isPrepped()) {
                            dni._nodeStatus = NodeStatus.Down;
                            var msg = getNodeStatusString(dd.getNodeIdentifier());
                            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
                        }
                    } catch (ExecStoppedException ex) {
                        return;
                    } catch (NoRouteForIOException ex) {
                        LogManager.logInfo(LOG_SOURCE, "No route to device %s", dd.getNodeName());
                    }
                }
            }
        }

        // TODO

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    // -------------------------------------------------------------------------
    // Core
    // -------------------------------------------------------------------------

    /**
     * Given the MFD-relative address of a main item sector 0 for a file cycle,
     * we translate the given file-relative track id to an LDAT and physical device track.
     * @param mainItem0Address main item address of file cycle
     * @param fileTrackId file-relative track id
     * @return HardwareTrackId object if the given track is allocated, else null
     * @throws ExecStoppedException if the main item is not accelerated (generally meaning it is not assigned)
     */
    private HardwareTrackId convertFileRelativeTrackId(
        final MFDRelativeAddress mainItem0Address,
        final long fileTrackId
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "convertFileRelativeTrackId(%s, %d)", mainItem0Address.toString(), fileTrackId);
        var fa = _acceleratedFileAllocations.get(mainItem0Address);
        if (fa == null) {
            LogManager.logFatal(LOG_SOURCE, "mainItem0 is not accelerated");
            Exec.getInstance().stop(StopCode.DirectoryErrors);
            throw new ExecStoppedException();
        }

        var hwTid = fa.resolveFileRelativeTrackId(fileTrackId);
        LogManager.logTrace(LOG_SOURCE, "returning %s", hwTid);
        return hwTid;
    }

    PackInfo loadDiskPackInfo(final DeviceNodeInfo nodeInfo,
                              final ArraySlice label) {
        var pi = new PackInfo().setLabel(label);

        if (!Word36.toStringFromASCII(label._array[0]).equals("VOL1")) {
            var msg = String.format("Pack on %s has no label", nodeInfo._node.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            return pi;
        }

        var packName = Word36.toStringFromASCII(label._array[1]) + Word36.toStringFromASCII(label._array[2]);
        packName = packName.substring(0, 6).trim();
        if (!Exec.isValidPackName(packName)) {
            var msg = String.format("Pack on %s has an invalid pack name", nodeInfo._node.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            return pi;
        }

        pi.setPackName(packName);

        var prepFactor = (int)Word36.getH2(label._array[4]);
        if (!Exec.isValidPrepFactor(prepFactor)) {
            var msg = String.format("Pack on %s has an invalid prep factor: %d",
                                    nodeInfo._node.getNodeName(),
                                    prepFactor);
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            return pi;
        }

        pi.setIsPrepped(true)
          .setPrepFactor(prepFactor)
          .setDirectoryTrackId(label._array[3])
          .setTrackCount(label._array[016]);

        return pi;
    }

    void loadNodeGraph() {
        // Load node graph based on the configuration TODO
        // The following is temporary
        var disk0 = new FileSystemDiskDevice("DISK0", "media/disk0.pack", false);
        var disk1 = new FileSystemDiskDevice("DISK1", "media/disk1.pack", false);
        var disk2 = new FileSystemDiskDevice("DISK2", "media/disk2.pack", false);
        var disk3 = new FileSystemDiskDevice("DISK3", "media/disk3.pack", false);

        var dch0 = new DiskChannel("CHDSK0");
        dch0.attach(disk0);
        dch0.attach(disk1);
        dch0.attach(disk2);
        dch0.attach(disk3);

        var dch1 = new DiskChannel("CHDSK1");
        dch1.attach(disk0);
        dch1.attach(disk1);
        dch1.attach(disk2);
        dch1.attach(disk3);

        var tape0 = new FileSystemTapeDevice("TAPE0");
        var tape1 = new FileSystemTapeDevice("TAPE1");

        var tch = new TapeChannel("CHTAPE");
        tch.attach(tape0);
        tch.attach(tape1);

        _nodeGraph.put(dch0.getNodeIdentifier(), new ChannelNodeInfo(dch0));
        _nodeGraph.put(dch1.getNodeIdentifier(), new ChannelNodeInfo(dch1));
        _nodeGraph.put(tch.getNodeIdentifier(), new ChannelNodeInfo(tch));

        _nodeGraph.put(disk0.getNodeIdentifier(), new DeviceNodeInfo(disk0));
        _nodeGraph.put(disk1.getNodeIdentifier(), new DeviceNodeInfo(disk1));
        _nodeGraph.put(disk2.getNodeIdentifier(), new DeviceNodeInfo(disk2));
        _nodeGraph.put(disk3.getNodeIdentifier(), new DeviceNodeInfo(disk3));
        _nodeGraph.put(tape0.getNodeIdentifier(), new DeviceNodeInfo(tape0));
        _nodeGraph.put(tape1.getNodeIdentifier(), new DeviceNodeInfo(tape1));
        // end temporary code
    }

    /**
     * Prompts the operator to load a pack on a disk unit,
     * waits for it to happen, then verifies the packname.
     * @param runControlEntry describes the invoking run
     * @param nodeInfo NodeInfo object tracking exec information regarding the disk unit
     * @param disk DiskDevice object associated with the disk unit
     * @param packName pack name requested by the run
     * @return true if the pack is loaded - the operator may deny pack loading based on pack name mismatch.
     * @throws ExecStoppedException if we notice the exec has stopped during processing
     */
    private boolean promptLoadPack(
        final RunControlEntry runControlEntry,
        final NodeInfo nodeInfo,
        final DiskDevice disk,
        final String packName
    ) throws ExecStoppedException {
        // TODO while waiting we need to monitor the rce to see if it has been err'd, aborted, etc
        //   so we can stop waiting, post E:260733 Run has been aborted, and return false
        var loadMsg = String.format("Load %s %s %s",
                                    packName,
                                    disk.getNodeName(),
                                    runControlEntry.getRunId());
        Exec.getInstance().sendExecReadOnlyMessage(loadMsg, null);
        var serviceMsg = loadMsg.replace("Load", "Service");

        var nextMessageTime = Instant.now().plusSeconds(120);
        while (!disk.isReady()) {
            if (!Exec.getInstance().isRunning()) {
                throw new ExecStoppedException();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // do nothing
            }

            // Service message every {n} minutes
            if (Instant.now().isAfter(nextMessageTime)) {
                Exec.getInstance().sendExecReadOnlyMessage(serviceMsg, null);
                nextMessageTime = Instant.now().plusSeconds(120);
            }
        }

        // compare pack names - if there is a mismatch, consult the operator.
        // If the operator is upset about it, un-assign the unit from the run and post appropriate status.
        var currentPackName = nodeInfo.getMediaInfo().getMediaName();
        if (currentPackName != null && !currentPackName.equals(packName)) {
            var candidates = new String[]{ "Y", "N" };
            var msg = String.format("Allow %s as substitute pack on %s YN?", currentPackName, nodeInfo.getNode().getNodeName());
            var response = Exec.getInstance().sendExecRestrictedReadReplyMessage(msg, candidates, null);
            return response.equals("Y");
        }

        return true;
    }

    private FileSpecification resolveQualifier(
        final FileSpecification initialSpec,
        final RunControlEntry runControlEntry
    ) {
        return new FileSpecification(runControlEntry.getEffectiveQualifier(initialSpec),
                                     initialSpec.getFilename(),
                                     initialSpec.getFileCycleSpecification(),
                                     initialSpec.getReadKey(),
                                     initialSpec.getWriteKey());
    }

    Channel selectRoute(final Device device) throws ExecStoppedException, NoRouteForIOException {
        var ni = _nodeGraph.get(device.getNodeIdentifier());
        if (ni == null) {
            LogManager.logFatal(LOG_SOURCE, "cannot find NodeInfo for %s", device.getNodeName());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        if (ni instanceof DeviceNodeInfo dni) {
            for (int cx = 0; cx < dni._routes.size(); cx++) {
                var chan = dni._routes.pop();
                dni._routes.push(chan);
                var chi = _nodeGraph.get(chan.getNodeIdentifier());
                if (chi == null) {
                    LogManager.logFatal(LOG_SOURCE, "cannot find NodeInfo for %s", chan.getNodeName());
                    Exec.getInstance().stop(StopCode.FacilitiesComplex);
                    throw new ExecStoppedException();
                }

                if (chi._nodeStatus == NodeStatus.Up) {
                    return (Channel) chi._node;
                }
            }

            // if we get here, there aren't any routes
            throw new NoRouteForIOException(ni._node.getNodeIdentifier());
        } else {
            LogManager.logFatal(LOG_SOURCE, "ni is not DeviceNodeInfo for %s", device.getNodeName());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }
    }
}
