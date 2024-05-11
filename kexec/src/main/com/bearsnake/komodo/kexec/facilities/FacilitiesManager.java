/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.Channel;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DeviceType;
import com.bearsnake.komodo.hardwarelib.DiskChannel;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.FileSystemDiskDevice;
import com.bearsnake.komodo.hardwarelib.FileSystemTapeDevice;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.NodeCategory;
import com.bearsnake.komodo.hardwarelib.TapeChannel;
import com.bearsnake.komodo.hardwarelib.TapeDevice;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.FileCycleDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.FileSetDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;
import com.bearsnake.komodo.kexec.exec.RunType;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.facItems.AbsoluteDiskItem;
import com.bearsnake.komodo.kexec.facilities.facItems.DiskFileFacilitiesItem;
import com.bearsnake.komodo.kexec.facilities.facItems.FixedDiskItemFile;
import com.bearsnake.komodo.kexec.facilities.facItems.RemovableDiskItemFile;
import com.bearsnake.komodo.kexec.mfd.DiskFileCycleInfo;
import com.bearsnake.komodo.kexec.mfd.FileCycleInfo;
import com.bearsnake.komodo.kexec.mfd.FileSetInfo;
import com.bearsnake.komodo.kexec.mfd.FileType;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bearsnake.komodo.baselib.Word36.A_OPTION;
import static com.bearsnake.komodo.baselib.Word36.T_OPTION;
import static com.bearsnake.komodo.baselib.Word36.X_OPTION;

public class FacilitiesManager implements Manager {

    public enum DeleteBehavior {
        None,
        DeleteOnNormalRunTermination,
        DeleteOnAnyRunTermination,
    }

    public enum DirectoryOnlyBehavior {
        None,
        DirectoryOnlyMountPacks,
        DirectoryOnlyDoNotMountPacks,
    }

    static final String LOG_SOURCE = "FacMgr";

    // Inventory of all the hardware nodes, keyed by node identifier.
    // It is loaded at initialization(), and will remain unchanged during the application existence.
    final HashMap<Integer, NodeInfo> _nodeGraph = new HashMap<>();

    public FacilitiesManager() {
        Exec.getInstance().managerRegister(this);
    }

    /*
        TODO INFO misc console messages we may post somewhere in this mess
        NO PATH AVAILABLE FOR DEVICE device
        NOT ALL FIXED DEVICES RECOVERED - CONTINUE? YN
        No usable path found to device
        File SYS$*RLIB$ not properly catalogued
        File SYS$*LIB$ not properly catalogued
        File SYS$*RUN$ not properly catalogued
        dir-id pack-id TO BECOME FIXED YN?
        (dir-id FIXED PACK MOUNTED ON device IGNORED)
     */

    // -------------------------------------------------------------------------
    // Manager interface
    // -------------------------------------------------------------------------

    @Override
    public void boot(final boolean recoveryBoot) {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);

        // update verbosity of nodes
        for (var ni : _nodeGraph.values()) {
            ni.getNode().setLogIos(Exec.getInstance().getConfiguration().getLogIos());
        }

        // clear cached disk labels
        for (var ni : _nodeGraph.values()) {
            ni.setMediaInfo(null);
        }

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    @Override
    public synchronized void dump(final PrintStream out,
                                  final String indent,
                                  final boolean verbose) {
        out.printf("%sFacilitiesManager ********************************\n", indent);

        out.printf("%s  Node Graph:\n", indent);
        var subIndent = indent + "    ";
        for (var ni : _nodeGraph.values()) {
            out.printf("%s%s\n", subIndent, ni.toString());
        }
    }

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");

        loadNodeGraph();

