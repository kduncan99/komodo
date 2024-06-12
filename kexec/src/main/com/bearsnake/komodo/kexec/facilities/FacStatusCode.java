/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

public enum FacStatusCode {
    // Infos
    RunHeldForDevice(000133),
    RunHeldForRemovable(000233),
    RunHeldForPack(000333),
    RunHeldForReel(000433),
    RunHeldForComLine(000533),
    RunHeldForComGroup(000633),
    RunHeldForMassStorageSpace(000733),
    RunHeldForTapeUnitAvailability(001033),
    RunHeldForExclusiveFileUseRelease(001133),
    RunHeldForNeedOfExclusiveUse(001233),
    RunHeldForDiskUnitAvailability(001333),
    RunHeldForRollback(001433),
    RunHeldForFileCycleConflict(001533),
    RunHeldForDiskPackMount(001633),
    DeviceIsSelected(001733),
    RunHeldForCacheControl(002033),
    RunHeldForDisketteUnitAvailability(002133),
    RunHeldForDisketteMount(002233),
    Complete(002333),

    // Warnings
    FileAlreadyAssigned(0120133),
    FileAlreadyAssignedToAnotherRun(0120333),
    FilenameNotKnown(0120433),
    FilenameNotUnique(0120533),
    PlacementFieldIgnored(0121133),
    FileCatalogedAsReadOnly(0121433),
    ReadKeyExists(0121333),
    FileAssignedDuringSystemFailure(0122133),
    FileUnloaded(0122233),
    WriteKeyExists(0122333),
    FileCatalogedAsWriteOnly(0122433),
    OptionConflictOptionsIgnored(0122533),
    FileAlreadyExclusivelyAssigned(0123233),

    // Errors
    InvalidDeviceControlUnitName(0200133),
    DeviceAlreadyInUseByThisRun(0200333),
    DeviceIsFixed(0200433),
    MnemonicIsNotConfigured(0201033),
    UnitNameIsNotConfigured(0201133),
    OperatorDoesNotAllowAbsoluteAssign(0201333),
    IllegalOptionCombination(0201433),
    IllegalOption(0201533),
    PlacementNotFixedMassStorage(0202333),
    UnitIsNotReserved(0203133),
    DeviceIsNotUp(0203233),
    UnitIsNotUpOrReserved(0203333),
    AssignMnemonicDoesNotAllowPackIds(0240633),
    AssignMnemonicTooLong(0240733),
    AssignMnemonicMustBeSpecifiedWithPackId(0241033),
    AssignMnemonicMustBeWordAddressable(0241133),
    FileIsBeingDropped(0241233),
    IllegalAttemptToChangeAssignmentType(0241533),
    AttemptToChangeGenericType(0241633),
    AttemptToChangeGranularity(0241733),
    IllegalValueForFCycle(0242433),
    FileCycleOutOfRange(0242533),
    CannotCatalogReadWriteInhibited(0242633),
    IllegalDroppingPrivateFile(0243233),
    DuplicateMediaIdsAreNotAllowed(0243433),
    SyntaxErrorInImage(0243533),
    IllegalControlStatement(0243733),
    FilenameIsRequired(0244333),
    FileAlreadyCataloged(0244433),
    FileIsNotCataloged(0244533),
    DisabledCorruptedDirectory(0244633),
    FreeDFileNotAssigned(0245133),
    IllegalValueForGranularity(0245233),
    InternalNameRequired(0245533),
    IllegalInitialReserve(0246333),
    ReadWriteKeysNeeded(0246433),
    IllegalMaxGranules(0247333),
    MaximumIsLessThanInitialReserve(0247433),
    MaximumNumberOfPackIdsExceeded(0247633),
    IOptionOnlyAllowed(0250733),
    NumberOfPackIdsNotEqualToFile(0251233),
    NumberOfPackIdsNotEqualToMFD(0251333),
    PackIdsNotEqualToFile(0251433),
    PackIdsNotInSameOrder(0251533),
    PackIdIsRequired(0251633),
    Plus1IllegalWithAOption(0252033),
    PlacementFieldNotAllowed(0252133),
    PlacementOnNonMassStorageDevice(0252233),
    PlacementFieldNotAllowedForRemovable(0252333),
    IncorrectPrivacyKey(0252533),
    IncorrectReadKey(0253333),
    FileNotCatalogedWithReadKey(0253433),
    RelativeFCycleConflict(0253733),
    FileBackupNotAvailable(0254333),
    IllegalCharactersInPlacementField(0245333),
    UndefinedFieldOrSubfield(0255733),
    IncorrectWriteKey(0256633),
    FileNotCatalogedWithWriteKey(0256733),
    HoldForReelRejected(0257033),
    HoldForPackRejected(0257133),
    HoldForTapeUnitRejected(0257233),
    HoldForDiskUnitRejected(0257333),
    HoldForXUseRejected(0257433),
    HoldForReleaseXUseRejected(0257533),
    HoldForRollbackRejected(0257633),
    HoldForRemDiskRejected(0260133),
    HoldForDevCURejected(0260233),
    HoldForFCycleConflictRejected(0260333),
    HoldForMassStorageSpaceRejected(0260433),
    DisabledForCacheDrainFailure(0260633),
    DirectoryAndQualifierMayNotAppear(0262633),
    DirectoryOrQualifierMustAppear(0263033),
    PacksCanOnlyBeAddedWithAOption(0271333),
    PacksCanNotBeAddedWithYOption(0271433),
    PacksCanNotBeAddedIfAssigned(0271533),
    PacksCanOnlyBeAddedWithSingleCycle(0271633);

