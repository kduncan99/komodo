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
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.FileCycleDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.FileSetDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;
import com.bearsnake.komodo.kexec.exec.RunType;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.facItems.AbsoluteDiskItem;
import com.bearsnake.komodo.kexec.mfd.FileCycleInfo;
import com.bearsnake.komodo.kexec.mfd.FileSetInfo;
import com.bearsnake.komodo.kexec.mfd.FileType;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class FacilitiesManager implements Manager {

    public enum DeleteBehavior {
        None,
        DeleteOnNormalRunTermination,
        DeleteOnAnyRunTermination,
    }

    public enum DirectoryOnlyBehavior {
        None,
        DirectoryOnlyMountPacks,
        DirectoryOnlyDoNotMountPacks,
    }

    static final String LOG_SOURCE = "FacMgr";

    // Inventory of all the hardware nodes, keyed by node identifier.
    // It is loaded at initialization(), and will remain unchanged during the application existence.
    final HashMap<Integer, NodeInfo> _nodeGraph = new HashMap<>();

    public FacilitiesManager() {
        Exec.getInstance().managerRegister(this);
    }

    /*
        Console messages TODO
        NO PATH AVAILABLE FOR DEVICE device
        NOT ALL FIXED DEVICES RECOVERED - CONTINUE? YN
        No usable path found to device
        File SYS$*RLIB$ not properly catalogued
        File SYS$*LIB$ not properly catalogued
        File SYS$*RUN$ not properly catalogued
        dir-id pack-id TO BECOME FIXED YN?
        (dir-id FIXED PACK MOUNTED ON device IGNORED)
     */

    // -------------------------------------------------------------------------
    // Manager interface
    // -------------------------------------------------------------------------

    @Override
    public void boot(final boolean recoveryBoot) {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);

        // update verbosity of nodes
        for (var ni : _nodeGraph.values()) {
            ni.getNode().setLogIos(Exec.getInstance().getConfiguration().getLogIos());
        }

        // clear cached disk labels
        for (var ni : _nodeGraph.values()) {
            ni.setMediaInfo(null);
        }

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    @Override
    public synchronized void dump(final PrintStream out,
                                  final String indent,
                                  final boolean verbose) {
        out.printf("%sFacilitiesManager ********************************\n", indent);

        out.printf("%s  Node Graph:\n", indent);
        var subIndent = indent + "    ";
        for (var ni : _nodeGraph.values()) {
            out.printf("%s%s\n", subIndent, ni.toString());
        }
    }

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");

        loadNodeGraph();

        // set up routes
        for (var ni : _nodeGraph.values()) {
            if (ni instanceof ChannelNodeInfo) {
                var chNode = (Channel) ni.getNode();
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

    public boolean assignCatalogedDiskFileToExec(
        final FileSpecification fileSpecification,
        final boolean exclusiveUse,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "assignCatalogedDiskFileToExec %s", fileSpecification.toString());

        var result = assignCatalogedDiskFileToRun(Exec.getInstance().getRunControlEntry(),
                                                  fileSpecification,
                                                  null,
                                                  null,
                                                  null,
                                                  DeleteBehavior.None,
                                                  DirectoryOnlyBehavior.None,
                                                  false,
                                                  true,
                                                  false,
                                                  exclusiveUse,
                                                  false,
                                                  false,
                                                  fsResult);

        LogManager.logTrace(LOG_SOURCE, "assignCatalogedFileToExec result:%s", fsResult.toString());
        return result;
    }

    public synchronized boolean assignCatalogedDiskFileToRun(
        final RunControlEntry runControlEntry,
        final FileSpecification fileSpecification,
        final Integer initialReserve,  // null if not specified, attempt to change existing value
        final Granularity granularity, // null if not specified, must match existing file otherwise
        final Integer maxGranules,     // null if not specified, attempt to change existing value
        final DeleteBehavior deleteBehavior,               // D/K options
        final DirectoryOnlyBehavior directoryOnlyBehavior, // E/Y options
        final boolean saveOnCheckpoint,                    // M option
        final boolean assignIfDisabled,                    // Q option
        final boolean readOnly,                            // R option
        final boolean exclusiveUse,                        // X option
        final boolean releaseOnTaskEnd,                    // I option
        final boolean doNotHoldRun,                        // Z option
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "assignCatalogedDiskFileToRun %s %s",
                            runControlEntry.getRunId(),
                            fileSpecification.toString());

        var mm = Exec.getInstance().getMFDManager();
        FileSetInfo fsInfo;
        try {
            fsInfo = mm.getFileSetInfo(fileSpecification.getQualifier(), fileSpecification.getFilename());
        } catch (FileSetDoesNotExistException ex) {
            fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
            fsResult.mergeStatusBits(0_400010_000000L);
            return false;
        }

        if ((fsInfo.getFileType() != FileType.Fixed) && (fsInfo.getFileType() != FileType.Removable)) {
            fsResult.postMessage(FacStatusCode.AttemptToChangeGenericType);
            fsResult.mergeStatusBits(0_420000_000000L);
            return false;
        }

        if (!checkKeys(runControlEntry, fsInfo, fileSpecification, fsResult)) {
            return false;
        }

        // Is this an absolute file cycle request?
        if (fileSpecification.hasFileCycleSpecification() && fileSpecification.getFileCycleSpecification().isAbsolute()) {
            // Go get the file cycle info.
            FileCycleInfo fcInfo;
            try {
                fcInfo = mm.getFileCycleInfo(fileSpecification.getQualifier(),
                                             fileSpecification.getFilename(),
                                             fileSpecification.getFileCycleSpecification().getCycle());
            } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                // we already checked for file set not existing, but we have to catch it here anyway.
                fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
                fsResult.mergeStatusBits(0_400010_000000L);
                return false;
            }

            // Check the existing facility items to see if this file cycle is already assigned to this run.
            // If it is,
            //   change any option-driven settings if necessary
            //   post already-assigned.
            // Otherwise
            //   check public/private
            //   make sure we're not being asked to change anything in a funny way
            //   create new fac item with option-driven settings as necessary
        } else {
            // It's either relative or unspecified. If unspecified, we treat it like relative +0.
            // Check the fac items to see if the file is already assigned with this relative cycle number.
            // If so,
            //   change any option-driven settings if necessary
            //   post already-assigned.
            // Otherwise, get the absolute cycle from the fcInfo, and go through the fac items again.
            // If it is assigned (comparing the absolute cycles)
            //   with a different relative cycle, post f-cycle conflict and stop
            //   else with no relative cycle, add this relative cycle to that fac item
            //     change any option-driven settings if necessary
            //     post already-assigned.
            // Otherwise
            //   check public/private (as above)
            //   make sure we're not being asked to change anything in a funny way (as above)
            //   create new fac item with option-driven settings as necessary
        }

        // Now we have a fac item (either pre- or newly-existing).
        // Does there need to be a hold put in place? If so, do it.
        // Is there already a hold in place? If so, wait on it.

        // Done waiting for the thing - accelerate it via MFD (it should not have been yet).

                /*
E:241433 Attempt to change assign mnemonic.
E:241533 Illegal attempt to change assignment type.
E:241633 Attempt to change generic type of the file.
E:241733 Attempt to change granularity.
E:242033 Attempt to change initial reserve of write inhibited file.
E:242133 Attempt to change maximum granules of a file cataloged under a different account.
E:242233 Attempt to change maximum granules on a write inhibited file.
         */

        LogManager.logTrace(LOG_SOURCE, "assignCatalogedFileToRun %s result:%s",
                            runControlEntry.getRunId(),
                            fsResult.toString());
        return true;
    }

    /**
     * For assigning any disk to the exec. Possibly only used by facilities manager...?
     * The device must not be assigned to any run (other than the EXEC) and it must be ready.
     * @param nodeIdentifier node identifier of the device
     * @param fsResult fac status result
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public boolean assignDiskUnitToExec(
        final int nodeIdentifier,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToExec node:%d", nodeIdentifier);

        var nodeInfo = _nodeGraph.get(nodeIdentifier);
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToExec() Cannot find node %012o", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if ((node.getNodeCategory() != NodeCategory.Device) || ((Device) node).getDeviceType() != DeviceType.DiskDevice) {
            LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToExec() Node %012o is not a disk device", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var dev = (Device) node;
        var execRCE = Exec.getInstance().getRunControlEntry();
        synchronized (nodeInfo) {
            if (!dev.isReady()) {
                LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToExec() Node %012o is not a disk device", nodeIdentifier);
                Exec.getInstance().stop(StopCode.FacilitiesComplex);
                throw new ExecStoppedException();
            }
            var currentRCE = nodeInfo.getAssignedTo();
            if ((currentRCE != null) && (currentRCE != execRCE)) {
                LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToExec() Node %012o is already assigned", nodeIdentifier);
                Exec.getInstance().stop(StopCode.FacilitiesComplex);
                throw new ExecStoppedException();
            }
            nodeInfo.setAssignedTo(execRCE);
        }

        var packInfo = nodeInfo.getMediaInfo();
        var facItem = new AbsoluteDiskItem(node, packInfo.getMediaName());
        facItem.setIsAssigned(true)
               .setAbsoluteCycle(0)
               .setQualifier(execRCE.getDefaultQualifier())
               .setFilename("UNIT$" + node.getNodeName())
               .setIsTemporary(true);

        var fit = execRCE.getFacilityItemTable();
        synchronized (fit) {
            fit.addFacilitiesItem(facItem);
        }

        LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToExec %s result:%s", node.getNodeName(), fsResult.toString());
        return true;
    }

    /**
     * For assigning a (reserved) disk to a run.
     * This assignment can only be temporary, and the device must be reserved.
     * @param runControlEntry describes the run
     * @param fileSpecification describes the file name so that we can figure out the correct qualifier to use
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
        LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun %s %s node:%d pack:%s I:%s Z:%s",
                            runControlEntry.getRunId(),
                            fileSpecification.toString(),
                            nodeIdentifier,
                            packName,
                            releaseOnTaskEnd,
                            doNotHoldRun);

        var nodeInfo = _nodeGraph.get(nodeIdentifier);
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToRun() Cannot find node %012o", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if ((node.getNodeCategory() != NodeCategory.Device) || ((Device) node).getDeviceType() != DeviceType.DiskDevice) {
            LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToRun() Node %012o is not a disk device", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var disk = (DiskDevice) node;
        if (nodeInfo.getNodeStatus() != NodeStatus.Reserved) {
            var params = new String[]{nodeInfo.getNode().getNodeName()};
            fsResult.postMessage(FacStatusCode.UnitIsNotReserved, params);
            fsResult.mergeStatusBits(0_600000_000000L);
            return false;
        }

        // TODO check requested pack name - if it is already assigned to this run, reject the request

        // Create an appropriate facilities item.
        var facItem = new AbsoluteDiskItem(node, packName);
        facItem.setQualifier(fileSpecification.getQualifier())
               .setFilename(fileSpecification.getFilename())
               .setIsTemporary(true)
               .setReleaseOnTaskEnd(releaseOnTaskEnd);
        if (fileSpecification.hasFileCycleSpecification()) {
            var fcSpec = fileSpecification.getFileCycleSpecification();
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
                    if (facTable.getExactFacilitiesItem(fileSpecification) != null) {
                        fsResult.postMessage(FacStatusCode.IllegalAttemptToChangeAssignmentType);
                        fsResult.mergeStatusBits(0_400000_000000L);
                        return false;
                    }
                }

                if (fi instanceof AbsoluteDiskItem adi) {
                    if (adi._node == node) {
                        var params = new String[]{node.getNodeName()};
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
        while (true) {
            if (!Exec.getInstance().isRunning()) {
                throw new ExecStoppedException();
            }
            runControlEntry.incrementWaitingForPeripheral();
            synchronized (nodeInfo) {
                if (nodeInfo.getAssignedTo() == null) {
                    nodeInfo.setAssignedTo(runControlEntry);
                    facItem.setIsAssigned(true);
                    runControlEntry.decrementWaitingForPeripheral();
                    break;
                }
            }

            if (!doNotHoldRun) {
                break;
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

        if (!facItem.isAssigned()) {
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun promptLoadPack returns false");
            // z-option bail-out
            facItems.removeFacilitiesItem(facItem);
            fsResult.postMessage(FacStatusCode.HoldForDiskUnitRejected);
            fsResult.mergeStatusBits(0_400001_000000L);
            return false;
        }

        if (!promptLoadPack(runControlEntry, nodeInfo, disk, packName)) {
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun promptLoadPack returns false");
            facItems.removeFacilitiesItem(facItem);
            nodeInfo.setAssignedTo(null);
            var params = new String[]{packName};
            fsResult.postMessage(FacStatusCode.OperatorDoesNotAllowAbsoluteAssign, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun %s %s result:%s",
                                runControlEntry.getRunId(),
                                node.getNodeName(),
                                fsResult.toString());
            return false;
        }

        var packInfo = (PackInfo) nodeInfo.getMediaInfo();
        // TODO do we really want to restrict from using fixed here?
        if (packInfo.isFixed() && (runControlEntry.getRunType() != RunType.Exec)) {
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun pack is fixed");
            facItems.removeFacilitiesItem(facItem);
            nodeInfo.setAssignedTo(null);
            var params = new String[]{packName};
            fsResult.postMessage(FacStatusCode.DeviceIsFixed, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun %s %s result:%s",
                                runControlEntry.getRunId(),
                                node.getNodeName(),
                                fsResult.toString());
            return false;
        }

        LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun %s %s result:%s",
                            runControlEntry.getRunId(),
                            node.getNodeName(),
                            fsResult.toString());
        return true;
    }

    /**
     * Returns the facilities NodeInfo for the given node.
     * @param nodeName name of the node - we'd prefer it to be uppercase, but whatever...
     * @return NodeInfo of the node if it is configured, else null
     */
    public NodeInfo getNodeInfo(
        final String nodeName
    ) {
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> ni.getNode().getNodeName().equalsIgnoreCase(nodeName))
                         .findFirst()
                         .orElse(null);

    }

    /**
     * Returns a collection of NodeInfo objects of all the nodes of a particular category
     * @param category category of interest
     * @return collection
     */
    public Collection<NodeInfo> getNodeInfos(
        final NodeCategory category
    ) {
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> ni.getNode().getNodeCategory() == category)
                         .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Returns a collection of NodeInfo objects of all the device nodes of a particular type
     * @param deviceType device type of interest
     * @return collection
     */
    public Collection<NodeInfo> getNodeInfos(
        final DeviceType deviceType
    ) {
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
    public Collection<NodeInfo> getNodeInfosForChannel(
        final ChannelNodeInfo channelInfo
    ) {
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
     * @return node status string
     * @throws ExecStoppedException If the exec stops during this function
     */
    public String getNodeStatusString(
        final int nodeIdentifier
    ) throws ExecStoppedException {
        var ni = _nodeGraph.get(nodeIdentifier);
        if (ni == null) {
            LogManager.logFatal(LOG_SOURCE, "getNodeStatusString nodeIdentifier %d not found", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var sb = new StringBuilder();
        sb.append(ni.getNode().getNodeName()).append("     ").setLength(6);
        sb.append(" ").append(ni.getNodeStatus().getDisplayString());
        sb.append(isDeviceAccessible(nodeIdentifier) ? "   " : " NA");

        if (ni.getNode() instanceof DiskDevice) {
            // [[*] [R|F] PACKID pack-id]
            sb.append(ni.getAssignedTo() == null ? "  " : " *");
            if (ni.getMediaInfo() instanceof PackInfo pi) {
                if (pi.isFixed()) {
                    sb.append(" F");
                } else if (pi.isRemovable()) {
                    sb.append(" R");
                } else {
                    sb.append("  ");
                }

                sb.append(" PACKID ").append(pi.getPackName());
            }
        } else if (ni.getNode() instanceof TapeDevice) {
            // [* RUNID run-id] [REEL reel [RING|NORING] [POS [*]ffff[+|-][*]bbbbbb | POS LOST]]]
            // First part is printed if the drive is assigned
            // Second part is printed if a reel is mounted (could be not assigned, in the case of pre-mount)
            if (ni.getAssignedTo() != null) {
                var runId = ni.getAssignedTo().getRunId();
                sb.append("* RUNID ").append(String.format("%-6s", runId)).append(" ");
            }
            if (ni.getMediaInfo() instanceof VolumeInfo vi) {
                sb.append("REEL ").append(String.format("%-6s", vi.getVolumeName())).append(" ");
                // TODO RING/NORING which we get from the facitem (if assigned), and POS which we get from the device
            }
        }

        return sb.toString();
    }

    /**
     * Indicates whether the device with the given identifier is accessible
     * (it has at least one channel path for which the channel is not DN).
     * @param nodeIdentifier identifier of the device
     * @return true if the device is accessible
     */
    public boolean isDeviceAccessible(
        final int nodeIdentifier
    ) {
        var ni = _nodeGraph.get(nodeIdentifier);
        if (ni instanceof DeviceNodeInfo dni) {
            for (var chan : dni._routes) {
                var cni = _nodeGraph.get(chan.getNodeIdentifier());
                if (cni != null && cni.getNodeStatus() == NodeStatus.Up) {
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
     * @throws ExecStoppedException if the exec stops during this function
     * @throws NoRouteForIOException if the destination device has no available path
     */
    public void routeIo(
        final ChannelProgram channelProgram
    ) throws ExecStoppedException, NoRouteForIOException {
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
    public void startup() throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "startup()");

        var e = Exec.getInstance();
        dropTapes();
        readDiskLabels();
        var fixedDisks = getAccessibleFixedDisks();
        if (fixedDisks.isEmpty()) {
            e.sendExecReadOnlyMessage("No Fixed Disk Configured", null);
            e.stop(StopCode.InitializationSystemConfigurationError);
            throw new ExecStoppedException();
        }

        var mm = Exec.getInstance().getMFDManager();
        if (Exec.getInstance().isJumpKeySet(13)) {
            mm.initializeMassStorage(fixedDisks);
        } else {
            mm.recoverMassStorage(fixedDisks);
        }

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    // -------------------------------------------------------------------------
    // Core
    // -------------------------------------------------------------------------

    /**
     * Checks the read/write keys provided (or not) in a FileSpecification object
     * against the keys which do (or do not) exist in the FileSetInfo object.
     * Posts any fac results necessary, aborts the run if necessary, etc.
     * @return true if no problems exist, else false
     */
    private boolean checkKeys(
        final RunControlEntry rce,
        final FileSetInfo fsInfo,
        final FileSpecification fileSpec,
        final FacStatusResult fsResult
    ) {
        var err = false;
        var postedReadWrite = false;
        var existingReadKey = fsInfo.getReadKey();
        var hasReadKey = !existingReadKey.isEmpty();
        var givenReadKey = fileSpec.getReadKey();
        var gaveReadKey = givenReadKey != null;
        if (hasReadKey) {
            if (!gaveReadKey && (!rce.isPrivileged() || fsInfo.isGuarded())) {
                fsResult.postMessage(FacStatusCode.ReadWriteKeysNeeded);
                fsResult.mergeStatusBits(0_600000_000000L);
                postedReadWrite = true;
                err = true;
            } else if (!existingReadKey.equalsIgnoreCase(givenReadKey)) {
                fsResult.postMessage(FacStatusCode.IncorrectReadKey);
                fsResult.mergeStatusBits(0_401000_000000L);
                if (rce.hasTask()) {
                    rce.postContingency(017, 0, 0, 015);
                }
                err = true;
            }
        } else {
            if (gaveReadKey) {
                fsResult.postMessage(FacStatusCode.FileNotCatalogedWithReadKey);
                fsResult.mergeStatusBits(0_400040_000000L);
                if (rce.hasTask()) {
                    rce.postContingency(017, 0, 0, 015);
                }
                err = true;
            }
        }

        var existingWriteKey = fsInfo.getWriteKey();
        var hasWriteKey = !existingWriteKey.isEmpty();
        var givenWriteKey = fileSpec.getWriteKey();
        var gaveWriteKey = givenWriteKey != null;
        if (hasWriteKey) {
            if (!gaveWriteKey && (!rce.isPrivileged() || fsInfo.isGuarded())) {
                if (!postedReadWrite) {
                    fsResult.postMessage(FacStatusCode.ReadWriteKeysNeeded);
                    fsResult.mergeStatusBits(0_600000_000000L);
                }
                err = true;
            } else if (!existingWriteKey.equalsIgnoreCase(givenWriteKey)) {
                fsResult.postMessage(FacStatusCode.IncorrectWriteKey);
                fsResult.mergeStatusBits(0_400400_000000L);
                if (rce.hasTask()) {
                    rce.postContingency(017, 0, 0, 015);
                }
                err = true;
            }
        } else {
            if (gaveWriteKey) {
                fsResult.postMessage(FacStatusCode.FileNotCatalogedWithWriteKey);
                fsResult.mergeStatusBits(0_400020_000000L);
                if (rce.hasTask()) {
                    rce.postContingency(017, 0, 0, 015);
                }
                err = true;
            }
        }

        return !err;
    }

    /**
     * Unloads all the ready tape devices - used during boot.
     * @throws ExecStoppedException if something goes wrong while we're doing this.
     */
    private void dropTapes() throws ExecStoppedException {
        for (var ni : _nodeGraph.values()) {
            if ((ni instanceof DeviceNodeInfo dni)
                && (ni.getNode() instanceof TapeDevice td)
                && td.isReady()) {
                // TODO set up and route channel program to unmount the tape device
            }
        }
    }

    /**
     * Retrieves a collection of NodeInfo objects representing UP and SU disk units.
     * Also assigns the units to the Exec.
     */
    private Collection<NodeInfo> getAccessibleFixedDisks() throws ExecStoppedException {
        var list = new LinkedList<NodeInfo>();
        for (var ni : _nodeGraph.values()) {
            var node = ni.getNode();
            if ((node instanceof DiskDevice)
                && ((ni.getNodeStatus() == NodeStatus.Up) || (ni.getNodeStatus() == NodeStatus.Suspended))) {
                list.add(ni);
                var fsResult = new FacStatusResult();
                assignDiskUnitToExec(node.getNodeIdentifier(), fsResult);
            }
        }
        return list;
    }

    /**
     * Creates and populates PackInfo based on the ArraySlice containing the label for a disk pack
     */
    private PackInfo loadDiskPackInfo(
        final NodeInfo diskNodeInfo
    ) throws NoRouteForIOException, ExecStoppedException {
        var diskDevice = (DiskDevice) diskNodeInfo.getNode();
        var diskLabel = readPackLabel(diskDevice);
        if (diskLabel == null) {
            var msg = getNodeStatusString(diskDevice.getNodeIdentifier());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        var initialDirTrack = readInitialDirectoryTrack(diskDevice, diskLabel);
        var pi = PackInfo.loadFromLabel(diskLabel, initialDirTrack);
        if (pi == null) {
            var msg = String.format("Pack on %s has no label", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!Exec.isValidPackName(pi.getPackName())) {
            var msg = String.format("Pack on %s has an invalid pack name", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!Exec.isValidPrepFactor(pi.getPrepFactor())) {
            var msg = String.format("Pack on %s has an invalid prep factor: %d",
                                    diskDevice.getNodeName(),
                                    pi.getPrepFactor());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!pi.isPrepped()) {
            var msg = String.format("Pack on %s is not prepped", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        var sb = new StringBuilder();
        sb.append(pi.getPackName()).append(" on ").append(diskDevice.getNodeName()).append(" is ");
        if (pi.isFixed()) {
            sb.append("FIXED ");
            if (pi.getLDATIndex() == 0) {
                sb.append("INIT");
            } else {
                sb.append(pi.getLDATIndex());
            }
        } else if (pi.isRemovable()) {
            sb.append("REM ");
            if (pi.getLDATIndex() == 0) {
                sb.append("INIT");
            } else {
                sb.append(pi.getLDATIndex());
            }
        } else {
            sb.append("not prepped");
        }

        sb.append(" PREP FACTOR ").append(pi.getPrepFactor());
        Exec.getInstance().sendExecReadOnlyMessage(sb.toString(), null);

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

        // Read pack label
        try {
            var label = readPackLabel(disk);
            if (label == null) {
                return false;
            }
            var info = loadDiskPackInfo(nodeInfo);
            if (info != null) {
                nodeInfo.setMediaInfo(info);
            }
        } catch (NoRouteForIOException ex) {
            return false;
        }

        // compare pack names - if there is a mismatch, consult the operator.
        // If the operator is upset about it, un-assign the unit from the run and post appropriate status.
        var currentPackName = nodeInfo.getMediaInfo().getMediaName();
        if (currentPackName != null && !currentPackName.equals(packName)) {
            var candidates = new String[]{ "Y", "N" };
            var msg = String.format("Allow %s as substitute pack on %s YN?", currentPackName, nodeInfo.getNode().getNodeName());
            var response = Exec.getInstance().sendExecRestrictedReadReplyMessage(msg, candidates, null);
            return response.equalsIgnoreCase("Y");
        }

        return true;
    }

    /**
     * Reads the labels and directory tracks for all UP and SU disks.
     * Used early in booting.
     * @throws ExecStoppedException if something goes wrong while we're doing this
     */
    private void readDiskLabels() throws ExecStoppedException {
        for (var ni : _nodeGraph.values()) {
            if ((ni.getNodeStatus() == NodeStatus.Up) || (ni.getNodeStatus() == NodeStatus.Suspended)) {
                if ((ni instanceof DeviceNodeInfo dni) && (ni.getNode() instanceof DiskDevice dd)) {
                    try {
                        var info = loadDiskPackInfo(dni);
                        if (info != null) {
                            ni.setMediaInfo(info);
                        }
                    } catch (NoRouteForIOException ex) {
                        LogManager.logInfo(LOG_SOURCE, "No route to device %s", dd.getNodeName());
                    }
                }
            }
        }
    }

    private ArraySlice readInitialDirectoryTrack(
        final DiskDevice disk,
        final ArraySlice diskLabel
    ) throws NoRouteForIOException, ExecStoppedException {
        var dirTrackAddr = diskLabel.get(3);
        var blocksPerTrack = Word36.getH1(diskLabel.get(4));
        var dirBlockAddr = dirTrackAddr * blocksPerTrack;
        var channel = selectRoute(disk);
        var cw = new ChannelProgram.ControlWord().setDirection(ChannelProgram.Direction.Increment)
                                                 .setBuffer(new ArraySlice(new long[1792]))
                                                 .setTransferCount(1792);
        var cp = new ChannelProgram().addControlWord(cw)
                                     .setFunction(ChannelProgram.Function.Read)
                                     .setBlockId(dirBlockAddr)
                                     .setNodeIdentifier(disk.getNodeIdentifier());
        channel.routeIo(cp);
        if (cp.getIoStatus() != IoStatus.Complete) {
            LogManager.logError(LOG_SOURCE, "readPackLabel ioStatus=%s", cp.getIoStatus());
            var msg = String.format("%s Cannot read directory track %s", disk.getNodeName(), cp.getIoStatus());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            return null;
        }

        return cw.getBuffer();
    }

    private ArraySlice readPackLabel(
        final DiskDevice disk
    ) throws NoRouteForIOException, ExecStoppedException {
        var channel = selectRoute(disk);
        var cw = new ChannelProgram.ControlWord().setDirection(ChannelProgram.Direction.Increment)
                                                 .setBuffer(new ArraySlice(new long[28]))
                                                 .setTransferCount(28);
        var cp = new ChannelProgram().addControlWord(cw)
                                     .setFunction(ChannelProgram.Function.Read)
                                     .setBlockId(0)
                                     .setNodeIdentifier(disk.getNodeIdentifier());
        channel.routeIo(cp);
        if (cp.getIoStatus() != IoStatus.Complete) {
            LogManager.logError(LOG_SOURCE, "readPackLabel ioStatus=%s", cp.getIoStatus());
            var msg = String.format("%s Cannot read pack label %s", disk.getNodeName(), cp.getIoStatus());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            return null;
        }

        return cw.getBuffer();
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

                if (chi.getNodeStatus() == NodeStatus.Up) {
                    return (Channel) chi.getNode();
                }
            }

            // if we get here, there aren't any routes
            throw new NoRouteForIOException(ni.getNode().getNodeIdentifier());
        } else {
            LogManager.logFatal(LOG_SOURCE, "ni is not DeviceNodeInfo for %s", device.getNodeName());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }
    }
}
