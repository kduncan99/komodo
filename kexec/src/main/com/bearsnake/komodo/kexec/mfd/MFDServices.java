package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.kexec.apis.IMFDManager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;

public class MFDServices implements IMFDManager {

    private static final String LOG_SOURCE = MFDManager.LOG_SOURCE;

    private final MFDManager _mgr;

    public MFDServices(MFDManager manager) {
        _mgr = manager;
    }

    // -------------------------------------------------------------------------
    // Service API
    // -------------------------------------------------------------------------

    /*
// AccelerateFileCycle loads information about a particular file cycle -- primarily DAD tables --
// into memory. This is for facilities to invoke when a file gets assigned.
// This should correspond one-for-one with assigning a file to a run.
// In doing this, we update the assign count in the main item for the file cycle.
func (mgr *MFDManager) AccelerateFileCycle(
	fcIdentifier FileCycleIdentifier,
) MFDResult {
	klog.LogTraceF("MFDMgr", "AccelerateFileCycle %012o", fcIdentifier)
	mgr.mutex.Lock()
	defer mgr.mutex.Unlock()

	mainItem0Addr := kexec.MFDRelativeAddress(fcIdentifier)
	mainItem0, err := mgr.getMFDSector(mainItem0Addr)
	if err != nil {
		return MFDInternalError
	}

	// increment total times assigned *and* assigned indicator
	mainItem0[017].SetH1(mainItem0[017].GetH1() + 1)
	mainItem0[021].SetT2(mainItem0[021].GetT2() + 1)
	mgr.markDirectorySectorDirty(mainItem0Addr)

	_, err = mgr.loadFileAllocationSet(mainItem0Addr)
	if err != nil {
		return MFDInternalError
	}

	return MFDSuccessful
}

func (mgr *MFDManager) ChangeFileSetName(
	leadItem0Address kexec.MFDRelativeAddress,
	newQualifier string,
	newFilename string,
) error {
	// TODO implement ChangeFileSetName()
	return nil
}

// CreateFileSet creates lead items to establish an empty file set.
// When the MFD is in a normalized state, no empty file sets should exist -
// hence we expect the client to subsequently create a file as part of the file set.
// If we return MFDInternalError, the exec has been stopped
// If we return MFDFileNameConflict, a file set already exists with this file name.
func (mgr *MFDManager) CreateFileSet(
	fileType FileType,
	qualifier string,
	filename string,
	projectId string,
	readKey string,
	writeKey string,
) (fsIdentifier FileSetIdentifier, result MFDResult) {
	leadItem0Address := kexec.InvalidLink
	result = MFDSuccessful

	mgr.mutex.Lock()
	defer mgr.mutex.Unlock()

	key := qualifier + "*" + filename
	_, ok := mgr.fileLeadItemLookupTable[key]
	if ok {
		result = MFDFileNameConflict
		return
	}

	leadItem0Address, leadItem0, err := mgr.allocateDirectorySector(kexec.InvalidLDAT)
	if err != nil {
		result = MFDInternalError
		return
	}

	leadItem0[0].SetW(uint64(kexec.InvalidLink))
	leadItem0[0].Or(0_500000_000000)

	pkg.FromStringToFieldata(qualifier, leadItem0[1:3])
	pkg.FromStringToFieldata(filename, leadItem0[3:5])
	pkg.FromStringToFieldata(projectId, leadItem0[5:7])
	leadItem0[7].FromStringToFieldata(readKey)
	leadItem0[8].FromStringToAscii(writeKey)
	leadItem0[9].SetS1(uint64(fileType))

	mgr.markDirectorySectorDirty(leadItem0Address)
	fsIdentifier = FileSetIdentifier(leadItem0Address)

	return
}

// CreateFixedFileCycle creates a new file cycle within the file set specified by fsIdentifier.
// fcSpecification is nil if no file cycle was specified.
// Returns
//
//		FileCycleIdentifier of the newly-created file cycle if successful, and
//		MFDResult values which may include
//	     MFDSuccessful if things went well
//	     MFDInternalError if something is badly wrong, and we've stopped the exec
//	     MFDAlreadyExists if user does not specify a cycle, and a file cycle already exists
//		     MFDInvalidRelativeFileCycle if the caller specified a negative relative file cycle
//	      MFDInvalidAbsoluteFileCycle
//	         any file cycle out of range
//	         an absolute file cycle which conflicts with an existing cycle
//	     MFDPlusOneCycleExists if caller specifies +1 and a +1 already exists for the file set
//	     MFDDropOldestCycleRequired is returned if everything else would be fine if the oldest file cycle did not exist
func (mgr *MFDManager) CreateFixedFileCycle(
	fsIdentifier FileSetIdentifier,
	fcSpecification *kexec.FileCycleSpecification,
	accountId string,
	assignMnemonic string,
	descriptorFlags DescriptorFlags,
	pcharFlags PCHARFlags,
	inhibitFlags InhibitFlags,
	initialReserve uint64,
	maxGranules uint64,
	diskPacks []DiskPackEntry,
) (fcIdentifier FileCycleIdentifier, result MFDResult) {
	result = MFDSuccessful

	mgr.mutex.Lock()
	defer mgr.mutex.Unlock()

	leadItem0Addr := kexec.MFDRelativeAddress(fsIdentifier)
	leadItem1Addr, leadItem0, leadItem1, err := mgr.getLeadItems(leadItem0Addr)
	if err != nil {
		result = MFDInternalError
		return
	}

	// get a FileSetInfo for convenience
	fsInfo := &FileSetInfo{}
	fsInfo.FileSetIdentifier = fsIdentifier
	fsInfo.populateFromLeadItems(leadItem0, leadItem1)

	absCycle, cycIndex, shift, newRange, plusOne, result := mgr.checkCycle(fcSpecification, fsInfo)
	if result != MFDSuccessful {
		return
	}

	// Do we need to allocate a lead item sector 1?
	if leadItem1 == nil && newRange > 28-(11+fsInfo.NumberOfSecurityWords) {
		leadItem1Addr, leadItem1, err = mgr.allocateLeadItem1(leadItem0Addr, leadItem0)
		if err != nil {
			result = MFDInternalError
			return
		}
	}

	// Do we need to shift links?
	if shift > 0 {
		adjustLeadItemLinks(leadItem0, leadItem1, shift)
		mgr.markDirectorySectorDirty(leadItem0Addr)
		mgr.markDirectorySectorDirty(leadItem1Addr)
	}

	if plusOne {
		leadItem0[012].Or(0_200000_000000)
	}

	// Create necessary main items for the new file cycle
	preferredLDAT := getLDATIndexFromMFDAddress(leadItem0Addr)
	mainItem0Addr, mainItem0, err := mgr.allocateDirectorySector(preferredLDAT)
	if err != nil {
		result = MFDInternalError
		return
	}
	mainItem1Addr, mainItem1, err := mgr.allocateDirectorySector(preferredLDAT)
	if err != nil {
		result = MFDInternalError
		return
	}

	packNames := make([]string, 0)
	if diskPacks != nil {
		for _, dp := range diskPacks {
			packNames = append(packNames, dp.PackName)
		}
	}

	populateMassStorageMainItem0(
		mainItem0,
		leadItem0Addr,
		mainItem1Addr,
		fsInfo.Qualifier,
		fsInfo.Filename,
		uint64(absCycle),
		fsInfo.ReadKey,
		fsInfo.WriteKey,
		fsInfo.ProjectId,
		accountId,
		assignMnemonic,
		descriptorFlags,
		pcharFlags,
		inhibitFlags,
		false,
		initialReserve,
		maxGranules)

	populateFixedMainItem1(
		mainItem1,
		fsInfo.Qualifier,
		fsInfo.Filename,
		mainItem0Addr,
		uint64(absCycle))

	// If this cycle is guarded, make the file set guarded.
	// Don't worry about marking lead item dirty - that happens anyway (further below).
	if inhibitFlags.IsGuarded {
		leadItem0[012] |= 0_400000_000000
	}

	// Link the new file cycle into the lead item
	lw := getLeadItemLinkWord(leadItem0, leadItem1, cycIndex)
	lw.SetW(uint64(mainItem0Addr))
	// update count only if this new cycle is *not* to-be-cataloged
	if !descriptorFlags.ToBeCataloged {
		leadItem0[011].SetS2(leadItem0[011].GetS2() + 1)
	}
	leadItem0[011].SetS4(uint64(newRange))
	if absCycle > uint(leadItem0[011].GetT3()) {
		leadItem0[011].SetT3(uint64(absCycle))
	}

	mgr.markDirectorySectorDirty(leadItem0Addr)
	if leadItem1 != nil {
		mgr.markDirectorySectorDirty(leadItem1Addr)
	}

	fsIdentifier = FileSetIdentifier(mainItem0Addr)
	return
}

func (mgr *MFDManager) CreateRemovableFileCycle(
	fsIdentifier FileSetIdentifier,
	fcSpecification *kexec.FileCycleSpecification,
	accountId string,
	assignMnemonic string,
	descriptorFlags DescriptorFlags,
	pcharFlags PCHARFlags,
	inhibitFlags InhibitFlags,
	initialReserve uint64,
	maxGranules uint64,
	diskPacks []DiskPackEntry,
) (fcIdentifier FileCycleIdentifier, result MFDResult) {
	// TODO implement CreateRemovableFileCycle()
	return
}

// DecelerateFileCycle decrements the assign count of a particular file cycle.
// If that count reaches zero we decelerate the file cycle's allocation information from memory,
// and (maybe) take to-be action.
// For to-be-deleted, the action is to drop the cycle if and only if preventToBeAction is false.
// For to-be-cataloged, the action is to drop the cycle if and only if preventToBeAction is true.
// For to-be-*-only, the action is to set the appropriate file cycle flags regardless of preventToBeAction.
func (mgr *MFDManager) DecelerateFileCycle(
	fcIdentifier FileCycleIdentifier,
	preventToBeAction bool,
) MFDResult {
	mgr.mutex.Lock()
	defer mgr.mutex.Unlock()

	mainItem0Addr := kexec.MFDRelativeAddress(fcIdentifier)
	mainItem0, err := mgr.getMFDSector(mainItem0Addr)
	if err != nil {
		return MFDInternalError
	}

	asgCount := mainItem0[021].GetT2()
	if asgCount == 0 {
		log.Printf("MFDMgr:Decelerate called but asg count is zero for %012o", mainItem0Addr)
		mgr.exec.Stop(kexec.StopDirectoryErrors)
		return MFDInternalError
	}

	asgCount--
	mainItem0[021].SetT2(asgCount)

	if asgCount == 0 {
		delete(mgr.acceleratedFileAllocations, mainItem0Addr)
		dFlags := ExtractNewDescriptorFlags(mainItem0[014].GetT1())
		fileDropped := false
		if preventToBeAction {
			if dFlags.ToBeCataloged {
				result := mgr.dropFileCycle(mainItem0Addr)
				if result != MFDSuccessful {
					return result
				}
				fileDropped = true
			}
		} else {
			if dFlags.ToBeDropped {
				result := mgr.dropFileCycle(mainItem0Addr)
				if result != MFDSuccessful {
					return result
				}
				fileDropped = true
			}
		}

		if !fileDropped {
			// update lead item (one or the other of them)
			leadItem0Addr, leadItem1Addr, leadItem0, leadItem1, err := mgr.getLeadItemsForMainItem(mainItem0)
			if err != nil {
				return MFDInternalError
			}

			absCycle := uint(mainItem0[021].GetT3())
			linkWord, _ := getLeadItemLinkWordForCycle(leadItem0, leadItem1, absCycle)
			linkWord.Or(0_077777_777777)
			mgr.markDirectorySectorDirty(leadItem0Addr)
			if leadItem1 != nil {
				mgr.markDirectorySectorDirty(leadItem1Addr)
			}

			// update inhibit flags in the main item
			inhibitFlags := ExtractNewInhibitFlags(mainItem0[021].GetS2())
			if dFlags.ToBeReadOnly {
				inhibitFlags.IsReadOnly = true
			}
			if dFlags.ToBeWriteOnly {
				inhibitFlags.IsWriteOnly = true
			}
			inhibitFlags.IsAssignedExclusive = false
			mainItem0[021].SetS2(inhibitFlags.Compose())

			// clear to-be settings in main item
			dFlags.ToBeCataloged = false
			dFlags.ToBeDropped = false
			dFlags.ToBeReadOnly = false
			dFlags.ToBeWriteOnly = false
			mainItem0[014].SetT1(dFlags.Compose())
			mgr.markDirectorySectorDirty(mainItem0Addr)
		}
	}

	return MFDSuccessful
}

// DropFileCycle causes MFDManager to delete a particular file cycle (and the file set, if that is the only cycle).
// It is intended to be used by fac mgr for dropping the oldest file cycle when a new file cycle would cause
// that to be required... but it will work for any caller to delete any particular file cycle.
func (mgr *MFDManager) DropFileCycle(
	fcIdentifier FileCycleIdentifier,
) (mfdResult MFDResult) {
	mfdResult = MFDSuccessful

	mgr.mutex.Lock()
	defer mgr.mutex.Unlock()

	mainItem0Addr := kexec.MFDRelativeAddress(fcIdentifier)

	_, err := mgr.loadFileAllocationSet(mainItem0Addr)
	if err != nil {
		return MFDInternalError
	}

	mfdResult = mgr.dropFileCycle(mainItem0Addr)
	return
}

// GetFileCycleInfo returns a FileCycleInfo struct representing the file cycle corresponding to the given
// file cycle identifier.
// If we return MFDInternalError, the exec has been stopped
// If we return MFDNotFound then there is no such file cycle
func (mgr *MFDManager) GetFileCycleInfo(
	fcIdentifier FileCycleIdentifier,
) (fcInfo *FixedFileCycleInfo, mfdResult MFDResult) {
	fcInfo = nil
	mfdResult = MFDSuccessful

	mgr.mutex.Lock()
	defer mgr.mutex.Unlock()

	mainItem0, err := mgr.getMFDSector(kexec.MFDRelativeAddress(fcIdentifier))
	if err != nil {
		mfdResult = MFDNotFound
		return
	}

	mainItem1Address := kexec.MFDRelativeAddress(mainItem0[015].GetW()) & 0_007777_777777
	mainItem1, err := mgr.getMFDSector(mainItem1Address)
	if err != nil {
		mfdResult = MFDInternalError
		return
	}

	mainItems := make([][]pkg.Word36, 2)
	mainItems[0] = mainItem0
	mainItems[1] = mainItem1
	link := mainItem1[0].GetW()
	for link&0_400000_000000 == 0 {
		mi, err := mgr.getMFDSector(kexec.MFDRelativeAddress(link & 0_007777_777777))
		if err != nil {
			mfdResult = MFDInternalError
			return
		}
		mainItems = append(mainItems, mi)
	}

	fcInfo = &FixedFileCycleInfo{}
	fcInfo.SetFileCycleIdentifier(fcIdentifier)
	fcInfo.SetFileSetIdentifier(FileSetIdentifier(mainItem0[013] & 0_007777_777777))
	fcInfo.populateFromMainItems(mainItems)

	return fcInfo, MFDSuccessful
}

// GetFileSetIdentifier retrieves an opaque value which uniquely identifies a file set,
// given a qualifier and filename.
// Returns MFDNotFound if no fileset exists, or MFDSuccessful
func (mgr *MFDManager) GetFileSetIdentifier(
	qualifier string,
	filename string,
) (fsi FileSetIdentifier, mfdResult MFDResult) {
	key := qualifier + "*" + filename
	leadItem0Address, ok := mgr.fileLeadItemLookupTable[key]
	if !ok {
		return 0, MFDNotFound
	} else {
		return FileSetIdentifier(leadItem0Address), MFDSuccessful
	}
}

// GetFileSetInfo returns a FileSetInfo struct representing the file set corresponding to the given
// file set identifier.
// If we return MFDInternalError, the exec has been stopped
// If we return MFDNotFound then there is no such file set
func (mgr *MFDManager) GetFileSetInfo(
	fsIdentifier FileSetIdentifier,
) (fsInfo *FileSetInfo, mfdResult MFDResult) {
	fsInfo = nil
	mfdResult = MFDSuccessful

	mgr.mutex.Lock()
	defer mgr.mutex.Unlock()

	leadItem0Addr := kexec.MFDRelativeAddress(fsIdentifier)
	_, leadItem0, leadItem1, err := mgr.getLeadItems(leadItem0Addr)
	if err != nil {
		mfdResult = MFDNotFound
		return
	}

	fsInfo = &FileSetInfo{}
	fsInfo.FileSetIdentifier = fsIdentifier
	fsInfo.populateFromLeadItems(leadItem0, leadItem1)
	return
}

func (mgr *MFDManager) GetTrackCountsForPack(nodeIdentifier hardware.NodeIdentifier) (
	msAccessible hardware.TrackCount,
	msAvailable hardware.TrackCount,
	mfdAccessible hardware.TrackCount,
	mfdAvailable hardware.TrackCount,
) {
	_, packDesc, ok := mgr.getPackDescriptorForNodeIdentifier(nodeIdentifier)
	if ok {
		msAccessible = packDesc.freeSpaceTable.Capacity
		msAvailable = packDesc.freeSpaceTable.GetFreeTrackCount()
		mfdAccessible = 4096
		mfdAvailable = mfdAccessible - packDesc.mfdTrackCount
	} else {
		msAccessible = 0
		msAvailable = 0
		mfdAccessible = 0
		mfdAvailable = 0
	}
	return
}

// InitializeMassStorage handles MFD initialization for what is effectively a JK13 boot.
// If we return an error, we must previously stop the exec.
func (mgr *MFDManager) InitializeMassStorage() {
	// Get the list of disks from the node manager
	disks := make([]*nodeMgr.DiskDeviceInfo, 0)
	fm := mgr.exec.GetFacilitiesManager()
	nm := mgr.exec.GetNodeManager().(*nodeMgr.NodeManager)
	for _, dInfo := range nm.GetDeviceInfos() {
		if dInfo.GetNodeDeviceType() == hardware.NodeDeviceDisk {
			disks = append(disks, dInfo.(*nodeMgr.DiskDeviceInfo))
		}
	}

	// Check the labels on the disks so that we may segregate them into fixed and isRemovable lists.
	// Any problems at this point will lead us to DN the unit.
	// At this point, FacMgr should know about all the disks.
	fixedDisks := make(map[*nodeMgr.DiskDeviceInfo]*kexec.DiskAttributes)
	removableDisks := make(map[*nodeMgr.DiskDeviceInfo]*kexec.DiskAttributes)
	for _, ddInfo := range disks {
		nodeAttr, _ := fm.GetNodeAttributes(ddInfo.GetNodeIdentifier())
		if nodeAttr.GetFacNodeStatus() == kexec.FacNodeStatusUp {
			// Get the pack attributes from fac mgr
			diskAttr, ok := fm.GetDiskAttributes(ddInfo.GetNodeIdentifier())
			if !ok {
				mgr.exec.SendExecReadOnlyMessage("Internal configuration error", nil)
				mgr.exec.Stop(kexec.StopInitializationSystemConfigurationError)
				return
			}

			if diskAttr.PackLabelInfo == nil {
				msg := fmt.Sprintf("No valid label exists for pack on device %v", ddInfo.GetNodeName())
				mgr.exec.SendExecReadOnlyMessage(msg, nil)
				_ = fm.SetNodeStatus(ddInfo.GetNodeIdentifier(), kexec.FacNodeStatusDown)
				continue
			}

			// Read sector 1 of the initial directory track.
			// This is a little messy due to the potential of problematic block sizes.
			wordsPerBlock := diskAttr.PackLabelInfo.WordsPerRecord
			dirTrackWordAddr := uint64(diskAttr.PackLabelInfo.FirstDirectoryTrackAddress)
			dirTrackBlockId := hardware.BlockId(dirTrackWordAddr / uint64(wordsPerBlock))
			if wordsPerBlock == 28 {
				dirTrackBlockId++
			}

			buf := make([]pkg.Word36, wordsPerBlock)
			ioStat := mgr.readBlockFromDisk(ddInfo.GetNodeIdentifier(), buf, dirTrackBlockId)
			if ioStat == ioPackets.IosInternalError {
				return
			} else if ioStat != ioPackets.IosComplete {
				msg := fmt.Sprintf("IO error reading directory track on device %v", ddInfo.GetNodeName())
				log.Printf("MFDMgr:%v", msg)
				mgr.exec.SendExecReadOnlyMessage(msg, nil)
				_ = fm.SetNodeStatus(ddInfo.GetNodeIdentifier(), kexec.FacNodeStatusDown)
				continue
			}

			var sector1 []pkg.Word36
			if wordsPerBlock == 28 {
				sector1 = buf
			} else {
				sector1 = buf[28:56]
			}

			// get the LDAT field from sector 1
			// If it is 0, it is a isRemovable pack
			// 0400000, it is an uninitialized fixed pack
			// anything else, it is a pre-used fixed pack which we're going to initialize
			ldat := sector1[5].GetH1()
			if ldat == 0 {
				removableDisks[ddInfo] = diskAttr
			} else {
				fixedDisks[ddInfo] = diskAttr
				diskAttr.IsFixed = true
			}
		}
	}

	err := mgr.initializeFixed(fixedDisks)
	if err != nil {
		return
	}

	// Make sure we have at least one fixed pack after the previous shenanigans
	if len(mgr.fixedPackDescriptors) == 0 {
		mgr.exec.SendExecReadOnlyMessage("No Fixed disks - Cannot Continue Initialization", nil)
		mgr.exec.Stop(kexec.StopInitializationSystemConfigurationError)
		return
	}

	err = mgr.initializeRemovable(removableDisks)
	return
}

func PopulateInitialDirectoryTrack(
	packLabel []pkg.Word36,
	isFixed bool,
	directoryTrack []pkg.Word36,
) {
	for wx := 0; wx < 1792; wx++ {
		directoryTrack[wx].SetW(0)
	}

	packName := strings.TrimRight((packLabel[1].ToStringAsAscii() + packLabel[2].ToStringAsAscii())[0:6], " ")
	recordsPerTrack := packLabel[04].GetH1()
	wordsPerRecord := packLabel[04].GetH2()
	availableTracks := packLabel[016].GetW() - 2 // subtract label track and first directory track

	// sector 0
	sector0 := directoryTrack[0:28]
	sector0[1].SetW(0_600000_000000) // first 2 sectors are allocated
	for dx := 3; dx < 27; dx += 3 {
		sector0[dx].SetW(0_400000_000000)
	}
	sector0[27].SetW(0_400000_000000)

	// sector 1
	s1 := directoryTrack[28:56]
	// leave +0 and +1 alone (We aren't doing HMBT/SMBT so we don't need the addresses)
	s1[2].SetW(availableTracks)
	s1[3].SetW(availableTracks)
	s1[4].FromStringToFieldata(packName)
	if isFixed {
		s1[5].SetH1(0_400000)
	}
	s1[010].SetT1(recordsPerTrack)
	s1[010].SetS3(1) // Sector 1 version
	s1[010].SetT3(wordsPerRecord)
}

// PurgeDirectory causes the MFD to write all dirty MFD sectors to disk
// Callers of other services should be sure to invoke this at the end of the process.
func (mgr *MFDManager) PurgeDirectory() MFDResult {
	err := mgr.writeMFDCache()
	if err != nil {
		return MFDInternalError
	}
	return MFDSuccessful
}
*/