    final int _value;
    
    FacStatusCode(int value) {
        _value = value;
    }
}

/*
	Bit Description
	0*	Request not accepted; check other bits for reason.
	1*	Field error in control statement other than syntax.
		Option conflict (for example, MHL, OE, or IB) or noise constant specification error.
		Requested hardware not currently part of the system.
		CSF$ returns a Status of 600000000000 when there is inadequate storage available for either
		a fixed (absolute request) or removable request.
	2	File specified is already assigned or cataloged (@ASG and @CAT), already released (@FREE),
		or not assigned (@MODE). The request is rejected for @CAT and @MODE control statements.
	3	The file was previously cataloged.
	4*	Equipment type specified on the @ASG control statement is not compatible with the cataloged type,
		or file specified on the @MODE control statement is not magnetic tape.
	5	Data bank overflow attempting to acquire file-related buffer space.
		Down pack environment full. (Either the local or shared pack Status table is full.)
	6	That portion of the file Name used as the internal Name for I/O packets is not unique.
	7	X option specified; file already in exclusive use.
	8*†	Incorrect read key for cataloged file
	9*†	Incorrect write key for cataloged file
	10	Write key that exists in the master file directory is not specified in the @ASG control statement
		(file assigned in the read-only mode).
	11	Read key that exists in the master file directory is not specified in the @ASG control statement
		(file assigned in the write-only mode).
	12*†Read key specified in the @ASG control statement; none exists in the master file directory.
	13*†Write key specified in the @ASG control statement; none exists in the master file directory.
	14*	An A option was specified in the @ASG control statement and the file Name cannot be found
		in the master file directory.
	15*	Invalid or duplicate reel number or pack-id specified on an @ASG control statement for a cataloged tape
		or removable or fixed pack file; or, duplicate reel numbers or pack-ids specified on an @ASG control statement
		for a temporary tape or removable or fixed pack file. The requested tape reel is already assigned by this run.
	16*	Mass storage file has been rolled out (only if the Z option is used;
		otherwise, the run is held until the file is rolled in).
	17*	Request on wait Status for facilities. For a tape file, this usually means a tape unit is not currently
		available. For a disk file, this usually is caused by an exclusive use conflict with another run
		(only if the Z option is used; otherwise, the run is held).
	18*	For cataloged files, an option conflict occurred:
			The D and K options were specified.
			C or U, or P, R, or W in combination with C or U, was specified for a file that already exists
				in the directory.
			C was specified on a @CAT image.
		For a tape, an option conflict occurred on tape assignment (for example, FJ without Media Manager installed).
	19*	File assigned exclusively to another run
	20	Find was made on a cataloged file request and the file was already assigned to another run.
	21*	File to be decataloged when no run has file assigned
	22*	Project-id incorrect for cataloged private file
	23	(reserved)
	24	Read-only file cataloged with an R option
	25	Write-only file cataloged with a W option
	26	Equipment requested is down.
	27*	File specified in an @ASG control statement is disabled because the links pertinent to its
		master file directory items have been destroyed.
	28	File specified on the @ASG control statement has been disabled because the file was assigned during a
		system failure.
	29*	File specified on the @ASG control statement has been disabled because the file has been rolled out and the
		backup copy is unrecoverable, unless an @ENABLE command, followed by an @ASG,A command, is used to retry the
		loading operation.
	30*	F-cycle conflict:
			Cataloging of the requested F-cycle forces deletion of a currently assigned F-cycle.
			F-cycle generation is inhibited due to existence of +1 F-cycle.
			F-cycle requested is not in the currently acceptable range.
	31*	Quota limits prohibit this request; V or G option was used and is not permitted for this account.
	32	Security violation; or incorrect application private-id for application private file.
		An attempt was made to read or write to a TIP duplex file on a unit- duplexed device.
		Shared file access:
			An attempt was made to create shared files by a user who does not have correct privileges.
			An attempt was made to assign shared files by a user who does not have correct privileges.

	* Request was rejected. For dynamic requests through ER CSF$, bit 0 is set in the Status word returned in register A0.
	† If the statement was submitted by ER CSF$, the run is aborted and no Status word is returned in A0.
*/

