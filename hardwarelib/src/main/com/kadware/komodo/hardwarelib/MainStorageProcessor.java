/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class which models a Main Storage Processor.
 * An MSP contains the main storage, or memory, for the emulated mainframe complex.
 * There must be at least one and up to four depending upon the size of the host's memory and the number of other
 * Processors in the configuration (there must be at least one IP and one IOP as well).
 *
 * The MSP provides a configured amount of fixed space, accessible as segment zero.
 * The actual amount is configurable, but does not need to be overly large..
 * it will likely only contain the operating system (or at least the important bits of it).
 * The rest of the segments are allocated via requests from the operating system - these are intended to contain
 * user and other temporary data structures and code.
 *
 * This design relieves the burden of memory management from the operating system,
 * placing it in the host operating system which knows far better how to page.
 */
@SuppressWarnings("Duplicates")
public class MainStorageProcessor extends Processor {

    private final ArraySlice _fixedStorage;
    private final Map<Integer, ArraySlice> _dynamicStorage = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(MainStorageProcessor.class);

    /**
     * constructor
     * @param name node name of the MSP
     * @param upi UPI for the MSP
     * @param fixedStorageSize number of words of fixed storage - minimum of 256KW.
     */
    MainStorageProcessor(
        final String name,
        final int upi,
        final int fixedStorageSize
    ) {
        super(Type.MainStorageProcessor, name, upi);
        if (fixedStorageSize < 256 * 1024) {
            throw new RuntimeException(String.format("Bad size for MSP:%d words", fixedStorageSize));
        }
        _fixedStorage = new ArraySlice(new long[fixedStorageSize]);
    }

    /**
     * Clears the processor
     */
    @Override
    public void clear(
    ) {
        _dynamicStorage.clear();
        _fixedStorage.clear();
        super.clear();
    }

    /**
     * Allocates a new segment
     * @param storageSize requested size in words - must be greater than zero
     * @return segmentIndex of newly-allocated segment
     * @throws AddressingExceptionInterrupt if the given storageSize is incorrect
     */
    int createSegment(
        final int storageSize
    ) throws AddressingExceptionInterrupt {
        if (storageSize <= 0) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   0,
                                                   0);
        }

        synchronized (this) {
            int newSegment = 1;
            while (_dynamicStorage.containsKey(newSegment)) {
                ++newSegment;
            }
            ArraySlice newSlice = new ArraySlice(new long[storageSize]);
            _dynamicStorage.put(newSegment, newSlice);
            return newSegment;
        }
    }

    /**
     * Deallocates an existing dynamic segment
     * @param segmentIndex segment to be deleted
     * @throws AddressingExceptionInterrupt if the segment does not exist
     */
    void deleteSegment(
        final int segmentIndex
    ) throws AddressingExceptionInterrupt {
        synchronized (this) {
            ArraySlice slice = _dynamicStorage.get(segmentIndex);
            if (slice == null) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       0,
                                                       0);
            }

            _dynamicStorage.remove(segmentIndex);
        }
    }

    /**
     * Getter to retrieve the full storage for a segment
     * @return Word36Array representing the storage for the MSP
     */
    public ArraySlice getStorage(
        final int segmentIndex
    ) throws AddressingExceptionInterrupt {
        if (segmentIndex == 0) {
            return _fixedStorage;
        } else {
            ArraySlice result = _dynamicStorage.get(segmentIndex);
            if (result == null) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       0,
                                                       0);
            } else {
                return result;
            }
        }
    }

    /**
     * MSPs have no ancestors
     * @param ancestor candidate ancestor
     * @return always false
     */
    @Override
    public final boolean canConnect(
        final Node ancestor
    ) {
        return false;
    }

    /**
     * For debugging
     * @param writer destination for output
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
    }

    /**
     * Reallocates an existing dynamic segment
     * If the new storageSize is less than the existing size, the new segment will contain the front portion
     * of the original segment, truncated as necessary.
     * If the new storageSize is greater than the existing size, the new additional space at the end of the
     * segment will be initialized to zero.
     * In any case, storageSize must be greater than zero.
     * Because this will almost certainly cause the allocation of a new ArraySlice object, the operating system
     * must replace any AbsoluteAddress objects which refer to this segment with new objects.
     * @param segmentIndex Index of the segment
     * @param storageSize new size of the segment
     * @return (probably new) ArraySlice associated with the segment
     */
    synchronized ArraySlice resizeSegment(
        final int segmentIndex,
        final int storageSize
    ) throws AddressingExceptionInterrupt {
        synchronized (this) {
            ArraySlice originalSlice = _dynamicStorage.get(segmentIndex);
            if (originalSlice == null) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       0,
                                                       0);
            }

            if (storageSize == originalSlice.getSize()) {
                return originalSlice;
            } else {
                ArraySlice newSlice = originalSlice.copyOf(storageSize);
                _dynamicStorage.put(segmentIndex, newSlice);
                return newSlice;
            }
        }
    }

    /**
     * processor thread - we don't really do that much here
     */
    @Override
    public void run() {
        LOGGER.info(_name + " worker thread starting");

        while (!_workerTerminate) {
            synchronized (_upiPendingAcknowledgements) {
                for (Processor source : _upiPendingAcknowledgements) {
                    LOGGER.error(String.format("%s received a UPI ACK from %s", _name, source._name));
                }
                _upiPendingAcknowledgements.clear();
            }

            synchronized (_upiPendingInterrupts) {
                for (Processor source : _upiPendingInterrupts) {
                    LOGGER.error(String.format("%s received a UPI interrupt from %s", _name, source._name));
                }
                _upiPendingInterrupts.clear();
            }

            try {
                synchronized (this) { wait(100); }
            } catch (InterruptedException ex) {
                LOGGER.catching(ex);
            }
        }

        LOGGER.info(_name + " worker thread terminating");
    }
}
