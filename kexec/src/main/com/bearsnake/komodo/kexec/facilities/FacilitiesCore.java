/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.hardwarelib.Channel;
import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DiskChannel;
import com.bearsnake.komodo.hardwarelib.FileSystemDiskDevice;
import com.bearsnake.komodo.hardwarelib.TapeChannel;
import com.bearsnake.komodo.kexec.HardwareTrackId;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.mfd.MFDRelativeAddress;
import com.bearsnake.komodo.logger.LogManager;

public class FacilitiesCore {

    private static final String LOG_SOURCE = FacilitiesManager.LOG_SOURCE;

    private final FacilitiesManager _mgr;

    public FacilitiesCore(FacilitiesManager manager) {
        _mgr = manager;
    }

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
        var fa = _mgr._acceleratedFileAllocations.get(mainItem0Address);
        if (fa == null) {
            LogManager.logFatal(LOG_SOURCE, "mainItem0 is not accelerated");
            Exec.getInstance().stop(StopCode.DirectoryErrors);
            throw new ExecStoppedException();
        }

        var hwTid = fa.resolveFileRelativeTrackId(fileTrackId);
        LogManager.logTrace(LOG_SOURCE, "returning %s", hwTid);
        return hwTid;
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

        var tape0 = new FileSystemDiskDevice("TAPE0");
        var tape1 = new FileSystemDiskDevice("TAPE1");

        var tch = new TapeChannel("CHTAPE");
        tch.attach(tape0);
        tch.attach(tape1);

        _mgr._nodeGraph.put(dch0.getNodeIdentifier(), new ChannelNodeInfo(dch0));
        _mgr._nodeGraph.put(dch1.getNodeIdentifier(), new ChannelNodeInfo(dch1));
        _mgr._nodeGraph.put(tch.getNodeIdentifier(), new ChannelNodeInfo(tch));

        _mgr._nodeGraph.put(disk0.getNodeIdentifier(), new DeviceNodeInfo(disk0));
        _mgr._nodeGraph.put(disk1.getNodeIdentifier(), new DeviceNodeInfo(disk1));
        _mgr._nodeGraph.put(disk2.getNodeIdentifier(), new DeviceNodeInfo(disk2));
        _mgr._nodeGraph.put(disk3.getNodeIdentifier(), new DeviceNodeInfo(disk3));
        _mgr._nodeGraph.put(tape0.getNodeIdentifier(), new DeviceNodeInfo(tape0));
        _mgr._nodeGraph.put(tape1.getNodeIdentifier(), new DeviceNodeInfo(tape1));
        // end temporary code
    }

    private Channel selectRoute(final Device device) throws ExecStoppedException, NoRouteForIOException {
        var ni = _mgr._nodeGraph.get(device.getNodeIdentifier());
        if (ni == null) {
            LogManager.logFatal(LOG_SOURCE, "cannot find NodeInfo for %s", device.getNodeName());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        if (ni instanceof DeviceNodeInfo dni) {
            for (int cx = 0; cx < dni._routes.size(); cx++) {
                var chan = dni._routes.pop();
                dni._routes.push(chan);
                var chi = _mgr._nodeGraph.get(chan.getNodeIdentifier());
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
