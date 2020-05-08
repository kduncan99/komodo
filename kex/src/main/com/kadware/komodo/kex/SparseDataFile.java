/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex;

import com.kadware.komodo.baselib.Word36;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a sparse data file - intended for storing things external to the emulator,
 * in a manner easily-translated into the komodo world.
 *
 * For a SparseDataFile, we allocate and track space in resolutions of 1 track, a track being comprised of 64 sectors.
 * Our view of the underlying data file is provided by the super class, thus we do all of our IOs in terms of WORD36 sectors.
 * All track allocations are noted in a track allocation table, which is comprised of a number of track allocation entries
 * spread througout the file.
 *
 * All IOs are still sector-id oriented; we translate sector identifiers into track identifiers and sector offsets in order
 * to find the underlying sector ids to do the appropriate IOs.
 *
 * Representation on media:
 *  The entire track allocation table (TAT) is stored in the first {n} contiguous tracks of the file.
 *  As the TAT is expanded, data tracks may be reallocated to make room for the expanded TAT.
 *  The TAT is formatted as such:
 *      +0      number of valid entries in the TAT (entries are never invalidated)
 *      +2      first two-word entry
 *                  first word = logical track identifier
 *                  second word = physical track identifier
 *      +4->{n} subsequent two-word entries
 */
public class SparseDataFile extends DataFile {

    private final int SECTORS_PER_TRACK = 64;
    private final int WORDS_PER_TRACK = WORDS_PER_SECTOR * SECTORS_PER_TRACK;

    //  Key is logical track id, which is essentially the logical sector id >> 6.
    //  Value is the underlying physical track id where the logical track can be found.
    private final TreeMap<Long, Long> _logicalTrackAllocations = new TreeMap<>();

    public SparseDataFile(
        final File file
    ) {
        super(file);
    }

    public SparseDataFile(
        final String fileName
    ) {
        super(fileName);
    }

    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Retrieves the physical track id of the first track which is not in use
     */
    private long getFreePhysicalTrackId() {
        long candidate = 1;
        while (_logicalTrackAllocations.containsValue(candidate)) {
            ++candidate;
        }
        return candidate;
    }

    /**
     * Translates a logical track id to a physical track id.
     * If the logical track is not yet allocated, we allocate it.
     */
    private long getPhysicalTrackId(
        final long logicalTrackId
    ) {
        if (_logicalTrackAllocations.containsKey(logicalTrackId)) {
            return _logicalTrackAllocations.get(logicalTrackId);
        } else {
            long physicalTrackId = getFreePhysicalTrackId();
            _logicalTrackAllocations.put(logicalTrackId, physicalTrackId);
            return physicalTrackId;
        }
    }

    /**
     * Loads track allocations from the underlying file, presuming a TAT exists.
     */
    private void loadTrackAllocationTable(
    ) throws IOException {
        //  Process first TAT track (physical track 0)
        long tatTrackId = 0;
        Word36[] buffer = super.readSectors(tatTrackId << 6, SECTORS_PER_TRACK);
        long remainingEntries = buffer[0].getW();
        for (int wx = 2; (wx < WORDS_PER_TRACK) && (remainingEntries > 0); wx += 2, remainingEntries--) {
            long logicalTrackId = buffer[wx].getW();
            long physicalTrackId = buffer[wx + 1].getW();
            _logicalTrackAllocations.put(logicalTrackId, physicalTrackId);
        }

        while (remainingEntries > 0) {
            ++tatTrackId;
            buffer = super.readSectors(tatTrackId << 6, SECTORS_PER_TRACK);
            for (int wx = 0; (wx < WORDS_PER_TRACK) && (remainingEntries > 0); wx += 2, remainingEntries--) {
                long logicalTrackId = buffer[wx].getW();
                long physicalTrackId = buffer[wx + 1].getW();
                _logicalTrackAllocations.put(logicalTrackId, physicalTrackId);
            }
        }
    }

