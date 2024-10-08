/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.channels.ChannelIoPacket;
import com.bearsnake.komodo.hardwarelib.channels.TransferFormat;
import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.HardwareTrackId;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.exceptions.AbsoluteCycleConflictException;
import com.bearsnake.komodo.kexec.exceptions.AbsoluteCycleOutOfRangeException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.FileCycleDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.FileSetAlreadyExistsException;
import com.bearsnake.komodo.kexec.exceptions.FileSetDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.kexec.facilities.PackInfo;
import com.bearsnake.komodo.logger.LogManager;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class MFDManager implements Manager {

    static final String LOG_SOURCE = "MFDMgr";
    public static final long INVALID_LINK = 0_400000_000000L;

    // Used for finding the DAS entry for a particular directory sector.
    private static class DASLocation {

        public final long _dasTrackId; // MFD-relative track-id of track containing DAS for a given sector address
        public final int _trackOffset; // track id % 9, indicates a particular word-pair within DAS
        public final int _wordMod;  // indicates which word in the word-pair
        public final int _bit;      // indicates the bit, 0 is left-most bit while 31 is right most, with 4 trailing unused bits

        public DASLocation(final MFDRelativeAddress sectorAddress) {
            var ldat = sectorAddress.getLDATIndex();
            var trackId = sectorAddress.getTrackId();
            var sectorId = sectorAddress.getSectorId();
            _dasTrackId = (ldat << 12) | (trackId / 9);
            _trackOffset = (int)(trackId % 9);
            _wordMod = (int)sectorId >> 5;
            _bit = (int)sectorId & 037;
        }

        public MFDRelativeAddress getDASTrackAddress() {
            return new MFDRelativeAddress(_dasTrackId << 6);
        }
    }

    // This is in-core MFD. Each track is keyed with the MFD relative address of sector 0 of the directory track.
    private MFDRelativeAddress _mfdFileAddress;
    private final HashMap<MFDRelativeAddress, ArraySlice> _cachedMFDTracks = new HashMap<>();

    // Lookup table for lead items, keyed by a concatenation of qualifier, asterisk, and filename.
    private final HashMap<String, FileSetInfo> _leadItemLookupTable = new HashMap<>();

    // Lookup table for AcceleratedCycleInfo objects representing assigned file cycles.
    // The assign count for the file cycle is maintained in the FileCycleInfo object, so we know when we
    // can release the thing. There is an entry here for every file cycle currently assigned to at least
    // one run. The key is the address of the main item sector 0.
    private final Map<MFDRelativeAddress, AcceleratedCycleInfo> _acceleratedFileCycles = new HashMap<>();

    // This is the MFD sector free list. It's a tree set to make debugging slightly easier.
    private final TreeSet<MFDRelativeAddress> _freeMFDSectors = new TreeSet<>();

    // This is the Logical Device Address Table which maps LDAT index to a fac mgr NodeInfo object
    private final ConcurrentHashMap<Integer, NodeInfo> _logicalDATable = new ConcurrentHashMap<>();
    private int _fixedPackCount;

    // This is a set of all the MFD tracks (keyed by MFD-relative track ID) which need to be persisted to disk.
    private final HashSet<Long> _dirtyCacheTracks = new HashSet<>();

    public MFDManager() {
        Exec.getInstance().managerRegister(this);
    }

    // -------------------------------------------------------------------------
    // Manager interface
    // -------------------------------------------------------------------------

    @Override
    public void boot(final boolean recoveryBoot) {
        LogManager.logTrace(LOG_SOURCE, "boot(%s) - nothing to do", recoveryBoot);
    }

    @Override
    public void close() {
        LogManager.logTrace(LOG_SOURCE, "close()");
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
            for (var e : _leadItemLookupTable.entrySet()) {
                var luKey = e.getKey();
                var fsInfo = e.getValue();
                out.printf("%s    %s:  %s\n", indent, luKey, fsInfo._leadItem0Address);
            }

            // MFD tracks
            out.printf("%s  Cached MFD Tracks:\n", indent);
            for (var addr : _cachedMFDTracks.keySet()) {
                out.printf("%s    %s\n", indent, addr.toString());
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
                        var wbase = (int) (sector * 28);
                        for (int wx = 0; wx < 28; wx += 7) {
                            var sb = new StringBuilder();
                            sb.append(indent).append("    ").append(prefix);
                            for (int wy = 0; wy < 7; wy++) {
                                sb.append(String.format(" %012o", trackData.get(wbase + wx + wy)));
                            }

                            for (int wy = 0; wy < 7; wy++) {
                                sb.append(" ");
                                sb.append(Word36.toStringFromFieldata(trackData._array[wbase + wx + wy]));
                            }

                            for (int wy = 0; wy < 7; wy++) {
                                sb.append(" ");
                                sb.append(Word36.toStringFromASCII(trackData._array[wbase + wx + wy]));
                            }

                            out.printf("%s    %s\n", indent, sb);
                            prefix = "             ";
                        }
                    }
                }
            }

            // Dirty cache blocks
            out.printf("%s  Dirty mfd-relative track-IDs:\n", indent);
            for (var addr : _dirtyCacheTracks) {
                out.printf("%s    %012o\n", indent, addr);
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
        out.printf("%s  MFD$$ file address:%s\n", indent, _mfdFileAddress);
        out.printf("%s  Accelerated file cycles:\n", indent);
        for (var entry : _acceleratedFileCycles.entrySet()) {
            var addr = entry.getKey();
            var aci = entry.getValue();
            var fci = aci.getFileCycleInfo();
            out.printf("%s    %s:%s*%s(%d)\n", indent, addr, fci.getQualifier(), fci.getFilename(), fci.getAbsoluteCycle());
            if (verbose) {
                for (var fa : aci.getFileAllocationSet().getFileAllocations()) {
                    out.printf("%s      %s\n", indent, fa.toString());
                }
            }
        }
    }

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");
    }

    @Override
    public void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
    }

    // -------------------------------------------------------------------------
    // Service API
    // -------------------------------------------------------------------------

    /**
     * Accelerates a file cycle. This involves loading meta information regarding the file cycle
     * into storage, and leaving it there until the file is decelerated. If the file is already
     * accelerated, the acceleration count is incremented.
     * @param qualifier qualifier of the file
     * @param filename filename of the file
     * @param absoluteCycle absolute cycle of the file
     * @return AcceleratedCycleInfo object describing (some of) the meta-information
     * @throws ExecStoppedException if something goes monkey-wise
     * @throws FileCycleDoesNotExistException the cycle indicated by the absolute cycle does not exist
     * @throws FileSetDoesNotExistException no cycle with the given qualifier and filename exists
     */
    public synchronized AcceleratedCycleInfo accelerateFileCycle(
        final String qualifier,
        final String filename,
        final int absoluteCycle
    ) throws ExecStoppedException,
             FileCycleDoesNotExistException,
             FileSetDoesNotExistException {
        LogManager.logTrace(LOG_SOURCE, "accelerateFileCycle %s*%s(%d)", qualifier, filename, absoluteCycle);

        var luKey = composeLookupKey(qualifier, filename);
        var fsInfo = _leadItemLookupTable.get(luKey);
        if (fsInfo == null) {
            throw new FileSetDoesNotExistException();
        }

        AcceleratedCycleInfo acInfo = null;
        for (var cycInfo : fsInfo.getCycleInfo()) {
            if (cycInfo.getAbsoluteCycle() == absoluteCycle) {
                var mainItem0Addr = cycInfo.getMainItem0Address();
                acInfo = _acceleratedFileCycles.get(mainItem0Addr);
                if (acInfo == null) {
                    var miChain = getMainItemChain(cycInfo.getMainItem0Address());
                    FileCycleInfo fcInfo;
                    switch (fsInfo.getFileType()) {
                        case Fixed -> {
                            fcInfo = new FixedDiskFileCycleInfo();
                            fcInfo.loadFromMainItemChain(miChain);
                            var dadChain = getDADChain(mainItem0Addr);
                            var fas = FileAllocationSet.createFromDADChain(dadChain);
                            acInfo = new AcceleratedCycleInfo(fcInfo, fas);
                        }
                        case Removable -> {
                            fcInfo = new RemovableDiskFileCycleInfo();
                            fcInfo.loadFromMainItemChain(miChain);
                            var dadChain = getDADChain(mainItem0Addr);
                            var fas = FileAllocationSet.createFromDADChain(dadChain);
                            acInfo = new AcceleratedCycleInfo(fcInfo, fas);
                        }
                        case Tape -> {
                            fcInfo = new TapeFileCycleInfo();
                            fcInfo.loadFromMainItemChain(miChain);
                            acInfo = new AcceleratedCycleInfo(fcInfo);
                        }
                    }
                    _acceleratedFileCycles.put(mainItem0Addr, acInfo);
                }
                break;
            }
        }

        if (acInfo == null) {
            throw new FileCycleDoesNotExistException();
        }

        // update assign counts - they are in main item sector 0, so that makes it easier for us.
        var fcInfo = acInfo.getFileCycleInfo();
        var cumulativeCount = fcInfo.getCumulativeAssignCount();
        fcInfo.setCumulativeAssignCount(cumulativeCount + 1);
        var curCount = acInfo.incrementAssignCount();
        fcInfo.setCurrentAssignCount(curCount);
        markDirectorySectorDirty(fcInfo._mainItem0Address);

        return acInfo;
    }

    /**
     * Allocates space for a particular file allocation set to ensure that a particular
     * fas-relative extent is entirely allocated. Updates the fas if/as necessary.
     * Does NOT account for quota or max tracks - we don't have insight into those restrictions at this level.
     * @param fileAllocationSet file allocation set to which the allocation should be made
     * @param firstTrackId first track id of interest
     * @param trackCount number of tracks of interest
     * @return true if successful, false if we could not allocate space because we are out.
     */
    public synchronized boolean allocateDataExtent(
        final FileAllocationSet fileAllocationSet,
        final long firstTrackId,
        final long trackCount
    ) {
        // This algorithm is suboptimal from the perspective of minimizing fragmentation.
        // Some day later we should come back here and improve it.
        // For now, just iterate over the tracks and do the simple thing.
        var trackId = firstTrackId;
        var remaining = trackCount;
        while (remaining > 0) {
            var hwTid = fileAllocationSet.resolveFileRelativeTrackId(trackId);
            if (hwTid == null) {
                hwTid = allocateHardwareTrackId(null, null);
                if (hwTid == null) {
                    return false;
                }

                var extent = new LogicalTrackExtent(trackId, 1);
                var fileAllocation = new FileAllocation(extent, hwTid);
                fileAllocationSet.mergeIntoFileAllocationSet(fileAllocation);
            }
            remaining--;
            trackId++;
        }

        return true;
    }

    /**
     * Creates MFD sector(s) describing a file cycle.
     * Updates the MFD sector(s) describing the file set accordingly.
     * Updates the main item sector addresses in the fcInfo object
     * @param fsInfo FileSetInfo describing the file set (which might be empty)
     *               MUST have the leadItem0Address value set properly.
     * @param fcInfo FileCycleInfo describing the file cycle to be created
     * @throws ExecStoppedException if something fatal occurs
     */
    public synchronized void createFileCycle(
        final FileSetInfo fsInfo,
        final FileCycleInfo fcInfo
    ) throws ExecStoppedException,
             AbsoluteCycleConflictException,
             AbsoluteCycleOutOfRangeException {
        LogManager.logTrace(LOG_SOURCE, "createFileCycle for %s*%s(%d)",
                            fsInfo.getQualifier(), fsInfo.getFilename(), fcInfo.getAbsoluteCycle());

        // If fsInfo has no file cycles, we don't need to verify absolute file cycle.
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

        fcInfo._leadItem0Address = fsInfo._leadItem0Address;
        fcInfo._mainItem0Address = mainItems.getFirst().getAddress();
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
     * The fileset has no security words, and is initialized to allow a max of 32 file cycles.
     * @param fileSetInfo Fully describes the fileset to be created (and cataloged), excepting the leadItem0Address field
     *                    which is populated by us.
     * @return MFD sector address of the lead item sector 0
     * @throws ExecStoppedException if something goes badly
     */
    public synchronized MFDRelativeAddress createFileSet(
        final FileSetInfo fileSetInfo
    ) throws ExecStoppedException, FileSetAlreadyExistsException {
        LogManager.logTrace(LOG_SOURCE, "createFileSet for %s*%s", fileSetInfo.getQualifier(), fileSetInfo.getFilename());

        var luKey = composeLookupKey(fileSetInfo.getQualifier(), fileSetInfo.getFilename());
        if (_leadItemLookupTable.containsKey(luKey)) {
            throw new FileSetAlreadyExistsException();
        }

        var mfdSectors = new LinkedList<MFDSector>();
        var mainItemCount = fileSetInfo.isSector1Required() ? 2 : 1;
        for (int sx = 0; sx < mainItemCount; sx++) {
            mfdSectors.add(allocateDirectorySector());
        }

        fileSetInfo.populateLeadItemSectors(mfdSectors);
        fileSetInfo._leadItem0Address = mfdSectors.getFirst().getAddress();
        markDirectorySectorsDirty(mfdSectors);
        _leadItemLookupTable.put(luKey, fileSetInfo);
        return mfdSectors.getFirst().getAddress();
    }

    /**
     * Decelerates a file cycle. This involves decrementing the acceleration count for an accelerated
     * file cycle, and unloading the corresponding meta information if the count goes to zero.
     * @param fcInfo describes the file cycle to be accelerated
     */
    public void decelerateFileCycle(
        final FileCycleInfo fcInfo
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "decelerateFileCycle %s*%s(%d)",
                            fcInfo.getQualifier(),
                            fcInfo.getFilename(),
                            fcInfo.getAbsoluteCycle());

        if (fcInfo.getInhibitFlags().isAssignedExclusively()) {
            fcInfo.getInhibitFlags().setIsAssignedExclusively(false);
            persistFileCycleInfo(fcInfo);
        }

        var acInfo = _acceleratedFileCycles.get(fcInfo._mainItem0Address);
        if (acInfo != null) {
            var newCount = acInfo.decrementAssignCount();
            fcInfo.setCurrentAssignCount(newCount);
            markDirectorySectorDirty(fcInfo._mainItem0Address);
            if (newCount == 0) {
                _acceleratedFileCycles.remove(fcInfo._mainItem0Address);
            }
        }
    }

    /**
     * Deletes a cataloged file cycle, deleting the fileset as well if it has no other cataloged cycles.
     * If the file is currently accelerated we will just mark it do-be-dropped (if it is not already).
     * (accelerated is functionally equivalent to assigned).
     * @param qualifier qualifier of the file
     * @param filename filename of the file
     * @param absoluteCycle absolute cycle of the file
     */
    public synchronized void deleteFileCycle(
        final String qualifier,
        final String filename,
        final int absoluteCycle
    ) throws ExecStoppedException,
             FileCycleDoesNotExistException,
             FileSetDoesNotExistException {
        LogManager.logTrace(LOG_SOURCE, "dropFileCycleInfo %s*%s(%d)", qualifier, filename, absoluteCycle);

        var luKey = composeLookupKey(qualifier, filename);
        var fsInfo = _leadItemLookupTable.get(luKey);
        if (fsInfo == null) {
            throw new FileSetDoesNotExistException();
        }

        for (var cycInfo : fsInfo.getCycleInfo()) {
            if (cycInfo.getAbsoluteCycle() == absoluteCycle) {
                var mainItem0Addr = cycInfo.getMainItem0Address();
                var acInfo = _acceleratedFileCycles.get(mainItem0Addr);
                if (acInfo != null) {
                    if (!cycInfo.isToBeDropped()) {
                        // we must update the file cycle info in the MFD manually.
                        var fcInfo = acInfo.getFileCycleInfo();
                        cycInfo.setToBeDropped(true);
                        var e = Exec.getInstance();
                        var mm = e.getMFDManager();
                        var sector0 = mm.getMFDSector(mainItem0Addr);
                        var df = new DescriptorFlags().extract(sector0.getT1(014));
                        df.setToBeDropped(true);
                        sector0.setT1(014, df.compose());
                        mm.markDirectorySectorDirty(mainItem0Addr);

                        // but we can update the file set info automatically... sort of.
                        fcInfo.getDescriptorFlags().setToBeDropped(true);
                        persistLeadItems(fsInfo);
                    }
                } else {
                    dropCycle(fsInfo, mainItem0Addr);
                }

                return;
            }
        }

        throw new FileCycleDoesNotExistException();
    }

    /**
     * Dumps file content in octal, fieldata, and ASCII along with addresses to a dump file.
     * Only acts on the highest absolute cycle if multiple cycles exist
     * @param qualifier qualifier of the file
     * @param filename filename of the file
     * @return name of the host file created, null if something went wrong
     * @throws FileSetDoesNotExistException if the indicated file set does not exist
     * @throws FileCycleDoesNotExistException if the indicated file cycle does not exist
     */
    public synchronized String dumpFileContent(
        final String qualifier,
        final String filename
    ) throws FileSetDoesNotExistException, FileCycleDoesNotExistException {
        var luKey = composeLookupKey(qualifier, filename);
        var fsInfo = _leadItemLookupTable.get(luKey);
        if (fsInfo == null) {
            throw new FileSetDoesNotExistException();
        } else if (fsInfo.getCycleInfo().isEmpty()) {
            throw new FileCycleDoesNotExistException();
        }

        var cycInfo = fsInfo.getCycleInfo().getLast();
        var absCycle = cycInfo.getAbsoluteCycle();

        var dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String dtStr = dateTime.format(formatter);
        var dumpFilename = String.format("mfd-%s.dump", dtStr);
        PrintStream out;
        try {
            out = new PrintStream(dumpFilename);
        } catch (FileNotFoundException ex) {
            return null;
        }

        out.printf("File Content Dump for %s*%s(%d) ----------------------------------------------------\n",
                   filename, qualifier, absCycle);

        var fm = Exec.getInstance().getFacilitiesManager();
        var buffer = new ArraySlice(new long[1792]);
        var channelPacket = new ChannelIoPacket().setBuffer(buffer)
                                                 .setFormat(TransferFormat.Packed)
                                                 .setIoFunction(IoFunction.Read);
        try {
            var dadChain = getDADChain(cycInfo.getMainItem0Address());
            var faSet = FileAllocationSet.createFromDADChain(dadChain);
            for (var fa : faSet.getFileAllocations()) {
                var fr = fa.getFileRegion();
                var frTrackId = fr.getTrackId();
                var trackCount = fr.getTrackCount();
                var ht = fa.getHardwareTrackId();
                var ldat = ht.getLDATIndex();
                var devTrackId = ht.getTrackId();

                var nodeInfo = _logicalDATable.get(ldat);
                channelPacket.setNodeIdentifier(nodeInfo.getNode().getNodeIdentifier());
                for (int tx = 0; tx < trackCount; tx++) {
                    out.printf("\nFileRel TrackID:%08o LDAT:%04o DevTrackId:%08o\n",
                               frTrackId, ldat, devTrackId);
                    channelPacket.setDeviceWordAddress(devTrackId * 1792);
                    fm.routeIo(channelPacket);
                }

                var bx = 0;
                for (int sx = 0; sx < 64; sx++) {
                    var prefix = String.format("%010o", (frTrackId << 6) | sx);
                    for (int wx = 0; wx < 28; wx += 7) {
                        var oct = new StringBuilder();
                        var fd = new StringBuilder();
                        var asc = new StringBuilder();
                        for (int wy = 0; wy < 7; wy++) {
                            var word = buffer.get(bx++);
                            oct.append(' ').append(Word36.toOctal(word));
                            fd.append(' ').append(Word36.toStringFromFieldata(word));
                            asc.append(' ').append(Word36.toStringFromASCII(word));
                        }

                        out.printf("%s: %s %s %s\n", prefix, oct, fd, asc);
                        prefix = "          ";
                    }
                }
            }
        } catch (ExecStoppedException | NoRouteForIOException ex) {
            LogManager.logCatching(LOG_SOURCE, ex);
            out.printf("ERROR: %s\n", ex);
        }

        out.close();
        return dumpFilename;
    }

    /**
     * Creates a FileCycleInfo object for a particular file cycle.
     * @param qualifier qualifier of the file
     * @param filename filename of the file
     * @param absoluteCycle absolute cycle of the file
     * @return FileCycleInfo object describing (some of) the meta-information
     * @throws ExecStoppedException if something goes monkey-wise
     * @throws FileCycleDoesNotExistException the cycle indicated by the absolute cycle does not exist
     * @throws FileSetDoesNotExistException no cycle with the given qualifier and filename exists
     */
    public synchronized FileCycleInfo getFileCycleInfo(
        final String qualifier,
        final String filename,
        final int absoluteCycle
    ) throws ExecStoppedException,
             FileCycleDoesNotExistException,
             FileSetDoesNotExistException {
        LogManager.logTrace(LOG_SOURCE, "getFileCycleInfo %s*%s(%d)", qualifier, filename, absoluteCycle);

        var luKey = composeLookupKey(qualifier, filename);
        var fsInfo = _leadItemLookupTable.get(luKey);
        if (fsInfo == null) {
            throw new FileSetDoesNotExistException();
        }

        for (var cycInfo : fsInfo.getCycleInfo()) {
            if (cycInfo.getAbsoluteCycle() == absoluteCycle) {
                var mainItem0Addr = cycInfo.getMainItem0Address();
                var acInfo = _acceleratedFileCycles.get(mainItem0Addr);
                if (acInfo != null) {
                    var newAsgCount = acInfo.incrementAssignCount();
                    LogManager.logTrace(LOG_SOURCE, "file cycle assign count = %d", newAsgCount);
                    return acInfo.getFileCycleInfo();
                }

                var miChain = getMainItemChain(cycInfo.getMainItem0Address());
                var fcInfo = switch (fsInfo.getFileType()) {
                    case Fixed -> new FixedDiskFileCycleInfo();
                    case Removable -> new RemovableDiskFileCycleInfo();
                    case Tape -> new TapeFileCycleInfo();
                };
                fcInfo.loadFromMainItemChain(miChain);
                return fcInfo;
            }
        }

        throw new FileCycleDoesNotExistException();
    }

    /**
     * Creates a FileSetInfo object for a particular file set.
     * @param qualifier qualifier of the file
     * @param filename filename of the file
     * @return FileSetInfo object describing (some of) the meta-information
     * @throws FileSetDoesNotExistException no cycle with the given qualifier and filename exists
     */
    public synchronized FileSetInfo getFileSetInfo(
        final String qualifier,
        final String filename
    ) throws FileSetDoesNotExistException {
        LogManager.logTrace(LOG_SOURCE, "getFileSetInfo %s*%s", qualifier, filename);

        var luKey = composeLookupKey(qualifier, filename);
        var fsInfo = _leadItemLookupTable.get(luKey);
        if (fsInfo == null) {
            throw new FileSetDoesNotExistException();
        }

        return fsInfo;
    }

    /**
     * Retrieves a collection of all the file set info objects
     */
    public synchronized Collection<FileSetInfo> getFileSetInfos() {
        return _leadItemLookupTable.values();
    }

    /**
     * Retrieves NodeInfo for a particular LDAT
     * @param ldatIndex ldat index
     * @return NodeInfo object, or null if the LDAT is not known
     */
    public NodeInfo getNodeInfoForLDAT(
        final int ldatIndex
    ) {
        return _logicalDATable.get(ldatIndex);
    }

    /**
     * initializes mass storage given a collection of NodeInfo objects describing the fixed disk units
     * which are UP or SU.
     */
    public void initializeMassStorage(
        final Collection<NodeInfo> fixedDiskInfo
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "initializeMassStorage");

        var e = Exec.getInstance();
        try {
            var msg = String.format("Fixed MS Devices = %d - Continue? YN", fixedDiskInfo.size());
            var allowed = new String[]{"Y", "N"};
            var response = e.sendExecRestrictedReadReplyMessage(msg, allowed);
            if (!response.equals("Y")) {
                e.stop(StopCode.ConsoleResponseRequiresReboot);
                throw new ExecStoppedException();
            }

            var start = Instant.now();

            _cachedMFDTracks.clear();
            _dirtyCacheTracks.clear();
            _leadItemLookupTable.clear();
            _freeMFDSectors.clear();
            _logicalDATable.clear();
            _fixedPackCount = 0;
            var mfdFas = new FileAllocationSet();

            var ldatIndex = 1;
            for (var ni : fixedDiskInfo) {
                var packInfo = (PackInfo) ni.getMediaInfo();
                packInfo.setLDATIndex(ldatIndex);

                var fs = packInfo.getFreeSpace();
                if (fs == null) {
                    packInfo.setTrackCount(packInfo.getTrackCount());
                    fs = packInfo.getFreeSpace();
                }
                fs.reset();
                fs.markAllocated(0, 1);
                fs.markAllocated(packInfo.getDirectoryTrackAddress() / 1792, 1);

                _logicalDATable.put(ldatIndex, ni);

                // create a cached initial directory track
                var dirTrackAddr = new MFDRelativeAddress(ldatIndex, 0, 0);
                var dirTrackArray = new long[1792];
                var dirTrack = new ArraySlice(dirTrackArray);
                _cachedMFDTracks.put(dirTrackAddr, dirTrack);

                for (var sectorId = 2; sectorId <= 077; ++sectorId) {
                    var sectorAddr = new MFDRelativeAddress(ldatIndex, 0, sectorId);
                    _freeMFDSectors.add(sectorAddr);
                    markDirectorySectorDirty(sectorAddr);
                }

                packInfo.setMFDTrackCount(1);
                var faRegion = new LogicalTrackExtent(dirTrackAddr.getValue() >> 6, 1);
                var hwTid = new HardwareTrackId(ldatIndex, packInfo.getDirectoryTrackAddress() / 1792);
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
                var sector1 = new ArraySlice(dirTrack, 28, 28);
                sector1.set(2, packInfo.getTrackCount());
                sector1.set(3, packInfo.getTrackCount() - 2);
                String packId = String.format("%-6s", packInfo.getPackName());
                sector1.set(4, Word36.stringToWordFieldata(packId));
                sector1.set(5, INVALID_LINK);

                // +010,T1 is blocks per track
                // +010,S3 is version (1)
                // +010,T3 is prep factor
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
            writeDirtyCacheTracks();

            // I think we're all done here.
            var elapsed = Duration.between(start, Instant.now()).getNano() / 1000000;
            msg = String.format("Mass Storage Initialized %d MS.", elapsed);
            e.sendExecReadOnlyMessage(msg, ConsoleType.System);
        } catch (ExecStoppedException ex) {
            throw ex;
        } catch (Throwable t) {
            LogManager.logCatching(LOG_SOURCE, t);
            e.stop(StopCode.ExecActivityTakenToEMode);
            throw new ExecStoppedException();
        }

        LogManager.logTrace(LOG_SOURCE, "initializeMassStorage exiting");
    }

    /**
     * Persists the information provided in the given fileCycleInfo object.
     * Takes special care to preserve DAD or reel tables, if they exist.
     * Assumes the addresses in the object are set (and correct), implying the file is cataloged.
     * @param fileCycleInfo describes the file cycle.
     * @throws ExecStoppedException if something goes wrong
     */
    public synchronized void persistFileCycleInfo(
        final FileCycleInfo fileCycleInfo
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "persistFileCycleInfo");

        var mainChain = getMainItemChain(fileCycleInfo._mainItem0Address);
        var link = mainChain.getFirst().getSector().get(0);
        var reqItemCount = fileCycleInfo.getRequiredNumberOfMainItems();

        while (mainChain.size() > reqItemCount) {
            var mfdSector = mainChain.pollLast();
            if (mfdSector != null) {
                releaseDirectorySector(mfdSector.getAddress());
            }
        }
        while (mainChain.size() < reqItemCount) {
            mainChain.addLast(allocateDirectorySector());
        }

        fileCycleInfo.populateMainItems(mainChain);
        mainChain.getFirst().getSector().set(0, link);
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
            var response = e.sendExecRestrictedReadReplyMessage(msg, allowed);
            if (!response.equals("Y")) {
                e.stop(StopCode.ConsoleResponseRequiresReboot);
                throw new ExecStoppedException();
            }
        }

        // TODO recover mass storage

        var elapsed = Duration.between(start, Instant.now()).getNano() / 1000 / 1000;
        var msg = String.format("Mass Storage Recovered %d MS.", elapsed);
        e.sendExecReadOnlyMessage(msg);
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

        var dasLoc = new DASLocation(addr);
        var dasAddr = dasLoc.getDASTrackAddress();
        var das = getMFDSector(dasAddr);
        var wx = (dasLoc._trackOffset * 3) + 1;
        var mask = 0_400000_000000L >> dasLoc._bit;
        das.set(wx, das.get(wx) | mask);
        markDirectorySectorDirty(dasAddr);

        var result = new MFDSector(addr, getMFDSector(addr));
        LogManager.logTrace(LOG_SOURCE, "allocateDirectorySector returning addr %s", addr);
        return result;
    }

    /**
     * Allocates fixed space from the requested LDAT and track id if it is unallocated;
     * else from the preferred LDAT is available, else from anywhere else available.
     * @param preferredLDATIndex requested LDAT; null if we don't care
     * @param preferredTrackId requested device-relative track-id; null if we don't care
     * @return LDAT index and device-relative track-id that was allocated, null if we are out of fixed space.
     */
    private HardwareTrackId allocateHardwareTrackId(
        final Integer preferredLDATIndex,
        final Long preferredTrackId
    ) {
        if (preferredLDATIndex != null) {
            var nodeInfo = _logicalDATable.get(preferredLDATIndex);
            if (nodeInfo != null) {
                var packInfo = (PackInfo) nodeInfo.getMediaInfo();
                if (packInfo.isFixed()) {
                    var fs = packInfo.getFreeSpace();
                    if (preferredTrackId != null) {
                        if (fs.markAllocated(preferredTrackId, 1)) {
                            return new HardwareTrackId(preferredLDATIndex, preferredTrackId);
                        }
                    }

                    var trackId = fs.allocateTrack();
                    if (trackId != null) {
                        return new HardwareTrackId(preferredLDATIndex, trackId);
                    }
                }
            }
        }

        for (var entry : _logicalDATable.entrySet()) {
            var ldat = entry.getKey();
            if ((preferredLDATIndex == null) || !Objects.equals(ldat, preferredLDATIndex)) {
                var nodeInfo = entry.getValue();
                var packInfo = (PackInfo) nodeInfo.getMediaInfo();
                if (packInfo.isFixed()) {
                    var fs = packInfo.getFreeSpace();
                    if (preferredTrackId != null) {
                        if (fs.markAllocated(preferredTrackId, 1)) {
                            return new HardwareTrackId(ldat, preferredTrackId);
                        }
                    }

                    var trackId = fs.allocateTrack();
                    if (trackId != null) {
                        return new HardwareTrackId(ldat, trackId);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Composes a lookup key from a given qualifier and filename.
     * All code which creates or uses look-up keys must invoke this, so that we can change it
     * if we think of a better algorithm.
     */
    private static String composeLookupKey(
        final String qualifier,
        final String filename
    ) {
        return qualifier + "*" + filename;
    }

    /**
     * Part of an initial boot - this code creates the artifacts which comprise the MFDF$$ file.
     * @param mfdAllocationSet FileAllocationSet describing the layout of the master file directory file's content.
     */
    private void createMFDFile(
        final FileAllocationSet mfdAllocationSet
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();
        try {
            var mfdQualifier = exec.getDefaultQualifier();
            var mfdFilename = "MFDF$$";
            var mfdProjectId = exec.getProjectId();
            var mfdAccountId = exec.getAccountId();
            var mfdEquip = cfg.getStringValue(Tag.MDFALT);
            var fFlags = new FileFlags().setIsLargeFile(true);
            var inhFlags = new InhibitFlags().setIsGuarded(true).setIsPrivate(true).setIsUnloadInhibited(true);
            var pchFlags = new PCHARFlags().setGranularity(Granularity.Track);
            var usInd = new UnitSelectionIndicators().setMultipleDevices(true).setInitialLDATIndex(01);

            var fsInfo = new FileSetInfo().setFileType(FileType.Fixed)
                                          .setQualifier(mfdQualifier)
                                          .setFilename(mfdFilename)
                                          .setProjectId(mfdProjectId)
                                          .setIsGuarded(true);

            var fcInfo = new FixedDiskFileCycleInfo();
            fcInfo.setUnitSelectionIndicators(usInd)
                  .setFileFlags(fFlags)
                  .setPCHARFlags(pchFlags)
                  .setQualifier(mfdQualifier)
                  .setFilename(mfdFilename)
                  .setProjectId(mfdProjectId)
                  .setAccountId(mfdAccountId)
                  .setAbsoluteCycle(1)
                  .setAssignMnemonic(mfdEquip)
                  .setInhibitFlags(inhFlags);

            var leadMFDSector = allocateDirectorySector();
            leadMFDSector.getSector().set(0, INVALID_LINK);
            fsInfo._leadItem0Address = leadMFDSector.getAddress();
            var luKey = composeLookupKey(fsInfo.getQualifier(), fsInfo.getFilename());
            _leadItemLookupTable.put(luKey, fsInfo);

            createFileCycle(fsInfo, fcInfo);
            _mfdFileAddress = fcInfo._mainItem0Address;

            // Create DAD tables for the MFD$$ file
            persistDADTables(_mfdFileAddress, mfdAllocationSet);

            // Assign MFDF$$ to the exec
            var fs = new FileSpecification(mfdQualifier, mfdFilename, null, null, null);
            var fr = new FacStatusResult();
            var result = exec.getFacilitiesManager().assignCatalogedDiskFileToExec(fs, true, fr);
            if (!result) {
                exec.stop(StopCode.FileAssignErrorOccurredDuringSystemInitialization);
                throw new ExecStoppedException();
            }

            var msg = String.format("Created %s*%s", mfdQualifier, mfdFilename);
            exec.sendExecReadOnlyMessage(msg);
        } catch (AbsoluteCycleOutOfRangeException
                 | AbsoluteCycleConflictException ex) {
            LogManager.logCatching(LOG_SOURCE, ex);
            Exec.getInstance().stop(StopCode.DirectoryErrors);
            throw new ExecStoppedException();
        }
    }

    /**
     * Drops the file cycle indicated my the main item address,
     * Releases DAD tables and/or reel tables if/as appropriate as well as all main item sectors.
     * Drops the file set if this file cycle is the only file cycle.
     * @param mainItem0Address MFD relative address of main item 0 sector for the file cycle.
     */
    private void dropCycle(
        final FileSetInfo fsInfo,
        final MFDRelativeAddress mainItem0Address
    ) throws ExecStoppedException {
        var miChain = getMainItemChain(mainItem0Address);
        var sector0 = miChain.getFirst().getSector();

        // Update the fileset first
        var fsChain = getLeadItemChain(fsInfo._leadItem0Address);
        if (fsInfo.getCycleCount() == 1) {
            // There is only the one cycle -
            for (var fce : fsChain) {
                releaseDirectorySector(fce.getAddress());
            }
        } else {
            // Are we deleting the highest or the lowest cycle? Note it for later.
            var ciList = fsInfo.getCycleInfo();
            boolean isHighest = ciList.getFirst().getMainItem0Address().equals(mainItem0Address);
            boolean isLowest = ciList.getFirst().getMainItem0Address().equals(mainItem0Address);

            // Remove the cycle info for the cycle to be deleted and update the current cycle count.
            var iter = ciList.iterator();
            while (iter.hasNext()) {
                var ci = iter.next();
                if (ci.getMainItem0Address().equals(mainItem0Address)) {
                    iter.remove();
                    fsInfo.setCycleCount(ciList.size());
                    break;
                }
            }

            // Re-evaluate whether plus-one-exists...
            // If it *did* exist, and we're deleting the highest cycle, then it no longer exists.
            if (fsInfo.plusOneExists() && isHighest) {
                fsInfo.setPlusOneExists(false);
            }

            // If the cycle being deleted is guarded, then we have to re-evaluate whether the file set is guarded,
            // which it is if any of the surviving cycles are guarded.
            var inh = new InhibitFlags().extract(sector0.getS2(021));
            if (fsInfo.isGuarded() && inh.isGuarded()) {
                fsInfo.setIsGuarded(false);

                // To do this, we have to read main item sector 0's until we find a guarded one,
                // or until we run out of main items.
                for (var ci : fsInfo.getCycleInfo()) {
                    var mainItems = getMainItemChain(ci.getMainItem0Address());
                    FileCycleInfo fci;
                    fci = switch (fsInfo.getFileType()) {
                        case FileType.Fixed -> new FixedDiskFileCycleInfo();
                        case FileType.Removable -> new RemovableDiskFileCycleInfo();
                        case FileType.Tape -> new TapeFileCycleInfo();
                    };

                    fci.populateMainItems(mainItems);
                    if (fci.getInhibitFlags().isGuarded()) {
                        fsInfo.setIsGuarded(true);
                        break;
                    }
                }
            }

            // If the cycle being deleted is the first or last, we need to re-evaluate the current cycle range.
            // Also re-evaluate the current highest cycle.
            if (isHighest || isLowest) {
                var highestAbsolute = ciList.getFirst().getAbsoluteCycle();
                var lowestAbsolute = ciList.getLast().getAbsoluteCycle();
                var newRange = highestAbsolute >= lowestAbsolute ?
                    highestAbsolute - lowestAbsolute + 1 : highestAbsolute + 999 - lowestAbsolute + 1;
                fsInfo.setCurrentCycleRange(newRange);
                fsInfo.setHighestAbsoluteCycle(ciList.getFirst().getAbsoluteCycle());
            }

            // Finally we need to rewrite the lead items.
            fsInfo.populateLeadItemSectors(fsChain);
            markDirectorySectorsDirty(fsChain);
        }

        // Now turn our attention to the mfd sectors for the file cycle
        var df = new DescriptorFlags().extract(sector0.getT1(014));
        if (df.isTapeFile()) {
            // lose reel table
            var reelChain = getReelTableChain(mainItem0Address);
            for (var rce : reelChain) {
                releaseDirectorySector(rce.getAddress());
            }
        } else {
            // lose DAD table
            var dadChain = getDADChain(mainItem0Address);
            for (var dce : dadChain) {
                releaseDirectorySector(dce.getAddress());
            }
        }

        // lose main items
        for (var ce : miChain) {
            releaseDirectorySector(ce.getAddress());
        }
    }

    /**
     * Adds a directory track to the current fixed MFD.
     * Chooses the pack which has the most free space for the new directory track.
     * @throws ExecStoppedException if something goes wrong
     */
    private void expandDirectory() throws ExecStoppedException {
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

        // Record-keeping stuff - if we get this far, there is at least one track
        // so we don't need to check devTrackId for null.
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
     * Retrieves the chain of main items indicated by the main item sector 0 address
     * @param mainItem0Address address of main item sector 0
     * @return main item chain containing references to one or more main items.
     * @throws ExecStoppedException if things go wonky.
     */
    private LinkedList<MFDSector> getMainItemChain(
        final MFDRelativeAddress mainItem0Address
    ) throws ExecStoppedException {
        var result = new LinkedList<MFDSector>();

        var mainItem0 = getMFDSector(mainItem0Address);
        result.add(new MFDSector(mainItem0Address, mainItem0));
        var link = mainItem0.get(013) & 0_007777_777777L;
        while (link != 0) {
            var addr = new MFDRelativeAddress(link & 0_007777_777777L);
            var mainItem = getMFDSector(addr);
            result.add(new MFDSector(addr, mainItem));
            link = mainItem.get(0) & 0_007777_777777L;
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
     * Retrieves a list of sectors which comprise the complete set of
     * reel tables for a cataloged tape file cycle.
     * @param mainItem0Address address of main item sector 0 for the file cycle.
     * @return list
     * @throws ExecStoppedException if something goes wrong
     */
    private LinkedList<MFDSector> getReelTableChain(
        final MFDRelativeAddress mainItem0Address
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "getReelTableChain(%s)", mainItem0Address.toString());
        var reelTableChain = new LinkedList<MFDSector>();

        var mainItem0 = getMFDSector(mainItem0Address);
        var dadLink = mainItem0.get(0) & 0_007777_777777;
        while (dadLink != 0) {
            var dadAddr = new MFDRelativeAddress(dadLink);
            var dad = getMFDSector(dadAddr);
            reelTableChain.add(new MFDSector(dadAddr, dad));
            dadLink = dad.get(0);
        }

        LogManager.logTrace(LOG_SOURCE, "getReelTableChain returning");
        return reelTableChain;
    }

    /**
     * Marks a directory sector as being dirty.
     * Since we do IO on track boundaries, we don't need to actually mark the sectors dirty,
     * just the containing tracks.
     * @param address MFD-relative sector address of dirty sector
     */
    private synchronized void markDirectorySectorDirty(
        final MFDRelativeAddress address
    ) {
        _dirtyCacheTracks.add((address.getValue() & 0_007777_777700L) >> 6);
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

        // Get the main item 0 sector so we can make sure the link is correct
        var mainItem0 = getMFDSector(mainItem0Address);

        // Go get the current chain of DAD entries.
        // It might be smaller or larger than we need (or non-existent), but we'll deal with that presently.
        var dadChain = getDADChain(mainItem0Address);

        // Special case - no space allocated. If there is a chain, release all the entries on the chain
        // and clear the link in the main item.
        var allocations = faSet.getFileAllocations();
        if (allocations.isEmpty()) {
            var w = mainItem0.get(0);
            w = (w | 0_400000_000000L) & 0_740000_000000L;
            mainItem0.set(0, w);
            markDirectorySectorDirty(mainItem0Address);

            for (var ds : dadChain) {
                releaseDirectorySector(ds.getAddress());
            }
            LogManager.logTrace(LOG_SOURCE, "persistDADTables return empty");
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
                        // and the main item needs to point here.
                        newDAD.set(1, mainItem0Address.getValue());
                        mainItem0.set(0, 0_200000_000000L | (newDADAddr.getValue() & 0_037777_777777L));
                        markDirectorySectorDirty(mainItem0Address);
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
            } else {
                // Otherwise, the previous entry maybe had the last-entry flag set. Unset it.
                int wx = (3 * (ex - 1)) + 4;
                var flags = currentDAD.getH1(wx + 2);
                flags &= 0_777773;
                currentDAD.setH1(wx + 2, flags);
            }

            // Does this entry immediately follow the previous entry in the file-relative address space?
            // If not, we need to create a hole DAD.
            long gapTrackCount = fileRegion.getTrackId() - expectedNextTrackId;
            if ((ax > 0) && (gapTrackCount > 0)) {
                // If this hole entry would go into the last entry of the DAD, there's no reason to create it.
                if (ex < 7) {
                    int wx = (3 * ex) + 4;
                    currentDAD.set(wx, 0); // no device-relative address for hole descriptors
                    currentDAD.set(wx + 1, gapTrackCount * 1792);
                    currentDAD.setH2(wx + 2, 0_400000);
                }
                ex++;
                expectedNextTrackId += gapTrackCount;
                continue; // go back through things again
            }

            // Now create a DAD entry (and update the last word + 1 in the header).
            // Always set this entry as the last in the DAD... we'll unset it on the next entry in the DAD
            //   if there is a next one, and leave it be if there isn't.
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

        markDirectorySectorsDirty(dadChain);
        LogManager.logTrace(LOG_SOURCE, "persistDADTables return");
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
        LogManager.logTrace(LOG_SOURCE, "persistLeadItems for %s*%s", fsInfo.getQualifier(), fsInfo.getFilename());

        var chain = getLeadItemChain(fsInfo._leadItem0Address);
        if (fsInfo.isSector1Required() && (chain.size() == 1)) {
            chain.add(allocateDirectorySector());
        }
        fsInfo.populateLeadItemSectors(chain);
        markDirectorySectorsDirty(chain);
    }

    /**
     * Persists the reel table as a set of reel table entries attached to the given main item 0.
     * Note that we do not persist entries 0 and 1, as they are persisted in main item 1.
     * @param mainItem0Address main item 0 address
     * @param reelTable list of reel number which comprise the cataloged tape file
     * @throws ExecStoppedException if something goes wrong
     */
    private void persistReelTables(
        final MFDRelativeAddress mainItem0Address,
        final LinkedList<String> reelTable
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "persistReelTables for %s", mainItem0Address.toString());

        // Go get the current chain of reel table entries.
        // It might be smaller or larger than we need (or non-existent), but we'll deal with that presently.
        var reelTableChain = getReelTableChain(mainItem0Address);

        // Special case - fewer than two reels in the file.
        // If there is a chain, release all the entries on the chain and clear the link in the main item.
        if (reelTable.size() < 2) {
            for (var ds : reelTableChain) {
                releaseDirectorySector(ds.getAddress());
            }
            LogManager.logTrace(LOG_SOURCE, "persistReelTables return empty");
            return;
        }

        int rx = 2;
        int rtx = -1;
        int ex = 8;
        MFDRelativeAddress currentReelTableAddr = null;
        ArraySlice currentReelTable = null;

        while (rx < reelTable.size()) {
            if (ex == 8) {
                // current entry is full, move on to the next one.
                // If there isn't a next one, we need to allocate a new one and put in on the chain.
                rtx++;
                if (rtx == reelTableChain.size()) {
                    var newReelTableSector = allocateDirectorySector();
                    var newReelTableAddr = newReelTableSector.getAddress();
                    var newReelTable = newReelTableSector.getSector();
                    IntStream.range(0, 28).forEach(x -> newReelTable._array[x] = 0);
                    reelTableChain.add(new MFDSector(newReelTableAddr, newReelTable));

                    // link this address to the previous entry (but only if this is not the first).
                    if (currentReelTable != null) {
                        currentReelTable.set(0, newReelTableAddr.getValue());
                        newReelTable.set(1, currentReelTableAddr.getValue());
                    } else {
                        // If it *is* the first, then the previous link needs to point to the main item
                        newReelTable.set(1, mainItem0Address.getValue());
                    }
                }
                currentReelTableAddr = reelTableChain.get(rtx).getAddress();
                currentReelTable = reelTableChain.get(rtx).getSector();
                ex = 0;
            }

            currentReelTable.set(2 + ex, Word36.stringToWordFieldata(reelTable.get(rx)));
            ex++;
            rx++;
        }

        // Release any unused entries, and ensure the last remaining entry does not have a forward link
        if (ex > 0) {
            rtx++;
        }
        while (rtx < reelTableChain.size()) {
            releaseDirectorySector(reelTableChain.get(rtx++).getAddress());
        }
        reelTableChain.getLast().getSector().set(0, 0);

        LogManager.logTrace(LOG_SOURCE, "persistReelTables return");
    }

    /**
     * Releases an MFD directory sector.
     * Clears the DAS bit for the sector, and adds the MFD relative address to the available chain.
     */
    private void releaseDirectorySector(
        final MFDRelativeAddress address
    ) throws ExecStoppedException {
        var dasLoc = new DASLocation(address);
        var dasAddr = dasLoc.getDASTrackAddress();
        var das = getMFDSector(dasAddr);
        var wx = (dasLoc._trackOffset * 3) + 1;
        var mask = 0_400000_000000L >> dasLoc._bit;
        das.set(wx, ~((~das.get(wx)) | mask));
        markDirectorySectorDirty(dasAddr);
        _freeMFDSectors.add(address);
    }

    /**
     * Writes all dirty cache tracks to underlying disk storage
     * @throws ExecStoppedException if something goes wrong
     */
    private void writeDirtyCacheTracks() throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "writeDirtyCacheTracks() enter");
        // use direct IO - don't need to use fac mgr, other than it needs to be assigned
        // *by* fac mgr so that the fcinfo is accelerated.
        var acInfo = _acceleratedFileCycles.get(_mfdFileAddress);
        var faSet = acInfo.getFileAllocationSet();
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();

        var channelPacket = new ChannelIoPacket().setIoFunction(IoFunction.Write)
                                                 .setFormat(TransferFormat.Packed);
        var iter = _dirtyCacheTracks.iterator();
        while (iter.hasNext()) {
            var mfdRelativeTrackId = iter.next();
            iter.remove();
            var hwTid = faSet.resolveFileRelativeTrackId(mfdRelativeTrackId);
            var ldat = hwTid.getLDATIndex();
            var trackId = hwTid.getTrackId();
            var nodeInfo = _logicalDATable.get(ldat);

            // set up IO
            var mfdRelAddr = new MFDRelativeAddress(mfdRelativeTrackId << 6);
            channelPacket.setBuffer(_cachedMFDTracks.get(mfdRelAddr))
                         .setNodeIdentifier(nodeInfo.getNode().getNodeIdentifier())
                         .setDeviceWordAddress(trackId * 1792);
            try {
                fm.routeIo(channelPacket);
            } catch (NoRouteForIOException ex) {
                exec.stop(StopCode.InternalExecIOFailed);
                throw new ExecStoppedException();
            }
        }
        LogManager.logTrace(LOG_SOURCE, "writeDirtyCacheTracks() exit");
    }
}
