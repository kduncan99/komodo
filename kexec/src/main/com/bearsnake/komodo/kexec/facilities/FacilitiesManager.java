/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.hardwarelib.channels.Channel;
import com.bearsnake.komodo.hardwarelib.channels.ChannelIoPacket;
import com.bearsnake.komodo.hardwarelib.devices.Device;
import com.bearsnake.komodo.hardwarelib.devices.DeviceType;
import com.bearsnake.komodo.hardwarelib.channels.DiskChannel;
import com.bearsnake.komodo.hardwarelib.devices.DiskDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemDiskDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemPrinterDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemCardReaderDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemCardPunchDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemTapeDevice;
import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.NodeCategory;
import com.bearsnake.komodo.hardwarelib.channels.SymbiontChannel;
import com.bearsnake.komodo.hardwarelib.channels.TapeChannel;
import com.bearsnake.komodo.hardwarelib.devices.TapeDevice;
import com.bearsnake.komodo.hardwarelib.channels.TransferFormat;
import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.configuration.Configuration;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.configuration.MnemonicType;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.exceptions.AbsoluteCycleConflictException;
import com.bearsnake.komodo.kexec.exceptions.AbsoluteCycleOutOfRangeException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.FileCycleDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.FileSetAlreadyExistsException;
import com.bearsnake.komodo.kexec.exceptions.FileSetDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.ERIO$Status;
import com.bearsnake.komodo.kexec.scheduleManager.Run;
import com.bearsnake.komodo.kexec.scheduleManager.RunType;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.facItems.AbsoluteDiskItem;
import com.bearsnake.komodo.kexec.facilities.facItems.DiskFileFacilitiesItem;
import com.bearsnake.komodo.kexec.facilities.facItems.FacilitiesItem;
import com.bearsnake.komodo.kexec.facilities.facItems.FixedDiskFileFacilitiesItem;
import com.bearsnake.komodo.kexec.facilities.facItems.NameItem;
import com.bearsnake.komodo.kexec.facilities.facItems.RemovableDiskFileFacilitiesItem;
import com.bearsnake.komodo.kexec.facilities.facItems.TapeFileFacilitiesItem;
import com.bearsnake.komodo.kexec.mfd.DescriptorFlags;
import com.bearsnake.komodo.kexec.mfd.DisableFlags;
import com.bearsnake.komodo.kexec.mfd.DiskFileCycleInfo;
import com.bearsnake.komodo.kexec.mfd.DiskPackEntry;
import com.bearsnake.komodo.kexec.mfd.FileCycleInfo;
import com.bearsnake.komodo.kexec.mfd.FileFlags;
import com.bearsnake.komodo.kexec.mfd.FileSetInfo;
import com.bearsnake.komodo.kexec.mfd.FileType;
import com.bearsnake.komodo.kexec.mfd.FixedDiskFileCycleInfo;
import com.bearsnake.komodo.kexec.mfd.InhibitFlags;
import com.bearsnake.komodo.kexec.mfd.PCHARFlags;
import com.bearsnake.komodo.kexec.mfd.RemovableDiskFileCycleInfo;
import com.bearsnake.komodo.kexec.mfd.UnitSelectionIndicators;
import com.bearsnake.komodo.logger.LogManager;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bearsnake.komodo.baselib.Word36.*;
import static com.bearsnake.komodo.logger.Level.Trace;

public class FacilitiesManager implements Manager {

    static final String LOG_SOURCE = "FacMgr";

    private final static String[] ABGM_RESPONSES = new String[]{ "A", "B", "G", "M" };
    private final static String ABGM_RESPONSE_STR = "ABGM";
    private final static String[] AGM_RESPONSES = new String[]{ "A", "G", "M" };
    private final static String AGM_RESPONSE_STR = "AGM";

    static final HashMap<IoStatus, ERIO$Status> _ioStatusTranslateTable = new HashMap<>();
    static {
        /* NotStarted does not have a corresponding ERIO status */
        _ioStatusTranslateTable.put(IoStatus.Successful, ERIO$Status.Success);
        _ioStatusTranslateTable.put(IoStatus.Canceled, ERIO$Status.SystemError);
        _ioStatusTranslateTable.put(IoStatus.AtLoadPoint, ERIO$Status.EndOfTape);
        /*
        TODO
            InvalidChannelProgram,
            DataException, // something in the device meta-data is bad
            DeviceDoesNotExist,
        */
        _ioStatusTranslateTable.put(IoStatus.DeviceIsDown, ERIO$Status.DeviceDownOrNotAvailable);
        _ioStatusTranslateTable.put(IoStatus.DeviceIsNotAccessible, ERIO$Status.DeviceDownOrNotAvailable);
        _ioStatusTranslateTable.put(IoStatus.DeviceIsNotAttached, ERIO$Status.DeviceDownOrNotAvailable);
        /* TODO DeviceIsNotReady should cause a retry at the console, culminating with ... what? */
        _ioStatusTranslateTable.put(IoStatus.EndOfFile, ERIO$Status.EndOfFile);
        _ioStatusTranslateTable.put(IoStatus.EndOfTape, ERIO$Status.EndOfTape);
        /*
        TODO
            InternalError,
            InvalidAddress,
            InvalidBlockCount,
            InvalidBlockId,
            InvalidBufferSize,
            InvalidFunction,
            InvalidNodeType,
            InvalidPacket,
            InvalidPackName,
            InvalidPrepFactor,
            InvalidTapeBlock,
            InvalidTrackCount,
            InvalidTransferDirection,
            InvalidTransferFormat,
        */
        _ioStatusTranslateTable.put(IoStatus.LostPosition, ERIO$Status.LostPosition);
        /*
            MediaAlreadyMounted,
            MediaNotMounted,
            NonIntegralRead,
            PackNotPrepped,
            ReadNotAllowed,
            ReadOverrun,
        */
        _ioStatusTranslateTable.put(IoStatus.SystemError, ERIO$Status.SystemError);
        _ioStatusTranslateTable.put(IoStatus.WriteProtected, ERIO$Status.WriteInhibited);
    }

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

        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();

        // update verbosity of nodes
        for (var ni : _nodeGraph.values()) {
            ni.getNode().setLogIos(cfg.getBooleanValue(Tag.IODBUG));
        }