    /**
     * Writes the track allocation table to track allocation sectors in the underlying file
     */
    private void writeTrackAllocationTable(
    ) throws IOException {
        //  do we have more entries than will fit in the current TAT tracks?
        //  If so, rearrange the deck chairs until we have enough space at the front of the file
        //  to store the entire TAT.
        int tatWords = 2 + (2 * _logicalTrackAllocations.size());
        int tatTracks = tatWords / WORDS_PER_TRACK;
        if (tatWords % WORDS_PER_TRACK > 0) {
            tatTracks++;
        }
        long highestPhysicalTATTrack = tatTracks - 1;

        //  If necessary, move data tracks out of the way
        Iterator<Map.Entry<Long, Long>> iter = _logicalTrackAllocations.entrySet().iterator();
        long nextFreePhysicalTrackId = getFreePhysicalTrackId();
        while (iter.hasNext()) {
            Map.Entry<Long, Long> entry = iter.next();
            long entryLogicalTrackId = entry.getKey();
            long entryPhysicalTrackId = entry.getValue();
            if (entryLogicalTrackId <= highestPhysicalTATTrack) {
                long newEntryPhysicalTrackId = nextFreePhysicalTrackId++;
                Word36[] buffer = super.readSectors(entryPhysicalTrackId << 6, SECTORS_PER_TRACK);
                super.writeSectors(newEntryPhysicalTrackId << 6, SECTORS_PER_TRACK, buffer);
                _logicalTrackAllocations.put(entryLogicalTrackId, newEntryPhysicalTrackId);
            }
        }

        //  Now write the TAT
        Word36[] tatBuffer = new Word36[tatTracks * WORDS_PER_TRACK];
        tatBuffer[0] = new Word36(_logicalTrackAllocations.size());
        tatBuffer[1] = new Word36();
        int tbx = 0;
        for (Map.Entry<Long, Long> entry : _logicalTrackAllocations.entrySet()) {
            long entryLogicalTrackId = entry.getKey();
            long entryPhysicalTrackId = entry.getValue();
            tatBuffer[tbx++] = new Word36(entryLogicalTrackId);
            tatBuffer[tbx++] = new Word36(entryPhysicalTrackId);
        }

        super.writeSectors(0, tatTracks * SECTORS_PER_TRACK, tatBuffer);
    }

    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public void clear(
    ) throws IOException {
        super.clear();
        _logicalTrackAllocations.clear();
    }

    @Override
    public void close(
    ) throws IOException {
        writeTrackAllocationTable();
        super.close();
    }

    @Override
    public long getHighestSectorWritten() {
        if (_logicalTrackAllocations.isEmpty()) {
            return -1;
        } else {
            return _logicalTrackAllocations.firstEntry().getKey();
        }
    }

    @Override
    public void open(
    ) throws IOException {
        if (super.getHighestSectorWritten() == -1) {
            _logicalTrackAllocations.clear();
        } else {
            loadTrackAllocationTable();
        }
    }

    @Override
    public Word36[] readSectors(
        final long sectorId,
        final int sectorCount
    ) throws IOException {
        //  break this up into track-aligned IOs
        Word36[] result = new Word36[sectorCount * WORDS_PER_SECTOR];
        long ioLogicalSectorId = sectorId;
        int sectorsLeft = sectorCount;
        int destIndex = 0;
        while (sectorsLeft > 0) {
            long ioLogicalTrackId = ioLogicalSectorId >> 6;
            int sectorOffset = (int)(ioLogicalSectorId & 077);
            int ioSectorCount = SECTORS_PER_TRACK - sectorOffset;

            Long ioActualTrackId = _logicalTrackAllocations.get(ioLogicalTrackId);
            if (ioActualTrackId == null) {
                throw new TrackNotAllocatedException(ioLogicalTrackId);
            }

            long ioSectorId = ioActualTrackId << 6;
            Word36[] subResult = super.readSectors(ioSectorId, ioSectorCount);
            for (int sx = 0; sx < subResult.length; ++sx, ++destIndex) {
                result[destIndex] = subResult[sx];
            }

            sectorsLeft -= ioSectorCount;
            ioLogicalSectorId += ioSectorCount;
        }

        return result;
    }

    @Override
    public void writeSectors(
        final long sectorId,
        final int sectorCount,
        final Word36[] buffer
    ) throws IOException {
        //  break this up into track-aligned IOs
        long ioLogicalSectorId = sectorId;
        int sectorsLeft = sectorCount;
        int sourceIndex = 0;
        while (sectorsLeft > 0) {
            long ioLogicalTrackId = ioLogicalSectorId >> 6;
            int sectorOffset = (int)(ioLogicalSectorId & 077);
            int ioSectorCount = SECTORS_PER_TRACK - sectorOffset;

            long ioActualTrackId = getPhysicalTrackId(ioLogicalTrackId);
            long ioSectorId = ioActualTrackId << 6;
            Word36[] subBuffer = new Word36[ioSectorCount * WORDS_PER_SECTOR];
            for (int dx = 0; dx < subBuffer.length; ++dx, ++sourceIndex) {
                subBuffer[dx] = buffer[sourceIndex];
            }

            super.writeSectors(ioSectorId, ioSectorCount, subBuffer);
            sectorsLeft -= ioSectorCount;
            ioLogicalSectorId += ioSectorCount;
        }
    }
}