/*
I:000133 Run xxxxxx held for unit for abs device/CU unit assign for nn min.
I:000233 Run xxxxxx held for unit for abs rem disk for nn min.
I:000333 Run xxxxxx held for pack availability for nn min.
I:000433 Run xxxxxx held for reel availability for nn min.
I:000533 Run xxxxxx held for com line availability for nn min.
I:000633 Run xxxxxx held for com group availability for nn min.
I:000733 Run xxxxxx held for mass storage space for nn min.
I:001033 Run xxxxxx held for tape unit availability for nn min.
I:001133 Run xxxxxx held for exclusive file use release for nn min.
I:001233 Run xxxxxx held for need of exclusive use for nn min.
I:001333 Run xxxxxx held for disk unit availability for nn min.
I:001433 Run xxxxxx held for rollback of unloaded file for nn min.
I:001533 Run xxxxxx held for file cycle conflict for nn min.
I:001633 Run nn held for disk pack to be mounted for nn min.
I:001733 device-Name is selected reel-id filename-index run-id
I:002033 Run xxxxxx held for control of caching for nn min.
I:002133 Run xxxxxx held for diskette unit availability for nn min.
I:002233 Run xxxxxx held for diskette to be mounted for nn min.
I:002333 {ASG|CAT|FREE|MODE|QUAL|USE} complete

W:100033 Assign mnemonic asg-mnem does not allow block numbering to be turned off, BLKOFF subfield is ignored.
W:100133 Assign mnemonic asg-mnem does not allow data compression to be turned off, CMPOFF subfield is ignored.
W:120133 File is already assigned.
W:120233 File is already assigned, this image ignored.
W:120333 File is assigned to another run.
W:120433 Filename not known to this run.
W:120533 Filename not unique.
W:120633 I/O error encountered on MFD during FREE.
W:120733 Line is assigned in simulation mode.
W:121033 Not all warning messages printed.
W:121133 Placement field ignored.
W:121233 File cannot be deleted because it is on the print queue.
W:121333 A read key exists on the file.
W:121433 File is cataloged as a read-only file.
W:121533 Security ACR does not allow read access.
W:121633 Security ACR does not allow write access.
W:121733 Security ACR does not allow execute access.
W:122033 Security clearance level allows read only access.
W:122133 File was assigned during system failure.
W:122233 File is unloaded.
W:122333 A write key exists on the file.
W:122433 File is cataloged write-only.
W:122533 Option conflict with previous assign options, all options ignored except i, q, x, y, or z.
W:122633 Option(s) conflict with previous assign options-option conflict ignored.
W:122733 Expiration exceeds the configured maximum. Configured maximum used.
W:123033 Read/write keys are ignored in an ownership system for owned files.
W:123133 G option not enforced.
W:123233 X option ignored, file already exclusively assigned by this run.
W:123333 System is configured to ignore Z option for batch in control mode.
W:123433 System is configured to release unused mass storage space.
W:123533 Security compartments allow read only access.
W:123633 The file cycle set is private and owned - therefore this cycle is private.
W:123733 The file cycle set is semi-private and owned - therefore this cycle is semi-private.
W:124033 The file cycle set is public and owned-therefore this cycle is public
W:124133 Security does not allow the use of unlabeled tapes. If written to, this tape will become labeled.
W:124233 The file is security disabled.
W:124333 Media Manager is not available.
W:124433 File cannot be deleted because an XPC contains data for this file.
W:124733 The memory assign mnemonic specification is ignored for an XPC cached file.
W:125133 The file created is semiprivate because the caller has a to-be-attached ACR.

E:200033 Unit device-Name not compatible with assign mnemonic.
E:200133 device-Name is an invalid device/control unit Name.
E:200233 device-Name is an invalid device Name.
E:200333 Device device-Name already in use by this run.
E:200433 Device device-Name is fixed.
E:200533 Device not available on control unit unit-Name.
E:200633 Pack pack-id cannot be mounted-equipment type does not permit interchanging of packs.
E:200733 Insufficient number of units available on control unit unit-Name.
E:201033 asg-mnem is not a configured assign mnemonic.
E:201133 device-Name is not a configured Name.
E:201233 Name is not a line Name or group Name.
E:201333 The operator does not allow absolute assignment of pack pack-id.
E:201433 Illegal option combination fj.
E:201533 Illegal option x.
E:201633 Addition of option(s) causes illegal combination xx.
E:201733 Pack pack-id already in use by this run.
E:202033 Pack pack-id failed label checking.
E:202133 Pack pack-id is assigned on a different equipment type.
E:202233 Pack pack-id is not a removable pack.
E:202333 Placement device device is not fixed mass storage.
E:202433 Quota does not allow use of x option.
E:202533 reel nnnnnn already in use by this run.
E:202633 reel nnnnnn is not in the master file directory.
E:202733 Security does not allow use of x option.
E:203033 Available unit not found on CU unit-Name because space is not available or quota limit exceeded.
E:203133 unit-Name is not in the reserved state.
E:203233 device-Name is not up.
E:203333 Name is not in the up or reserved state.
E:203433 Device device-Name is already actively caching.
E:203533 Device device-Name is not capable of caching.
E:203733 Directory of device device-Name is not equal to directory of file.
E:204033 Directory of pack pack-id is not equal to directory of file.
E:204133 Directory of placement device device-Name is not equal to directory of file.
E:204233 Assignment of pack pack-id could not be completed due to insufficient system resources.
E:204333 Assignments not allowed on pack pack-id because the operator has placed assignment restrictions on the pack.
E:204433 Assign mnemonic asg-mnem does not support 6-bit packed format.
E:204533 Assign mnemonic asg-mnem requires the J option.
E:204633 Assign mnemonic asg-mnem does not allow a noise field.
E:204733 Assign mnemonic asg-mnem does not support M-L-H-V density options.
E:205033 Assign mnemonic asg-mnem does not allow block numbering subfield.
E:205133 Assign mnemonic asg-mnem does not allow data compression subfield.
E:205233 Security does not allow the use of xxxxx equipment type.
E:206233 Assign mnemonic asg-mnem does not allow buffering to be turned off.
E:206333 Assign mnemonic asg-mnem does not support M-L-V-S density options.
E:206433 [mmspec] is an invalid Media Manager specification.
E:206533 [cartridge|non-cartridge] equipment type specified for [non-cartridge|cartridge] tape
E:206633 Volume [volume] unknown to Media Manager; FJ option combination is not allowed.
E:206733 Volume [volume] unknown to Media Manager, and BYPASS of Media Manager is not allowed.
E:207033 ACS Name acs-Name is not a legal Name
E:207233 Assign mnemonic aaaaaa does not support the media type xxxxxx for volume vvvvvv.
E:207333 Packs cannot be added due to the lack of removable directory space on pack pack-id.
E:207433 File cannot be created due to lack of removable directory space on pack pack-id
E:240133 Absolute device specification is not allowed on cataloged file.
E:240233 Absolute assignment of tape not allowed with CY or UY options.
E:240333 Illegal options on absolute assignment.
E:240433 Account could not be found.
E:240533 CY and UY options are not allowed with removable disk.
E:240633 Assign mnemonic does not allow pack-ids.
E:240733 Assign mnemonic cannot be longer than 6 characters.
E:241033 Assign mnemonic must be specified with a packid.
E:241133 Assign mnemonic must be word addressable.
E:241233 File is being dropped.
E:241333 Blank reel-id must be alone.
E:241433 Attempt to change assign mnemonic.
E:241533 Illegal attempt to change assignment type.
E:241633 Attempt to change generic type of the file.
E:241733 Attempt to change granularity.
E:242033 Attempt to change initial reserve of write inhibited file.
E:242133 Attempt to change maximum granules of a file cataloged under a different account.
E:242233 Attempt to change maximum granules on a write inhibited file.
E:242333 Assignment of units of the requested equipment type is not allowed.
E:242433 Illegal value specified for F-cycle.
E:242533 File cycle out of range.
E:242633 Cannot catalog file because read or write access not allowed.
E:242733 Illegal value specified for data converter.
E:243033 Data converter and translator cannot be used together.
E:243133 Delete access is not allowed.
E:243233 Creation of file would require illegal dropping of private file.
E:243333 Illegal value specified for data transfer format.
E:243433 Duplicate media-ids are not allowed.
E:243533 Syntax error in image submitted to ER CSI$.
E:243633 Length of image submitted to ER CSI$ is bad.
E:243733 Illegal control statement type submitted to ER CSI$.
E:244033 Expiration not allowed on CAT image.
E:244133 Expiration exceeds the configured maximum.
E:244233 Illegal value specified for expiration period.
E:244333 A filename is required on the image.
E:244433 File is already catalogued.
E:244533 File is not catalogued.
E:244633 File can not be recovered because master file directory (MFD) information has been corrupted.
E:244733 File is not tape.
E:245033 Facility FacInventory internal error 1, undefined generic equipment type.
E:245133 Attempt to delete via @FREE,D but file was not assigned
E:245233 Illegal value specified for granularity.
E:245333 Illegal character(s) in placement field.
E:245433 Insufficient number of units available.
E:245533 Internal Name is required.
E:245633 I/O error encountered on MFD assign count not decremented.
E:245733 I/O error encountered on the master file directory.
E:246033 I/O error encountered on MFD mediaids not read successfully.
E:246133 I/O error encountered on MFD--(+1) cycle cannot be deleted.
E:246233 File initial reserve granule limits exceeded.
E:246333 Illegal value specified for initial reserve.
E:246433 Read and/or write keys are needed.
E:246533 Line already assigned to this run.
E:246633 Line or group is not available.
E:246733 Logical channel not allowed with absolute device assign.
E:247033 Illegal value specified for logical channel.
E:247133 Maximum granules less than highest granule allocated.
E:247233 File maximum granule limits exceeded.
E:247333 Illegal value specified for maximum.
E:247433 Maximum is less than the initial reserve.
E:247533 Maximum number of mediaids exceeded.
E:247633 Maximum number of packids exceeded.
E:247733 Maximum number of reelids exceeded.
E:250033 File named on the mode is not assigned.
E:250133 Mass storage has overflowed.
E:250233 Number of units not allowed with absolute device assign.
E:250333 Illegal value for number of units.
E:250433 Illegal noise constant value.
E:250533 Noring cannot be specified with SCRTCH or BLANK reelid.
E:250633 Illegal use of I option.
E:250733 I option is the only legal option on USE.
E:251033 Options M and L are not allowed with 9 track tape units.
E:251133 Options V and S are not allowed with 7 track tape units.
E:251233 Number of packids on image not equal to number of packs assigned to file.
E:251333 Number of packids on the image not equal to number of packids in master file directory.
E:251433 Packids on image are not equal to packids assigned to file.
E:251533 Packids on image are not in same order as file's packids.
E:251633 Packid is required.
E:251733 Data bank overflow attempting to acquire file-related buffer space.
E:252033 F-cycle of +1 is illegal with A option.
E:252133 Placement field is not allowed with CAT.
E:252233 Placement requested on a non--mass storage device.
E:252333 Placement is not allowed with a removable disk file.
E:252433 Illegal syntax in placement subfield.
E:252533 Incorrect privacy key for private file.
E:252633 Quota set does not allow absolute device assignment.
E:252733 Loading of file causes quota mass storage limits to be exceeded.
E:253033 Quota mass storage limits exceeded.
E:253133 Quota number of units exceeded.
E:253233 Quota set does not allow communications assign.
E:253333 Incorrect read key.
E:253433 File is not cataloged with read key.
E:253533 Number of reelids on image greater than number of reelids in master file directory.
E:253633 None of the packs are currently registered.
E:253733 Relative F-cycle conflict.
E:254033 Ring specification not allowed on CAT image.
E:254133 Illegal value for ring specification.
E:254233 Scrtch is not allowed on a cataloged file.
E:254333 File backup is not available.
E:254433 Security does not allow absolute device assignment.
E:254533 Security access list does not allow access.
E:254633 Security clearance level does not allow access.
E:254733 Security does not allow delete access.
E:255033 Security group Name validation failed.
E:255133 Facility FacInventory internal error - Security packet error.
E:233233 Simulation is not allowed for a C/SP line.
E:255333 Simulation is not configured.
E:255433 S option not allowed with a group mnemonic.
E:255533 Error in translation specification.
E:255633 Requested translator is not configured.
E:255733 Image contains an undefined field or subfield.
E:256033 Requested unit(s) not available.
E:256133 A unit is not assigned to the file.
E:256233 Unit not compatible with request.
E:256333 Units and logical subfields not allowed on CAT image.
E:256433 User modified image or MFD for the file during facility processing.
E:256533 Attempt to change to word addressable not allowed.
E:256633 Incorrect write key.
E:256733 File is not cataloged with write key.
E:257033 Hold for reel rejected because of Z option.
E:257133 Hold for pack rejected because of Z option.
E:257233 Hold for tape unit rejected because of Z option.
E:257333 Hold for disk unit rejected because of Z option.
E:257433 Hold for x-use rejected because of Z option.
E:257533 Hold for release of x--use rejected because of Z option.
E:257633 Hold for rollback of unloaded file rejected because of Z option.
E:257733 Hold for com line rejected because of Z option.
E:260033 Hold for com group rejected because of Z option.
E:260133 Hold for *rem disk rejected because of Z option.
E:260233 Hold for *DEV/CU rejected because of Z option.
E:260333 Hold for F-cycle conflict rejected because of Z option.
E:260433 Hold for mass storage space rejected because of Z option.
E:260533 Maximum number of use names and/or filenames allowed to a run exceeded.
E:260633 File may contain corrupted data. file resided in cache disk memory at time of unrecoverable hardware error.
E:260733 Run has been aborted.
E:261033 The ER CSI$ request packet has non zero values in a reserved field.
E:261133 The file index passed in the ER CSI$ request packet is invalid.
E:261233 The ER CSI$ request causes the maximum number of filenames using file indices to be exceeded.
E:261333 The exec is in control of caching, request cannot be held.
E:261433 Hold for control of caching rejected because of Z option.
E:261533 A diskette id is required on the image.
E:261633 CAT of diskette file is not allowed.
E:261733 Hold for diskette unit rejected because of Z option.
E:262033 Too many diskette ids on the image.
E:262133 Illegal ACR Name.
E:262233 All cycles of a file must be same access mode.
E:262333 Acr specification illegal with A or T options.
E:262433 Quota does not allow creation of shared files.
E:262533 Quota does not allow deletion of shared files.
E:262633 Directory id and qualifier may not appear on image when R option is used.
E:262733 Absolute control unit assign of a diskette unit is not allowed.
E:263033 Directory id or qualifier must appear on image.
E:263133 Block numbering can only be changed at load point.
E:263233 Data compression requires block numbering.
E:263333 Data compression and translator cannot be used together.
E:263433 Illegal value for block numbering.
E:263533 Illegal value for data compression.
E:263633 Directory id of file is invalid.
E:263733 File sharing is down, Use of shared files is not allowed.
E:264033 Shared files cannot be temporary.
E:264133 No tape units are configured on this system. Tape unit cannot be allocated.
E:264233 File sharing is reserved. Creation of additional shared
          files is not allowed. Additional shared files cannot be
          assigned unless at least one shared file is already assigned.
E:264333 The file sharing feature is not configured. Use of shared files is not allowed.
E:264433 Data compression requires GCR recording mode.
E:264533 Absolute assignment of unit which is duplexing not allowed.
E:264633 The specified ACR does not match the ACR already controlling access to the other file cycles.
E:264733 Security compartment validation failed.
E:265033 A cartridge allows only L density option.
E:265133 Illegal value in buffered write subfield.
E:265233 File is privately assigned to an application. Access is not allowed.
E:265333 Security does not allow the use of unlabeled tapes, the J option is not allowed.
E:266033 The file is security disabled, no access allowed.
E:266133 The file Name section you were pointed to has been deleted.
E:266233 Free not allowed. File is in use by the exec.
E:266333 Hold for CTL availability rejected due to `Z' option.
E:266433 Cartridge-id is not available in the CTL.
E:266533 All cartridges be located in the same CLU.
E:266633 Cartridge tape library system is not available.
E:266733 Ctl-pool Name is not recognized.
E:267033 Memory files must be in the local directory.
E:267133 Media manager is not loaded.
E:267233 Bypass of Media Manager is not allowed.
E:267333 File contains data for a TIP duplexed file. Access is allowed only via TIP.
E:267433 Illegal value for the expanded buffer capability option.
E:267533 A file cannot be assigned with both the old and new names within the same run.
E:267633 CTL-pool Name cannot be used with a non-library mnemonic.
E:267733 NONCTL cannot be used with a library mnemonic.
E:270033 Hold for network device availability because of the Z option.
E:270133 Facility Inventory internal error: network device could not be marked assigned.
E:270333 The B option is not allowed when assigning a virtual tape.
E:270433 The DLT cartridge only allows the V density option.
E:270533 A file is cataloged in a different directory.
E:270633 Security does not allow creation of Shared Files.
E:270733 Security does not allow assignment of Shared Files.
E:271033 Read-only equipment is requested in a way that implies writing to it.
E:271133 Impersonated user-ids are not allowed to assign tapes.
E:271233 Impersonated user-ids are not allowed to do absolute assigns.
E:271333 Packs can only be added to removable files using an A option assignment.
E:271433 Packs cannot be added to removable files using a Y option assignment.
E:271533 Packs cannot be added to removable files that are currently assigned.
E:271633 Packs can only be added to removable files with a single file cycle.
E:271733 Adding packs requires delete access.

 */
