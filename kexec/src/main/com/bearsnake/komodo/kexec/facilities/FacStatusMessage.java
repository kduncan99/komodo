/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import java.util.concurrent.ConcurrentHashMap;

public class FacStatusMessage {

    private final FacStatusCategory _category;
    private final FacStatusCode _code;
    private final String _template;

    public FacStatusMessage(
        final FacStatusCategory category,
        final FacStatusCode code,
        final String template
    ) {
        _category = category;
        _code = code;
        _template = template;
    }

    public FacStatusCategory getCategory() { return _category; }
    public FacStatusCode getCode() { return _code; }
    public String getTemplate() { return _template; }

    public static void putError(
        final FacStatusCode code,
        final String template
    ) {
        var temp = new FacStatusMessage(FacStatusCategory.Error, code, template);
        FacStatusMessages.put(code, temp);
    }

    public static void putInfo(
        final FacStatusCode code,
        final String template
    ) {
        var temp = new FacStatusMessage(FacStatusCategory.Info, code, template);
        FacStatusMessages.put(code, temp);
    }

    public static void putWarning(
        final FacStatusCode code,
        final String template
    ) {
        var temp = new FacStatusMessage(FacStatusCategory.Warning, code, template);
        FacStatusMessages.put(code, temp);
    }

    public static final ConcurrentHashMap<FacStatusCode, FacStatusMessage> FacStatusMessages = new ConcurrentHashMap<>();
    static {
        putInfo(FacStatusCode.Complete, "%s complete");
        putInfo(FacStatusCode.DeviceIsSelected,  "%s is selected %s %s %s");
        putInfo(FacStatusCode.RunHeldForCacheControl, "Run %s held for control of caching for %d min.");
	    putInfo(FacStatusCode.RunHeldForComGroup, "Run %s held for com group availability for %d min.");
        putInfo(FacStatusCode.RunHeldForComLine, "Run %s held for com line availability for %d min.");
        putInfo(FacStatusCode.RunHeldForDevice, "Run %s held for unit for %s device assign for %d min.");
        putInfo(FacStatusCode.RunHeldForDisketteMount, "Run %s held for diskette to be mounted for %d min.");
        putInfo(FacStatusCode.RunHeldForDisketteUnitAvailability, "Run %s held for diskette unit availability for %d min.");
        putInfo(FacStatusCode.RunHeldForDiskPackMount, "Run %s held for disk pack to be mounted for %d min.");
        putInfo(FacStatusCode.RunHeldForDiskUnitAvailability, "Run %s held for disk unit availability for %d min.");
        putInfo(FacStatusCode.RunHeldForExclusiveFileUseRelease, "Run %s held for exclusive file use release for %d min.");
        putInfo(FacStatusCode.RunHeldForFileCycleConflict, "Run %s held for file cycle conflict for %d min.");
        putInfo(FacStatusCode.RunHeldForMassStorageSpace, "Run %s held for mass storage space for %d min.");
        putInfo(FacStatusCode.RunHeldForNeedOfExclusiveUse, "Run %s held for need of exclusive use for %d min.");
        putInfo(FacStatusCode.RunHeldForPack, "Run %s held for pack availability for %d min.");
        putInfo(FacStatusCode.RunHeldForReel, "Run %s held for reel availability for %d min.");
        putInfo(FacStatusCode.RunHeldForRemovable, "Run %s held for %s for abs rem disk for %d min.");
        putInfo(FacStatusCode.RunHeldForRollback, "Run %s held for rollback of unloaded file for %d min.");
	    putInfo(FacStatusCode.RunHeldForTapeUnitAvailability, "Run %s held for tape unit availability for %d min.");

        putWarning(FacStatusCode.FileAlreadyAssigned, "File is already assigned.");
        putWarning(FacStatusCode.FilenameNotKnown, "Filename not known to this run.");
        putWarning(FacStatusCode.FilenameNotUnique, "Filename not unique.");

        putError(FacStatusCode.AssignMnemonicMustBeSpecifiedWithPackId, "Assign mnemonic must be specified with a packid.");
        putError(FacStatusCode.AssignMnemonicMustBeWordAddressable, "Assign mnemonic must be word addressable.");
        putError(FacStatusCode.AssignMnemonicTooLong, "Assign mnemonic cannot be longer than 6 characters.");
        putError(FacStatusCode.AttemptToChangeGenericType, "Attempt to change generic type of the file.");
        putError(FacStatusCode.DirectoryAndQualifierMayNotAppear, "Directory id and qualifier may not appear on image when R option is used.");
        putError(FacStatusCode.DirectoryOrQualifierMustAppear, "Directory id or qualifier must appear on image.");
        putError(FacStatusCode.FileAlreadyCataloged, "File is already catalogued.");
        putError(FacStatusCode.FileCycleOutOfRange, "File cycle out of range.");
        putError(FacStatusCode.FileIsNotCataloged, "File is not catalogued.");
        putError(FacStatusCode.FilenameIsRequired, "A filename is required on the image.");
        putError(FacStatusCode.FileNotCatalogedWithReadKey, "File is not cataloged with a read key.");
        putError(FacStatusCode.FileNotCatalogedWithWriteKey, "File is not cataloged with a write key.");
        putError(FacStatusCode.HoldForReelRejected, "Hold for reel rejected because of Z option.");
        putError(FacStatusCode.HoldForPackRejected, "Hold for pack rejected because of Z option.");
        putError(FacStatusCode.HoldForTapeUnitRejected, "Hold for tape unit rejected because of Z option.");
        putError(FacStatusCode.HoldForDiskUnitRejected, "Hold for disk unit rejected because of Z option.");
        putError(FacStatusCode.HoldForXUseRejected, "Hold for x-use rejected because of Z option.");
        putError(FacStatusCode.HoldForReleaseXUseRejected, "Hold for release of x--use rejected because of Z option.");
        putError(FacStatusCode.HoldForRollbackRejected, "Hold for rollback of unloaded file rejected because of Z option.");
        putError(FacStatusCode.HoldForRemDiskRejected, "Hold for *rem disk rejected because of Z option.");
        putError(FacStatusCode.HoldForDevCURejected, "Hold for *DEV/CU rejected because of Z option.");
        putError(FacStatusCode.HoldForFCycleConflictRejected, "Hold for F-cycle conflict rejected because of Z option.");
        putError(FacStatusCode.HoldForMassStorageSpaceRejected, "Hold for mass storage space rejected because of Z option.");
        putError(FacStatusCode.IllegalControlStatement, "Illegal control statement type submitted to ER CSI$.");
        putError(FacStatusCode.IllegalDroppingPrivateFile, "Creation of file would require illegal dropping of private file.");
        putError(FacStatusCode.IllegalInitialReserve, "Illegal value specified for initial reserve.");
        putError(FacStatusCode.IllegalMaxGranules, "Illegal value specified for maximum.");
        putError(FacStatusCode.IllegalOption, "Illegal option %s.");
        putError(FacStatusCode.IllegalOptionCombination, "Illegal option combination %s%s.");
        putError(FacStatusCode.IllegalValueForFCycle, "Illegal value specified for F-cycle.");
        putError(FacStatusCode.IllegalValueForGranularity, "Illegal value specified for granularity.");
        putError(FacStatusCode.IncorrectPrivacyKey, "Incorrect privacy key for private file.");
        putError(FacStatusCode.IncorrectReadKey, "Incorrect read key.");
        putError(FacStatusCode.IncorrectWriteKey, "Incorrect write key.");
        putError(FacStatusCode.InternalNameRequired, "Internal Name is required.");
        putError(FacStatusCode.IOptionOnlyAllowed, "I option is the only legal option on USE.");
        putError(FacStatusCode.MaximumIsLessThanInitialReserve, "Maximum is less than the initial reserve.");
        putError(FacStatusCode.MnemonicIsNotConfigured, "%s is not a configured assign mnemonic.");
        putError(FacStatusCode.OperatorDoesNotAllowAbsoluteAssign, "The operator does not allow absolute assignment of pack pack-id.");
        putError(FacStatusCode.PackIdIsRequired, "Packid is required.");
        putError(FacStatusCode.PlacementFieldNotAllowed, "Placement field is not allowed with CAT.");
        putError(FacStatusCode.ReadWriteKeysNeeded, "Read and/or write keys are needed.");
        putError(FacStatusCode.RelativeFCycleConflict, "Relative F-cycle conflict.");
        putError(FacStatusCode.SyntaxErrorInImage, "Syntax error in image submitted to ER CSI$.");
        putError(FacStatusCode.UndefinedFieldOrSubfield, "Image contains an undefined field or subfield.");
        putError(FacStatusCode.UnitIsNotUpOrReserved, "%s is not in the up or reserved state.");
        putError(FacStatusCode.UnitNameIsNotConfigured, "%s is not a configured Name.");
        putError(FacStatusCode.UnitIsNotReserved, "%s is not in the reserved state.");
    }
}
