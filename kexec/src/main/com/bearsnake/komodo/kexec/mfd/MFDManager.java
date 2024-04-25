/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.HardwareTrackId;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.kexec.facilities.PackInfo;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class MFDManager implements Manager {

    static final String LOG_SOURCE = "MFDMgr";
    public static final long INVALID_LINK = 0_400000_000000L;

    // This is in-core MFD. Each track is keyed with the MFD relative address of sector 0 of the directory track.
    private final HashMap<MFDRelativeAddress, ArraySlice> _cachedMFDTracks = new HashMap<>();

    private final HashMap<String, MFDRelativeAddress> _fileMainItemLookupTable = new HashMap<>();

    // This is the MFD sector free list. It's a tree set to make debugging slightly easier.
    private final TreeSet<MFDRelativeAddress> _freeMFDSectors = new TreeSet<>();

    // This is the Logical Device Address Table which maps LDAT index to a fac mgr NodeInfo object
    private final ConcurrentHashMap<Integer, NodeInfo> _logicalDATable = new ConcurrentHashMap<>();
    private int _fixedPackCount;

    // This is a list of MFD-relative addresses of sectors which are the first sector in any physical block
    // which contains at least one dirty sector. For example if there are 4 sectors in a physical block,
    // and the 2nd and 3rd sectors in a particular block are dirty, this list will contain the address of
    // the 1st sector in the block. We do this for I/O efficiency, but it does mean that we need to know the
    // physical block size for each pack we manage.
    private final HashSet<MFDRelativeAddress> _dirtyCacheBlocks = new HashSet<>();

    private FileAllocationSet _mfdFas; // TODO this is temporary until we get accelerated table going.

    public MFDManager() {
        Exec.getInstance().managerRegister(this);
    }

    // -------------------------------------------------------------------------
    // Manager interface
    // -------------------------------------------------------------------------

    @Override
    public void boot(final boolean recoveryBoot) {
        LogManager.logTrace(LOG_SOURCE, "boot(%d) - nothing to do", recoveryBoot);
    }

    @Override
    public synchronized void dump(final PrintStream out,
                                  final String indent,
                                  final boolean verbose) {
        out.printf("%sMFDManager ********************************\n", indent);

        out.printf("%s  LDATable:\n", indent);
        for (var entry : _logicalDATable.entrySet()) {
            out.printf("%s    %04o: %s\n", indent, entry.getKey(), entry.getValue().getNode().getNodeName());
        }

        if (verbose) {
            // File main item lookup table
            out.printf("%s  File MainItem lookup table:\n", indent);
            for (var e : _fileMainItemLookupTable.entrySet()) {
                out.printf("%s    %s:  %s\n", indent, e.getKey(), e.getValue().toString());
            }

            // MFD sectors which are in use
            out.printf("%s  In-use MFD Sectors:\n", indent);
            for (var e : _cachedMFDTracks.entrySet()) {
                var mfdTrackAddress = e.getKey();
                var trackData = e.getValue();
                var sectorAddr = new MFDRelativeAddress(mfdTrackAddress);
                for (long sector = 0; sector < 64; ++sector, sectorAddr.increment()) {
                    if (!_freeMFDSectors.contains(sectorAddr)) {
                        var prefix = String.format("%04o %04o %02o:",
                                                   sectorAddr.getLDATIndex(),
                                                   sectorAddr.getTrackId(),
                                                   sectorAddr.getSectorId());
                        var wbase = (int) (sector * 64);
                        for (int wx = 0; wx < 28; wx += 7) {
                            var sb = new StringBuilder();
                            sb.append(indent).append("    ").append(prefix);
                            for (int wy = 0; wy < 7; wy++) {
                                sb.append(String.format(" %012o", trackData.get(wbase + wx + wy)));
                            }

                            for (int wy = 0; wy < 7; wy++) {
                                sb.append(" ");
                                sb.append(String.format(Word36.toStringFromFieldata(trackData._array[wbase + wx + wy])));
                            }

                            for (int wy = 0; wy < 7; wy++) {
                                sb.append(" ");
                                sb.append(String.format(Word36.toStringFromFieldata(trackData._array[wbase + wx + wy])));
                            }

                            out.printf("%s    %s\n", indent, sb);
                            prefix = "             ";
                        }
                    }
                }
            }

            // Dirty cache blocks
            out.printf("%s  MFD addresses of 1st sector of dirty cache blocks:\n", indent);
            for (var addr : _dirtyCacheBlocks) {
                out.printf("%s    %s\n", indent, addr.toString());
            }

            // Free MFD sector list
            out.printf("%s  Free MFD Sectors:\n", indent);
            var sb = new StringBuilder();
            for (var fs : _freeMFDSectors) {
                if (sb.length() > 80) {
                    out.printf("%s    %s\n", indent, sb);
                    sb.setLength(0);
                }

                sb.append(fs.toString()).append(" ");
            }

            if (!sb.isEmpty()) {
                out.printf("%s    %s\n", indent, sb);
            }

            // MFD file allocation set
            out.printf("%s  MFD File Allocations:\n", indent);
            for (var fa : _mfdFas.getFileAllocations()) {
                out.printf("%s    MFDRegion:%s  DeviceLoc:%s\n", indent, fa.getFileRegion(), fa.getHardwareTrackId());
            }
        }
    }

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");
    }

    @Override
    public synchronized void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
    }

    // -------------------------------------------------------------------------
    // Service API
    // -------------------------------------------------------------------------

    /**
     * initializes mass storage given a collection of NodeInfo objects describing the fixed disk units
     * which are UP or SU.
     */
    public void initializeMassStorage(
        final Collection<NodeInfo> fixedDiskInfo
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "initializeMassStorage");

        var start = Instant.now();
        var e = Exec.getInstance();
        var msg = String.format("Fixed MS Devices = %d - Continue? YN", fixedDiskInfo.size());
        var allowed = new String[]{ "Y", "N" };
        var response = e.sendExecRestrictedReadReplyMessage(msg, allowed, null);
        if (!response.equals("Y")) {
            e.stop(StopCode.ConsoleResponseRequiresReboot);
            throw new ExecStoppedException();
        }

        _cachedMFDTracks.clear();
        _dirtyCacheBlocks.clear();
        _fileMainItemLookupTable.clear();
        _freeMFDSectors.clear();
        _logicalDATable.clear();
        _fixedPackCount = 0;
        _mfdFas = new FileAllocationSet();

        var ldatIndex = 1;
        for (var ni : fixedDiskInfo) {
            var packInfo = (PackInfo) ni.getMediaInfo();
            packInfo.setLDATIndex(ldatIndex);

            var fs = packInfo.getFreeSpace();
            fs.reset();
            fs.markAllocated(0, 1);
            fs.markAllocated(packInfo.getDirectoryTrackAddress() / 1792, 1);

            _logicalDATable.put(ldatIndex, ni);

            // create a cached initial directory track
            var dirTrackAddr = new MFDRelativeAddress(ldatIndex, 0, 0);
            var dirTrackArray = new long[1792];
            var dirTrack = new ArraySlice(dirTrackArray);
            _cachedMFDTracks.put(dirTrackAddr, dirTrack);
            for (var sectorId = 0; sectorId <= 077; ++sectorId) {
                _freeMFDSectors.add(new MFDRelativeAddress(ldatIndex, 0, sectorId));
            }

            packInfo.setMFDTrackCount(1);

            // populate directory track sector 0
            // First two sectors are allocated, so note that in [1].
            // DAS links to tracks 1 through 8 are invalid, as is link to next DAS.
            var sector0 = new ArraySlice(dirTrack, 0, 28);
            sector0.set(0, Word36.setH1(0, ldatIndex));
            sector0.set(01, 0_600000_000000L);
            for (int dx = 3; dx < 27; dx += 3) {
                sector0.set(dx, MFDManager.INVALID_LINK);
            }
            sector0.set(27, MFDManager.INVALID_LINK);

            // populate directory track sector 1
            // Leave +0 and +1 alone (We aren't doing HMBT/SMBT)
            // Set +2 and +3 to available tracks, +4 to pack-id
            // +5,H1 Bit35 needs to be set to indicate fixed pack
            var sector1 = new ArraySlice(dirTrack, 1, 28);
            sector1.set(2, packInfo.getTrackCount());
            sector1.set(3, packInfo.getTrackCount() - 2);
            String packId = String.format("%-6s", packInfo.getPackName());
            sector1.set(4, Word36.stringToWordFieldata(packId).getW());
            sector1.set(5, 0_400000_000000L);

            // +010,T1 is blocks per track
            // +010,S3 is version (1)
            // +010,T3 is prepfactor
            long value = Word36.setT1(0, 1792 / packInfo.getPrepFactor());
            value = Word36.setS3(value, 1);
            value = Word36.setT3(value, packInfo.getPrepFactor());
            sector1.set(010, value);

            var sector0Addr = new MFDRelativeAddress(dirTrackAddr);
            var sector1Addr = new MFDRelativeAddress(dirTrackAddr).setSectorId(1);
            _freeMFDSectors.remove(sector0Addr);
            _freeMFDSectors.remove(sector1Addr);

            markDirectorySectorDirty(sector0Addr);
            markDirectorySectorDirty(sector1Addr);

            ldatIndex++;
            _fixedPackCount++;
        }

        // TODO create MFD$$ file artifacts in the first directory track of the first disk pack
        //   we should be able to use the(a) generic allocateDirectorySector() method for this
        //   use _mfdFas for the file information (whatever that looks like)

        // TODO write the directory tracks via fac mgr using _dirtyCacheBlocks

        var elapsed = Duration.between(start, Instant.now()).get(ChronoUnit.MILLIS);
        msg = String.format("Mass Storage Initialized %d MS.", elapsed);
        e.sendExecReadOnlyMessage(msg, null);
        LogManager.logTrace(LOG_SOURCE, "initializeMassStorage exiting");
    }

    public void recoverMassStorage(
        final Collection<NodeInfo> fixedDiskInfo
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "recoverMassStorage");

        var start = Instant.now();
        var e = Exec.getInstance();
        if (fixedDiskInfo.size() != _fixedPackCount) {
            var msg = String.format("Fixed MS Devices = %d - Expected = %d - Continue? YN", fixedDiskInfo.size(), _fixedPackCount);
            var allowed = new String[]{"Y", "N"};
            var response = e.sendExecRestrictedReadReplyMessage(msg, allowed, null);
            if (!response.equals("Y")) {
                e.stop(StopCode.ConsoleResponseRequiresReboot);
                throw new ExecStoppedException();
            }
        }

        // TODO recover mass storage

        var elapsed = Duration.between(start, Instant.now()).get(ChronoUnit.MILLIS);
        var msg = String.format("Mass Storage Recovered %d MS.", elapsed);
        e.sendExecReadOnlyMessage(msg, null);
        LogManager.logTrace(LOG_SOURCE, "recoverMassStorage exiting");
    }

    // -------------------------------------------------------------------------
    // Core methods
    // -------------------------------------------------------------------------

    /**
     * Allocates a directory sector from the fixed MFD.
     * Expands the MFD if necessary.
     * @return MFD relative address of the allocated directory sector
     * @throws ExecStoppedException if something goes wrong
     */
    private synchronized MFDRelativeAddress allocateDirectorySector() throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "allocateDirectorySector()");

        if (_freeMFDSectors.isEmpty()) {
            expandDirectory();
        }

        var result = _freeMFDSectors.removeFirst();
        LogManager.logTrace(LOG_SOURCE, "allocateDirectorySector returning %0120", result);
        return result;
    }

    /**
     * Adds a directory track to the current fixed MFD.
     * Chooses the pack which has the most free space for the new directory track.
     * @throws ExecStoppedException if something goes wrong
     */
    private synchronized void expandDirectory() throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "expandDirectory");

        // Find the pack with the largest number of free tracks,
        // which has less than the maximum number of directory tracks.
        long freeTracks = 0;
        NodeInfo chosenNodeInfo = null;
        PackInfo chosenPackInfo = null;
        for (var entry : _logicalDATable.entrySet()) {
            var ni = entry.getValue();
            var pi = (PackInfo)ni.getMediaInfo();
            if (pi.isFixed() && (pi.getMFDTrackCount() < 07777) && (pi.getFreeSpace().getAvailableTrackCount() > freeTracks)) {
                chosenNodeInfo = ni;
                chosenPackInfo = pi;
                freeTracks = pi.getFreeSpace().getAvailableTrackCount();
            }
        }

        if (chosenNodeInfo == null) {
            Exec.getInstance().stop(StopCode.ExecRequestForMassStorageFailed);
            throw new ExecStoppedException();
        }

        // Record-keeping stuff
        var mfdTrackId = chosenPackInfo.getMFDTrackCount();
        var devTrackId = chosenPackInfo.getFreeSpace().allocateTrack();
        var fa = new FileAllocation(new LogicalTrackExtent(mfdTrackId, 1),
                                    new HardwareTrackId(chosenPackInfo.getLDATIndex(), devTrackId));
        _mfdFas.mergeIntoFileAllocationSet(fa);

        var dirTrackAddr = new MFDRelativeAddress(chosenPackInfo.getLDATIndex(), mfdTrackId, 0);
        var dirTrack = new ArraySlice(new long[1792]);
        _cachedMFDTracks.put(dirTrackAddr, dirTrack);

        for (int sectorId = 0; sectorId <= 077; ++sectorId) {
            var sectorAddr = new MFDRelativeAddress(chosenPackInfo.getLDATIndex(), mfdTrackId, sectorId);
            _freeMFDSectors.add(sectorAddr);
            markDirectorySectorDirty(sectorAddr);
        }

        chosenPackInfo.setMFDTrackCount(mfdTrackId + 1);

        // DAS stuff
        var mod = mfdTrackId % 9;
        if (mod == 0) {
            // this track has a DAS in sector 0.
            // Link the new DAS to the most recent existing previous DAS.
            var prevDasTrackId = mfdTrackId - 9;
            var prevDasAddr = new MFDRelativeAddress(dirTrackAddr).setTrackId(prevDasTrackId);
            var prevDas = getMFDSector(prevDasAddr);
            prevDas.set(033, dirTrackAddr.getValue());
            markDirectorySectorDirty(prevDasAddr);

            // Now populate the new DAS.
            var thisDas = getMFDSector(dirTrackAddr);
            long w = Word36.setH1(0, chosenPackInfo.getLDATIndex());
            thisDas.set(0, w);
            thisDas.set(1, 0_400000_000017L);
            thisDas.set(2, 0_000000_000017L);
            for (int dx = 3; dx < 033; dx++) {
                thisDas.set(dx, 0_400000_000000L);
                thisDas.set(dx + 1, 0_000000_000017L);
                thisDas.set(dx + 2, 0_000000_000017L);
            }
            thisDas.set(033, 0_400000_000000L);
            var dasAddr = new MFDRelativeAddress(dirTrackAddr);
            markDirectorySectorDirty(dasAddr);
        } else {
            // link this directory to the appropriate DAS
            var prevDasTrackId = mfdTrackId - mod;
            var prevDasAddr = new MFDRelativeAddress(dirTrackAddr).setTrackId(prevDasTrackId);
            var prevDas = getMFDSector(prevDasAddr);

            int dx = mod * 3;
            prevDas.set(dx, dirTrackAddr.getValue());
            prevDas.set(dx + 1, 0_000000_000017L);
            prevDas.set(dx + 2, 0_000000_000017L);
            markDirectorySectorDirty(prevDasAddr);
        }

        LogManager.logTrace(LOG_SOURCE, "expandDirectory added MFD address %s at ldat:%06o devTrack:%012o",
                            dirTrackAddr.toString(), chosenPackInfo.getLDATIndex(), devTrackId);
    }

    /**
     * Retrieves an ArraySlice containing a subset of a directory track
     * which represents the requested MFD sector
     * @param address MFD sector address
     * @return ArraySlice
     * @throws ExecStoppedException if we crashed
     */
    ArraySlice getMFDSector(final MFDRelativeAddress address) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "getMFDSector(%s)", address.toString());

        var trackAddr = new MFDRelativeAddress(address.getLDATIndex(), address.getTrackId(), 0);
        var mfdTrack = _cachedMFDTracks.get(trackAddr);
        if (mfdTrack == null) {
            LogManager.logFatal(LOG_SOURCE, "getMFDSector track address %s is not in cache", trackAddr.toString());
            Exec.getInstance().stop(StopCode.DirectoryErrors);
            throw new ExecStoppedException();
        }

        return new ArraySlice(mfdTrack, (int)(28 * address.getSectorId()), 28);
    }

    /**
     * Marks a directory sector as being dirty.
     * Since we do IO on block boundaries, we don't need to actually mark *all* the sectors dirty...
     * so we only mark the first sector in a physical block, for any sector within that block.
     * @param sectorAddress MFD-relative sector address of dirty sector
     */
    private void markDirectorySectorDirty(
        final MFDRelativeAddress sectorAddress
    ) {
        var ni = _logicalDATable.get((int)sectorAddress.getLDATIndex());
        var pi = (PackInfo)(ni.getMediaInfo());
        var aligned = pi.alignSectorAddressToBlock(sectorAddress);
        _dirtyCacheBlocks.add(aligned);
    }
}
