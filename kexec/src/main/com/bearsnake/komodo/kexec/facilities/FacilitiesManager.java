/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.hardwarelib.Channel;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.NodeCategory;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.mfd.FileAllocationSet;
import com.bearsnake.komodo.kexec.mfd.MFDRelativeAddress;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.util.HashMap;

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
     * Routes an IO described by a channel program.
     * For the case where some portion of the Exec needs to do device-specific IO.
     * @param channelProgram IO description
     */
    public void routeIo(final ChannelProgram channelProgram) throws ExecStoppedException, NoRouteForIOException {
        var nodeInfo = _nodeGraph.get(channelProgram._nodeIdentifier);
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE,
                                "Node %d from channel program is not configured",
                                channelProgram._nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if (node.getNodeCategory() != NodeCategory.Device) {
            LogManager.logFatal(LOG_SOURCE,
                                "Node %d from channel program is not a device",
                                channelProgram._nodeIdentifier);
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
                        if (cp._ioStatus != IoStatus.Complete) {
                            var msg = String.format("IO Error reading pack label on device %s", dd.getNodeName());
                            Exec.getInstance().sendExecReadOnlyMessage(msg, null);

                            LogManager.logInfo(LOG_SOURCE, "IO error device %s:%s",
                                               dd.getNodeName(),
                                               cp._ioStatus);

                            dni._nodeStatus = NodeStatus.Down;
                            msg = _core.getNodeStatusString(dd.getNodeIdentifier());
                            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
                            continue;
                        }

                        var pi = _core.loadDiskPackInfo(dni, diskLabel);
                        if (!pi._isPrepped) {
                            dni._nodeStatus = NodeStatus.Down;
                            var msg = _core.getNodeStatusString(dd.getNodeIdentifier());
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
}