        // set up routes
        for (var ni : _nodeGraph.values()) {
            if (ni instanceof ChannelNodeInfo) {
                var chNode = (Channel) ni.getNode();
                for (var devNode : chNode.getDevices()) {
                    var dni = (DeviceNodeInfo) _nodeGraph.get(devNode.getNodeIdentifier());
                    dni._routes.add(chNode);
                }
            }
        }
    }

    @Override
    public synchronized void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
    }

    // -------------------------------------------------------------------------
    // Services interface
    // -------------------------------------------------------------------------

    // short-cut for exec assign of cataloged fixed disk file for system purposes.
    public synchronized boolean assignCatalogedDiskFileToExec(
        final FileSpecification fileSpecification,
        final boolean exclusiveUse,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "assignCatalogedDiskFileToExec %s", fileSpecification.toString());

        var optionsWord = A_OPTION;
        if (exclusiveUse) {
            optionsWord |= X_OPTION;
        }

        var e = Exec.getInstance();
        var cfg = e.getConfiguration();
        var result = assignCatalogedDiskFileToRun(e.getRunControlEntry(),
                                                  fileSpecification,
                                                  optionsWord,
                                                  cfg.getMassStorageDefaultMnemonic(),
                                                  null,
                                                  null,
                                                  null,
                                                  null,
                                                  Collections.emptyList(),
                                                  DeleteBehavior.None,
                                                  DirectoryOnlyBehavior.None,
                                                  false,
                                                  true,
                                                  false,
                                                  exclusiveUse,
                                                  false,
                                                  false,
                                                  fsResult);

        LogManager.logTrace(LOG_SOURCE, "assignCatalogedFileToExec result:%s", fsResult.toString());
        return result;
    }

    public synchronized boolean assignCatalogedDiskFileToRun(
        final RunControlEntry runControlEntry,
        final FileSpecification fileSpecification,
        final long optionsWord,        // only to be used to populate a new facItem
        final String mnemonic,         // type/assign-mnemonic
        final Integer initialReserve,  // null if not specified, attempt to change existing value
        final Granularity granularity, // null if not specified, must match existing file otherwise
        final Integer maxGranules,     // null if not specified, attempt to change existing value
        final String placement,        // only for fixed, can be null (must be null for removable)
        final List<String> packIds,    // should be empty for fixed, optional for removable
        final DeleteBehavior deleteBehavior,               // D/K options
        final DirectoryOnlyBehavior directoryOnlyBehavior, // E/Y options
        final boolean saveOnCheckpoint,                    // M option (TODO update MFD item?)
        final boolean assignIfDisabled,                    // Q option
        final boolean readOnly,                            // R option
        final boolean exclusiveUse,                        // X option
        final boolean releaseOnTaskEnd,                    // I option
        final boolean doNotHoldRun,                        // Z option
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "assignCatalogedDiskFileToRun %s %s",
                            runControlEntry.getRunId(),
                            fileSpecification.toString());

        var e = Exec.getInstance();

        // --------------------------------------------------------
        // Pre-checks which are very generic
        // --------------------------------------------------------

        var mm = Exec.getInstance().getMFDManager();
        FileSetInfo fsInfo;
        try {
            fsInfo = mm.getFileSetInfo(fileSpecification.getQualifier(), fileSpecification.getFilename());
        } catch (FileSetDoesNotExistException ex) {
            fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
            fsResult.mergeStatusBits(0_400010_000000L);
            return false;
        }

        // If it is fixed, do not accept any pack-ids.
        if ((fsInfo.getFileType() == FileType.Fixed) && (!packIds.isEmpty())) {
            fsResult.postMessage(FacStatusCode.AssignMnemonicDoesNotAllowPackIds);
            fsResult.mergeStatusBits(0_600010_000000L);
            return false;
        }

        // Ensure we're not asking to assign a disk file when the file set is tape
        if ((fsInfo.getFileType() != FileType.Fixed) && (fsInfo.getFileType() != FileType.Removable)) {
            fsResult.postMessage(FacStatusCode.AttemptToChangeGenericType);
            fsResult.mergeStatusBits(0_420000_000000L);
            return false;
        }

        // Check read/write keys
        if (!checkKeys(runControlEntry, fsInfo, fileSpecification, fsResult)) {
            return false;
        }
        var readInhibit = ((fsResult.getStatusWord() & 0_000100_000000L) != 0)
            || (directoryOnlyBehavior != DirectoryOnlyBehavior.None);
        var writeInhibit = ((fsResult.getStatusWord() & 0_000200_000000L) != 0)
            || readOnly
            || (directoryOnlyBehavior != DirectoryOnlyBehavior.None);
        var alreadyAssigned = false;

        var isRem = fsInfo.getFileType() == FileType.Removable;

        // --------------------------------------------------------
        // Determine whether the indicated file cycle exists,
        // then get fcInfo and facItem accordingly
        // --------------------------------------------------------

        DiskFileFacilitiesItem facItem = null;
        DiskFileCycleInfo fcInfo = null;
        var fiTable = runControlEntry.getFacilitiesItemTable();
        if (fileSpecification.hasFileCycleSpecification() && fileSpecification.getFileCycleSpecification().isAbsolute()) {
            // This an absolute file cycle request - go get the file cycle info.
            // If it is *not* fixed and there are pack-ids, do sanity checking on them.
            try {
                fcInfo = (DiskFileCycleInfo) mm.getFileCycleInfo(fileSpecification.getQualifier(),
                                                                 fileSpecification.getFilename(),
                                                                 fileSpecification.getFileCycleSpecification().getCycle());
            } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                // we already checked for file set not existing, but we have to catch it here anyway.
                fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
                fsResult.mergeStatusBits(0_400010_000000L);
                return false;
            }

            // Check placement (only for fixed)
            // TODO
            //E:202333 Placement device device is not fixed mass storage. (this goes elsewhere, it's for fixed)
            //E:252133 Placement field is not allowed with CAT.
            //E:252233 Placement requested on a non--mass storage device.
            //E:252333 Placement is not allowed with a removable disk file.
            //E:252433 Illegal syntax in placement subfield.

            // Check the pack-ids on the file cycle against the provided pack-ids (if any).
            // Cannot allow pack-ids for fixed disks.
            // Rules:
            //   All cycles for a given file set must have the same pack list
            //   Max number of pack-ids is 510
            //   If a file cycle is currently unassigned, we can add one or more pack-ids to the *END* of the pack list
            //     *IF* the entire list is specified, with the new pack(s) at the end of the list
            //   Packs must only be added with the A option (which is why we are here), but *NOT* with the Y option.
            //   Packs cannot be added if the fileset has more than one cycle, or if the cycle is currently assigned.
            //   Adding packs requires delete access (for fundamental security, cycle must not be write-inhibited)
            if (!packIds.isEmpty()) {
                if (!isRem) {
                    // we'd like a better message, but this is all we have...
                    fsResult.postMessage(FacStatusCode.UndefinedFieldOrSubfield);
                    fsResult.mergeStatusBits(0_600000_000000L);
                    return false;
                }

                //E:271333 Packs can only be added to removable files using an A option assignment.
                //E:271433 Packs cannot be added to removable files using a Y option assignment.
                //E:271533 Packs cannot be added to removable files that are currently assigned.
                //E:271633 Packs can only be added to removable files with a single file cycle.
                //E:202233 Pack pack-id is not a removable pack.
                //E:247633 Maximum number of packids exceeded.
                //E:251233 Number of packids on image not equal to number of packs assigned to file.
                //E:251333 Number of packids on the image not equal to number of packids in master file directory.
                //E:251433 Packids on image are not equal to packids assigned to file.
                //E:251533 Packids on image are not in same order as file's packids.
            }

            // Check the existing facility items to see if this file cycle is already assigned to this run.
            facItem = (DiskFileFacilitiesItem) fiTable.getFacilitiesItemByAbsoluteCycle(fileSpecification.getQualifier(),
                                                                                        fileSpecification.getFilename(),
                                                                                        fileSpecification.getFileCycleSpecification().getCycle());
            if (facItem != null) {
                alreadyAssigned = true;
            } else {
                // It is not already assigned, but it does exist -
                // we have fcInfo for the existing file set, we just need a facItem.

                // Check public/private
                if (!checkPrivateAccess(runControlEntry, fcInfo, fsResult)) {
                    return false;
                }

                // Is the file read-only or write-only?
                readInhibit |= fcInfo.getInhibitFlags().isWriteOnly();
                writeInhibit |= fcInfo.getInhibitFlags().isReadOnly();

                // Create new fac item with option-driven settings as necessary and add it to the fac item table.
                facItem = isRem ? new RemovableDiskItemFile() : new FixedDiskItemFile();
                facItem.setQualifier(fcInfo.getQualifier())
                       .setFilename(fcInfo.getFilename())
                       .setAbsoluteCycle(fcInfo.getAbsoluteCycle())
                       .setIsTemporary(false)
                       .setOptionsWord(optionsWord)
                       .setReleaseOnTaskEnd(releaseOnTaskEnd);
                facItem.setDeleteOnAnyRunTermination(deleteBehavior == DeleteBehavior.DeleteOnAnyRunTermination)
                       .setDeleteOnNormalRunTermination(deleteBehavior == DeleteBehavior.DeleteOnNormalRunTermination)
                       .setIsExclusive(exclusiveUse)
                       .setIsReadable(!readInhibit)
                       .setIsWriteable(!writeInhibit);
            }
        } else {
            // It's either relative or unspecified. If unspecified, we treat it like relative +0.
            // Check the fac items to see if the file is already assigned with this relative cycle number.
            // If so, we are done here.
            // Otherwise, get the absolute cycle from the fcInfo, and go through the fac items again...
            // ...If it is assigned (comparing the absolute cycles)
            //   with a different relative cycle, post f-cycle conflict and stop
            //   else with no relative cycle, add this relative cycle to that fac item
            //     change any option-driven settings if necessary
            //     post already-assigned.
            // Otherwise
            //   check public/private (as above)
            //   make sure we're not being asked to change anything in a funny way (as above)
            //   create new fac item with option-driven settings as necessary
            //   update use-items to point to fac item as necessary
            var cycle = fileSpecification.hasFileCycleSpecification()
                ? fileSpecification.getFileCycleSpecification().getCycle() : 0;
            facItem =
                (DiskFileFacilitiesItem) fiTable.getFacilitiesItemByRelativeCycle(fileSpecification.getQualifier(),
                                                                                  fileSpecification.getFilename(),
                                                                                  cycle);
            if (facItem != null) {
                alreadyAssigned = true;
                var fsci = fsInfo.getCycleInfo().get(facItem.getAbsoluteCycle());
                try {
                    fcInfo = (DiskFileCycleInfo) mm.getFileCycleInfo(fsInfo.getQualifier(),
                                                                     fsInfo.getFilename(),
                                                                     fsci.getAbsoluteCycle());
                } catch (FileSetDoesNotExistException | FileCycleDoesNotExistException ex) {
                    LogManager.logFatal(LOG_SOURCE,
                                        "MFD cannot find a file cycle which must exist %s*%s(%d)",
                                        fsInfo.getQualifier(),
                                        fsInfo.getFilename(),
                                        fsci.getAbsoluteCycle());
                    e.stop(StopCode.FacilitiesComplex);
                    throw new ExecStoppedException();
                }
            } else {
                if (cycle == 1) {
                    // @ASG,A of +1 cycle is not allowed unless we already have it assigned,
                    // which is already accounted for in the code just above, and it cannot be assigned by
                    // absolute cycle - it would have been by +1.
                    fsResult.postMessage(FacStatusCode.Plus1IllegalWithAOption);
                    fsResult.mergeStatusBits(0_400000_000040L);
                    return false;
                }

                if (Math.abs(cycle) >= fsInfo.getCycleInfo().size()) {
                    fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
                    fsResult.mergeStatusBits(0_400010_000000L);
                    return false;
                }

                // Do we already have it assigned by the absolute cycle?
                var fsci = fsInfo.getCycleInfo().get(Math.abs(cycle));
                try {
                    fcInfo = (DiskFileCycleInfo) mm.getFileCycleInfo(fsInfo.getQualifier(),
                                                                     fsInfo.getFilename(),
                                                                     fsci.getAbsoluteCycle());
                } catch (FileSetDoesNotExistException | FileCycleDoesNotExistException ex) {
                    LogManager.logFatal(LOG_SOURCE,
                                        "MFD cannot find a file cycle which must exist %s*%s(%d)",
                                        fsInfo.getQualifier(),
                                        fsInfo.getFilename(),
                                        fsci.getAbsoluteCycle());
                    e.stop(StopCode.FacilitiesComplex);
                    throw new ExecStoppedException();
                }

                facItem =
                    (DiskFileFacilitiesItem) fiTable.getFacilitiesItemByRelativeCycle(fileSpecification.getQualifier(),
                                                                                      fileSpecification.getFilename(),
                                                                                      fsci.getAbsoluteCycle());
                if (facItem != null) {
                    // yes - attach this relative cycle to the already-existing fac item.
                    // If it already has a relative cycle, it would have to be the same as this one, so no worries.
                    alreadyAssigned = true;
                    facItem.setRelativeCycle(cycle);
                } else {
                    // okay, so we need a new facItem.
                    // Check public/private
                    if (!checkPrivateAccess(runControlEntry, fcInfo, fsResult)) {
                        return false;
                    }

                    // Is the file read-only or write-only?
                    readInhibit |= fcInfo.getInhibitFlags().isWriteOnly();
                    writeInhibit |= fcInfo.getInhibitFlags().isReadOnly();

                    // Create new fac item with option-driven settings as necessary and add it to the fac item table.
                    facItem = isRem ? new RemovableDiskItemFile() : new FixedDiskItemFile();
                    facItem.setQualifier(fcInfo.getQualifier())
                           .setFilename(fcInfo.getFilename())
                           .setAbsoluteCycle(fcInfo.getAbsoluteCycle())
                           .setIsTemporary(false)
                           .setOptionsWord(optionsWord)
                           .setReleaseOnTaskEnd(releaseOnTaskEnd);
                    facItem.setDeleteOnAnyRunTermination(deleteBehavior == DeleteBehavior.DeleteOnAnyRunTermination)
                           .setDeleteOnNormalRunTermination(deleteBehavior == DeleteBehavior.DeleteOnNormalRunTermination)
                           .setIsExclusive(exclusiveUse)
                           .setIsReadable(!readInhibit)
                           .setIsWriteable(!writeInhibit);
                }
            }
        }

        // --------------------------------------------------------
        // Error checking which cannot be done until we have
        // file cycle information for already existing file cycles,
        // and some general knowledge for both already-existing and
        // to-be existing file cycles.
        // These are failures which should be detected before
        // attempting to wait on facilities.
        // --------------------------------------------------------

        // What to do if E,Y options are different b/w initial and subsequent assign of a cycle? Nothing.
        //   If previously assigned E and/or Y, subsequent assign leaves the resulting behavior in place
        //   If previously assigned without E or Y, subsequent assign with E and/or Y has no effect

        // What to do if D,K options are different b/w initial and subsequent assign of a cycle?
        //   New D or K should override any previous behavior indicated by D or K, or lack thereof.
        //   Lack of D or K should have no effect on current behavior established by previous D or K.

        // The above notwithstanding, we do have this to post (but under what conditions?):
        // W:122533 Option conflict with previous assign options, all options ignored except i, q, x, y, or z.
        //   TODO

        // Is file disabled?
        var df = fcInfo.getDisableFlags();
        if (df.isDisabled()) {
            if (df.assignedAndWrittenAtExecStop()) {
                fsResult.postMessage(FacStatusCode.FileAssignedDuringSystemFailure);
                fsResult.mergeStatusBits(0_000000_000200L);
            }
            if (df.cacheDrainFailure()) {
                if (directoryOnlyBehavior == DirectoryOnlyBehavior.None) {
                    fsResult.postMessage(FacStatusCode.DisabledForCacheDrainFailure);
                    fsResult.mergeStatusBits(0_600000_000000L);
                }
            }
            if (df.directoryError()) {
                if (directoryOnlyBehavior == DirectoryOnlyBehavior.None) {
                    fsResult.postMessage(FacStatusCode.DisabledCorruptedDirectory);
                    fsResult.mergeStatusBits(0_600000_000400L);
                }
            }
            if (df.inaccessibleBackup()) {
                if (directoryOnlyBehavior == DirectoryOnlyBehavior.None) {
                    fsResult.postMessage(FacStatusCode.FileBackupNotAvailable);
                    fsResult.mergeStatusBits(0_400000_000100L);
                } else {
                    fsResult.postMessage(FacStatusCode.FileUnloaded);
                    fsResult.mergeStatusBits(0_000000_000100L);
                }
            }
        }

        // Illegal attempt to change assign type? e.g., trying option A with already in-place option C
        //   TODO

        // Illegal attempt to change generic type? e.g., fixed->rem, or tape->fixed
        //   TODO E:241633 Attempt to change generic type of the file.

        // Illegal attempt to change assign mnemonic? e.g.... what?
        //   TODO E:241433 Attempt to change assign mnemonic.

        // Attempt to change sector to word or word to sector?
        //   TODO E:256533 Attempt to change to word addressable not allowed.
        //     but are we allowed to change to sector addressable? If so, why? If not, what is the fac code?

        // Attempt to change granularity?
        if ((granularity != null) && (granularity != fcInfo.getPCHARFlags().getGranularity())) {
            fsResult.postMessage(FacStatusCode.AttemptToChangeGranularity);
            fsResult.mergeStatusBits(0_600000_000000L);
            return false;
        }

        // Bad attempt to change init-reserve of max?
        //   TODO E:242133 Attempt to change maximum granules of a file cataloged under a different account.
        //        E:246233 File initial reserve granule limits exceeded.
        //     there is some other trouble with changing max to less than highest,
        //     or init to more than max, or some such combinations of nonsense

        // Attempt to change init reserve or max on write-inhibited file?
        // Make it happen unless the file is write-inhibited. Otherwise, fail.
        //   TODO E:242033 Attempt to change initial reserve of write inhibited file.
        //   TODO E:242233 Attempt to change maximum granules on a write inhibited file.
        //        E:247133 Maximum granules less than highest granule allocated.
        //        E:247233 File maximum granule limits exceeded.
        //        E:247333 Illegal value specified for maximum.
        //        E:247433 Maximum is less than the initial reserve.

        if (alreadyAssigned) {
            // generic stuff for already-assigned file, which occurs before any putative wait
            // Attempt to set read-inhibited or write-inhibited?
            if (readInhibit && facItem.isReadable()) {
                facItem.setIsReadable(false);
            }
            if (writeInhibit && facItem.isWriteable()) {
                facItem.setIsWriteable(false);
            }

        } else {
            // Is file in to-be-cataloged state? If so, (and it's not assigned, so...) it should appear not to exist.
            if (fcInfo.getDescriptorFlags().toBeCataloged()) {
                fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
                fsResult.mergeStatusBits(0_400010_000000L);
                return false;
            }

            // Is file being dropped?
            if (fcInfo.getDescriptorFlags().toBeDropped()) {
                fsResult.postMessage(FacStatusCode.FileIsBeingDropped);
                fsResult.mergeStatusBits(0_400000_040000L);
                return false;
            }
        }

        // Is the file unloaded? If so, and it's not already assigned, we either fail or start a rollback.
        // If it *is* already assigned, we can just drop through as the current facItem should already have
        // all the rollback waiting states set properly.
        if (fcInfo.getDescriptorFlags().isUnloaded()
            && !alreadyAssigned
            && (directoryOnlyBehavior == DirectoryOnlyBehavior.None)) {
            if (doNotHoldRun) {
                fsResult.postMessage(FacStatusCode.HoldForRollbackRejected);
                fsResult.mergeStatusBits(0_400002_000000L);
                return false;
            }
            if (!facItem.isWaitingForRollback()) {
                facItem.setIsWaitingForRollback(true);
                // TODO update rce for hold condition...?
                // TODO initiate rollback
            }
        }

        // --------------------------------------------------------
        // Checking hold conditions which might be required
        // --------------------------------------------------------

        // Are we asking for x-use of a file assigned elsewhere?
        // This is applicable even if the file is already assigned, because we might be applying x-use now,
        // where it wasn't applied previously.
        if (exclusiveUse) {
            if (alreadyAssigned ? fcInfo.getCurrentAssignCount() > 1 : fcInfo.getCurrentAssignCount() > 0) {
                if (doNotHoldRun) {
                    fsResult.postMessage(FacStatusCode.HoldForXUseRejected);
                    fsResult.mergeStatusBits(0_400001_000000L);
                    return false;
                }

                if (!facItem.isWaitingForExclusiveUse()) {
                    // TODO update rce for hold condition
                }
            }
        }

        // Are we waiting, or do we need to wait, for a file which is exclusively assigned elsewhere?
        // If alreadyAssigned is set, then we don't worry about it, because the facItem should already
        // be set to the correct states.
        if (!alreadyAssigned && (fcInfo.getInhibitFlags().isAssignedExclusively())) {
            if (doNotHoldRun) {
                fsResult.postMessage(FacStatusCode.HoldForReleaseXUseRejected);
                fsResult.mergeStatusBits(0_400001_000000L);
                return false;
            }

            // TODO update rce for hold condition
        }

        // If we are removable, do we need to wait for any packs? Does that even apply here? How?
        //   TODO

        // --------------------------------------------------------
        // Now deal with any holds which might be required...
        // Mostly just waiting on exclusive use and/or rollback,
        // But also waiting for packs (rarely occurs) for rem disk files.
        // --------------------------------------------------------

        // Is there a hold in place? If so, wait on it.
        while (facItem.isWaiting()) {
            // TODO all the waiting stuff goes here
        }

        // --------------------------------------------------------
        // Final stuff to be done (mostly just intelligent posting
        // of various warnings which apply).
        // --------------------------------------------------------

        if (releaseOnTaskEnd) {
            facItem.setReleaseOnTaskEnd(true);
        }

        if (fcInfo.getInhibitFlags().isReadOnly()) {
            fsResult.postMessage(FacStatusCode.FileCatalogedAsReadOnly);
        }

        if (fcInfo.getInhibitFlags().isWriteOnly()) {
            fsResult.postMessage(FacStatusCode.FileCatalogedAsWriteOnly);
        }

        if (alreadyAssigned) {
            // post-wait generic stuff for already assigned disk file
            fsResult.postMessage(FacStatusCode.FileAlreadyAssigned);
            fsResult.mergeStatusBits(0_100000_000000L);

            // Is X-option specified and already exclusive? post warning
            if (exclusiveUse) {
                if (facItem.isExclusive()) {
                    fsResult.postMessage(FacStatusCode.FileAlreadyExclusivelyAssigned);
                    fsResult.mergeStatusBits(0_002000_000000L);
                } else {
                    facItem.setIsExclusive(true);
                }
            }

            // Is file already assigned to some other run? (it *is* assigned to us already)
            if (fcInfo.getCurrentAssignCount() > 1) {
                fsResult.postMessage(FacStatusCode.FileAlreadyAssignedToAnotherRun);
                fsResult.mergeStatusBits(0_000000_100000L);
            }
        } else {
            // Is file already assigned to some other run? (it's not assigned to us in any case)
            if (fcInfo.getCurrentAssignCount() > 0) {
                fsResult.postMessage(FacStatusCode.FileAlreadyAssignedToAnotherRun);
                fsResult.mergeStatusBits(0_000000_100000L);
            }

            // Is filename portion not unique compared to some other facItem?
            // We can do the following check because we have not yet created a facItem for
            // this file assignment, so any hit on filename would constitute filename-not-unique.
            if (fiTable.getFacilitiesItemByFilename(fcInfo.getFilename()) != null) {
                fsResult.postMessage(FacStatusCode.FilenameNotUnique);
                fsResult.mergeStatusBits(0_004000_000000L);
            }

            // Accelerate it via MFD (it should not have been yet).
            try {
                mm.accelerateFileCycle(facItem.getQualifier(), facItem.getFilename(), facItem.getAbsoluteCycle());
            } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                LogManager.logFatal(LOG_SOURCE,
                                    "MFD cannot find a file cycle which must exist %s*%s(%d)",
                                    fsInfo.getQualifier(),
                                    fsInfo.getFilename(),
                                    facItem.getAbsoluteCycle());
                e.stop(StopCode.FacilitiesComplex);
                throw new ExecStoppedException();
            }

            // Now (finally) add the new facItem to the facItemTable
            fiTable.addFacilitiesItem(facItem, runControlEntry.getUseItemTable());
        }

        LogManager.logTrace(LOG_SOURCE, "assignCatalogedFileToRun %s result:%s",
                            runControlEntry.getRunId(),
                            fsResult.toString());
        return true;
    }

    /**
     * For assigning any disk to the exec. Possibly only used by facilities manager...?
     * The device must not be assigned to any run (other than the EXEC) and it must be ready.
     * @param fileSpecification needed for creating facilities item
     * @param nodeIdentifier node identifier of the device
     * @param packName only for removable disk unit, null for fixed
     * @param fsResult fac status result
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public synchronized boolean assignDiskUnitToExec(
        final FileSpecification fileSpecification,
        final int nodeIdentifier,
        final String packName,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "assignDiskUnitToExec %s node:%d",
                            fileSpecification.toString(),
                            nodeIdentifier);

        var optionsWord = T_OPTION | X_OPTION;
        var result = assignDiskUnitToRun(Exec.getInstance().getRunControlEntry(),
                                         fileSpecification,
                                         nodeIdentifier,
                                         packName,
                                         optionsWord,
                                         false,
                                         true,
                                         fsResult);

        LogManager.logTrace(LOG_SOURCE,
                            "assignDiskUnitToExec result:%s",
                            fsResult.toString());
        return result;
    }

    /**
     * For assigning a reserved disk to a run (or any disk, if we are the Exec).
     * This assignment can only be temporary.
     * @param runControlEntry describes the run
     * @param fileSpecification describes the qualifier, file name, and file cycle for the fac item
     * @param nodeIdentifier node identifier of the device
     * @param packName pack name requested for the device
     * @param optionsWord options provided when the file was assigned
     * @param releaseOnTaskEnd I-option on assign
     * @param doNotHoldRun Z-option on assign
     * @param fsResult fac status result
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public synchronized boolean assignDiskUnitToRun(
        final RunControlEntry runControlEntry,
        final FileSpecification fileSpecification,
        final int nodeIdentifier,
        final String packName, // only for removable - null (and ignored) for fixed
        final long optionsWord,
        final boolean releaseOnTaskEnd,
        final boolean doNotHoldRun,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun %s %s node:%d pack:%s I:%s Z:%s",
                            runControlEntry.getRunId(),
                            fileSpecification.toString(),
                            nodeIdentifier,
                            packName,
                            releaseOnTaskEnd,
                            doNotHoldRun);

        var nodeInfo = _nodeGraph.get(nodeIdentifier);
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToRun() Cannot find node %012o", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if ((node.getNodeCategory() != NodeCategory.Device) || ((Device) node).getDeviceType() != DeviceType.DiskDevice) {
            LogManager.logFatal(LOG_SOURCE, "assignDiskUnitToRun() Node %012o is not a disk device", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var disk = (DiskDevice) node;
        if (!runControlEntry.isPrivileged() && nodeInfo.getNodeStatus() != NodeStatus.Reserved) {
            var params = new String[]{nodeInfo.getNode().getNodeName()};
            fsResult.postMessage(FacStatusCode.UnitIsNotReserved, params);
            fsResult.mergeStatusBits(0_600000_000000L);
            return false;
        }

        // Check the node assignment for the device - if it is already assigned to us, then fail.
        var dni = (DeviceNodeInfo) nodeInfo;
        if (Objects.equals(dni.getAssignedTo(), runControlEntry)) {
            var params = new String[]{node.getNodeName()};
            fsResult.postMessage(FacStatusCode.DeviceAlreadyInUseByThisRun, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            return false;
        }

        // Check requested pack name - if it is already assigned to this run, reject the request
        //   TODO E:201733 Pack pack-id already in use by this run.

        // If we are not the Exec make sure the pack is removable and that the device is not fixed
        if (!runControlEntry.isPrivileged()) {
            //   TODO E:202233 Pack pack-id is not a removable pack.
            //   TODO E:200433 Device device-Name is fixed.
        }

        // If there is a facilities item in the rce which matches the file specification which does not refer
        // to an absolute assign of this same unit, fail.
        var fiTable = runControlEntry.getFacilitiesItemTable();
        var existingFacItem = fiTable.getExactFacilitiesItem(fileSpecification);
        if (existingFacItem != null) {
            fsResult.postMessage(FacStatusCode.IllegalAttemptToChangeAssignmentType);
            fsResult.mergeStatusBits(0_400000_000000L);
            return false;
        }

        // Create an appropriate facilities item.
        var facItem = new AbsoluteDiskItem(node, packName);
        facItem.setQualifier(fileSpecification.getQualifier())
               .setFilename(fileSpecification.getFilename())
               .setOptionsWord(optionsWord)
               .setIsTemporary(true)
               .setReleaseOnTaskEnd(releaseOnTaskEnd);
        if (fileSpecification.hasFileCycleSpecification()) {
            var fcSpec = fileSpecification.getFileCycleSpecification();
            if (fcSpec.isAbsolute()) {
                facItem.setAbsoluteCycle(fcSpec.getCycle());
            } else if (fcSpec.isRelative()) {
                facItem.setRelativeCycle(fcSpec.getCycle());
            }
        }

        // Is the filename portion of the new facilities item not unique to the run?
        if (fiTable.getFacilitiesItemByFilename(fileSpecification.getFilename()) != null) {
            fsResult.postMessage(FacStatusCode.FilenameNotUnique);
            fsResult.mergeStatusBits(0_004000_000000L);
        }

        fiTable.addFacilitiesItem(facItem, runControlEntry.getUseItemTable());

        // Wait for the unit if necessary...
        var startTime = Instant.now();
        var nextMessageTime = startTime.plusSeconds(120);
        while (true) {
            if (!Exec.getInstance().isRunning()) {
                throw new ExecStoppedException();
            }
            runControlEntry.incrementWaitingForPeripheral();
            synchronized (nodeInfo) {
                if (nodeInfo.getAssignedTo() == null) {
                    nodeInfo.setAssignedTo(runControlEntry);
                    facItem.setIsAssigned(true);
                    runControlEntry.decrementWaitingForPeripheral();
                    break;
                }
            }

            if (!doNotHoldRun) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // do nothing
            }

            var now = Instant.now();
            if (now.isAfter(nextMessageTime)) {
                nextMessageTime = nextMessageTime.plusSeconds(120);
                if (!runControlEntry.hasTask()
                    && ((runControlEntry.getRunType() == RunType.Batch) || (runControlEntry.getRunType() == RunType.Demand))) {
                    long minutes = Duration.between(now, startTime).getSeconds() / 60;
                    var params = new Object[]{runControlEntry.getRunId(), minutes};
                    var facMsg = new FacStatusMessageInstance(FacStatusCode.RunHeldForDiskUnitAvailability, params);
                    runControlEntry.postToPrint(facMsg.toString(), 1);
                }
            }
        }

        if (!facItem.isAssigned()) {
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun promptLoadPack returns false");
            // z-option bail-out
            fiTable.removeFacilitiesItem(facItem, runControlEntry.getUseItemTable());
            fsResult.postMessage(FacStatusCode.HoldForDiskUnitRejected);
            fsResult.mergeStatusBits(0_400001_000000L);
            return false;
        }

        if (!promptLoadPack(runControlEntry, nodeInfo, disk, packName)) {
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun promptLoadPack returns false");
            fiTable.removeFacilitiesItem(facItem, runControlEntry.getUseItemTable());
            nodeInfo.setAssignedTo(null);
            var params = new String[]{packName};
            fsResult.postMessage(FacStatusCode.OperatorDoesNotAllowAbsoluteAssign, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun %s %s result:%s",
                                runControlEntry.getRunId(),
                                node.getNodeName(),
                                fsResult.toString());
            return false;
        }

        LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun %s %s result:%s",
                            runControlEntry.getRunId(),
                            node.getNodeName(),
                            fsResult.toString());
        return true;
    }

    /**
     * Returns the facilities NodeInfo for the given node.
     * @param nodeName name of the node - we'd prefer it to be uppercase, but whatever...
     * @return NodeInfo of the node if it is configured, else null
     */
    public NodeInfo getNodeInfo(
        final String nodeName
    ) {
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> ni.getNode().getNodeName().equalsIgnoreCase(nodeName))
                         .findFirst()
                         .orElse(null);

    }

    /**
     * Returns a collection of NodeInfo objects of all the nodes of a particular category
     * @param category category of interest
     * @return collection
     */
    public Collection<NodeInfo> getNodeInfos(
        final NodeCategory category
    ) {
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> ni.getNode().getNodeCategory() == category)
                         .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Returns a collection of NodeInfo objects of all the device nodes of a particular type
     * @param deviceType device type of interest
     * @return collection
     */
    public Collection<NodeInfo> getNodeInfos(
        final DeviceType deviceType
    ) {
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> (ni.getNode() instanceof Device dev) && (dev.getDeviceType() == deviceType))
                         .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Returns a collection of the NodeInfo objects associated with the nodes
     * which are accessible via the channel indicated by channelInfo.
     * @param channelInfo indicates the channel of interest
     * @return list of NodeInfo objects
     */
    public Collection<NodeInfo> getNodeInfosForChannel(
        final ChannelNodeInfo channelInfo
    ) {
        var chan = (Channel)channelInfo.getNode();
        return _nodeGraph.values()
                         .stream()
                         .filter(ni -> (ni instanceof DeviceNodeInfo dni) && dni._routes.contains(chan))
                         .map(ni -> (DeviceNodeInfo) ni)
                         .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Produces a string suitable for display node status upon the console
     * @param nodeIdentifier nodeIdentifier of the node we are interested in
     * @return node status string
     * @throws ExecStoppedException If the exec stops during this function
     */
    public String getNodeStatusString(
        final int nodeIdentifier
    ) throws ExecStoppedException {
        var ni = _nodeGraph.get(nodeIdentifier);
        if (ni == null) {
            LogManager.logFatal(LOG_SOURCE, "getNodeStatusString nodeIdentifier %d not found", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var sb = new StringBuilder();
        sb.append(ni.getNode().getNodeName()).append("     ").setLength(6);
        sb.append(" ").append(ni.getNodeStatus().getDisplayString());
        sb.append(isDeviceAccessible(nodeIdentifier) ? "   " : " NA");

        if (ni.getNode() instanceof DiskDevice) {
            // [[*] [R|F] PACKID pack-id]
            sb.append(ni.getAssignedTo() == null ? "  " : " *");
            if (ni.getMediaInfo() instanceof PackInfo pi) {
                if (pi.isFixed()) {
                    sb.append(" F");
                } else if (pi.isRemovable()) {
                    sb.append(" R");
                } else {
                    sb.append("  ");
                }

                sb.append(" PACKID ").append(pi.getPackName());
            }
        } else if (ni.getNode() instanceof TapeDevice) {
            // [* RUNID run-id] [REEL reel [RING|NORING] [POS [*]ffff[+|-][*]bbbbbb | POS LOST]]]
            // First part is printed if the drive is assigned
            // Second part is printed if a reel is mounted (could be not assigned, in the case of pre-mount)
            if (ni.getAssignedTo() != null) {
                var runId = ni.getAssignedTo().getRunId();
                sb.append("* RUNID ").append(String.format("%-6s", runId)).append(" ");
            }
            if (ni.getMediaInfo() instanceof VolumeInfo vi) {
                sb.append("REEL ").append(String.format("%-6s", vi.getVolumeName())).append(" ");
                // TODO RING/NORING which we get from the facitem (if assigned), and POS which we get from the device
            }
        }

        return sb.toString();
    }

    /**
     * Indicates whether the device with the given identifier is accessible
     * (it has at least one channel path for which the channel is not DN).
     * @param nodeIdentifier identifier of the device
     * @return true if the device is accessible
     */
    public boolean isDeviceAccessible(
        final int nodeIdentifier
    ) {
        var ni = _nodeGraph.get(nodeIdentifier);
        if (ni instanceof DeviceNodeInfo dni) {
            for (var chan : dni._routes) {
                var cni = _nodeGraph.get(chan.getNodeIdentifier());
                if (cni != null && cni.getNodeStatus() == NodeStatus.Up) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Routes an IO described by a channel program.
     * For the case where some portion of the Exec needs to do device-specific IO.
     * @param channelProgram IO description
     * @throws ExecStoppedException if the exec stops during this function
     * @throws NoRouteForIOException if the destination device has no available path
     */
    public void routeIo(
        final ChannelProgram channelProgram
    ) throws ExecStoppedException, NoRouteForIOException {
        var nodeInfo = _nodeGraph.get(channelProgram.getNodeIdentifier());
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE,
                                "Node %d from channel program is not configured",
                                channelProgram.getNodeIdentifier());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if (node.getNodeCategory() != NodeCategory.Device) {
            LogManager.logFatal(LOG_SOURCE,
                                "Node %d from channel program is not a device",
                                channelProgram.getNodeIdentifier());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        selectRoute((Device) node).routeIo(channelProgram);
    }

    /**
     * Invoked by Exec after boot() has been called for all managers.
     */
    public void startup() throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "startup()");

        var e = Exec.getInstance();
        dropTapes();
        readDiskLabels();
        var fixedDisks = getAccessibleFixedDisks();
        if (fixedDisks.isEmpty()) {
            e.sendExecReadOnlyMessage("No Fixed Disk Configured", null);
            e.stop(StopCode.InitializationSystemConfigurationError);
            throw new ExecStoppedException();
        }

        var mm = Exec.getInstance().getMFDManager();
        if (Exec.getInstance().isJumpKeySet(13)) {
            mm.initializeMassStorage(fixedDisks);
        } else {
            mm.recoverMassStorage(fixedDisks);
        }

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    // -------------------------------------------------------------------------
    // Core
    // -------------------------------------------------------------------------

    /**
     * Checks the read/write keys provided (or not) in a FileSpecification object
     * against the keys which do (or do not) exist in the FileSetInfo object.
     * Posts any fac results necessary, aborts the run if necessary, etc.
     * Caller should check bits 10 and 11 of the result code to determine whether the file should be
     * assigned write-inhibited and/or read-inhibited, respectively.
     * @return true if no problems exist, else false
     */
    private boolean checkKeys(
        final RunControlEntry rce,
        final FileSetInfo fsInfo,
        final FileSpecification fileSpec,
        final FacStatusResult fsResult
    ) {
        var err = false;
        var existingReadKey = fsInfo.getReadKey();
        var hasReadKey = !existingReadKey.isEmpty();
        var givenReadKey = fileSpec.getReadKey();
        var gaveReadKey = givenReadKey != null;
        if (hasReadKey) {
            if (!gaveReadKey && (!rce.isPrivileged() || fsInfo.isGuarded())) {
                fsResult.postMessage(FacStatusCode.ReadKeyExists);
                fsResult.mergeStatusBits(0_000100_000000L);
            } else if (!existingReadKey.equalsIgnoreCase(givenReadKey)) {
                fsResult.postMessage(FacStatusCode.IncorrectReadKey);
                fsResult.mergeStatusBits(0_401000_000000L);
                if (rce.hasTask()) {
                    rce.postContingency(017, 0, 0, 015);
                }
                err = true;
            }
        } else {
            if (gaveReadKey) {
                fsResult.postMessage(FacStatusCode.FileNotCatalogedWithReadKey);
                fsResult.mergeStatusBits(0_400040_000000L);
                if (rce.hasTask()) {
                    rce.postContingency(017, 0, 0, 015);
                }
                err = true;
            }
        }

        var existingWriteKey = fsInfo.getWriteKey();
        var hasWriteKey = !existingWriteKey.isEmpty();
        var givenWriteKey = fileSpec.getWriteKey();
        var gaveWriteKey = givenWriteKey != null;
        if (hasWriteKey) {
            if (!gaveWriteKey && (!rce.isPrivileged() || fsInfo.isGuarded())) {
                fsResult.postMessage(FacStatusCode.WriteKeyExists);
                fsResult.mergeStatusBits(0_000200_000000L);
            } else if (!existingWriteKey.equalsIgnoreCase(givenWriteKey)) {
                fsResult.postMessage(FacStatusCode.IncorrectWriteKey);
                fsResult.mergeStatusBits(0_400400_000000L);
                if (rce.hasTask()) {
                    rce.postContingency(017, 0, 0, 015);
                }
                err = true;
            }
        } else {
            if (gaveWriteKey) {
                fsResult.postMessage(FacStatusCode.FileNotCatalogedWithWriteKey);
                fsResult.mergeStatusBits(0_400020_000000L);
                if (rce.hasTask()) {
                    rce.postContingency(017, 0, 0, 015);
                }
                err = true;
            }
        }

        return !err;
    }

    /**
     * If the indicated file cycle is private, check the options and rce to see if the caller
     * is allowed to access the file. We produce fac status accordingly.
     * @return true if access is allowed, else false.
     */
    private boolean checkPrivateAccess(
        final RunControlEntry rce,
        final FileCycleInfo fcInfo,
        final FacStatusResult fsResult
    ) {
        var ok = !fcInfo.getInhibitFlags().isPrivate();
        if (!ok && (!fcInfo.getInhibitFlags().isGuarded())) {
            var privByAcct = Exec.getInstance().getConfiguration().getFilesPrivateByAccount();
            ok = (rce.isPrivileged())
                || (privByAcct && rce.getAccountId().equals(fcInfo.getAccountId()))
                || (!privByAcct && rce.getProjectId().equals(fcInfo.getProjectId()));
        }
        if (!ok) {
            fsResult.postMessage(FacStatusCode.IncorrectPrivacyKey);
            fsResult.mergeStatusBits(0_400000_020000L);
        }
        return ok;
    }

    /**
     * Unloads all the ready tape devices - used during boot.
     * @throws ExecStoppedException if something goes wrong while we're doing this.
     */
    private void dropTapes() throws ExecStoppedException {
        for (var ni : _nodeGraph.values()) {
            if ((ni instanceof DeviceNodeInfo dni)
                && (ni.getNode() instanceof TapeDevice td)
                && td.isReady()) {

                var cp = new ChannelProgram().setNodeIdentifier(ni.getNode().getNodeIdentifier())
                                             .setFunction(ChannelProgram.Function.Control)
                                             .setSubFunction(ChannelProgram.SubFunction.Unload);
                try {
                    routeIo(cp);
                } catch (NoRouteForIOException ex) {
                    LogManager.logCatching(LOG_SOURCE, ex);
                }
            }
        }
    }

    /**
     * Retrieves a collection of NodeInfo objects representing UP and SU disk units.
     * Also assigns the units to the Exec.
     */
    private Collection<NodeInfo> getAccessibleFixedDisks() throws ExecStoppedException {
        var list = new LinkedList<NodeInfo>();
        var execQualifier = Exec.getInstance().getRunControlEntry().getProjectId();
        for (var ni : _nodeGraph.values()) {
            var node = ni.getNode();
            if ((node instanceof DiskDevice)
                && ((ni.getNodeStatus() == NodeStatus.Up) || (ni.getNodeStatus() == NodeStatus.Suspended))) {
                list.add(ni);
                var fsResult = new FacStatusResult();
                var fileSpec = new FileSpecification(execQualifier,
                                                     "Fixed$" + node.getNodeName(),
                                                     null,
                                                     null,
                                                     null);
                assignDiskUnitToExec(fileSpec, node.getNodeIdentifier(), null, fsResult);
            }
        }
        return list;
    }

    /**
     * Creates and populates PackInfo based on the ArraySlice containing the label for a disk pack
     */
    private PackInfo loadDiskPackInfo(
        final NodeInfo diskNodeInfo
    ) throws NoRouteForIOException, ExecStoppedException {
        var diskDevice = (DiskDevice) diskNodeInfo.getNode();
        var diskLabel = readPackLabel(diskDevice);
        if (diskLabel == null) {
            var msg = getNodeStatusString(diskDevice.getNodeIdentifier());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        var initialDirTrack = readInitialDirectoryTrack(diskDevice, diskLabel);
        var pi = PackInfo.loadFromLabel(diskLabel, initialDirTrack);
        if (pi == null) {
            var msg = String.format("Pack on %s has no label", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!Exec.isValidPackName(pi.getPackName())) {
            var msg = String.format("Pack on %s has an invalid pack name", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!Exec.isValidPrepFactor(pi.getPrepFactor())) {
            var msg = String.format("Pack on %s has an invalid prep factor: %d",
                                    diskDevice.getNodeName(),
                                    pi.getPrepFactor());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!pi.isPrepped()) {
            var msg = String.format("Pack on %s is not prepped", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        var sb = new StringBuilder();
        sb.append(pi.getPackName()).append(" on ").append(diskDevice.getNodeName()).append(" is ");
        if (pi.isFixed()) {
            sb.append("FIXED ");
            if (pi.getLDATIndex() == 0) {
                sb.append("INIT");
            } else {
                sb.append(pi.getLDATIndex());
            }
        } else if (pi.isRemovable()) {
            sb.append("REM ");
            if (pi.getLDATIndex() == 0) {
                sb.append("INIT");
            } else {
                sb.append(pi.getLDATIndex());
            }
        } else {
            sb.append("not prepped");
        }

        sb.append(" PREP FACTOR ").append(pi.getPrepFactor());
        Exec.getInstance().sendExecReadOnlyMessage(sb.toString(), null);

        return pi;
    }

    void loadNodeGraph() {
        // Load node graph based on the configuration TODO
        // The following is temporary
        var disk0 = new FileSystemDiskDevice("DISK0", "media/disk0.pack", false);
        var disk1 = new FileSystemDiskDevice("DISK1", "media/disk1.pack", false);
        var disk2 = new FileSystemDiskDevice("DISK2", "media/disk2.pack", false);
        var disk3 = new FileSystemDiskDevice("DISK3", "media/disk3.pack", false);

        var dch0 = new DiskChannel("CHDSK0");
        dch0.attach(disk0);
        dch0.attach(disk1);
        dch0.attach(disk2);
        dch0.attach(disk3);

        var dch1 = new DiskChannel("CHDSK1");
        dch1.attach(disk0);
        dch1.attach(disk1);
        dch1.attach(disk2);
        dch1.attach(disk3);

        var tape0 = new FileSystemTapeDevice("TAPE0");
        var tape1 = new FileSystemTapeDevice("TAPE1");

        var tch = new TapeChannel("CHTAPE");
        tch.attach(tape0);
        tch.attach(tape1);

        _nodeGraph.put(dch0.getNodeIdentifier(), new ChannelNodeInfo(dch0));
        _nodeGraph.put(dch1.getNodeIdentifier(), new ChannelNodeInfo(dch1));
        _nodeGraph.put(tch.getNodeIdentifier(), new ChannelNodeInfo(tch));

        _nodeGraph.put(disk0.getNodeIdentifier(), new DeviceNodeInfo(disk0));
        _nodeGraph.put(disk1.getNodeIdentifier(), new DeviceNodeInfo(disk1));
        _nodeGraph.put(disk2.getNodeIdentifier(), new DeviceNodeInfo(disk2));
        _nodeGraph.put(disk3.getNodeIdentifier(), new DeviceNodeInfo(disk3));
        _nodeGraph.put(tape0.getNodeIdentifier(), new DeviceNodeInfo(tape0));
        _nodeGraph.put(tape1.getNodeIdentifier(), new DeviceNodeInfo(tape1));
        // end temporary code
    }

    /**
     * Prompts the operator to load a pack on a disk unit,
     * waits for it to happen, then verifies the packname.
     * @param runControlEntry describes the invoking run
     * @param nodeInfo NodeInfo object tracking exec information regarding the disk unit
     * @param disk DiskDevice object associated with the disk unit
     * @param packName pack name requested by the run
     * @return true if the pack is loaded - the operator may deny pack loading based on pack name mismatch.
     * @throws ExecStoppedException if we notice the exec has stopped during processing
     */
    private boolean promptLoadPack(
        final RunControlEntry runControlEntry,
        final NodeInfo nodeInfo,
        final DiskDevice disk,
        final String packName
    ) throws ExecStoppedException {
        // TODO while waiting we need to monitor the rce to see if it has been err'd, aborted, etc
        //   so we can stop waiting, post E:260733 Run has been aborted, and return false
        var loadMsg = String.format("Load %s %s %s",
                                    packName,
                                    disk.getNodeName(),
                                    runControlEntry.getRunId());
        Exec.getInstance().sendExecReadOnlyMessage(loadMsg, null);
        var serviceMsg = loadMsg.replace("Load", "Service");

        var nextMessageTime = Instant.now().plusSeconds(120);
        while (!disk.isReady()) {
            if (!Exec.getInstance().isRunning()) {
                throw new ExecStoppedException();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // do nothing
            }

            // Service message every {n} minutes
            if (Instant.now().isAfter(nextMessageTime)) {
                Exec.getInstance().sendExecReadOnlyMessage(serviceMsg, null);
                nextMessageTime = Instant.now().plusSeconds(120);
            }
        }

        // Read pack label
        try {
            var label = readPackLabel(disk);
            if (label == null) {
                return false;
            }
            var info = loadDiskPackInfo(nodeInfo);
            if (info != null) {
                nodeInfo.setMediaInfo(info);
            }
        } catch (NoRouteForIOException ex) {
            return false;
        }

        // compare pack names - if there is a mismatch, consult the operator.
        // If the operator is upset about it, un-assign the unit from the run and post appropriate status.
        var currentPackName = nodeInfo.getMediaInfo().getMediaName();
        if (currentPackName != null && !currentPackName.equals(packName)) {
            var candidates = new String[]{ "Y", "N" };
            var msg = String.format("Allow %s as substitute pack on %s YN?", currentPackName, nodeInfo.getNode().getNodeName());
            var response = Exec.getInstance().sendExecRestrictedReadReplyMessage(msg, candidates, null);
            return response.equalsIgnoreCase("Y");
        }

        return true;
    }

    /**
     * Reads the labels and directory tracks for all UP and SU disks.
     * Used early in booting.
     * @throws ExecStoppedException if something goes wrong while we're doing this
     */
    private void readDiskLabels() throws ExecStoppedException {
        for (var ni : _nodeGraph.values()) {
            if ((ni.getNodeStatus() == NodeStatus.Up) || (ni.getNodeStatus() == NodeStatus.Suspended)) {
                if ((ni instanceof DeviceNodeInfo dni) && (ni.getNode() instanceof DiskDevice dd)) {
                    try {
                        var info = loadDiskPackInfo(dni);
                        if (info != null) {
                            ni.setMediaInfo(info);
                        }
                    } catch (NoRouteForIOException ex) {
                        LogManager.logInfo(LOG_SOURCE, "No route to device %s", dd.getNodeName());
                    }
                }
            }
        }
    }

    private ArraySlice readInitialDirectoryTrack(
        final DiskDevice disk,
        final ArraySlice diskLabel
    ) throws NoRouteForIOException, ExecStoppedException {
        var dirTrackAddr = diskLabel.get(3);
        var blocksPerTrack = Word36.getH1(diskLabel.get(4));
        var dirBlockAddr = dirTrackAddr * blocksPerTrack;
        var channel = selectRoute(disk);
        var cw = new ChannelProgram.ControlWord().setDirection(ChannelProgram.Direction.Increment)
                                                 .setBuffer(new ArraySlice(new long[1792]))
                                                 .setTransferCount(1792);
        var cp = new ChannelProgram().addControlWord(cw)
                                     .setFunction(ChannelProgram.Function.Read)
                                     .setBlockId(dirBlockAddr)
                                     .setNodeIdentifier(disk.getNodeIdentifier());
        channel.routeIo(cp);
        if (cp.getIoStatus() != IoStatus.Complete) {
            LogManager.logError(LOG_SOURCE, "readPackLabel ioStatus=%s", cp.getIoStatus());
            var msg = String.format("%s Cannot read directory track %s", disk.getNodeName(), cp.getIoStatus());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            return null;
        }

        return cw.getBuffer();
    }

    private ArraySlice readPackLabel(
        final DiskDevice disk
    ) throws NoRouteForIOException, ExecStoppedException {
        var channel = selectRoute(disk);
        var cw = new ChannelProgram.ControlWord().setDirection(ChannelProgram.Direction.Increment)
                                                 .setBuffer(new ArraySlice(new long[28]))
                                                 .setTransferCount(28);
        var cp = new ChannelProgram().addControlWord(cw)
                                     .setFunction(ChannelProgram.Function.Read)
                                     .setBlockId(0)
                                     .setNodeIdentifier(disk.getNodeIdentifier());
        channel.routeIo(cp);
        if (cp.getIoStatus() != IoStatus.Complete) {
            LogManager.logError(LOG_SOURCE, "readPackLabel ioStatus=%s", cp.getIoStatus());
            var msg = String.format("%s Cannot read pack label %s", disk.getNodeName(), cp.getIoStatus());
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
            return null;
        }

        return cw.getBuffer();
    }

    Channel selectRoute(final Device device) throws ExecStoppedException, NoRouteForIOException {
        var ni = _nodeGraph.get(device.getNodeIdentifier());
        if (ni == null) {
            LogManager.logFatal(LOG_SOURCE, "cannot find NodeInfo for %s", device.getNodeName());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        if (ni instanceof DeviceNodeInfo dni) {
            for (int cx = 0; cx < dni._routes.size(); cx++) {
                var chan = dni._routes.pop();
                dni._routes.push(chan);
                var chi = _nodeGraph.get(chan.getNodeIdentifier());
                if (chi == null) {
                    LogManager.logFatal(LOG_SOURCE, "cannot find NodeInfo for %s", chan.getNodeName());
                    Exec.getInstance().stop(StopCode.FacilitiesComplex);
                    throw new ExecStoppedException();
                }

                if (chi.getNodeStatus() == NodeStatus.Up) {
                    return (Channel) chi.getNode();
                }
            }

            // if we get here, there aren't any routes
            throw new NoRouteForIOException(ni.getNode().getNodeIdentifier());
        } else {
            LogManager.logFatal(LOG_SOURCE, "ni is not DeviceNodeInfo for %s", device.getNodeName());
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }
    }
}