    public void recoverMassStorage() throws ExecStoppedException {
        // TODO implement
        Exec.getInstance().sendExecReadOnlyMessage("MFD Recovery is not yet implemented", null);
        Exec.getInstance().stop(StopCode.DirectoryErrors);
        throw new ExecStoppedException();
    }

/*
// SetFileCycleRange updates the max file cycle range for the indicated fileset.
// Returns
//
//	MFDSuccessful if all is well
//	MFDInternalError if things went badly, and the exec has been stopped
//	MFDInvalidCycleLimit if the cycleRange parameter is unacceptable
func (mgr *MFDManager) SetFileCycleRange(
	fsIdentifier FileSetIdentifier,
	cycleRange uint,
) (mfdResult MFDResult) {
	mfdResult = MFDSuccessful

	mgr.mutex.Lock()
	defer mgr.mutex.Unlock()

	leadItem0Addr := kexec.MFDRelativeAddress(fsIdentifier)
	leadItem0, err := mgr.getMFDSector(leadItem0Addr)
	if err != nil {
		return MFDInternalError
	}

	currentMax := uint(leadItem0[011].GetS3())
	if cycleRange < currentMax || cycleRange > 32 {
		return MFDInvalidCycleLimit
	}

	leadItem0[011].SetS3(uint64(cycleRange))
	mgr.markDirectorySectorDirty(leadItem0Addr)

	return
}
    */

    /**
     * Sets a particular file cycle to be deleted
     * @param mainItem0Address main item 0 address of the file cycle
     */
    public void setFileToBeDeleted(
        final MFDRelativeAddress mainItem0Address
    ) {
        // TODO implement this
    }

}
