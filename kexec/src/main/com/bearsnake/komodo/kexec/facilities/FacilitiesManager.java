/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.hardwarelib.Channel;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DeviceType;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.NodeCategory;
import com.bearsnake.komodo.hardwarelib.TapeDevice;
import com.bearsnake.komodo.kexec.FileSpecification;
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

    final FacilitiesCore _core;

    // All assigned disk files are recorded here so that we can easily access and manage the file allocations.
    final HashMap<MFDRelativeAddress, FileAllocationSet> _acceleratedFileAllocations = new HashMap<>();

    // Inventory of all the hardware nodes, keyed by node identifier.
    // It is loaded at initialization(), and will remain unchanged during the application existence.
    final HashMap<Integer, NodeInfo> _nodeGraph = new HashMap<>();

    public FacilitiesManager() {
        _core = new FacilitiesCore(this);
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

        _core.loadNodeGraph();

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

        // Add facilities item to the run
        var effectiveFileSpec = resolveQualifier(fileSpecification, runControlEntry);
        var facItem = new AbsoluteDiskItem(node, packName);
        facItem.setQualifier(effectiveFileSpec.getQualifier())
               .setFilename(effectiveFileSpec.getFilename())
               .setIsTemporary(true);
        if (effectiveFileSpec.hasFileCycleSpecification()) {
            var fcSpec = effectiveFileSpec.getFileCycleSpecification();
            if (fcSpec.isAbsolute()) {
                facItem.setAbsoluteCycle(fcSpec.getCycle());
            } else if (fcSpec.isRelative()) {
                facItem.setRelativeCycle(fcSpec.getCycle());
            }
        }

        // If there is a facilities item in the rce which matches the file specification, and it does not refer
        //  to an absolute assign of this same unit, post an error and return false
        // If there is any facilities item in the rce which refers to this unit, post a warning (already assigned)
        //  otherwise add a new facilities item to the rce
        // If the filename portion of the new facilities item is not unique to the run, post a warning
        //  (filename not unique)
        var facItems = runControlEntry.getFacilityItemTable();
        synchronized (facItems) {
            for (var fi : facItems._content) {
                // TODO note special case where one activity is in the process of assigning, and another activity
                //  comes in with the same idea...
            }
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
                    var params = new Object[]{ runControlEntry.getRunId(), minutes };
                    var facMsg = new FacStatusMessageInstance(FacStatusCode.RunHeldForDiskUnitAvailability, params);
                    runControlEntry.postToPrint(facMsg.toString(), 1);
                }
            }
        }

        // TODO the bit from here to TODO - END ... can it be extracted into a core function?
        var msg = String.format("Load %s %s %s",
                                packName,
                                disk.getNodeName(),
                                runControlEntry.getRunId());
        Exec.getInstance().sendExecReadOnlyMessage(msg, null);

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
        }

        // compare pack names - if there is a mismatch, consult the operator.
        // If the operator is upset about it, un-assign the unit from the run and post appropriate status.
        var currentPackName = nodeInfo.getMediaInfo().getMediaName();
        if (currentPackName != null && !currentPackName.equals(packName)) {
            var candidates = new String[]{ "Y", "N" };
            msg = String.format("Allow %s as substitute pack on %s YN?", currentPackName, nodeInfo.getNode().getNodeName());
            var response = Exec.getInstance().sendExecRestrictedReadReplyMessage(msg, candidates, null);
            if (!response.equals("Y")) {
                // TODO lose fac item
                nodeInfo._assignedTo = null;
                var params = new String[]{ packName };
                fsResult.postMessage(FacStatusCode.OperatorDoesNotAllowAbsoluteAssign, params);
                fsResult.mergeStatusBits(0_400000_000000L);
                return false;
            }
        }
        // TODO - END

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
            // TODO [[*] [R|F] PACKID pack-id]
        } else if (ni._node instanceof TapeDevice) {
            // TODO [* RUNID run-id REEL reel [RING|NORING] [POS [*]ffff[+|-][*]bbbbbb | POS LOST]]
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

        var chan = _core.selectRoute((Device) node);
        chan.routeIo(channelProgram);
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
                        var chan = _core.selectRoute(dd);
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

                        var pi = _core.loadDiskPackInfo(dni, diskLabel);
                        if (!pi._isPrepped) {
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
}
