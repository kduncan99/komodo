/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Worker;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class which models a Procesor node
 */
@SuppressWarnings("Duplicates")
public abstract class Processor extends Node implements Worker {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested things
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum Type {
        SystemProcessor(0),
        InstructionProcessor(1),
        InputOutputProcessor(2),
        MainStorageProcessor(3);

        private final int _code;
        Type(int code) { _code = code; }
        public int getCode() { return _code; }

        public static Type getValue(
            final int code
        ) {
            switch (code) {
                case 0:     return SystemProcessor;
                case 1:     return InstructionProcessor;
                case 2:     return InputOutputProcessor;
                case 3:     return MainStorageProcessor;
            }

            throw new RuntimeException("Invalid code for Type.getValue()");
        }
    }

    static class UPIIndexPair {
        final int _sourceIndex;
        final int _destinationIndex;

        UPIIndexPair(
            final int sourceIndex,
            final int destinationIndex
        ) {
            _sourceIndex = sourceIndex;
            _destinationIndex = destinationIndex;
        }

        @Override
        public boolean equals(
            final Object obj
        ) {
            return (obj instanceof UPIIndexPair)
                && (((UPIIndexPair) obj)._sourceIndex == _sourceIndex)
                && (((UPIIndexPair) obj)._destinationIndex == _destinationIndex);
        }

