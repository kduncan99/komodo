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
    public MFDRelativeAddress getMainItem0Address() { return _mainItem0Address; }
    public boolean isUpdated() { return _isUpdated; }

    public synchronized long getHighestTrackAllocated() {
        return _fileAllocations.isEmpty() ? -1 : _fileAllocations.getLast().getFileRegion().getHighestTrack();
    }

    // For unit testing
    LinkedList<FileAllocation> getFileAllocations() { return _fileAllocations; }

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

    /*
    // FindPrecedingAllocation retrieves the FileAllocation which immediately precedes or contains the
// indicated file-relative track id. If we return nil, there is no such FileAllocation.
    func (fas *FileAllocationSet) FindPrecedingAllocation(
    fileTrackId hardware.TrackId,
        ) (alloc *FileAllocation) {
        alloc = nil
        for _, fa := range fas.FileAllocations {
            if fileTrackId >= fa.FileRegion.TrackId {
                alloc = fa
            } else {
                break
            }
        }
        return
    }

    // GetHighestTrackAssigned finds that value from the FileAllocationSet
    func (fas *FileAllocationSet) GetHighestTrackAssigned() hardware.TrackId {
        entryCount := len(fas.FileAllocations)
        if entryCount == 0 {
            return 0
        } else {
            last := entryCount - 1
            fAlloc := fas.FileAllocations[last]
            return fAlloc.FileRegion.TrackId + hardware.TrackId(fAlloc.FileRegion.TrackCount) - 1
        }
    }

    // resolveFileRelativeTrackId converts a file-relative track id (file-relative sector address * 28,
// or file-relative word address * 1792) to the LDAT index of the pack which contains that track,
// and to the corresponding device/pack-relative track ID.
// If we return false, no allocation exists (the space has not (yet) been allocated).
    func (fas *FileAllocationSet) resolveFileRelativeTrackId(
    fileTrackId hardware.TrackId,
        ) (LDATIndex, hardware.TrackId, bool) {
        for _, fa := range fas.FileAllocations {
            highestAllocTrack := hardware.TrackId(uint64(fa.FileRegion.TrackId) + uint64(fa.FileRegion.TrackCount) - 1)
            if fileTrackId >= fa.FileRegion.TrackId && fileTrackId <= highestAllocTrack {
                offset := fileTrackId - fa.FileRegion.TrackId
                return fa.LDATIndex, fa.DeviceTrackId + offset, true
            } else if highestAllocTrack < fileTrackId {
                break
            }
        }

        return 0, 0, false
    }
     */
}
