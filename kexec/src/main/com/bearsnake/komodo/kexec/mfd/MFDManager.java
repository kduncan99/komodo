/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.HardwareTrackId;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.AbsoluteCycleConflictException;
import com.bearsnake.komodo.kexec.exceptions.AbsoluteCycleOutOfRangeException;
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
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class MFDManager implements Manager {

    static final String LOG_SOURCE = "MFDMgr";
    public static final long INVALID_LINK = 0_400000_000000L;

    // This is in-core MFD. Each track is keyed with the MFD relative address of sector 0 of the directory track.
    private final HashMap<MFDRelativeAddress, ArraySlice> _cachedMFDTracks = new HashMap<>();

    // Lookup table for lead items, keyed by a concatenation of qualifier, asterisk, and filename.
    private final HashMap<String, MFDRelativeAddress> _fileLeadItemLookupTable = new HashMap<>();

    // Lookup table for AcceleratedCycleInfo objects representing assigned file cycles.
    // The assign count for the file cycle is maintained in the FileCycleInfo object, so we know when we
    // can release the thing. There is an entry here for every file cycle currently assigned to at least
    // one run. The key is the address of the main item sector 0.
    private final Map<MFDRelativeAddress, AcceleratedCycleInfo> _acceleratedFileCycles = new HashMap<>();
    private MFDRelativeAddress _mfdFileAddress;

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
            // lead item lookup table
            out.printf("%s  Lead Item lookup table:\n", indent);
            for (var e : _fileLeadItemLookupTable.entrySet()) {
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
        }

        // accelerated file cycles
        out.printf("%s  Accelerated file cycles:\n", indent);
        for (var aci : _acceleratedFileCycles.values()) {
            var fci = aci.getFileCycleInfo();
            out.printf("%s    %s*%s(%d)\n", indent, fci.getQualifier(), fci.getFilename(), fci.getAbsoluteCycle());
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
    ) throws ExecStoppedException, AbsoluteCycleConflictException, AbsoluteCycleOutOfRangeException {
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
        _fileLeadItemLookupTable.clear();
        _freeMFDSectors.clear();
        _logicalDATable.clear();
        _fixedPackCount = 0;
        var mfdFas = new FileAllocationSet();

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
                var sectorAddr = new MFDRelativeAddress(ldatIndex, 0, sectorId);
                _freeMFDSectors.add(sectorAddr);
                markDirectorySectorDirty(sectorAddr);
            }

            packInfo.setMFDTrackCount(1);
            var faRegion = new LogicalTrackExtent(dirTrackAddr.getValue(), 1);
            var hwTid = new HardwareTrackId(ldatIndex, packInfo.getDirectoryTrackAddress());
            mfdFas.mergeIntoFileAllocationSet(new FileAllocation(faRegion, hwTid));

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
            sector1.set(4, Word36.stringToWordFieldata(packId));
            sector1.set(5, INVALID_LINK);

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

        // Create MFD$$ file artifacts in the first directory track of the first disk pack,
        // then write the dirty sectors (i.e., the entire MFD) to disk.
        createMFDFile(mfdFas);
        writeDirtyCacheBlocks();

        // I think we're all done here.
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
     * @return MFDSector object containing relative address of the allocated directory sector and
     *          a reference to the sector
     * @throws ExecStoppedException if something goes wrong
     */
    private synchronized MFDSector allocateDirectorySector() throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "allocateDirectorySector()");

        if (_freeMFDSectors.isEmpty()) {
            expandDirectory();
        }

        var addr = _freeMFDSectors.pollFirst();
        if (addr == null) {
            // should never be here, but just in case...
            LogManager.logFatal(LOG_SOURCE, "No free MFD sector even after expand");
            Exec.getInstance().stop(StopCode.DirectoryErrors);
            throw new ExecStoppedException();
        }

        var result = new MFDSector(addr, getMFDSector(addr));
        LogManager.logTrace(LOG_SOURCE, "allocateDirectorySector returning addr %0120", addr);
        return result;
    }

    /**
     * Creates MFD sector(s) describing a file cycle.
     * Updates the MFD sector(s) describing the file set accordingly.
     * Updates the main item sector addresses in the fcInfo object
     * @param fsInfo FileSetInfo describing the file set (which might be empty)
     *               MUST have the mainItem0Address value set (by us).
     * @param fcInfo FileCycleInfo describing the file cycle to be created
     * @throws ExecStoppedException if something fatal occurs
     */
    private synchronized void createFileCycle(
        final FileSetInfo fsInfo,
        final FileCycleInfo fcInfo
    ) throws ExecStoppedException,
             AbsoluteCycleConflictException,
             AbsoluteCycleOutOfRangeException {
        // If fsInfo is empty, we don't need to verify absolute file cycle.
        if (fsInfo.getCycleCount() > 0) {
            for (var fci : fsInfo.getCycleInfo()) {
                if (fci.getAbsoluteCycle() == fcInfo.getAbsoluteCycle()) {
                    throw new AbsoluteCycleConflictException();
                }
            }

            // Check file cycle constraints - can we actually do this?
            // Bear in mind that we cycle around below 1 and above 999.
            if ((fsInfo.getHighestAbsoluteCycle() >= 967) && (fcInfo.getAbsoluteCycle() <= 32)) {
                // wrap-around, and new cycle is logically higher than existing highest cycle.
                var effectiveNew = fcInfo.getAbsoluteCycle() + 999;
                var lowestExisting = fsInfo.getCycleInfo().getLast().getAbsoluteCycle();
                int newCycleRange = effectiveNew - lowestExisting + 1;
                if (newCycleRange > fsInfo.getMaxCycleRange()) {
                    throw new AbsoluteCycleOutOfRangeException();
                }
            } else if ((fsInfo.getHighestAbsoluteCycle() <= 32) && (fcInfo.getAbsoluteCycle() >= 967)) {
                // wrap-around, new cycle is logically less than the existing highest cycle.
                var effectiveHighest = fsInfo.getHighestAbsoluteCycle() + 999;
                var potentialCycleRange = effectiveHighest - fcInfo.getAbsoluteCycle() + 1;
                if (potentialCycleRange > fsInfo.getMaxCycleRange()) {
                    throw new AbsoluteCycleOutOfRangeException();
                }
            } else {
                // no wrap-around
                if (fcInfo.getAbsoluteCycle() > fsInfo.getHighestAbsoluteCycle()) {
                    // new absolute is higher than the current highest
                    var lowestExisting = fsInfo.getCycleInfo().getLast().getAbsoluteCycle();
                    int newCycleRange = fcInfo.getAbsoluteCycle() - lowestExisting + 1;
                    if (newCycleRange > fsInfo.getMaxCycleRange()) {
                        throw new AbsoluteCycleOutOfRangeException();
                    }
                } else {
                    // new absolute is lower than the current highest
                    var potentialCycleRange = fsInfo.getHighestAbsoluteCycle() - fcInfo.getAbsoluteCycle() + 1;
                    if (potentialCycleRange > fsInfo.getMaxCycleRange()) {
                        throw new AbsoluteCycleOutOfRangeException();
                    }
                }
            }
        }

        // Populate the main items
        var mainItems = new LinkedList<MFDSector>();
        for (var mix = 0; mix < fcInfo.getRequiredNumberOfMainItems(); mix++) {
            mainItems.add(allocateDirectorySector());
        }

        fcInfo.populateMainItems(mainItems);

        // Link main item sector 0 into the lead item(s) and update the cycle information in the lead item.
        var cycInfo = new FileSetCycleInfo().setAbsoluteCycle(fcInfo.getAbsoluteCycle())
                                            .setMainItem0Address(mainItems.getFirst().getAddress())
                                            .setToBeCataloged(fcInfo.getDescriptorFlags().toBeCataloged());
        fsInfo.mergeFileSetCycleInfo(cycInfo);
        persistLeadItems(fsInfo);
    }

    /**
     * Creates MFD sector(s) describing an empty fileset.
     * Normally, empty file sets do not exist. The caller must follow this up by calling createFileCycle.
     * THe fileset has no security words, and is initialized to allow a max of 32 file cycles.
     * @return MFD sector address of the lead item sector 0
     * @throws ExecStoppedException if something goes badly
     */
    private synchronized MFDRelativeAddress createFileSet(
        final FileSetInfo fsInfo
    ) throws ExecStoppedException {
        var mfdSectors = new LinkedList<MFDSector>();
        var mainItemCount = fsInfo.isSector1Required() ? 2 : 1;
        for (int sx = 0; sx < mainItemCount; sx++) {
            mfdSectors.add(allocateDirectorySector());
        }

        fsInfo.populateLeadItemSectors(mfdSectors);
        markDirectorySectorsDirty(mfdSectors);
        return mfdSectors.getFirst().getAddress();
    }

    /**
     * Part of an initial boot - this code creates the artifacts which comprise the MFDF$$ file.
     * @param mfdAllocationSet FileAllocationSet describing the layout of the master file directory file's content.
     */
    private void createMFDFile(
        final FileAllocationSet mfdAllocationSet
    ) throws ExecStoppedException {
        var e = Exec.getInstance();
        var mfdQualifier = e.getRunControlEntry().getDefaultQualifier();
        var mfdFilename = "MFDF$$";
        var mfdProjectId = e.getRunControlEntry().getProjectId();
        var mfdAccountId = e.getRunControlEntry().getAccountId();
        var mfdEquip = e.getConfiguration().getMassStorageDefaultMnemonic();
        var fFlags = new FileFlags().setIsLargeFile(true);
        var inhFlags = new InhibitFlags().setIsGuarded(true).setIsPrivate(true).setIsUnloadInhibited(true);
        var pchFlags = new PCHARFlags().setGranularity(Granularity.Track);
        var usInd = new UnitSelectionIndicators().setMultipleDevices(true).setInitialLDATIndex(01);

        var fsInfo = new FileSetInfo().setFileType(FileType.Fixed)
                                      .setQualifier(mfdQualifier)
                                      .setFilename(mfdFilename)
                                      .setProjectId(mfdProjectId)
                                      .setIsGuarded(true);
        _mfdFileAddress = createFileSet(fsInfo);

        var fcInfo = new FixedDiskFileCycleInfo(new MFDSector(_mfdFileAddress, getMFDSector(_mfdFileAddress)));
        fcInfo.setUnitSelectionIndicators(usInd)
              .setFileFlags(fFlags)
              .setPCHARFlags(pchFlags)
              .setAccountId(mfdAccountId)
              .setAbsoluteCycle(1)
              .setAssignMnemonic(mfdEquip)
              .setInhibitFlags(inhFlags);

        try {
            createFileCycle(fsInfo, fcInfo);
        } catch (AbsoluteCycleOutOfRangeException
                 | AbsoluteCycleConflictException ex) {
            LogManager.logCatching(LOG_SOURCE, ex);
            Exec.getInstance().stop(StopCode.DirectoryErrors);
            throw new ExecStoppedException();
        }

        // Create DAD tables for the MFD$$ file
        persistDADTables(_mfdFileAddress, mfdAllocationSet);

        // Assign MFDF$$ to the exec
        //  TODO
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

        // update file allocation set for the MFD$$ file
        var mfdACI = _acceleratedFileCycles.get(_mfdFileAddress);
        mfdACI.getFileAllocationSet().mergeIntoFileAllocationSet(fa);

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
                thisDas.set(dx, INVALID_LINK);
                thisDas.set(dx + 1, 0_000000_000017L);
                thisDas.set(dx + 2, 0_000000_000017L);
            }
            thisDas.set(033, INVALID_LINK);
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
     * Retrieves a list of sectors which comprise the complete DAD table for a mass storage file cycle.
     * @param mainItem0Address address of main item sector 0 for the file cycle.
     * @return list
     * @throws ExecStoppedException if something goes wrong
     */
    private LinkedList<MFDSector> getDADChain(
        final MFDRelativeAddress mainItem0Address
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "getDADChain(%s)", mainItem0Address.toString());
        var dadChain = new LinkedList<MFDSector>();

        var mainItem0 = getMFDSector(mainItem0Address);
        var dadLink = mainItem0.get(0) & 0_007777_777777;
        while (dadLink != 0) {
            var dadAddr = new MFDRelativeAddress(dadLink);
            var dad = getMFDSector(dadAddr);
            dadChain.add(new MFDSector(dadAddr, dad));
            dadLink = dad.get(0);
        }

        LogManager.logTrace(LOG_SOURCE, "getDADChain returning");
        return dadChain;
    }

    /**
     * Retrieves the chain of lead items indicated by the lead item sector 0 address.
     * @param leadItem0Address address of lead item sector 0
     * @return lead item chain containing references to one or two lead items.
     * @throws ExecStoppedException if things go badly
     */
    private LinkedList<MFDSector> getLeadItemChain(
        final MFDRelativeAddress leadItem0Address
    ) throws ExecStoppedException {
        var result = new LinkedList<MFDSector>();

        var leadItem0 = getMFDSector(leadItem0Address);
        result.add(new MFDSector(leadItem0Address, leadItem0));
        if ((leadItem0.get(0) & 0_400000_000000L) == 0) {
            var leadItem1Address = new MFDRelativeAddress(leadItem0.get(0) & 0_007777_777777L);
            var leadItem1 = getMFDSector(leadItem1Address);
            result.add(new MFDSector(leadItem1Address, leadItem1));
        }

        return result;
    }

    /**
     * Retrieves an ArraySlice containing a subset of a directory track
     * which represents the requested MFD sector
     * @param address MFD sector address
     * @return ArraySlice
     * @throws ExecStoppedException if we crashed
     */
    private ArraySlice getMFDSector(
        final MFDRelativeAddress address
    ) throws ExecStoppedException {
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

    /**
     * Wrapper around the above function
     */
    private void markDirectorySectorsDirty(
        final LinkedList<MFDSector> mfdSectors
    ) {
        mfdSectors.stream().map(MFDSector::getAddress).forEach(this::markDirectorySectorDirty);
    }

    /**
     * Persists DAD tables to the MFD representing the content of the given FileAllocationSet,
     * for the file cycle indicated by the given main item sector 0 address.
     */
    public void persistDADTables(
        final MFDRelativeAddress mainItem0Address,
        final FileAllocationSet faSet
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "persistDADTables for %s", mainItem0Address.toString());

        // Go get the current chain of DAD entries.
        // It might be smaller or larger than we need (or non-existent), but we'll deal with that presently.
        var dadChain = getDADChain(mainItem0Address);

        // Special case - no space allocated. If there is a chain, release all the entries on the chain
        // and clear the link in the main item.
        var allocations = faSet.getFileAllocations();
        if (allocations.isEmpty()) {
            dadChain.stream().map(MFDSector::getAddress).forEach(this::releaseDirectorySector);
            return;
        }

        // Set ax to the index of the next allocation to be persisted.
        // Also, set dx to the index of the dad sector being populated (for now, -1), and ex to the index
        // of the next entry in that sector to be populated (for now, 8) -- these settings will ensure that
        // we select the next (in this case, first) entry to populate.
        // Finally, set currentDAD null - this is a reference to the DAD sector which corresponds to dx.
        int ax = 0;
        int dx = -1;
        int ex = 8;
        MFDRelativeAddress currentDADAddr = null;
        ArraySlice currentDAD = null;

        long expectedNextTrackId = 0; // this does not need an initial value

        while (ax < allocations.size()) {
            if (ex == 8) {
                // current DAD entry is full, move on to the next one.
                // If there isn't a next one, we need to allocate a new one and put in on the chain.
                dx++;
                if (dx == dadChain.size()) {
                    var newDADSector = allocateDirectorySector();
                    var newDADAddr = newDADSector.getAddress();
                    var newDAD = newDADSector.getSector();
                    IntStream.range(0, 28).forEach(x -> newDAD._array[x] = 0);
                    dadChain.add(new MFDSector(newDADAddr, newDAD));

                    // link this address to the previous DAD entry (but only if this is not the first).
                    if (currentDAD != null) {
                        currentDAD.set(0, newDADAddr.getValue());
                        newDAD.set(1, currentDADAddr.getValue());
                    } else {
                        // If it *is* the first, then the previous link needs to point to the main item
                        newDAD.set(1, mainItem0Address.getValue());
                    }
                }
                currentDADAddr = dadChain.get(dx).getAddress();
                currentDAD = dadChain.get(dx).getSector();
                ex = 0;
            }

            var alloc = allocations.get(ax);
            var fileRegion = alloc.getFileRegion();
            var hwTid = alloc.getHardwareTrackId();

            // If this is the first entry in this DAD, create DAD header values and reset expectedNextTrackId.
            if (ex == 0) {
                currentDAD.set(2, fileRegion.getTrackId() * 1792);
                currentDAD.set(3, fileRegion.getTrackId() * 1792);
                expectedNextTrackId = fileRegion.getTrackId();
            }

            // Does this entry immediately follow the previous entry in the file-relative address space?
            // If not, we need to create a hole DAD.
            if ((ax > 0) && (fileRegion.getTrackId() != expectedNextTrackId)) {
                // If this hole entry would go into the last entry of the DAD, there's no reason to create it.
                if (ex < 7) {
                    int wx = (3 * ex) + 4;
                    currentDAD.set(wx, expectedNextTrackId * 1792);
                    currentDAD.set(wx + 1, (fileRegion.getTrackId() - expectedNextTrackId) * 1792);
                    currentDAD.setH2(wx + 2, 0_400000);
                }
                ex++;
            }

            // Now create a DAD entry (and update the last word + 1 in the header)
            // Always set this entry as the last in the DAD... we'll unset it on the next entry in the DAD
            // if there is a next one, and leave it be if there isn't.
            int wx = (3 * ex) + 4;
            currentDAD.set(wx, hwTid.getTrackId() * 1792);
            currentDAD.set(wx + 1, fileRegion.getTrackCount() * 1792);
            currentDAD.setH1(wx + 2, 0_000004);
            currentDAD.setH2(wx + 2, hwTid.getLDATIndex());

            // (Here's where we unset the last-DAD bit in the previous entry)
            int wy = wx - 3;
            currentDAD.setH1(wy + 2, 0_000000);

            expectedNextTrackId += fileRegion.getTrackCount();
            currentDAD.set(3, expectedNextTrackId * 1792);
            ex++;
            ax++;
        }

        // Release any unused DAD entries, and ensure the last DAD does not have a forward link
        if (ex > 0) {
            dx++;
        }
        while (dx < dadChain.size()) {
            releaseDirectorySector(dadChain.get(dx++).getAddress());
        }
        dadChain.getLast().getSector().set(0, 0);

        // Mark all the DAD sectors dirty
        dadChain.stream().map(MFDSector::getAddress).forEach(this::markDirectorySectorDirty);
    }

    /**
     * Gets the lead item chain, updates it (entirely) from the given FileSetInfo object,
     * then marks the lead item sectors dirty for eventual writing to disk
     * @param fsInfo FileSetInfo object
     * @throws ExecStoppedException if something goes wrong
     */
    private void persistLeadItems(
        final FileSetInfo fsInfo
    ) throws ExecStoppedException {
        var chain = getLeadItemChain(fsInfo.getLeadItem0Address());
        if (fsInfo.isSector1Required() && (chain.size() == 1)) {
            chain.add(allocateDirectorySector());
        }
        fsInfo.populateLeadItemSectors(chain);
        markDirectorySectorsDirty(chain);
    }

    /**
     * Releases an MFD directory sector
     */
    private void releaseDirectorySector(
        final MFDRelativeAddress address
    ) {
        _freeMFDSectors.add(address);
    }

    /**
     * Writes all dirty cache blocks to underlying disk storage
     * @throws ExecStoppedException if something goes wrong
     */
    private void writeDirtyCacheBlocks() throws ExecStoppedException {
        // TODO
        //  try to write contiguous blocks in one IO
    }
}
