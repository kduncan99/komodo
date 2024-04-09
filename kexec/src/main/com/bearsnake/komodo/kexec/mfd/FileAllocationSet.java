/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import static com.bearsnake.komodo.kexec.exec.Exec.INVALID_LDAT;

import com.bearsnake.komodo.kexec.HardwareTrackId;

import java.util.LinkedList;

public class FileAllocationSet {

    private MFDRelativeAddress _dadItem0Address;
    private MFDRelativeAddress _mainItem0Address;
    private final LinkedList<FileAllocation> _fileAllocations = new LinkedList<>();
    private boolean _isUpdated;

    public FileAllocationSet(final MFDRelativeAddress dataItem0Address,
                             final MFDRelativeAddress mainItem0Address) {
        _dadItem0Address = dataItem0Address;
        _mainItem0Address = mainItem0Address;
        _isUpdated = false;
    }

    public MFDRelativeAddress getDadItem0Address() { return _dadItem0Address; }
    LinkedList<FileAllocation> getFileAllocations() { return _fileAllocations; }
    public MFDRelativeAddress getMainItem0Address() { return _mainItem0Address; }
    public boolean isUpdated() { return _isUpdated; }

    public synchronized long getHighestTrackAllocated() {
        return _fileAllocations.isEmpty() ? -1 : _fileAllocations.getLast().getFileRegion().getHighestTrack();
    }

    /**
     * Extracts the allocation (combination of file-relative track id and track count) described by
     * the given region from this file allocation set.
     * Caller MUST ensure that the requested region is a subset (or a match) of exactly one existing
     * file allocation.
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

    public void mergeIntoFileAllocationSet(final FileAllocation newEntry) {
        var newRegion = newEntry.getFileRegion();
        var newHWTrk = newEntry.getHardwareTrackId();
        for (int fax = 0; fax < _fileAllocations.size(); fax++) {
            var fa = _fileAllocations.get(fax);
            if (fa.merge(newEntry)) {
                _isUpdated = true;
                return;
            }

            var faRegion = fa.getFileRegion();
            var faHWTrk = fa.getHardwareTrackId();
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
     *  TODO change name to findContainingAllocation()
     * Finds the FileAllocation entry which contains the indicated file-relative track id.
     * @param fileTrackId file-relative track id we are interested in
     * @return containing FileAllocation entry, or nil if the track is not allocated
     */
    public synchronized FileAllocation findPrecedingAllocation(final long fileTrackId) {
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
