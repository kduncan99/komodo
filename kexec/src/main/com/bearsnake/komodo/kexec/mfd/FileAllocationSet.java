/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import static com.bearsnake.komodo.kexec.exec.Exec.INVALID_LDAT;

import com.bearsnake.komodo.kexec.HardwareTrackId;

import java.util.LinkedList;

public class FileAllocationSet {

    private final LinkedList<FileAllocation> _fileAllocations = new LinkedList<>();
    private boolean _isUpdated;

    public FileAllocationSet() {
        _isUpdated = false;
    }

    public LinkedList<FileAllocation> getFileAllocations() { return _fileAllocations; }
    public boolean isUpdated() { return _isUpdated; }
    public void setIsUpdated(final boolean value) { _isUpdated = value; }

    public synchronized long getHighestTrackAllocated() {
        return _fileAllocations.isEmpty() ? -1 : _fileAllocations.getLast().getFileRegion().getHighestTrack();
    }

    /**
     * Creates a FileAllocationSet to represent the content of a DAD chain
     */
    public static FileAllocationSet createFromDADChain(
        final LinkedList<MFDSector> dadChain
    ) {
        var fas = new FileAllocationSet();
        for (var msDAD : dadChain) {
            var dad = msDAD.getSector();
            var frAddress = dad.get(2); // file-relative address of first word described in this DAD
            var frLast = dad.get(3);    // file-relative address of last word + 1
            for (int wx = 4; wx < 28; wx += 3) {
                long devAddr = dad.get(wx);
                long wordCount = dad.get(wx + 1);
                int dadFlags = (int)dad.getH1(wx + 2);
                boolean lastEntry = (dadFlags & 04) != 0;
                int ldatIndex = (int)dad.getH2(wx + 2);

                if (ldatIndex != 0_400000) {
                    // this is not a hole-DAD... create an FA
                    var region = new LogicalTrackExtent(frAddress / 1792, wordCount / 1792);
                    var hwTid = new HardwareTrackId(ldatIndex, devAddr / 1792);
                    var fa = new FileAllocation(region, hwTid);
                    fas.mergeIntoFileAllocationSet(fa);
                }

                frAddress += wordCount;
                if (lastEntry || (frAddress >= frLast)) {
                    break;
                }
            }
        }

        return fas;
    }

    /**
     * Extracts the allocation (combination of file-relative track id and track count) described by
     * the given region from this file allocation set.
     * Caller MUST ensure that the requested region is a subset (or a match) of exactly one existing
     * file allocation.
     * This is used in the process of releasing space from a file.
     * @param region Describes the region to be extracted
     * @return HardwareTrackId containing the LDAT index and devivce-relative track ID describing the
     * physical location of the first track in the requested region.
     */
    public synchronized HardwareTrackId extractRegionFromFileAllocationSet(final LogicalTrackExtent region) {
        for (int fax = 0; fax < _fileAllocations.size(); ++fax) {
            var fa = _fileAllocations.get(fax);
            var faRegion = fa.getFileRegion();
            var hwTrk = fa.getHardwareTrackId();
            if (faRegion.getTrackId() == region.getTrackId()) {
                var ldatIndex = hwTrk.getLDATIndex();
                var deviceTrackId = hwTrk.getTrackId();

                if (faRegion.getTrackCount() == region.getTrackCount()) {
                    // deallocating the entire file allocation
                    _fileAllocations.remove(fax);
                } else {
                    // deallocating from the front of the file allocation
                    faRegion.addToTrackId(region.getTrackCount());
                    hwTrk.addToTrackId(region.getTrackCount());
                    faRegion.addToTrackCount(-region.getTrackCount());
                }

                _isUpdated = true;
                return new HardwareTrackId(ldatIndex, deviceTrackId);
            } else if (faRegion.getTrackId() < region.getTrackId()) {
                var ldat = hwTrk.getLDATIndex();
                var devTrackId = hwTrk.getTrackId() + (region.getTrackId() - faRegion.getTrackId());

                var entryLimit = region.getTrackId() + region.getTrackCount();
                var allocLimit = faRegion.getTrackId() + faRegion.getTrackCount();
                if (entryLimit == allocLimit) {
                    // we are deallocating from the back of the file allocation
                    faRegion.addToTrackCount(-region.getTrackCount());
                } else {
                    // we are deallocating from inside the existing file allocation
                    // with tracks remaining ahead and behind
                    var newTrackId = entryLimit;
                    var newTrackCount = allocLimit - entryLimit;
                    var newRegion = new LogicalTrackExtent(newTrackId, newTrackCount);
                    var newDevTrkId = hwTrk.getTrackId() + (newTrackId - faRegion.getTrackId());
                    var newHWTrkId = new HardwareTrackId(ldat, newDevTrkId);
                    var newAlloc = new FileAllocation(newRegion, newHWTrkId);

                    faRegion.setTrackCount(region.getTrackId() - faRegion.getTrackId());
                    _fileAllocations.add(fax + 1, newAlloc);
                }

                _isUpdated = true;
                return new HardwareTrackId(ldat, devTrackId);
            }
        }

        return new HardwareTrackId(INVALID_LDAT, 0);
    }

    /**
     * Merges a particular file allocation into the file allocation set.
     * Used for noting additional allocations for a file.
     * @param newEntry new allocation
     */
    public void mergeIntoFileAllocationSet(final FileAllocation newEntry) {
        var newRegion = newEntry.getFileRegion();
        for (int fax = 0; fax < _fileAllocations.size(); fax++) {
            var fa = _fileAllocations.get(fax);
            if (fa.merge(newEntry)) {
                _isUpdated = true;
                return;
            }

            var faRegion = fa.getFileRegion();
            if (newRegion.getTrackId() < faRegion.getTrackId()) {
                // the new entry appears before the indexed entry and after the previous entry,
                // and is not contiguous with the previous entry, nor with the next.
                // splice it in place as a separate entry.
                _fileAllocations.add(fax, newEntry);
                _isUpdated = true;
                return;
            }
        }

        // If we get here, the new entry is definitely not contiguous with any existing entry.
        _fileAllocations.add(newEntry);
        _isUpdated = true;
    }

    /**
     * Finds the FileAllocation entry which contains the indicated file-relative track id.
     * @param fileTrackId file-relative track id we are interested in
     * @return containing FileAllocation entry, or nil if the track is not allocated
     */
    public synchronized FileAllocation findContainingAllocation(final long fileTrackId) {
        return _fileAllocations.stream()
                               .filter(fa -> fa.containsFileRelativeTrack(fileTrackId))
                               .findFirst()
                               .orElse(null);
    }

    /**
     * Finds the track ID of the highest file-relative track.
     * @return highest track ID, or null if no tracks are allocated.
     */
    public synchronized Long getHighestTrackAssigned() {
        return _fileAllocations.isEmpty() ? null : _fileAllocations.getLast().getFileRegion().getHighestTrack();
    }

    /**
     * Converts the file-relative track ID to the corresponding pack LDAT and device track.
     * @param fileRelativeTrackId file-relative track ID
     * @return HardwareTrackId containing LDAT index and device track if the track is allocated, else null.
     */
    public synchronized HardwareTrackId resolveFileRelativeTrackId(final long fileRelativeTrackId) {
        for (var fa : _fileAllocations) {
            if (fa.containsFileRelativeTrack(fileRelativeTrackId)) {
                long offset = fileRelativeTrackId - fa.getFileRegion().getTrackId();
                var hwTid = fa.getHardwareTrackId();
                return new HardwareTrackId(hwTid.getLDATIndex(), hwTid.getTrackId() + offset);
            }
        }

        return null;
    }
}