        // clear cached disk labels
        for (var ni : _nodeGraph.values()) {
            ni.setMediaInfo(null);
        }

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    @Override
    public void close() {
        LogManager.logTrace(LOG_SOURCE, "close()");
        for (var ni : _nodeGraph.values()) {
            ni.getNode().close();
        }
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
            var mi = ni.getMediaInfo();
            if (mi != null) {
                mi.dump(out, subIndent);
            }
        }
    }

    @Override
    public void initialize() throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "initialize()");

        // load node graph and set up routes
        loadNodeGraph();
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
    // These routines are generally better for internal use, as they may have
    // certain expectations which are not filtered out otherwise.
    // Where possible, CSI should be used as the interface to facilities.
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

        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();
        var req = new AssignCatalogedDiskFileRequest(fileSpecification).setOptionsWord(optionsWord)
                                                                       .setMnemonic(cfg.getStringValue(Tag.MDFALT))
                                                                       .setAssignIfDisabled()
                                                                       .setExclusiveUse(exclusiveUse);
        var result = assignCatalogedDiskFileToRun(exec, req, fsResult);

        LogManager.logTrace(LOG_SOURCE, "assignCatalogedFileToExec result:%s", fsResult.toString());
        return result;
    }

    /**
     * Assigns a cataloged disk file...
     * Parameters are generally apparent, but note that fileSpecification must be resolved w.r.t.
     * internal use names and default/implied qualifiers.
     * @throws ExecStoppedException if the exec is found to be stopped (or we stop it) during execution
     */
    public synchronized boolean assignCatalogedDiskFileToRun(
        final Run run,
        final AssignCatalogedDiskFileRequest request,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "assignCatalogedDiskFileToRun %s %s",
                            run.getActualRunId(),
                            request._fileSpecification.toString());

        var exec = Exec.getInstance();

        // --------------------------------------------------------
        // Pre-checks which are very generic
        // --------------------------------------------------------

        var mm = Exec.getInstance().getMFDManager();
        FileSetInfo fsInfo;
        try {
            fsInfo = mm.getFileSetInfo(request._fileSpecification.getQualifier(), request._fileSpecification.getFilename());
        } catch (FileSetDoesNotExistException ex) {
            fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
            fsResult.mergeStatusBits(0_400010_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        // If it is fixed, do not accept any pack-ids.
        if ((fsInfo.getFileType() == FileType.Fixed) && (!request._packIds.isEmpty())) {
            fsResult.postMessage(FacStatusCode.AssignMnemonicDoesNotAllowPackIds);
            fsResult.mergeStatusBits(0_600010_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        // Ensure we're not asking to assign a disk file when the file set is tape
        if ((fsInfo.getFileType() != FileType.Fixed) && (fsInfo.getFileType() != FileType.Removable)) {
            fsResult.postMessage(FacStatusCode.AttemptToChangeGenericType);
            fsResult.mergeStatusBits(0_420000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        // Check read/write keys
        if (!checkKeys(run, fsInfo, request._fileSpecification, fsResult)) {
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }
        var readInhibit = (fsResult.getStatusWord() & 0_000100_000000L) != 0;
        var writeInhibit = (fsResult.getStatusWord() & 0_000200_000000L) != 0;

        // --------------------------------------------------------
        // Determine whether the indicated file cycle exists,
        // then get fcInfo and facItem accordingly
        // --------------------------------------------------------

        DiskFileFacilitiesItem facItem;
        DiskFileCycleInfo fcInfo;
        String qualifier = request._fileSpecification.getQualifier();
        String filename = request._fileSpecification.getFilename();
        int absCycle;
        var fiTable = run.getFacilitiesItemTable();

        if (request._fileSpecification.hasFileCycleSpecification()
            && request._fileSpecification.getFileCycleSpecification().isAbsolute()) {

            // This an absolute file cycle request._
            // Go get the file cycle info if the file exists (else fail).
            // Get the existing fac item if the file is already assigned to the run (but it is not an error if it wasn't).
            absCycle = request._fileSpecification.getFileCycleSpecification().getCycle();
            try {
                fcInfo = (DiskFileCycleInfo) mm.getFileCycleInfo(qualifier, filename, absCycle);
                facItem = (DiskFileFacilitiesItem) fiTable.getFacilitiesItem(qualifier, filename, absCycle);
            } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                // we already checked for file set not existing, but we have to catch it here anyway.
                fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
                fsResult.mergeStatusBits(0_400010_000000L);
                fsResult.log(Trace, LOG_SOURCE);
                return false;
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
            var relCycle = 0;
            if (request._fileSpecification.hasFileCycleSpecification()) {
                relCycle = request._fileSpecification.getFileCycleSpecification().getCycle();
            }

            facItem = (DiskFileFacilitiesItem) fiTable.getFacilitiesItemByRelativeCycle(qualifier, filename, relCycle);
            if (facItem != null) {
                var fsci = fsInfo.getCycleInfo().get(facItem.getAbsoluteCycle());
                absCycle = fsci.getAbsoluteCycle();
                try {
                    fcInfo = (DiskFileCycleInfo) mm.getFileCycleInfo(qualifier, filename, absCycle);
                } catch (FileSetDoesNotExistException | FileCycleDoesNotExistException ex) {
                    LogManager.logFatal(LOG_SOURCE,
                                        "MFD cannot find a file cycle which must exist %s*%s(%d)",
                                        qualifier, filename, absCycle);
                    exec.stop(StopCode.FacilitiesComplex);
                    throw new ExecStoppedException();
                }
            } else {
                if (relCycle == 1) {
                    // @ASG,A of +1 cycle is not allowed unless we already have it assigned,
                    // which is already accounted for in the code just above, and it cannot be assigned by
                    // absolute cycle - if it was already assigned, it would have been by relative cycle +1.
                    fsResult.postMessage(FacStatusCode.Plus1IllegalWithAOption);
                    fsResult.mergeStatusBits(0_400000_000040L);
                    fsResult.log(Trace, LOG_SOURCE);
                    return false;
                }

                if (Math.abs(relCycle) >= fsInfo.getCycleInfo().size()) {
                    fsResult.postMessage(FacStatusCode.FileIsNotCataloged);
                    fsResult.mergeStatusBits(0_400010_000000L);
                    fsResult.log(Trace, LOG_SOURCE);
                    return false;
                }

                // Do we already have it assigned by the absolute cycle?
                var fsci = fsInfo.getCycleInfo().get(Math.abs(relCycle));
                absCycle = fsci.getAbsoluteCycle();
                try {
                    fcInfo = (DiskFileCycleInfo) mm.getFileCycleInfo(qualifier, filename, absCycle);
                } catch (FileSetDoesNotExistException | FileCycleDoesNotExistException ex) {
                    LogManager.logFatal(LOG_SOURCE,
                                        "MFD cannot find a file cycle which must exist %s*%s(%d)",
                                        qualifier, filename, absCycle);
                    exec.stop(StopCode.FacilitiesComplex);
                    throw new ExecStoppedException();
                }

                facItem = (DiskFileFacilitiesItem) fiTable.getFacilitiesItemByRelativeCycle(qualifier, filename, absCycle);
                if (facItem != null) {
                    // yes - attach this relative cycle to the already-existing fac item.
                    // If it already has a relative cycle, it would have to be the same as this one, so no worries.
                    facItem.setRelativeCycle(relCycle);
                }
            }
        }

        var isRemovable = fsInfo.getFileType() == FileType.Removable;
        // TODO what should we do about NameItem fac items?
        //   i.e., @USE FOO.,SYS$LIB$*NUMPY.
        //         @ASG,A FOO.

        var wasAlreadyAssigned = (facItem != null);
        if (wasAlreadyAssigned) {
            // We need to filter out the effects of D,E,K,R, and M since we are already assigned.
            // We could not do this previously, as we developed the idea of being already assigned
            // throughout the course of the preceding nonsense. But we are here now, so...
            // We filter it out by simply not allowing these items to propagate to the already-existing
            // facilities item - only applying them to a newly-created facilities item in the alternate
            // conditional branch below.
            // We do have to post a warning if any of the options were presented for an already assigned file...
            if ((request._optionsWord & (D_OPTION | E_OPTION | K_OPTION | R_OPTION | M_OPTION)) != 0) {
                fsResult.postMessage(FacStatusCode.OptionConflictOptionsIgnored);
            }
        }

        // --------------------------------------------------------
        // Check placement or pack-id list
        // --------------------------------------------------------

        PlacementInfo placementInfo;
        if (isRemovable) {
            // We are removable, check pack-ids (and ensure placement was not specified)
            if (request._placement != null) {
                fsResult.postMessage(FacStatusCode.PlacementFieldNotAllowedForRemovable);
                fsResult.mergeStatusBits(0_600000_000000L);
                fsResult.log(Trace, LOG_SOURCE);
                return false;
            }

            var remInfo = (RemovableDiskFileCycleInfo) fcInfo;
            if (!request._packIds.isEmpty()
                && !checkPackIdsForAssign(fsInfo, remInfo, request._optionsWord, request._packIds, fsResult)) {
                fsResult.log(Trace, LOG_SOURCE);
                return false;
            }
        } else {
            // We are fixed, check placement validity (and ensure pack-ids were not specified)
            if (!request._packIds.isEmpty()) {
                // we'd like a better message, but this is all we have...
                fsResult.postMessage(FacStatusCode.UndefinedFieldOrSubfield);
                fsResult.mergeStatusBits(0_600000_000000L);
                fsResult.log(Trace, LOG_SOURCE);
                return false;
            }

            if (request._placement != null) {
                placementInfo = checkPlacement(request._placement, fsResult);
                if (placementInfo == null) {
                    fsResult.log(Trace, LOG_SOURCE);
                    return false;
                }
            }
        }

        // --------------------------------------------------------
        // If the file was not already assigned, create a fac item
        // --------------------------------------------------------

        if (!wasAlreadyAssigned) {
            // Check public/private - not necessary if the file was already assigned, but it wasn't, so...
            if (!run.isPrivileged() || fcInfo.getInhibitFlags().isGuarded()) {
                if (!checkPrivateAccess(run, fcInfo)) {
                    fsResult.postMessage(FacStatusCode.IncorrectPrivacyKey);
                    fsResult.mergeStatusBits(0_400000_020000L);
                    fsResult.log(Trace, LOG_SOURCE);
                    return false;
                }
            }

            // Is the file read-only or write-only?
            readInhibit |= fcInfo.getInhibitFlags().isWriteOnly();
            readInhibit |= (request._directoryOnlyBehavior != DirectoryOnlyBehavior.None);
            writeInhibit |= fcInfo.getInhibitFlags().isReadOnly();
            writeInhibit |= request._readOnly || (request._directoryOnlyBehavior != DirectoryOnlyBehavior.None);

            // Create new fac item with option-driven settings as necessary and add it to the fac item table.
            facItem = isRemovable ? new RemovableDiskFileFacilitiesItem() : new FixedDiskFileFacilitiesItem();
            facItem.setIsExclusive(request._exclusiveUse)
                   .setDeleteOnNormalRunTermination(request._deleteBehavior == DeleteBehavior.DeleteOnNormalRunTermination)
                   .setDeleteOnAnyRunTermination(request._deleteBehavior == DeleteBehavior.DeleteOnAnyRunTermination)
                   .setIsReadable(!readInhibit)
                   .setIsWriteable(!writeInhibit)
                   .setQualifier(fcInfo.getQualifier())
                   .setFilename(fcInfo.getFilename())
                   .setAbsoluteCycle(fcInfo.getAbsoluteCycle())
                   .setIsTemporary(false)
                   .setOptionsWord(request._optionsWord)
                   .setReleaseOnTaskEnd(request._releaseOnTaskEnd);
        }

        // --------------------------------------------------------
        // Error checking which cannot be done until we have
        // file cycle information for already existing file cycles,
        // and some general knowledge for both already-existing and
        // to-be existing file cycles, and a facItem (new or not).
        // These are failures which should be detected before
        // attempting to wait on facilities.
        // --------------------------------------------------------

        // Is file disabled? (should not check this until after we know it is accessible)
        if (!checkDisabled(fcInfo, request._directoryOnlyBehavior, request._assignIfDisabled, fsResult)) {
            fsResult.log(Trace, LOG_SOURCE);
            return false;
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
        if ((request._granularity != null) && (request._granularity != fcInfo.getPCHARFlags().getGranularity())) {
            fsResult.postMessage(FacStatusCode.AttemptToChangeGranularity);
            fsResult.mergeStatusBits(0_600000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
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

        if (wasAlreadyAssigned) {
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
                fsResult.log(Trace, LOG_SOURCE);
                return false;
            }

            // Is file being dropped?
            if (fcInfo.getDescriptorFlags().toBeDropped()) {
                fsResult.postMessage(FacStatusCode.FileIsBeingDropped);
                fsResult.mergeStatusBits(0_400000_040000L);
                fsResult.log(Trace, LOG_SOURCE);
                return false;
            }
        }

        // Is the file unloaded? If so, and it's not already assigned, we either fail or start a rollback.
        // If it *is* already assigned, we can just drop through as the current facItem should already have
        // all the rollback waiting states set properly.
        if (fcInfo.getDescriptorFlags().isUnloaded()
            && !wasAlreadyAssigned
            && (request._directoryOnlyBehavior == DirectoryOnlyBehavior.None)) {
            if (request._doNotHoldRun) {
                fsResult.postMessage(FacStatusCode.HoldForRollbackRejected);
                fsResult.mergeStatusBits(0_400002_000000L);
                fsResult.log(Trace, LOG_SOURCE);
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
        if (request._exclusiveUse) {
            if (wasAlreadyAssigned ? fcInfo.getCurrentAssignCount() > 1 : fcInfo.getCurrentAssignCount() > 0) {
                if (request._doNotHoldRun) {
                    fsResult.postMessage(FacStatusCode.HoldForXUseRejected);
                    fsResult.mergeStatusBits(0_400001_000000L);
                    fsResult.log(Trace, LOG_SOURCE);
                    return false;
                }

                if (!facItem.isWaitingForExclusiveUse()) {
                    // TODO update rce for hold condition
                }
            }
        }

        // Are we waiting, or do we need to wait, for a file which is exclusively assigned elsewhere?
        // If wasAlreadyAssigned is set, then we don't worry about it, because the facItem should already
        // be set to the correct states.
        if (!wasAlreadyAssigned && (fcInfo.getInhibitFlags().isAssignedExclusively())) {
            if (request._doNotHoldRun) {
                fsResult.postMessage(FacStatusCode.HoldForReleaseXUseRejected);
                fsResult.mergeStatusBits(0_400001_000000L);
                fsResult.log(Trace, LOG_SOURCE);
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

        if (request._releaseOnTaskEnd) {
            facItem.setReleaseOnTaskEnd(true);
        }

        if (fcInfo.getInhibitFlags().isReadOnly()) {
            fsResult.postMessage(FacStatusCode.FileCatalogedAsReadOnly);
        }

        if (fcInfo.getInhibitFlags().isWriteOnly()) {
            fsResult.postMessage(FacStatusCode.FileCatalogedAsWriteOnly);
        }

        if (wasAlreadyAssigned) {
            // post-wait generic stuff for already assigned disk file
            fsResult.postMessage(FacStatusCode.FileAlreadyAssigned);
            fsResult.mergeStatusBits(0_100000_000000L);

            // Is X-option specified and already exclusive? post warning
            if (request._exclusiveUse) {
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
                var acInfo = mm.accelerateFileCycle(qualifier, filename, absCycle);
                facItem.setAcceleratedCycleInfo(acInfo);
            } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                LogManager.logFatal(LOG_SOURCE,
                                    "MFD cannot find a file cycle which must exist %s*%s(%d)",
                                    fsInfo.getQualifier(),
                                    fsInfo.getFilename(),
                                    facItem.getAbsoluteCycle());
                exec.stop(StopCode.FacilitiesComplex);
                throw new ExecStoppedException();
            }

            // Now (finally) add the new facItem to the facItemTable
            fiTable.addFacilitiesItem(facItem);
        }

        // TODO (re)allocate initial reserve?

        LogManager.logTrace(LOG_SOURCE, "assignCatalogedFileToRun %s result:%s",
                            run.getActualRunId(),
                            fsResult.toString());
        fsResult.log(Trace, LOG_SOURCE);
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
        var result = assignDiskUnitToRun(Exec.getInstance(),
                                         fileSpecification,
                                         nodeIdentifier,
                                         packName,
                                         optionsWord,
                                         false,
                                         true,
                                         fsResult);

        fsResult.log(Trace, LOG_SOURCE);
        return result;
    }

    /**
     * For assigning a reserved disk to a run.
     * This assignment can only be temporary, and fileSpecification must be fully resolved.
     * @param run describes the run
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
        final Run run,
        // TODO following 6 parameters should probably be implemented in an AssignDiskUnitRequest class
        final FileSpecification fileSpecification,
        final int nodeIdentifier,
        final String packName,
        final long optionsWord,
        final boolean releaseOnTaskEnd,
        final boolean doNotHoldRun,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun %s %s node:%d pack:%s I:%s Z:%s",
                            run.getActualRunId(),
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
        if (!run.isPrivileged() && nodeInfo.getNodeStatus() != NodeStatus.Reserved) {
            var params = new String[]{nodeInfo.getNode().getNodeName()};
            fsResult.postMessage(FacStatusCode.UnitIsNotReserved, params);
            fsResult.mergeStatusBits(0_600000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        // Check the node assignment for the device - if it is already assigned to us, then fail.
        var dni = (DeviceNodeInfo) nodeInfo;
        if (Objects.equals(dni.getAssignedTo(), run)) {
            var params = new String[]{node.getNodeName()};
            fsResult.postMessage(FacStatusCode.DeviceAlreadyInUseByThisRun, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        // Check requested pack name - if it is already assigned to this run, reject the request
        //   TODO E:201733 Pack pack-id already in use by this run.

        // If we are not the Exec make sure the pack is removable and that the device is not fixed
        if (!run.isPrivileged()) {
            //   TODO E:202233 Pack pack-id is not a removable pack.
            //   TODO E:200433 Device device-Name is fixed.
        }

        // If there is a facilities item in the rce which matches the file specification which does not refer
        // to an absolute assign of this same unit, fail.
        // TODO what should we do about NameItem fac items?
        var fiTable = run.getFacilitiesItemTable();
        var existingFacItem = fiTable.getExactFacilitiesItem(fileSpecification);
        if (existingFacItem != null) {
            fsResult.postMessage(FacStatusCode.IllegalAttemptToChangeAssignmentType);
            fsResult.mergeStatusBits(0_400000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
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

        fiTable.addFacilitiesItem(facItem);

        // Wait for the unit if necessary...
        var startTime = Instant.now();
        var nextMessageTime = startTime.plusSeconds(120);
        while (true) {
            if (!Exec.getInstance().isRunning()) {
                throw new ExecStoppedException();
            }
            run.incrementWaitingForPeripheral();
            synchronized (nodeInfo) {
                if (nodeInfo.getAssignedTo() == null) {
                    nodeInfo.setAssignedTo(run);
                    facItem.setIsAssigned(true);
                    run.decrementWaitingForPeripheral();
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
                if (!run.hasTask()
                    && ((run.getRunType() == RunType.Batch) || (run.getRunType() == RunType.Demand))) {
                    long minutes = Duration.between(now, startTime).getSeconds() / 60;
                    var params = new Object[]{run.getActualRunId(), minutes};
                    var facMsg = new FacStatusMessageInstance(FacStatusCode.RunHeldForDiskUnitAvailability, params);
                    run.postToPrint(facMsg.toString(), 1);
                }
            }
        }

        if (!facItem.isAssigned()) {
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun promptLoadPack returns false");
            // z-option bail-out
            fiTable.removeFacilitiesItem(facItem);
            fsResult.postMessage(FacStatusCode.HoldForDiskUnitRejected);
            fsResult.mergeStatusBits(0_400001_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        if (!promptLoadPack(run, nodeInfo, disk, packName)) {
            LogManager.logTrace(LOG_SOURCE, "assignDiskUnitToRun promptLoadPack returns false");
            fiTable.removeFacilitiesItem(facItem);
            nodeInfo.setAssignedTo(null);
            var params = new String[]{packName};
            fsResult.postMessage(FacStatusCode.OperatorDoesNotAllowAbsoluteAssign, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        fsResult.log(Trace, LOG_SOURCE);
        return true;
    }

    /**
     * For assigning a reserved disk to a run.
     * This assignment can only be temporary, and fileSpecification must be fully resolved.
     * @param fileSpecification needed for creating facilities item
     * @param nodeIdentifier node identifier of the device
     * @param packName only for removable disk unit, null for fixed
     * @param fsResult fac status result
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public synchronized boolean assignFixedDiskUnitToExec(
        final FileSpecification fileSpecification,
        final int nodeIdentifier,
        final String packName,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "assignFixedDiskUnitToExec %s node:%d",
                            fileSpecification.toString(),
                            nodeIdentifier);

        var nodeInfo = _nodeGraph.get(nodeIdentifier);
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE, "assignFixedDiskUnitToRun() Cannot find node %012o", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if ((node.getNodeCategory() != NodeCategory.Device) || ((Device) node).getDeviceType() != DeviceType.DiskDevice) {
            LogManager.logFatal(LOG_SOURCE, "assignFixedDiskUnitToRun() Node %012o is not a disk device", nodeIdentifier);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        // Check the node assignment for the device - if it is already assigned to us, then fail.
        var exec = Exec.getInstance();
        var dni = (DeviceNodeInfo) nodeInfo;
        if (Objects.equals(dni.getAssignedTo(), exec)) {
            var params = new String[]{node.getNodeName()};
            fsResult.postMessage(FacStatusCode.DeviceAlreadyInUseByThisRun, params);
            fsResult.mergeStatusBits(0_400000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        // If there is a facilities item in the rce which matches the file specification which does not refer
        // to an absolute assign of this same unit, fail.
        // TODO what should we do about NameItem fac items?
        var fiTable = exec.getFacilitiesItemTable();
        var existingFacItem = fiTable.getExactFacilitiesItem(fileSpecification);
        if (existingFacItem != null) {
            fsResult.postMessage(FacStatusCode.IllegalAttemptToChangeAssignmentType);
            fsResult.mergeStatusBits(0_400000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        // Create an appropriate facilities item.
        var facItem = new AbsoluteDiskItem(node, packName);
        var optionsWord = T_OPTION | X_OPTION;
        facItem.setQualifier(fileSpecification.getQualifier())
               .setFilename(fileSpecification.getFilename())
               .setOptionsWord(optionsWord)
               .setIsTemporary(true);
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

        fiTable.addFacilitiesItem(facItem);

        fsResult.log(Trace, LOG_SOURCE);
        return true;
    }

    /**
     * Catalogs a disk file - to be used when no fileset exists.
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public synchronized boolean catalogDiskFile(
        final CatalogDiskFileRequest request,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "catalogDiskFile %s type=%s proj=%s acct=%s",
                            request._fileSpecification.toString(), request._mnemonic, request._projectId, request._accountId);

        var mnemonicType = Configuration.getMnemonicType(request._mnemonic);
        if (mnemonicType == null) {
            fsResult.postMessage(FacStatusCode.MnemonicIsNotConfigured, new String[]{ request._mnemonic });
            fsResult.mergeStatusBits(0_600000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        var exec = Exec.getInstance();
        var mm = exec.getMFDManager();
        var plusOne = request._fileSpecification.hasFileCycleSpecification()
                      && request._fileSpecification.getFileCycleSpecification().isRelative()
                      && request._fileSpecification.getFileCycleSpecification().getCycle() == 1;

        var absInfo = getAbsoluteCycleForCatalog(null, request._fileSpecification, fsResult);
        if (!absInfo.isAllowed) {
            return false;
        }

        // Check initial and max granularity
        if (request._initialGranules > request._maximumGranules) {
            fsResult.postMessage(FacStatusCode.MaximumIsLessThanInitialReserve);
            fsResult.mergeStatusBits(0_600000_000000L);
            return false;
        }

        // Ensure pack-ids (if specified) are correct.
        // There should be no duplicates, there should be no more than 510,
        // and they should all be known removable packs.
        if (!checkPackIdsForCatalog(request._packIds, fsResult)) {
            return false;
        }

        var fsInfo = new FileSetInfo().setQualifier(request._fileSpecification.getQualifier())
                                      .setFilename(request._fileSpecification.getFilename())
                                      .setIsGuarded(request._isGuarded)
                                      .setPlusOneExists(plusOne)
                                      .setProjectId(request._projectId)
                                      .setReadKey(request._fileSpecification.getReadKey())
                                      .setWriteKey(request._fileSpecification.getWriteKey())
                                      .setFileType(request._packIds.isEmpty() ? FileType.Fixed : FileType.Removable);
        try {
            mm.createFileSet(fsInfo);
        } catch (FileSetAlreadyExistsException ex) {
            LogManager.logFatal(LOG_SOURCE, "file set %s should not already exist", fsInfo);
            exec.stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        boolean result = catalogDiskFileCycleCommon(fsInfo,
                                                    absInfo.absoluteCycle,
                                                    request._mnemonic,
                                                    mnemonicType,
                                                    request._accountId,
                                                    request._isGuarded,
                                                    request._isPrivate,
                                                    request._isUnloadInhibited,
                                                    request._isReadOnly,
                                                    request._isWriteOnly,
                                                    request._saveOnCheckpoint,
                                                    request._granularity,
                                                    request._initialGranules,
                                                    request._maximumGranules,
                                                    request._packIds,
                                                    false,
                                                    fsResult);

        fsResult.log(Trace, LOG_SOURCE);
        return result;
    }

    /**
     * Catalogs an additional disk file cycle in an existing fileset.
     * It is expected (but I'm not sure that it is required) that the fileset contains at least one cycle.
     * @param run rce for requesting run
     * @param fsResult fac status result
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public synchronized boolean catalogDiskFileCycle(
        final Run run,
        final CatalogDiskFileCycleRequest request,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "catalogDiskFile %s", request._fileSpecification.toString());

        var mnemonicType = Configuration.getMnemonicType(request._mnemonic);
        if (mnemonicType == null) {
            fsResult.postMessage(FacStatusCode.MnemonicIsNotConfigured, new String[]{ request._mnemonic });
            fsResult.mergeStatusBits(0_600000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        // Check read/write keys in fileSpecification against fileSetInfo
        if (!checkKeys(run, request._fileSetInfo, request._fileSpecification, fsResult)) {
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }
        var readInhibit = (fsResult.getStatusWord() & 0_000100_000000L) != 0;
        var writeInhibit = (fsResult.getStatusWord() & 0_000200_000000L) != 0;

        // Cannot create a file cycle if we do not have read and write access
        if (readInhibit || writeInhibit) {
            fsResult.postMessage(FacStatusCode.CannotCatalogReadWriteInhibited);
            fsResult.mergeStatusBits(0_400000_000000L);
            return false;
        }

        var absInfo = getAbsoluteCycleForCatalog(request._fileSetInfo, request._fileSpecification, fsResult);
        if (!absInfo.isAllowed)
            return false;

        // If we need to drop the oldest cycle, make sure we have access to do so
        if (absInfo.requiresDroppingOldestCycle) {
            // TODO
        }

        // Check initial and max granularity
        if (request._initialGranules > request._maximumGranules) {
            fsResult.postMessage(FacStatusCode.MaximumIsLessThanInitialReserve);
            fsResult.mergeStatusBits(0_600000_000000L);
            return false;
        }

        // Ensure pack-ids are not specified for fixed, and that if specified for removable, they are correct.
        if (!checkPackIdsForCatalog(request._packIds, fsResult)) {
            return false;
        }

        boolean result = catalogDiskFileCycleCommon(request._fileSetInfo,
                                                    absInfo.absoluteCycle,
                                                    request._mnemonic,
                                                    mnemonicType,
                                                    request._accountId,
                                                    request._isGuarded,
                                                    request._isPrivate,
                                                    request._isUnloadInhibited,
                                                    request._isReadOnly,
                                                    request._isWriteOnly,
                                                    request._saveOnCheckpoint,
                                                    request._granularity == null ? Granularity.Track : request._granularity,
                                                    request._initialGranules,
                                                    request._maximumGranules,
                                                    request._packIds,
                                                    absInfo.requiresDroppingOldestCycle,
                                                    fsResult);

        fsResult.log(Trace, LOG_SOURCE);
        return result;
    }

    /**
     * Catalogs a tape file - to be used when no fileset exists.
     * @param fileSpecification needed for creating facilities item - must be fully resolved
     * @param type assign mnemonic to be used
     * @param fsResult fac status result
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public synchronized boolean catalogTapeFile(
        final FileSpecification fileSpecification,
        final String type,
        // TODO TAPE
        // TODO need to create a CatalogTapeFileRequest object
        // lots of tape-related options
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "catalogTapeFile %s",
                            fileSpecification.toString());

        var exec = Exec.getInstance();
        var mType = exec.getConfiguration().getMnemonicType(type);
        if (mType == null) {
            fsResult.postMessage(FacStatusCode.MnemonicIsNotConfigured, new String[]{ type });
            fsResult.mergeStatusBits(0_600000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        fsResult.log(Trace, LOG_SOURCE);
        return true;
    }

    /**
     * Catalogs an additional tape file cycle in an existing fileset.
     * @param fileSpecification needed for creating facilities item - must be fully resolved
     * @param fileSetInfo describes the existing fileset
     * @param type assign mnemonic to be used
     * @param fsResult fac status result
     * @return true if we are successful
     * @throws ExecStoppedException if the exec is stopped
     */
    public synchronized boolean catalogTapeFileCycle(
        final FileSpecification fileSpecification,
        final FileSetInfo fileSetInfo,
        final String type,
        // TODO TAPE
        // TODO need to create a CatalogTapeFileCycleRequest object
        // lots of tape-related options
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "catalogTapeFileCycle %s",
                            fileSpecification.toString());

        var mType = Configuration.getMnemonicType(type);
        if (mType == null) {
            fsResult.postMessage(FacStatusCode.MnemonicIsNotConfigured, new String[]{ type });
            fsResult.mergeStatusBits(0_600000_000000L);
            fsResult.log(Trace, LOG_SOURCE);
            return false;
        }

        fsResult.log(Trace, LOG_SOURCE);
        return true;
    }

    /**
     * Establishes a use item in the given run control entry
     * @param run run control entry of interest
     * @param internalName internal name to be applied
     * @param fileSpecification file specification indicating the external or referenced name - must be fully resolved
     * @param releaseOnTaskEnd true to release this use item at the next task termination
     */
    public void establishUseItem(
        final Run run,
        final String internalName,
        final FileSpecification fileSpecification,
        final boolean releaseOnTaskEnd
    ) {
        LogManager.logTrace(LOG_SOURCE, "establishUseItem %s %s->%s I:%s",
                            run.getActualRunId(),
                            internalName,
                            fileSpecification.toString(),
                            releaseOnTaskEnd);

        var fiTable = run.getFacilitiesItemTable();
        synchronized (fiTable) {
            var facItem = fiTable.findFacilitiesItem(run, fileSpecification);
            if (facItem == null) {
                // create a name item
                facItem = new NameItem().setQualifier(fileSpecification.getQualifier())
                                        .setFilename(fileSpecification.getFilename())
                                        .setIsTemporary(true);
                if (fileSpecification.hasFileCycleSpecification()) {
                    var fcSpec = fileSpecification.getFileCycleSpecification();
                    if (fcSpec.isAbsolute()) {
                        facItem.setAbsoluteCycle(fcSpec.getCycle());
                    } else if (fcSpec.isRelative()) {
                        facItem.setRelativeCycle(fcSpec.getCycle());
                    }
                }

                fiTable.addFacilitiesItem(facItem);
            }

            if (releaseOnTaskEnd) {
                facItem.setReleaseOnTaskEnd(true);
            }

            facItem.setInternalName(internalName);
        }

        LogManager.logTrace(LOG_SOURCE, "establishUseItem %s exit", run.getActualRunId());
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
                var runId = ni.getAssignedTo().getActualRunId();
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
     * Finds the facilities item in the fac item table which best matches the given internal name.
     * @param run run of interest
     * @param internalName name for which we search
     */
    public synchronized DiskFileFacilitiesItem ioGetDiskFileFacilitiesItem(
        final Run run,
        final String internalName
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();

        var facItem = run.getFacilitiesItemTable().getFacilitiesItemByFilenameSearch(run, internalName);
        switch (facItem) {
            case null -> {
                LogManager.logFatal(LOG_SOURCE, "[%s] Cannot find facItem for file %s", run.getActualRunId(), internalName);
                exec.stop(StopCode.InternalExecIOFailed);
                throw new ExecStoppedException();
            }
            case NameItem nameItem -> {
                LogManager.logFatal(LOG_SOURCE, "[%s] File %s is not assigned", run.getActualRunId(), internalName);
                exec.stop(StopCode.InternalExecIOFailed);
                throw new ExecStoppedException();
            }
            case DiskFileFacilitiesItem dfi -> {
                return dfi;
            }
            default -> {
                LogManager.logFatal(LOG_SOURCE, "[%s] File %s is not a disk file", run.getActualRunId(), internalName);
                exec.stop(StopCode.InternalExecIOFailed);
                throw new ExecStoppedException();
            }
        }
    }

    /**
     * Handles mass-storage read.
     * Such operations are file-relative, with sector or word addresses indicating the starting address of the IO,
     * and word counts indicating the size of the IO. Such transfers are not required to be on track or block
     * (or even device) boundaries, although IO is more efficient if they are.
     * @param run the run which is requesting the IO
     * @param internalName the internal name of the file to be read from
     * @param fileRelativeAddress sector or word address of the read operation (depends on the file mode)
     * @param buffer buffer into which data is to be read
     * @param isWordAddressable true if file is word-addressable
     * @param ioResult where we return IO status
     * @throws ExecStoppedException if we stop the exec, or determine that it has been stopped during processing
     */
    public synchronized void ioReadFromDiskFile(
        final Run run,
        final String internalName,
        final long fileRelativeAddress,
        final ArraySlice buffer,
        final boolean isWordAddressable,
        final IOResult ioResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "ioReadFromDiskFile(%s, '%s', addr=%d words=%d wAddr=%s",
                            run.getActualRunId(), internalName, fileRelativeAddress, buffer.getSize(), isWordAddressable);
        var exec = Exec.getInstance();
        var mm = exec.getMFDManager();

        try {
            var dfi = ioGetDiskFileFacilitiesItem(run, internalName);
            var aci = dfi.getAcceleratedCycleInfo();
            var fas = aci.getFileAllocationSet();
            var fci = (DiskFileCycleInfo)aci.getFileCycleInfo();
            var maxTracks = fci.getMaxGranules();
            if (fci.getPCHARFlags().getGranularity() == Granularity.Position) {
                maxTracks <<= 6;
            }

            // maxTracks, if zero, indicates no limit. This is a Komodo peculiarity.
            // Convert file-relative address to word-address, then do the calisthenics necessary to
            // determine whether the IO extend is beyond  max tracks.
            long fileRelWordAddress = fileRelativeAddress * (isWordAddressable ? 1 : 28);
            if (maxTracks > 0) {
                long fileRelWordLimit = fileRelWordAddress + buffer.getSize();
                long trackLimit = fileRelWordLimit / 1792;
                if (fileRelWordLimit % 1792 > 0) {
                    trackLimit += 1;
                }
                if (trackLimit >= maxTracks) {
                    ioResult.setStatus(ERIO$Status.ReadExtentOutOfRange)
                            .setWordsTransferred(0);
                    throw new IOException();
                }
            }

            int wordsRead = 0;
            int wordsRemaining = buffer.getSize();
            var channelPacket = new ChannelIoPacket().setFormat(TransferFormat.Packed)
                                                     .setIoFunction(IoFunction.Read);

            while (wordsRemaining > 0) {
                // Find the LDAT and hardware track containing the file-relative track(s) we're trying to read.
                var relativeTrack = fileRelWordAddress / 1792;
                var hwTid = fas.resolveFileRelativeTrackId(relativeTrack);
                if (hwTid == null) {
                    ioResult.setStatus(ERIO$Status.UnallocatedArea)
                            .setWordsTransferred(wordsRead);
                    throw new Exception();
                }

                var nodeInfo = mm.getNodeInfoForLDAT(hwTid.getLDATIndex());
                var nodeIdentifier = nodeInfo.getNode().getNodeIdentifier();

                // Calculate how far our file-rel word addr is from the start of the containing track,
                // then determine how many non-slop words we can read from this track.
                // Adjust that value downwards if necessary, in case the amount we have left to read is less.
                var leadingSlop = (int)(fileRelWordAddress % 1792);
                var subWordCount = Math.min(1792 - leadingSlop, wordsRemaining);

                // Set up an IO to read this portion of the area
                var subSlice = new ArraySlice(buffer, wordsRead, subWordCount);
                var deviceRelWordAddress = (hwTid.getTrackId() * 1792) + leadingSlop;
                channelPacket.setBuffer(subSlice)
                             .setNodeIdentifier(nodeIdentifier)
                             .setDeviceWordAddress(deviceRelWordAddress);

                try {
                    routeIo(channelPacket);
                } catch (NoRouteForIOException ex) {
                    ioResult.setStatus(ERIO$Status.DeviceDownOrNotAvailable)
                            .setWordsTransferred(wordsRead);
                    throw new IOException();
                }

                if (channelPacket.getIoStatus() != IoStatus.Successful) {
                    ioResult.setStatus(_ioStatusTranslateTable.get(channelPacket.getIoStatus()))
                            .setWordsTransferred(wordsRead);
                    throw new IOException();
                }

                wordsRead += subWordCount;
                wordsRemaining -= subWordCount;
            }

            ioResult.setStatus(ERIO$Status.Success)
                    .setWordsTransferred(wordsRead);
        } catch (Exception e) {
            // do nothing
        } catch (Throwable t) {
            LogManager.logCatching(LOG_SOURCE, t);
            exec.stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        LogManager.logTrace(LOG_SOURCE, "ioReadFromDiskFile returning %s", ioResult);
    }

    /**
     * Handles mass-storage write
     * Such operations are file-relative, with sector or word addresses indicating the starting address of the IO,
     * and word counts indicating the size of the IO. Such transfers are not required to be on track or block
     * (or even device) boundaries, although IO is more efficient if they are.
     * @param run the run which is requesting the IO
     * @param internalName the internal name of the file to be written to
     * @param fileRelativeAddress sector or word address of the read operation (depends on the file mode)
     * @param buffer buffer containing the data to be written
     * @param isWordAddressable true if file is word-addressable
     * @param ioResult where we return IO status
     * @throws ExecStoppedException if we stop the exec, or determine that it has been stopped during processing
     */
    public synchronized void ioWriteToDiskFile(
        final Run run,
        final String internalName,
        final long fileRelativeAddress,
        final ArraySlice buffer,
        final boolean isWordAddressable,
        final IOResult ioResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "ioWriteToDiskFile(%s, '%s', addr=%d words=%d wAddr=%s",
                            run.getActualRunId(), internalName, fileRelativeAddress, buffer.getSize(), isWordAddressable);
        var exec = Exec.getInstance();
        var mm = exec.getMFDManager();

        try {
            var dfi = ioGetDiskFileFacilitiesItem(run, internalName);
            if (!dfi.isWriteable()) {
                ioResult.setStatus(ERIO$Status.WriteInhibited).setWordsTransferred(0);
                return;
            }

            var aci = dfi.getAcceleratedCycleInfo();
            var fas = aci.getFileAllocationSet();
            var fci = (DiskFileCycleInfo)aci.getFileCycleInfo();
            var maxTracks = fci.getMaxGranules();
            if (fci.getPCHARFlags().getGranularity() == Granularity.Position) {
                maxTracks <<= 6;
            }

            // maxTracks, if zero, indicates no limit. This is a Komodo peculiarity.
            // Convert file-relative address to word-address, then do the calisthenics necessary to
            // determine whether the IO extend is beyond  max tracks.
            long fileRelWordAddress = fileRelativeAddress * (isWordAddressable ? 1 : 28);
            if (maxTracks > 0) {
                long fileRelWordLimit = fileRelWordAddress + buffer.getSize();
                long trackLimit = fileRelWordLimit / 1792;
                if (fileRelWordLimit % 1792 > 0) {
                    trackLimit += 1;
                }
                if (trackLimit >= maxTracks) {
                    ioResult.setStatus(ERIO$Status.WriteExtentOutOfRange)
                            .setWordsTransferred(0);
                    throw new IOException();
                }
            }

            int wordsWritten = 0;
            int wordsRemaining = buffer.getSize();
            var channelPacket = new ChannelIoPacket().setFormat(TransferFormat.Packed)
                                                     .setIoFunction(IoFunction.Write);

            while (wordsRemaining > 0) {
                // Find the LDAT and hardware track containing the file-relative track(s) we're trying to write.
                var relativeTrack = fileRelWordAddress / 1792;
                var hwTid = fas.resolveFileRelativeTrackId(relativeTrack);
                if (hwTid == null) {
                    // We need to allocate a track for this IO, and possibly zero it out
                    if (!mm.allocateDataExtent(fas, relativeTrack, 1)) {
                        ioResult.setStatus(ERIO$Status.CannotExpandFile).setWordsTransferred(wordsWritten);
                        throw new IOException();
                    }

                    hwTid = fas.resolveFileRelativeTrackId(relativeTrack);
                }

                var nodeInfo = mm.getNodeInfoForLDAT(hwTid.getLDATIndex());
                var nodeIdentifier = nodeInfo.getNode().getNodeIdentifier();

                // Calculate how far our file-rel word addr is from the start of the containing track,
                // then determine how many non-slop words we can read from this track.
                // Adjust that value downwards if necessary, in case the amount we have left to read is less.
                var leadingSlop = (int)(fileRelWordAddress % 1792);
                var subWordCount = Math.min(1792 - leadingSlop, wordsRemaining);

                // Set up an IO to read this portion of the area
                var subSlice = new ArraySlice(buffer, wordsWritten, subWordCount);
                var deviceRelWordAddress = (hwTid.getTrackId() * 1792) + leadingSlop;
                channelPacket.setBuffer(subSlice)
                             .setNodeIdentifier(nodeIdentifier)
                             .setDeviceWordAddress(deviceRelWordAddress);

                try {
                    routeIo(channelPacket);
                } catch (NoRouteForIOException ex) {
                    ioResult.setStatus(ERIO$Status.DeviceDownOrNotAvailable)
                            .setWordsTransferred(wordsWritten);
                    throw new IOException();
                }

                if (channelPacket.getIoStatus() != IoStatus.Successful) {
                    ioResult.setStatus(_ioStatusTranslateTable.get(channelPacket.getIoStatus()))
                            .setWordsTransferred(wordsWritten);
                    throw new IOException();
                }

                wordsWritten += subWordCount;
                wordsRemaining -= subWordCount;
            }

            if (fas.isUpdated()) {
                mm.persistFileCycleInfo(fci);
            }

            ioResult.setStatus(ERIO$Status.Success)
                    .setWordsTransferred(wordsWritten);
        } catch (Exception e) {
            // do nothing
        } catch (Throwable t) {
            LogManager.logCatching(LOG_SOURCE, t);
            exec.stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        LogManager.logTrace(LOG_SOURCE, "ioWriteToDiskFile returning %s", ioResult);
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
     * Releases a facilities item or use items (or some combination thereof).
     * Specification of semantically contradictory options produces undefined behavior... e.g.:
     *      ReleaseBehavior.ReleaseUseItemOnly with deleteFileCycle, inhibitCatalog, releaseExclusiveUseOnly
     *      deleteFileCycle or inhibitCatalog with releaseExclusiveUseOnly
     * @param run indicates which run is requesting this release
     * @param fileSpecification File specification of interest - if it is an internal name, it may refer to a use item.
     *                          Unliked most other APIs here, this does not need to be fully resolved, indeed it must not
     *                          be, in order for the various optional parameters to be properly effective.
     * @param behavior indicates whether the file, or a use item, or all use items, are to be released
     * @param deleteFileCycle forces deletion of the referenced file cycle (even if assigned with C/U option)
     * @param inhibitCatalog forces deletion of the referenced file cycle ONLY if it was assigned with C/U option
     * @param releaseExclusiveUseOnly releases exclusive use of the file, but nothing else
     * @param retainPhysicalTapeUnits releases the file, but retains assignment of the physical tape unit(s)
     * @param fsResult where we post facilities status messages
     * @return true if we are successful, else false
     */
    public boolean releaseFile(
        final Run run,
        final FileSpecification fileSpecification,
        final ReleaseBehavior behavior,
        final boolean deleteFileCycle,
        final boolean inhibitCatalog,
        final boolean releaseExclusiveUseOnly,
        final boolean retainPhysicalTapeUnits,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE,
                            "releaseFile %s %s del=%s inhCat=%s relX=%s",
                            fileSpecification.toString(),
                            behavior,
                            deleteFileCycle,
                            inhibitCatalog,
                            releaseExclusiveUseOnly);

        var exec = Exec.getInstance();
        var mm = exec.getMFDManager();

        // Resolve file spec to a facilities item and whether it is an internal name
        FacilitiesItem facItem = null;
        var fiTable = run.getFacilitiesItemTable();
        if (fileSpecification.couldBeInternalName()) {
            facItem = fiTable.getFacilitiesItemByInternalName(fileSpecification.getFilename());
        }

        boolean isInternal = facItem != null;
        if (!isInternal) {
            facItem = fiTable.getExactFacilitiesItem(fileSpecification);
        }

        // deleteFileCycle is true for @FREE,D
        // facItemDeleteFlag is true if there is a facItem assigned with @ASG,D or @ASG,K
        // deleteFlag is true if any of the above are true
        var facItemDeleteFlag = false;
        if (facItem instanceof DiskFileFacilitiesItem dfi) {
            facItemDeleteFlag = dfi.deleteOnAnyRunTermination() || dfi.deleteOnNormalRunTermination();
        } else if (facItem instanceof TapeFileFacilitiesItem tfi) {
            facItemDeleteFlag = tfi.deleteOnNormalRunTermination() || tfi.deleteOnAnyRunTermination();
        }
        var deleteFlag = deleteFileCycle || facItemDeleteFlag;

        // If there is no facitem, a @FREE without a D option is a warning; with a D option is an error
        if (facItem == null) {
            if (deleteFileCycle) {
                fsResult.postMessage(FacStatusCode.FreeDFileNotAssigned);
                fsResult.mergeStatusBits(0_600000_000000L);
                fsResult.log(Trace, LOG_SOURCE);
                return false;
            } else {
                fsResult.postMessage(FacStatusCode.FilenameNotKnown);
                fsResult.mergeStatusBits(0_100000_000000L);
                fsResult.log(Trace, LOG_SOURCE);
                return true;
            }
        }

        // Check read/write keys if they were provided (if not, we'll do further checks later if needed).
        // TODO

        // There is no check against deleteFileCycle (@FREE,D) for a non-cataloged file.
        // In this case, @FREE,D is silently ignored.
        // There cannot be facItemDeleteFlag (@ASG,D/K) for a non-cataloged file.
        // We *do* need an fcInfo if there *is* a cataloged file if we are deleting the thing...
        // Note that our algorithm properly prefers @FREE,D over @ASG,C/P and will result in the file
        // not being cataloged.
        FileCycleInfo fcInfo = null;
        if (deleteFlag) {
            try {
                fcInfo = mm.getFileCycleInfo(facItem.getQualifier(), facItem.getFilename(), facItem.getAbsoluteCycle());
            } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                // do nothing here = fcInfo is already (and should be) null.
            }
        }

        if (deleteFileCycle && !facItemDeleteFlag && (fcInfo != null)) {
            if (!run.isPrivileged() || fcInfo.getInhibitFlags().isGuarded()) {
                // TODO if current fac item shows file was not assigned with necessary read or write keys,
                //  ensure they were specified on the @FREE - this may require us to store some sort of flag
                //  in the fac item at @asg time.
                //E:246433 Read and/or write keys are needed.

                // If the run's account / project does not match the file and the file is not public, disallow
                if (!checkPrivateAccess(run, fcInfo)) {
                    fsResult.postMessage(FacStatusCode.IncorrectPrivacyKey);
                    fsResult.mergeStatusBits(0_400000_020000L);
                    fsResult.log(Trace, LOG_SOURCE);
                    return false;
                }
            }

            if ((run.getRunType() != RunType.Exec)
                && exec.getFacilitiesItemTable().getExactFacilitiesItem(fileSpecification) != null) {
                fsResult.postMessage(FacStatusCode.FreeNotAllowedFileInUseByExec);
                fsResult.mergeStatusBits(0_600000_000000L);
                fsResult.log(Trace, LOG_SOURCE);
                return false;
            }
        }

        boolean releaseExplicitUseItem = (behavior == ReleaseBehavior.ReleaseUseItemOnly)
            || (behavior == ReleaseBehavior.ReleaseUseItemOnlyUnlessLast);
        boolean releaseFacItem = !releaseExplicitUseItem && !releaseExclusiveUseOnly;
        boolean releaseAllUseItems = behavior != ReleaseBehavior.Normal;

        if (releaseExclusiveUseOnly
            && (facItem instanceof DiskFileFacilitiesItem dfi) && dfi.isExclusive()
            && (fcInfo != null)) {
            // Specifically clear exclusive use in the MFD here, even if we eventually would do so anyway.
            // Because we might *not* eventually do so.
            fcInfo.getInhibitFlags().setIsAssignedExclusively(false);
            dfi.setIsExclusive(false);
            mm.persistFileCycleInfo(fcInfo);
        }

        if (releaseExplicitUseItem) {
            facItem.removeInternalName(fileSpecification.getFilename());
            if ((behavior == ReleaseBehavior.ReleaseUseItemOnlyUnlessLast) && (facItem.getInternalNames().isEmpty())) {
                releaseFacItem = true;
            }
        }

        if (releaseFacItem) {
            if (facItem instanceof NameItem) {
                // A name item is simple - just remove it.
                // Unless, that is, we are supposed to retain the use names, in which case this is a NOP.
                if (releaseAllUseItems) {
                    fiTable.removeFacilitiesItem(facItem);
                }
                fsResult.log(Trace, LOG_SOURCE);
                return true;
            } else if (facItem instanceof AbsoluteDiskItem adi) {
                // TODO release fac item and unit
            } else if (facItem instanceof DiskFileFacilitiesItem dfi) {
                if (deleteFileCycle || dfi.deleteOnAnyRunTermination() || dfi.deleteOnNormalRunTermination()) {
                    try {
                        mm.deleteFileCycle(facItem.getQualifier(), facItem.getFilename(), facItem.getAbsoluteCycle());
                    } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                        // TODO stop the exec
                    }
                }

                // TODO release unused reserve?

                fiTable.removeFacilitiesItem(facItem);
                mm.decelerateFileCycle(dfi.getAcceleratedCycleInfo().getFileCycleInfo());
            } else if (facItem instanceof TapeFileFacilitiesItem tfi) {
                if (deleteFileCycle || tfi.deleteOnAnyRunTermination() || tfi.deleteOnNormalRunTermination()) {
                    try {
                        mm.deleteFileCycle(facItem.getQualifier(), facItem.getFilename(), facItem.getAbsoluteCycle());
                    } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                        // TODO stop the exec
                    }
                }

                fiTable.removeFacilitiesItem(facItem);
                mm.decelerateFileCycle(fcInfo);

                if (!retainPhysicalTapeUnits) {
                    // TODO release tape units
                }
            }
        }

        fsResult.log(Trace, LOG_SOURCE);
        return true;
    }

    /**
     * Routes an IO described by a channel packet
     * For the case where some portion of the Exec needs to do device-specific IO.
     * @param channelPacket describes the IO
     * @return selected Channel
     * @throws ExecStoppedException if the exec stops during this function
     * @throws NoRouteForIOException if the destination device has no available path
     */
    public Channel routeIo(
        final ChannelIoPacket channelPacket
    ) throws ExecStoppedException, NoRouteForIOException {
        var nodeId = channelPacket.getNodeIdentifier();
        var nodeInfo = _nodeGraph.get(nodeId);
        if (nodeInfo == null) {
            LogManager.logFatal(LOG_SOURCE, "Node %d from channel program is not configured", nodeId);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var node = nodeInfo.getNode();
        if (node.getNodeCategory() != NodeCategory.Device) {
            LogManager.logFatal(LOG_SOURCE, "Node %d from channel program is not a device", nodeId);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var channel = selectRoute((Device) node);
        channel.routeIo(channelPacket);
        return channel;
    }

    /**
     * Simple utility function to update the node status in NodeInfo
     * and to emit a console message regarding such.
     * Does NOT make any judgements whether exec should be stopped, whether the status is appropriate, etc.
     * @param nodeId indicates the node to be affected
     * @param status new status value
     * @param consoleId indicates the console to which the message should be sent
     */
    public void setNodeStatus(
        final int nodeId,
        final NodeStatus status,
        final ConsoleId consoleId
    ) throws ExecStoppedException {
        _nodeGraph.get(nodeId).setNodeStatus(status);
        // TODO need to lock out symbiont device
        var nss = getNodeStatusString(nodeId);
        Exec.getInstance().sendExecReadOnlyMessage(nss, consoleId);
    }

    /**
     * Simple utility function to update the node status in NodeInfo
     * and to emit a console message regarding such.
     * Does NOT make any judgements whether exec should be stopped, whether the status is appropriate, etc.
     * @param nodeId indicates the node to be affected
     * @param status new status value
     * @param consoleType indicates the console to which the message should be sent
     */
    public void setNodeStatus(
        final int nodeId,
        final NodeStatus status,
        final ConsoleType consoleType
    ) throws ExecStoppedException {
        _nodeGraph.get(nodeId).setNodeStatus(status);
        // TODO need to lock out symbiont device
        var nss = getNodeStatusString(nodeId);
        Exec.getInstance().sendExecReadOnlyMessage(nss, consoleType);
    }

    /**
     * Invoked by Exec after boot() has been called for all managers.
     */
    public void startup() throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "startup()");

        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();

        dropTapes();
        readDiskLabels();
        var fixedDisks = getAccessibleFixedDisks();
        if (fixedDisks.isEmpty()) {
            exec.sendExecReadOnlyMessage("No Fixed Disk Configured");
            exec.stop(StopCode.InitializationSystemConfigurationError);
            throw new ExecStoppedException();
        }

        var mm = Exec.getInstance().getMFDManager();
        if (Exec.getInstance().isJumpKeySet(13)) {
            mm.initializeMassStorage(fixedDisks);

            // Create all the system files
            exec.sendExecReadOnlyMessage("Creating Account files...");
            exec.catalogDiskFileForExec("SYS$",
                                        "ACCOUNT$R1",
                                        cfg.getStringValue(Tag.ACCTASGMNE),
                                        cfg.getIntegerValue(Tag.ACCTINTRES),
                                        9999);
            exec.catalogDiskFileForExec("SYS$",
                                        "SEC@ACCTINFO",
                                        cfg.getStringValue(Tag.ACCTASGMNE),
                                        cfg.getIntegerValue(Tag.ACCTINTRES),
                                        9999);

            exec.sendExecReadOnlyMessage("Creating ACR file...");
            exec.catalogDiskFileForExec("SYS$",
                                        "SEC@ACR$",
                                        cfg.getStringValue(Tag.SACRDASGMNE),
                                        cfg.getIntegerValue(Tag.SACRDINTRES),
                                        9999);

            exec.sendExecReadOnlyMessage("Creating UserID file...");
            exec.catalogDiskFileForExec("SYS$",
                                        "SEC@USERID$",
                                        cfg.getStringValue(Tag.USERASGMNE),
                                        cfg.getIntegerValue(Tag.USERINTRES),
                                        9999);

            exec.sendExecReadOnlyMessage("Creating privilege file...");
            exec.catalogDiskFileForExec("SYS$",
                                        "DLOC$",
                                        cfg.getStringValue(Tag.DLOCASGMNE),
                                        0,
                                        1);

            exec.sendExecReadOnlyMessage("Creating system library files...");
            exec.catalogDiskFileForExec("SYS$",
                                        "LIB$",
                                        cfg.getStringValue(Tag.LIBASGMNE),
                                        cfg.getIntegerValue(Tag.LIBINTRES),
                                        cfg.getIntegerValue(Tag.LIBMAXSIZ));
            exec.catalogDiskFileForExec("SYS$",
                                        "RUN$",
                                        cfg.getStringValue(Tag.RUNASGMNE),
                                        cfg.getIntegerValue(Tag.RUNINTRES),
                                        cfg.getIntegerValue(Tag.RUNMAXSIZ));
            exec.catalogDiskFileForExec("SYS$",
                                        "RLIB$",
                                        cfg.getStringValue(Tag.MDFALT),
                                        1,
                                        cfg.getIntegerValue(Tag.MAXGRN));

            // Assign the files to the exec (most of them)
            var filenames = new String[]{ "ACCOUNT$R1", "SEC@ACCTINFO", "SEC@ACR$", "SEC@USERID$", "LIB$", "RUN$", "RLIB$" };
            var fm = exec.getFacilitiesManager();
            for (var filename : filenames) {
                var fsResult = new FacStatusResult();
                var fs = new FileSpecification("MFD$", filename);
                fm.assignCatalogedDiskFileToExec(fs, true, fsResult);
                if (fsResult.hasErrorMessages()) {
                    // TODO
                }
            }

            // Load the library files from tape
            //  TODO
        } else {
            mm.recoverMassStorage(fixedDisks);
        }

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    // -------------------------------------------------------------------------
    // Core
    // -------------------------------------------------------------------------

    /**
     * Private version which may be called by either catalogDiskFile() or catalogDiskFileCycle()
     * Mostly exists so that we don't have to do preliminary checking in catalogDiskFileCycle()
     * if it were to be called by catalogDiskFile().
     * The given fileset might not have a cycle (it might have just been created).
     * Do all sanity checks *before* invoking this.
     * @param fileSetInfo describes the fileset into which the cycle is to be cataloged
     */
    private boolean catalogDiskFileCycleCommon(
        final FileSetInfo fileSetInfo,
        final int absoluteCycle,
        final String type,
        final MnemonicType mnemonicType,
        final String accountId,
        final boolean isGuarded,
        final boolean isPrivate,
        final boolean isUnloadInhibited,
        final boolean isReadOnly,
        final boolean isWriteOnly,
        final boolean saveOnCheckpoint,
        final Granularity granularity,
        final long initialGranules,
        final long maxGranules,
        final List<String> packIds, // only for removable, can be empty if fileSetInfo has a cycle
        final boolean dropOldestCycle,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();

        DiskFileCycleInfo fcInfo;
        if (fileSetInfo.getFileType() == FileType.Fixed) {
            fcInfo = new FixedDiskFileCycleInfo().setUnitSelectionIndicators(new UnitSelectionIndicators());
        } else if (fileSetInfo.getFileType() == FileType.Removable) {
            fcInfo = new RemovableDiskFileCycleInfo().setReadKey(fileSetInfo.getReadKey())
                                                     .setWriteKey(fileSetInfo.getWriteKey());
        } else {
            // we should never get here if we're not fixed or removable disk.
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        var pcf = new PCHARFlags().setGranularity(granularity)
                                  .setIsWordAddressable(mnemonicType == MnemonicType.WORD_ADDRESSABLE_DISK);

        var inh = new InhibitFlags().setIsGuarded(isGuarded)
                                    .setIsPrivate(isPrivate)
                                    .setIsUnloadInhibited(isUnloadInhibited)
                                    .setIsReadOnly(isReadOnly)
                                    .setIsWriteOnly(isWriteOnly);

        var desc = new DescriptorFlags().setSaveOnCheckPoint(saveOnCheckpoint)
                                        .setIsRemovableDiskFile(fileSetInfo.getFileType() == FileType.Removable)
                                        .setIsTapeFile(fileSetInfo.getFileType() == FileType.Tape);

        fcInfo.setFileFlags(new FileFlags())
              .setPCHARFlags(pcf)
              .setInitialGranulesReserved(initialGranules)
              .setMaxGranules(maxGranules)
              .setQualifier(fileSetInfo.getQualifier())
              .setFilename(fileSetInfo.getFilename())
              .setProjectId(fileSetInfo.getProjectId())
              .setAccountId(accountId)
              .setAssignMnemonic(type)
              .setAbsoluteCycle(absoluteCycle)
              .setInhibitFlags(inh)
              .setDescriptorFlags(desc)
              .setDisableFlags(new DisableFlags());

        var mm = exec.getMFDManager();

        // If this cycle is guarded, ensure the fileset is also guarded (it might not be).
        if (isGuarded && !fileSetInfo.isGuarded()) {
            fileSetInfo.setIsGuarded(true);
        }

        // Do we need to drop the oldest cycle? If so, do it now.
        if (dropOldestCycle) {
            try {
                mm.deleteFileCycle(fileSetInfo.getQualifier(), fileSetInfo.getFilename(), fcInfo.getAbsoluteCycle());
            } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
                LogManager.logCatching(LOG_SOURCE, ex);
                exec.stop(StopCode.FacilitiesComplex);
                throw new ExecStoppedException();
            }
        }

        // Create the file cycle
        try {
            mm.createFileCycle(fileSetInfo, fcInfo);
        } catch (AbsoluteCycleConflictException | AbsoluteCycleOutOfRangeException ex) {
            LogManager.logCatching(LOG_SOURCE, ex);
            exec.stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        // If this is removable, we need to create main item(s) on the various removable packs,
        // then update the disk pack entry list in the real main item.
        if (fcInfo instanceof RemovableDiskFileCycleInfo rmFci) {
            // TODO REMOVABLE
        }

        return true;
    }

    /**
     * Checks the file cycle info to see whether the corresponding file is disabled,
     * and sets various fac status entries according to this, and to the two mitigating settings
     * of directoryOnlyBehavior and assignIfDisabled.
     */
    private boolean checkDisabled(
        final FileCycleInfo fcInfo,
        final DirectoryOnlyBehavior directoryOnlyBehavior,
        final boolean assignIfDisabled,
        final FacStatusResult fsResult
    ) {
        var error = false;
        var df = fcInfo.getDisableFlags();
        if (df.isDisabled()) {
            if (df.assignedAndWrittenAtExecStop()) {
                fsResult.postMessage(FacStatusCode.FileAssignedDuringSystemFailure);
                fsResult.mergeStatusBits(0_000000_000200L);
                error = true;
            }

            if (df.cacheDrainFailure()
                && !assignIfDisabled
                && (directoryOnlyBehavior == DirectoryOnlyBehavior.None)) {
                fsResult.postMessage(FacStatusCode.DisabledForCacheDrainFailure);
                fsResult.mergeStatusBits(0_600000_000000L);
                error = true;
            }

            if (df.directoryError()
                && !assignIfDisabled
                && (directoryOnlyBehavior == DirectoryOnlyBehavior.None)) {
                fsResult.postMessage(FacStatusCode.DisabledCorruptedDirectory);
                fsResult.mergeStatusBits(0_600000_000400L);
                error = true;
            }

            if (df.inaccessibleBackup()) {
                if (assignIfDisabled || (directoryOnlyBehavior != DirectoryOnlyBehavior.None)) {
                    fsResult.postMessage(FacStatusCode.FileUnloaded);
                    fsResult.mergeStatusBits(0_000000_000100L);
                } else {
                    fsResult.postMessage(FacStatusCode.FileBackupNotAvailable);
                    fsResult.mergeStatusBits(0_400000_000100L);
                    error = true;
                }
            }
        }

        return !error;
    }

    /**
     * Checks the read/write keys provided (or not) in a FileSpecification object
     * against the keys which do (or do not) exist in the FileSetInfo object.
     * Posts any fac results necessary, aborts the run if necessary, etc.
     * Caller should check bits 10 and 11 of the result code to determine whether the file should be
     * assigned write-inhibited and/or read-inhibited, respectively.
     * @return true if no problems exist, else false
     */
    private boolean checkKeys(
        final Run rce,
        final FileSetInfo fsInfo,
        final FileSpecification fileSpec,
        final FacStatusResult fsResult
    ) throws ExecStoppedException {
        try {
            var err = false;
            var existingReadKey = fsInfo.getReadKey();
            var hasReadKey = (existingReadKey != null) && (!existingReadKey.isEmpty());
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
            var hasWriteKey = (existingWriteKey != null) && (!existingWriteKey.isEmpty());
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
        } catch (Throwable t) {
            LogManager.logCatching(LOG_SOURCE, t);
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }
    }

    /**
     * Checks a list of pack-ids for validity against a particular fileset and file-cycle.
     * Do not invoke with an empty pack list.
     * Rules:
     *  All cycles for a given file set must have the same pack list
     *  Max number of pack-ids is 510
     *  If a file cycle is currently unassigned, we can add one or more pack-ids to the *END* of the pack list
     *    *IF* the entire list is specified, with the new pack(s) at the end of the list
     *  Packs must only be added with the A option but *NOT* with the Y option.
     *  Packs cannot be added if the fileset has more than one cycle, or if the cycle is currently assigned.
     *  Adding packs requires delete access (for fundamental security, cycle must not be write-inhibited)
     * @param optionWord options specified on @ASG
     * @param fcInfo file cycle info for removable file cycle being assigned
     * @param packIds pack ids from caller
     * @param fsResult where we post fac status messages and codes
     * @return true if the check passes, else false
     */
    private boolean checkPackIdsForAssign(
        final FileSetInfo fsInfo,
        final RemovableDiskFileCycleInfo fcInfo,
        final long optionWord,
        final List<String> packIds,
        final FacStatusResult fsResult
    ) {
        if (packIds.size() > 510) {
            fsResult.postMessage(FacStatusCode.MaximumNumberOfPackIdsExceeded);
            fsResult.mergeStatusBits(0_400000_000000L);
            return false;
        }

        LinkedList<String> existingPackIds = fcInfo.getDiskPackEntries()
                                                   .stream()
                                                   .map(DiskPackEntry::getPackName)
                                                   .collect(Collectors.toCollection(LinkedList::new));

        // If the given list is smaller than the existing set of pack-ids, this is an immediate error
        if (packIds.size() < existingPackIds.size()) {
            fsResult.postMessage(FacStatusCode.NumberOfPackIdsNotEqualToMFD);
            fsResult.mergeStatusBits(0_400000_000000L);
            return false;
        }

        // If the given list is the same size or larger than the existing set, then the first {n}
        // given pack-ids must match the first {n} existing pack-ids
        for (int px = 0; px < existingPackIds.size(); px++) {
            if (!packIds.get(px).equals(existingPackIds.get(px))) {
                fsResult.postMessage(FacStatusCode.PackIdsNotEqualToFile);
                fsResult.mergeStatusBits(0_400000_000000L);
                return false;
            }
        }

        // Are there additional pack-ids?
        if (packIds.size() > existingPackIds.size()) {
            if ((optionWord & A_OPTION) == 0) {
                fsResult.postMessage(FacStatusCode.PacksCanOnlyBeAddedWithAOption);
                fsResult.mergeStatusBits(0_400000_000000L);
                return false;
            }

            if ((optionWord & Y_OPTION) != 0) {
                fsResult.postMessage(FacStatusCode.PacksCanNotBeAddedWithYOption);
                fsResult.mergeStatusBits(0_400000_000000L);
                return false;
            }

            if (fcInfo.getCurrentAssignCount() > 0) {
                fsResult.postMessage(FacStatusCode.PacksCanNotBeAddedIfAssigned);
                fsResult.mergeStatusBits(0_400000_000000L);
                return false;
            }

            if (fsInfo.getCycleCount() > 1) {
                fsResult.postMessage(FacStatusCode.PacksCanOnlyBeAddedWithSingleCycle);
                fsResult.mergeStatusBits(0_400000_000000L);
                return false;
            }

            // Any subsequent pack-ids in the given list must refer to removable packs
            for (int px = existingPackIds.size(); px < packIds.size(); px++) {
                // TODO REMOVABLE
                //E:202233 Pack pack-id is not a removable pack.
            }
        }

        return true;
    }

    /**
     * A smaller subset of pack-id checking...
     * ensure there aren't too many pack ids, and that there are no duplicates.
     * @param packIds pack ids from caller
     * @param fsResult where we post fac status messages and codes
     * @return true if the check passes, else false
     */
    private boolean checkPackIdsForCatalog(
        final List<String> packIds,
        final FacStatusResult fsResult
    ) {
        if (!packIds.isEmpty()) {
            for (int px = 0; px < packIds.size(); px++) {
                for (int py = px + 1; py < packIds.size(); py++) {
                    if (packIds.get(px).equalsIgnoreCase(packIds.get(py))) {
                        fsResult.postMessage(FacStatusCode.DuplicateMediaIdsAreNotAllowed);
                        fsResult.mergeStatusBits(0_400004_000000L);
                        return false;
                    }
                }
            }
            if (packIds.size() > 510) {
                fsResult.postMessage(FacStatusCode.MaximumNumberOfPackIdsExceeded);
                fsResult.mergeStatusBits(0_600000_000000L);
                return false;
            }
        }

        return true;
    }

    /**
     * Checks the placement string provided on the control statement for validity,
     * and returns a corresponding PlacementInfo object.
     * Do not invoke if placement string is null.
     * @param placement placement string
     * @param fsResult where we put facility status information if necessary
     * @return PlacementInfo object if placement is valid, null if it is not.
     */
    private PlacementInfo checkPlacement(
        final String placement,
        final FacStatusResult fsResult
    ) {
        if (placement.startsWith("*")) {
            // absolute placement, either by channel or by device
            var unitName = placement.substring(1).toUpperCase();
            var ni = getNodeInfoByName(unitName);
            if (ni == null) {
                fsResult.postMessage(FacStatusCode.UnitNameIsNotConfigured, new String[]{ unitName });
                fsResult.mergeStatusBits(0_400000_000000L);
                return null;
            }

            var node = ni.getNode();
            if (node.getNodeCategory() == NodeCategory.Channel) {
                var chan = (Channel) node;
                // TODO make sure this is a disk channel (not sure what message to post, if not)

                var cni = (ChannelNodeInfo) ni;
                if (cni.getNodeStatus() != NodeStatus.Up) {
                    fsResult.postMessage(FacStatusCode.DeviceIsNotUp, new String[]{ unitName });
                    fsResult.mergeStatusBits(0_400000_000000L);
                    return null;
                }

                // TODO make sure there is at least one unit available (not sure what message to post if not)

                return new PlacementInfo(PlacementType.AbsoluteByChannel, node.getNodeIdentifier());
            } else if (node.getNodeCategory() == NodeCategory.Device) {
                var dev = (Device) node;
                if (dev.getDeviceType() != DeviceType.DiskDevice) {
                    fsResult.postMessage(FacStatusCode.PlacementOnNonMassStorageDevice);
                    fsResult.mergeStatusBits(0_400000_000000L);
                    return null;
                }

                var dni = (DeviceNodeInfo) ni;
                if (ni.getNodeStatus() != NodeStatus.Up) {
                    fsResult.postMessage(FacStatusCode.DeviceIsNotUp, new String[]{ unitName });
                    fsResult.mergeStatusBits(0_400000_000000L);
                    return null;
                }

                var pi = (PackInfo)dni._mediaInfo;
                if (!pi.isFixed()) {
                    fsResult.postMessage(FacStatusCode.PlacementNotFixedMassStorage, new String[]{ unitName });
                    fsResult.mergeStatusBits(0_400000_000000L);
                    return null;
                }

                return new PlacementInfo(PlacementType.AbsoluteByDevice, node.getNodeIdentifier());
            } else {
                // not sure that this is the right message, but it seems the closest to what we want
                fsResult.postMessage(FacStatusCode.InvalidDeviceControlUnitName, new String[] { unitName });
                fsResult.mergeStatusBits(0_400000_000000L);
                return null;
            }
        } else {
            // logical placement by channel, and maybe by device
            // find the nth channel, so indicated by 'A' == 0, 'B' == 1, etc
            char thisChar = 'A';
            char reqChar = placement.charAt(0);
            NodeInfo selectedChannelNodeInfo = null;
            for (var entry : _nodeGraph.entrySet()) {
                // ignore non-channels
                var ni = entry.getValue();
                if (ni.getNode().getNodeCategory() == NodeCategory.Channel) {
                    if (thisChar == reqChar) {
                        selectedChannelNodeInfo = entry.getValue();
                        break;
                    }
                    thisChar++;
                }
            }

            if (selectedChannelNodeInfo == null) {
                // No such, no such TODO post error
                return null;
            }

            if (placement.length() == 1) {
                // just use the channel node
                return new PlacementInfo(PlacementType.LogicalByChannel,
                                         selectedChannelNodeInfo.getNode().getNodeIdentifier());
            }

            // Find all the available devices on the selected channel, then use the nth one.
            var chan = (Channel) selectedChannelNodeInfo.getNode();
            var devs = new LinkedList<>(chan.getDevices());
            int reqDeviceIndex = Integer.parseInt(placement.substring(1));
            if (reqDeviceIndex >= devs.size()) {
                // no such - TODO post error
                return null;
            }

            return new PlacementInfo(PlacementType.LogicalByDevice, devs.get(reqDeviceIndex).getNodeIdentifier());
        }
    }

    /**
     * If the indicated file cycle is private, check the options and rce to see if the caller
     * is allowed to access the file.
     * @return true if access is allowed, else false.
     */
    private boolean checkPrivateAccess(
        final Run rce,
        final FileCycleInfo fcInfo
    ) {
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();

        // if file is not private, we're good
        if (!fcInfo.getInhibitFlags().isPrivate()) {
            return true;
        }

        // if account/project matches, we're good
        if (cfg.getBooleanValue(Tag.SSPBP)) {
            return rce.getAccountId().equals(fcInfo.getAccountId());
        } else {
            return rce.getProjectId().equals(fcInfo.getProjectId());
        }
    }

    /**
     * Generic IO loop for all device IO
     * @param run requesting run
     * @param facilitiesItem the facilities item describing the file for which we are doing IO
     * @param channelPacket channel program to be executed - status information is updated in this packet.
     * @throws ExecStoppedException if something goes badly
     */
    private void ioLoop(
        final Run run,
        final FacilitiesItem facilitiesItem,
        final ChannelIoPacket channelPacket
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();

        // Loop over trying and maybe retrying the IO.
        // (This is a hardware level IO).
        while (true) {
            Channel channel;
            try {
                channel = routeIo(channelPacket);
            } catch (NoRouteForIOException ex) {
                LogManager.logCatching(LOG_SOURCE, ex);
                exec.stop(StopCode.InternalExecIOFailed);
                throw new ExecStoppedException();
            }

            String errorStr = "";
            String[] responses = null;
            String responseStr = "";
            switch (channelPacket.getIoStatus()) {
                case NotStarted -> { /* cannot be here */ }
                case Successful, AtLoadPoint, DeviceDoesNotExist, EndOfFile, EndOfTape, InvalidBlockCount,
                     InvalidBlockId, InvalidBufferSize, InvalidFunction, InvalidNodeType, InvalidPacket, InvalidPackName,
                     InvalidPrepFactor, InvalidTapeBlock, InvalidTrackCount, LostPosition, MediaAlreadyMounted, NonIntegralRead,
                     PackNotPrepped, ReadNotAllowed, ReadOverrun -> { return; }
                case Canceled -> throw new ExecStoppedException();
                // TODO - finish these cases
                case DataException -> {throw new RuntimeException("Unimplemented");}
                case DeviceIsDown -> {throw new RuntimeException("Unimplemented");}
                case DeviceIsNotAccessible -> {throw new RuntimeException("Unimplemented");}
                case DeviceIsNotAttached -> {throw new RuntimeException("Unimplemented");}
                case DeviceIsNotReady -> {throw new RuntimeException("Unimplemented");}
                case InternalError -> {throw new RuntimeException("Unimplemented");}
                case MediaNotMounted -> {throw new RuntimeException("Unimplemented");}
                case SystemError -> {
                    errorStr = "DEVERR";
                    responses = ABGM_RESPONSES;
                    responseStr = ABGM_RESPONSE_STR;
                }
                case WriteProtected -> {
                    errorStr = "R-ONLY";
                    responses = AGM_RESPONSES;
                    responseStr = AGM_RESPONSE_STR;
                }
            }

            boolean redisplay = true;
            while (redisplay) {
                redisplay = false;
                var deviceName = _nodeGraph.get(channelPacket.getNodeIdentifier()).getNode().getNodeName();
                var msg = String.format("%s %s %s %s %s %s",
                                        deviceName,
                                        channel.getNodeName(),
                                        errorStr,
                                        channelPacket.getIoFunction().toString(),
                                        run.getActualRunId(),
                                        responseStr);
                var response = exec.sendExecRestrictedReadReplyMessage(msg, responses, ConsoleType.InputOutput);
                switch (response) {
                    case "A":
                        // retry the operation
                        break;
                    case "B":
                        // unrecoverable error - pass error back to caller
                        // normally for disk we would bad-track the pack, but we don't do bad-tracking here, so...
                        return;
                    case "G":
                        // unrecoverable error - pass error back to caller
                        return;
                    case "M":
                        // display more information (if available) and ask again

                        // subfunction function line (We don't have one of these at the moment)
                        // The subfunction line for the M response is an optional line that is not dependent on the device type.
                        // This line appears for DIR$ functions only, such as READ VOL1, READ SECTOR1, and WRITE SECTOR1.
                        // Format:
                        //      targ-dev DEVICE = device SUBFUNCTION = subfunction where:
                        // targ-dev:    is the name of the target device that had the error.
                        // device:      is the name of the device that was used to issue the I/O that resulted in the error.
                        // subfunction: is one of the list DIR$ subfunctions: RDVOL1, RDSEC1, DEVCAP, WRVOL1, or WRSEC1.

                        // hardware function line for UTIL functions only
                        // Format:
                        //      device DEVICE = ctrl-unit H/W CMD = h/w-func where:
                        // device:      is the name of the device in error.
                        // ctrl-unit:   is the name of the control device in error.
                        //  h/w-func:   is the hardware command that found the error.
                        //      -- word interface hardware function mnemonics UTIL only --
                        //      -- looks like most of these are for cache disk controllers --
                        //      CLRICD  Clear internal cache down indicator
                        //      CLRTME  Clear segment timestamp
                        //      DELSEG  Delete segment
                        //      DRAIN   Drain
                        //      INIT    Initialize
                        //      PARAM   Parameterize
                        //      PRPTRK  Prep Track
                        //      RDFSI   Read file status indicator
                        //      RDHID   Read hardware ID attempt
                        //      RDICD   Read ICD data
                        //      RDICDX  Read ICD dta and microcode trace
                        //      RDMLEV  Read microcode revision level
                        //      RDPAGE  Read mode page ettempt
                        //      RDTABL  Read tables
                        //      -- HIC cartridge tape mnemonics --
                        //      ASSIGN  Assign
                        //      CLRPG   LOG-SENSE / LOG-SELECT could not be sent to the device
                        //      CONACC  Control access
                        //      INQRY   Inquiry
                        //      LOADCR  Load cartridge
                        //      LOADSP  Load display
                        //      MODSNS  MODE-SENSE could not be sent to the device
                        //      PROCLN
                        //      REPDEN
                        //      SPGID   Set path group-id
                        //      UNASGN  Unassign

                        // Run/File line
                        // Format:
                        //      device RUNID = program Q*F = qual*file(cycle)
                        var programName = run.hasTask() ? run.getActiveTask().getProgramName() : "";
                        msg = String.format("%s %s = %s Q*F = %s*%s(%d)",
                                            deviceName, run.getActualRunId(), programName,
                                            facilitiesItem.getQualifier(),
                                            facilitiesItem.getFilename(),
                                            facilitiesItem.getAbsoluteCycle());
                        exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);

                        String additionalStatus = channelPacket.getAdditionalStatus();
                        if (additionalStatus != null) {
                            msg = String.format("%s %s", deviceName, additionalStatus);
                            exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
                        }

                        redisplay = true;
                }
            }
        }
    }

    /**
     * Unloads all the ready tape devices - used during boot.
     * @throws ExecStoppedException if something goes wrong while we're doing this.
     */
    private void dropTapes() throws ExecStoppedException {
        for (var ni : _nodeGraph.values()) {
            if ((ni instanceof DeviceNodeInfo dni) && (ni.getNode() instanceof TapeDevice td) && td.isReady()) {
                var cp = new ChannelIoPacket().setNodeIdentifier(ni.getNode().getNodeIdentifier());
                cp.setIoFunction(IoFunction.RewindAndUnload);
                try {
                    routeIo(cp);
                } catch (NoRouteForIOException ex) {
                    LogManager.logCatching(LOG_SOURCE, ex);
                }
            }
        }
    }

    /**
     * Determines the absolute cycle to be used for cataloging a new file cycle,
     * given the current fileset state and the specified file cycle.
     * We do not hold a run for file cycle conflict; maybe one day we will.
     * @param fileSetInfo fileset info if a fileset exists for the given qualifier/filename, null if it does not.
     * @param fileSpecification file specification provided for cataloging the file
     * @param facStatusResult where we post any necessary facility status messages, usually due to returning 0.
     * @return AbsoluteCycleCatalogResult object
     */
    private AbsoluteCycleCatalogResult getAbsoluteCycleForCatalog(
        final FileSetInfo fileSetInfo,
        final FileSpecification fileSpecification,
        final FacStatusResult facStatusResult
    ) {
        var result = new AbsoluteCycleCatalogResult();

        // If fileSetInfo is null, there is no existing file set.
        // We would allow any valid absolute cycle, or any positive valid relative cycle.
        // If there is no file cycle, assume absolute cycle one.
        if (fileSetInfo == null) {
            if (fileSpecification.hasFileCycleSpecification()) {
                var fcs = fileSpecification.getFileCycleSpecification();
                if (fcs.isAbsolute()) {
                    if (fcs.getCycle() >= 1 && fcs.getCycle() <= 999) {
                        result.isAllowed = true;
                        result.absoluteCycle = fcs.getCycle();
                    } else {
                        facStatusResult.postMessage(FacStatusCode.FileCycleOutOfRange);
                        facStatusResult.mergeStatusBits(0_400000_000040L);
                    }
                } else {
                    if (fcs.getCycle() >= 0) {
                        result.isAllowed = true;
                        result.absoluteCycle = 1;
                    } else {
                        facStatusResult.postMessage(FacStatusCode.FileCycleOutOfRange);
                        facStatusResult.mergeStatusBits(0_400000_000040L);
                    }
                }
            } else {
                result.isAllowed = true;
                result.absoluteCycle = 1;
            }

            return result;
        }

        // There is a fileSetInfo, so we have to do some trick-sy things.
        if (!fileSpecification.hasFileCycleSpecification()) {
            // no cycle specified - file is already cataloged, so we cannot do this.
            facStatusResult.postMessage(FacStatusCode.FileAlreadyCataloged);
            facStatusResult.mergeStatusBits(0_500000_000000L);
            return result;
        }

        var fcs = fileSpecification.getFileCycleSpecification();
        if (fcs.isAbsolute()) {
            // Absolute cycle specified - this is the max trick-sy part.
            // First, simply check whether the absolute cycle already exists. That is easy.
            var cycInfo = fileSetInfo.getCycleInfo();
            for (var fsci : cycInfo) {
                if (fsci.getAbsoluteCycle() == fcs.getCycle()) {
                    facStatusResult.postMessage(FacStatusCode.FileAlreadyCataloged);
                    facStatusResult.mergeStatusBits(0_500000_000000L);
                    return result;
                }
            }

            int highestExisting = cycInfo.getFirst().getAbsoluteCycle();
            int lowestExisting = cycInfo.getLast().getAbsoluteCycle();
            if (lowestExisting > highestExisting) {
                // We are in a split situation - the highest cycle has wrapped around from 999 to 1,
                // but the lowest cycle is still slightly below 999.
                // We are guaranteed that the highest is between 1 and 31, and the lowest is between 969 and 999.
                // If we are higher than the lowest or lower than the highest, we are okay.
                if ((fcs.getCycle() > lowestExisting) || (fcs.getCycle() < highestExisting)) {
                    result.isAllowed = true;
                    result.absoluteCycle = fcs.getCycle();
                    return result;
                }
            } else {
                // We are not split. If the proposed cycle is between the highest and lowest, it is okay.
                if ((fcs.getCycle() > lowestExisting) && (fcs.getCycle() < highestExisting)) {
                    result.isAllowed = true;
                    result.absoluteCycle = fcs.getCycle();
                    return result;
                }
            }

            // It still might be okay...
            // The proposed cycle is outside the current range. Is it too far outside?
            int additional = 0;
            if (fcs.getCycle() - highestExisting < 32) {
                additional = fcs.getCycle() - highestExisting;
            } else if (lowestExisting - fcs.getCycle() < 32) {
                additional = lowestExisting - fcs.getCycle();
            } else {
                // not allowed
                return result;
            }

            int newRange = fileSetInfo.getCurrentCycleRange() + additional;
            if (newRange <= fileSetInfo.getMaxCycleRange()) {
                // We're within the max range, it's okay.
                result.isAllowed = true;
            } else if (newRange == fileSetInfo.getMaxCycleRange() + 1) {
                // We're outside the range, but just by one.
                result.isAllowed = true;
                result.requiresDroppingOldestCycle = true;
            }
            return result;
        }

        // Relative cycle specified.
        // If zero, this is the same as no specification at all. Reject it.
        if (fcs.getCycle() == 0) {
            facStatusResult.postMessage(FacStatusCode.FileAlreadyCataloged);
            facStatusResult.mergeStatusBits(0_500000_000000L);
            return result;
        }

        // If +1, we can do it if there is not already a plus-one.
        // This is the only case where we can catalog a file via relative file cycle.
        // We *might* need to drop the oldest cycle.
        if (fcs.getCycle() == 1) {
            if (fileSetInfo.plusOneExists()) {
                facStatusResult.postMessage(FacStatusCode.RelativeFCycleConflict);
                facStatusResult.mergeStatusBits(0_400000_000040L);
            } else {
                if (fileSetInfo.getCurrentCycleRange() == fileSetInfo.getMaxCycleRange()) {
                    result.requiresDroppingOldestCycle = true;
                }
                result.absoluteCycle = fileSetInfo.getHighestAbsoluteCycle() + 1;
                result.isAllowed = true;
            }
            return result;
        }

        var fcx = Math.abs(fcs.getCycle());
        var cycInfo = fileSetInfo.getCycleInfo();
        if (fcx < cycInfo.size()) {
            facStatusResult.postMessage(FacStatusCode.FileAlreadyCataloged);
            facStatusResult.mergeStatusBits(0_500000_000000L);
        } else {
            facStatusResult.postMessage(FacStatusCode.RelativeFCycleConflict);
            facStatusResult.mergeStatusBits(0_400000_000040L);
        }

        return result;
    }

    /**
     * Retrieves a collection of NodeInfo objects representing UP and SU disk units.
     * Also assigns the units to the Exec.
     */
    private Collection<NodeInfo> getAccessibleFixedDisks() throws ExecStoppedException {
        var list = new LinkedList<NodeInfo>();
        var execQualifier = Exec.getInstance().getProjectId();
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
                assignFixedDiskUnitToExec(fileSpec, node.getNodeIdentifier(), null, fsResult);
            }
        }
        return list;
    }

    private NodeInfo getNodeInfoByName(
        final String nodeName
    ) {
        synchronized (_nodeGraph) {
            return _nodeGraph.values()
                             .stream()
                             .filter(ni -> ni.getNode().getNodeName().equals(nodeName))
                             .findFirst()
                             .orElse(null);
        }
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
            Exec.getInstance().sendExecReadOnlyMessage(msg);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        var initialDirTrack = readInitialDirectoryTrack(diskDevice, diskLabel);
        var pi = PackInfo.loadFromLabel(diskLabel, initialDirTrack);
        if (pi == null) {
            var msg = String.format("Pack on %s has no label", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!Parser.isValidPackName(pi.getPackName())) {
            var msg = String.format("Pack on %s has an invalid pack name", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!Parser.isValidPrepFactor(pi.getPrepFactor())) {
            var msg = String.format("Pack on %s has an invalid prep factor: %d",
                                    diskDevice.getNodeName(),
                                    pi.getPrepFactor());
            Exec.getInstance().sendExecReadOnlyMessage(msg);
            diskNodeInfo.setNodeStatus(NodeStatus.Down);
            return null;
        }

        if (!pi.isPrepped()) {
            var msg = String.format("Pack on %s is not prepped", diskDevice.getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg);
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
        Exec.getInstance().sendExecReadOnlyMessage(sb.toString());

        return pi;
    }

    /**
     * Loads the node graph from the configuration.
     * You *did* load the configuration already, right?
     * Right?
     */
    private void loadNodeGraph() throws KExecException {
        boolean error = false;
        var nodes = Exec.getInstance().getConfiguration().getNodes();
        for (var configNode : nodes) {
            var nodeName = configNode.getName();
            switch (configNode.getEquipType()) {
                case CHANNEL_MODULE_DISK -> {
                    var node = new DiskChannel(nodeName);
                    _nodeGraph.put(node.getNodeIdentifier(), new ChannelNodeInfo(node));
                }
                case CHANNEL_MODULE_SYMBIONT -> {
                    var node = new SymbiontChannel(nodeName);
                    _nodeGraph.put(node.getNodeIdentifier(), new ChannelNodeInfo(node));
                }
                case CHANNEL_MODULE_TAPE -> {
                    var node = new TapeChannel(nodeName);
                    _nodeGraph.put(node.getNodeIdentifier(), new ChannelNodeInfo(node));
                }
                case FILE_SYSTEM_PRINTER -> {
                    var path = configNode.getArgument("PATH");
                    if (path == null) {
                        LogManager.logFatal(LOG_SOURCE, "Node %s has no configured PATH argument", nodeName);
                        error = true;
                        break;
                    }
                    var node = new FileSystemPrinterDevice(nodeName, path);
                    _nodeGraph.put(node.getNodeIdentifier(), new DeviceNodeInfo(node));
                }
                case FILE_SYSTEM_CARD_PUNCH -> {
                    var path = configNode.getArgument("PATH");
                    if (path == null) {
                        LogManager.logFatal(LOG_SOURCE, "Node %s has no configured PATH argument", nodeName);
                        error = true;
                        break;
                    }
                    var node = new FileSystemCardPunchDevice(nodeName, path);
                    _nodeGraph.put(node.getNodeIdentifier(), new DeviceNodeInfo(node));
                }
                case FILE_SYSTEM_CARD_READER -> {
                    var path = configNode.getArgument("PATH");
                    if (path == null) {
                        LogManager.logFatal(LOG_SOURCE, "Node %s has no configured PATH argument", nodeName);
                        error = true;
                        break;
                    }
                    var node = new FileSystemCardReaderDevice(nodeName, path);
                    _nodeGraph.put(node.getNodeIdentifier(), new DeviceNodeInfo(node));
                }
                case FILE_SYSTEM_DISK -> {
                    var path = configNode.getArgument("PATH");
                    if (path == null) {
                        LogManager.logFatal(LOG_SOURCE, "Node %s has no configured PATH argument", nodeName);
                        error = true;
                        break;
                    }
                    var node = new FileSystemDiskDevice(nodeName, path, false);
                    _nodeGraph.put(node.getNodeIdentifier(), new DeviceNodeInfo(node));
                }
                case FILE_SYSTEM_TAPE -> {
                    var node = new FileSystemTapeDevice(nodeName);
                    _nodeGraph.put(node.getNodeIdentifier(), new DeviceNodeInfo(node));
                }
            }
        }

        for (var cfgNode : nodes) {
            var cfgName = cfgNode.getName();
            var nodeInfo = getNodeInfo(cfgName);
            if (nodeInfo == null) {
                LogManager.logFatal(LOG_SOURCE, "Node %s was not placed in the node graph", cfgName);
                error = true;
                continue;
            }

            var node = nodeInfo.getNode();
            if (!(node instanceof Channel channel)) {
                if (!cfgNode.getSubordinates().isEmpty()) {
                    LogManager.logFatal(LOG_SOURCE, "Node %s is not a channel but it has children", cfgName);
                    error = true;
                }

                continue;
            }

            for (var childCfgNode : cfgNode.getSubordinates()) {
                var childName = childCfgNode.getName();
                var childNodeInfo = getNodeInfo(childName);
                if (childNodeInfo == null) {
                    LogManager.logFatal(LOG_SOURCE, "Node %s (child of %s) not found in node graph", childName, cfgName);
                    error = true;
                    continue;
                }

                var childNode = childNodeInfo.getNode();
                if (!(childNode instanceof Device device)) {
                    LogManager.logFatal(LOG_SOURCE, "Node %s (child of %s) is not a device", childName, cfgName);
                    error = true;
                    continue;
                }

                if (!channel.canAttach(device)) {
                    LogManager.logFatal(LOG_SOURCE, "Device %s cannot be attached to Channel %s", childName, cfgName);
                    error = true;
                    continue;
                }

                channel.attach(device);
            }
        }

        if (error) {
            System.out.println("Configuration has errors");
            throw new ExecStoppedException();
        }

        for (var nodeInfo : _nodeGraph.values()) {
            if (nodeInfo.getNode() instanceof Channel channel) {
                System.out.printf("Channel %s", channel.getNodeName());
                var devices = channel.getDevices();
                if (!devices.isEmpty()) {
                    System.out.print(" connects to");
                    for (var device : devices) {
                        System.out.printf(" %s", device.getNodeName());
                    }
                }
                System.out.println();
            }
        }
    }

    /**
     * Prompts the operator to load a pack on a disk unit,
     * waits for it to happen, then verifies the packname.
     * @param run describes the invoking run
     * @param nodeInfo NodeInfo object tracking exec information regarding the disk unit
     * @param disk DiskDevice object associated with the disk unit
     * @param packName pack name requested by the run
     * @return true if the pack is loaded - the operator may deny pack loading based on pack name mismatch.
     * @throws ExecStoppedException if we notice the exec has stopped during processing
     */
    private boolean promptLoadPack(
        final Run run,
        final NodeInfo nodeInfo,
        final DiskDevice disk,
        final String packName
    ) throws ExecStoppedException {
        // TODO while waiting we need to monitor the rce to see if it has been err'd, aborted, etc
        //   so we can stop waiting, post E:260733 Run has been aborted, and return false
        var loadMsg = String.format("Load %s %s %s",
                                    packName,
                                    disk.getNodeName(),
                                    run.getActualRunId());
        Exec.getInstance().sendExecReadOnlyMessage(loadMsg, ConsoleType.InputOutput);
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
                Exec.getInstance().sendExecReadOnlyMessage(serviceMsg, ConsoleType.InputOutput);
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
            var response = Exec.getInstance().sendExecRestrictedReadReplyMessage(msg, candidates, ConsoleType.InputOutput);
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
        var dirAddr = diskLabel.get(3);
        var channel = selectRoute(disk);
        var ioPkt = new ChannelIoPacket().setNodeIdentifier(disk.getNodeIdentifier())
                                         .setIoFunction(IoFunction.Read)
                                         .setFormat(TransferFormat.Packed)
                                         .setDeviceWordAddress(dirAddr)
                                         .setBuffer(new ArraySlice(new long[1792]));
        channel.routeIo(ioPkt);
        if (ioPkt.getIoStatus() != IoStatus.Successful) {
            LogManager.logError(LOG_SOURCE, "readPackLabel ioStatus=%s", ioPkt.getIoStatus());
            var msg = String.format("%s Cannot read directory track %s", disk.getNodeName(), ioPkt.getIoStatus());
            Exec.getInstance().sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
            return null;
        }

        return ioPkt.getBuffer();
    }

    private ArraySlice readPackLabel(
        final DiskDevice disk
    ) throws NoRouteForIOException, ExecStoppedException {
        var ioPkt = new ChannelIoPacket().setNodeIdentifier(disk.getNodeIdentifier())
                                         .setIoFunction(IoFunction.Read)
                                         .setFormat(TransferFormat.Packed)
                                         .setDeviceWordAddress(0L)
                                         .setBuffer(new ArraySlice(new long[28]));
        var channel = selectRoute(disk);
        channel.routeIo(ioPkt);
        if (ioPkt.getIoStatus() != IoStatus.Successful) {
            LogManager.logError(LOG_SOURCE, "readPackLabel ioStatus=%s", ioPkt.getIoStatus());
            var msg = String.format("%s Cannot read pack label %s", disk.getNodeName(), ioPkt.getIoStatus());
            Exec.getInstance().sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
            return null;
        }

        return ioPkt.getBuffer();
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
