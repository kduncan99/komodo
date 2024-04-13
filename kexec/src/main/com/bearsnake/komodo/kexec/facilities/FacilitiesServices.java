/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.kexec.apis.IFacilitiesServices;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.logger.LogManager;

public class FacilitiesServices implements IFacilitiesServices {

    private static final String LOG_SOURCE = FacilitiesManager.LOG_SOURCE;

    private final FacilitiesManager _mgr;

    public FacilitiesServices(FacilitiesManager manager) {
        _mgr = manager;
    }

    @Override
    public boolean isValidPackName(final String packName) {
        if (packName.isEmpty() || packName.length() > 6) {
            return false;
        }

        for (var ch : packName.getBytes()) {
            if ((ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9')) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isValidPrepFactor(int value) {
        return (value == 28)
            || (value == 56)
            || (value == 112)
            || (value == 224)
            || (value == 448)
            || (value == 1792);
    }

    @Override
    public void startup() throws KExecException {
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
        for (var ni : _mgr._nodeGraph.values()) {
            if ((ni._nodeStatus == NodeStatus.Up)
                || (ni._nodeStatus == NodeStatus.Suspended)
                || ni._nodeStatus == NodeStatus.Reserved) {
                if ((ni instanceof DeviceNodeInfo dni) && (ni._node instanceof DiskDevice dd)) {
                    try {
                        var chan = _mgr._core.selectRoute(dd);
                        cp.setNodeIdentifier(dd.getNodeIdentifier());
                        chan.routeIo(cp);
                        if (cp._ioStatus != IoStatus.Complete) {
                            var msg = String.format("IO Error reading pack label on device %s", dd.getNodeName());
                            Exec.getInstance().sendExecReadOnlyMessage(msg, null);

                            LogManager.logInfo(LOG_SOURCE, "IO error device %s:%s",
                                               dd.getNodeName(),
                                               cp._ioStatus);

                            dni._nodeStatus = NodeStatus.Down;
                            msg = _mgr._core.getNodeStatusString(dd.getNodeIdentifier());
                            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
                            continue;
                        }

                        var pi = _mgr._core.loadDiskPackInfo(dni, diskLabel);
                        if (!pi._isPrepped) {
                            dni._nodeStatus = NodeStatus.Down;
                            var msg = _mgr._core.getNodeStatusString(dd.getNodeIdentifier());
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