        @Override
        public int hashCode() {
            return (_sourceIndex << 16) | _destinationIndex;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(Processor.class);

    final Type _Type;

    /**
     * Uniquely identifies each processor in the configuration, in a manner transparent to the configurating entity.
     * Our own requirement is that the UPI must be greater than zero, and less than or equal to 15.
     * This is due to absolute address resolution for MSPs.
     */
    public final int _upiIndex;

    /**
     * Indicates the current target IP for broadcasts.  Must be kept updated.
     */
    private static InstructionProcessor _broadcastDestination = null;

    /**
     * When an interrupt or an ACK is sent to this processor, an entry is made in the
     * appropriate set below.  It is up to the individual processor to watch these
     * sets, and take any appropriate action.
     */
    final Set<Processor> _upiPendingAcknowledgements = new HashSet<>();
    final Set<Processor> _upiPendingInterrupts = new HashSet<>();

    /**
     * Lookup table of mailbox slots for UPI communication.
     * Populated by SystemProcessor at any appropriate point prior to OS initialization.
     * Key is the combination of source and destination UPI indices, value is the AbsoluteAddress
     * in storage where the communication area is located, for messages from the source processor
     * to the destination processor.
     * --
     * Not every combination of source/destination indices are valid, reflecting the fact that
     * not every combinaion of processors interrupt each other, that broadcast messages exist
     * where-in the destination is not known to the source, and that in some cases where
     * paths *do* exist from one processor to another, no communication area is necessary.
     * It is up to the SP to determine which combinations are to exist, to build that area in
     * storage, to store the location of the communications area in the configuration bank
     * so that the OS can access it, and to populate this container accordingly.
     * --
     * The communications area in storage is formatted in a manner only relevant to the SP and
     * to the OS - the actual absolute addresses of each specific communications slot is
     * all we care about here.
     */
    static final Map<UPIIndexPair, AbsoluteAddress> _upiCommunicationLookup = new HashMap<>();

    /**
     * All processors must implement a thread, if for no other reason than to monitor the UPI tables.
     * IPs, IOPs, and SPs will all have something useful to do.
     * MSPs will probably not.
     */
    protected boolean _workerTerminate;         //  worker thread must monitor this, and shut down when it goes true
    protected final Thread _workerThread;       //  reference to worker thread.

    /**
     * Indicates whether this Processor has completed initialization and is ready for work
     */
    protected boolean _isReady;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public Processor(
        final Type processorType,
        final String name,
        final int upiIndex
    ) {
        super(NodeCategory.Processor, name);
        _Type = processorType;
        _upiIndex = upiIndex;

        //  Start the processor thread
        _workerThread = new Thread(this);
        _workerTerminate = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when a new session is started by the system processor.
     * Any classes which have work to do to enter the cleared state should override this, do their specific clearing stuff,
     * then super back to this routine before returning.
     */
    @Override
    public void clear(
    ) {
        _broadcastDestination = null;
        _upiPendingInterrupts.clear();
        _upiPendingAcknowledgements.clear();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For debugging purposes
     * @param writer where we write
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Type:%s\n", _Type.toString()));

            StringBuilder sb = new StringBuilder();
            sb.append("  Pending SENDs from: ");
            synchronized (_upiPendingInterrupts) {
                for (Processor p : _upiPendingInterrupts) {
                    sb.append(String.format("  %s(%d)", p._name, p._upiIndex));
                }
            }
            writer.write(sb.toString());

            sb = new StringBuilder();
            sb.append("  Pending ACKs from: ");
            synchronized (_upiPendingAcknowledgements) {
                for (Processor p : _upiPendingAcknowledgements) {
                    sb.append(String.format("  %s(%d)", p._name, p._upiIndex));
                }
            }
            writer.write(sb.toString());
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    @Override
    public final String getWorkerName() { return _name; }

    /**
     * Starts the instantiated thread
     * The subclass should do any initialization in the run() method,
     * as we do not allow it to override this method.
     */
    @Override
    public final void initialize(
    ) {
        _workerThread.start();
        while (!_workerThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                LOGGER.catching(ex);
            }
        }
    }

    public final boolean isReady() {
        return _isReady;
    }

    /**
     * Invoked just before tearing down the configuration
     */
    @Override
    public final void terminate() {
        //  Tell the worker thread to go away.
        _workerTerminate = true;
        synchronized (this) { notify(); }
        while (_workerThread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                LOGGER.catching(ex);
            }
        }
    }

    /**
     * For the subclass to invoke the process of sending a UPI ACK.
     * @param upiIndex UPI index of the destination processor
     * @return true if the ack was posted, false if an ack from this processor to the destination processor is still pending.
     * If we return false, the caller must retry at some later point.
     * @throws UPINotAssignedException if an unassigned UPI index was provided
     */
    final boolean upiAcknowledge(
        final int upiIndex
    ) throws UPINotAssignedException  {
        Processor destProc = InventoryManager.getInstance().getProcessor(upiIndex);
        synchronized (destProc._upiPendingAcknowledgements) {
            boolean result = destProc._upiPendingAcknowledgements.add(this);
            if (result) {
                synchronized (destProc) { destProc.notify(); }
            }
            return result;
        }
    }

    /**
     * For the subclass to invoke the process of sending a UPI broadcast interrupt, to be handled by any active IP.
     * Any other use of this results in undefined behavior.
     * @return true if the broadcast was sent;
     * false if no IP was available which did not already have a pending interrupt from this source.
     * If we return false, the caller must retry at some later point.
     * @throws InvalidSystemConfigurationException if there are no configured IPs
     */
    final boolean upiSendBroadcast(
    ) throws InvalidSystemConfigurationException {
        InstructionProcessor ip = getBroadcastProcessor();
        synchronized (ip._upiPendingInterrupts) {
            boolean result = ip._upiPendingInterrupts.add(this);
            if (result) {
                synchronized (ip) { ip.notify(); }
            }
            return result;
        }
    }

    /**
     * For the subclass to invoke the process of sending a directed UPI interrupt.
     * @param upiIndex UPI index of the destination processor
     * @return true if the interrupt was posted;
     * false if some other interrupt from this processor to the targeted processor is already pending.
     * If we return false, the caller must retry at some later point.
     * @throws UPINotAssignedException if an unassigned UPI index was provided
     */
    final boolean upiSendDirected(
        final int upiIndex
    ) throws UPINotAssignedException  {
        Processor destProc = InventoryManager.getInstance().getProcessor(upiIndex);
        synchronized (destProc._upiPendingInterrupts) {
            boolean result = destProc._upiPendingInterrupts.add(this);
            if (result) {
                synchronized (destProc) { destProc.notify(); }
            }
            return result;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Retrieves selected processor
     * @return selected instruction processor
     * @throws InvalidSystemConfigurationException if there are no configured IPs
     */
    private static InstructionProcessor getBroadcastProcessor(
    ) throws InvalidSystemConfigurationException {
        synchronized (Processor.class) {
            if (_broadcastDestination == null) {
                updateBroadcastProcessor();
            }
            return _broadcastDestination;
        }
    }

    /**
     * Retrieves the value of a particular word of storage for a given MSP, as described by an AbsoluteAddress object
     * @param absoluteAddress MSP UPI and address of the word within the MSP
     * @return value of the word retrieved
     * @throws AddressingExceptionInterrupt for an invalid MSP or segment reference
     * @throws AddressLimitsException if the address is invalid
     * @throws UPIProcessorTypeException if the UPI is not for an MSP
     * @throws UPINotAssignedException if no processor exists for the given UPI
     */
    static long getStorageValue(
        final AbsoluteAddress absoluteAddress
    ) throws AddressingExceptionInterrupt,
             AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(absoluteAddress._upiIndex);
        if ((absoluteAddress._offset < 0)
            || (absoluteAddress._offset >= msp.getStorage(absoluteAddress._segment).getSize())) {
            throw new AddressLimitsException(absoluteAddress);
        }

        return msp.getStorage(absoluteAddress._segment).get(absoluteAddress._offset);
    }

    /**
     * Sets a value in main storage
     * @param absoluteAddress MSP UPI and address of the word within the MSP
     * @param value value to be stored
     * @throws AddressingExceptionInterrupt for an invalid MSP or segment reference
     * @throws AddressLimitsException if the address is invalid
     * @throws UPIProcessorTypeException if the UPI is not for an MSP
     * @throws UPINotAssignedException if no processor exists for the given UPI
     */
    static void setStorageValue(
        final AbsoluteAddress absoluteAddress,
        final long value
    ) throws AddressingExceptionInterrupt,
             AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(absoluteAddress._upiIndex);
        if ((absoluteAddress._offset < 0)
            || (absoluteAddress._offset >= msp.getStorage(absoluteAddress._segment).getSize())) {
            throw new AddressLimitsException(absoluteAddress);
        }

        msp.getStorage(absoluteAddress._segment).set(absoluteAddress._offset, value);
    }

    /**
     * Resets the broadcast destination.
     * To be invoked whenever:
     *      The system is restarted
     *      The broadcast eligibility flag is updated on any instruction processor
     * @throws InvalidSystemConfigurationException if there are no configured IPs
     */
    static void updateBroadcastProcessor(
    ) throws InvalidSystemConfigurationException {
        synchronized (Processor.class) {
            List<InstructionProcessor> processors = InventoryManager.getInstance().getInstructionProcessors();
            if (processors.isEmpty()) {
                throw new InvalidSystemConfigurationException();
            }
            for (InstructionProcessor ip : processors) {
                if (ip.getBroadcastInterruptEligibility()) {
                    _broadcastDestination = ip;
                    return;
                }
            }

            _broadcastDestination = processors.get(0);
        }
    }
}
