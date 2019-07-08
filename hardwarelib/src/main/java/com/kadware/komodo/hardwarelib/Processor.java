/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class which models a Procesor node
 */
@SuppressWarnings("Duplicates")
public abstract class Processor extends Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(Processor.class);

    final ProcessorType _processorType;

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
     * Absolute address of the communications mailboxes for UPI interrupt handling.
     * This is set by the SystemProcessor at some point prior to IPL, in conjunction with
     * populating the particular area of storage.
     * See SystemProcessor class for the format of this area.
     */
    protected static AbsoluteAddress _upiCommunicationsArea = null;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public Processor(
        final ProcessorType processorType,
        final String name,
        final int upiIndex
    ) {
        super(NodeCategory.Processor, name);
        _processorType = processorType;
        _upiIndex = upiIndex;
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
    public void clear() {
        _broadcastDestination = null;
    }

    /**
     * Invoked when the config is built, and before we allow anyone into it.
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public abstract void terminate();

    /**
     * All processors must implement this, even if they do not participate in UPI interrupts
     * @param source processor initiating the interrupt
     * @param broadcast true if this is a broadcast
     */
    abstract void upiHandleInterrupt(
        final Processor source,
        final boolean broadcast
    );


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
            writer.write(String.format("  Type:%s\n", _processorType.toString()));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * For the subclass to invoke the process of sending a UPI broadcast interrupt, to be handled by any active IP.
     * We depart from the standard architecture in that we require the destination to fully handle the SEND,
     * at least to the point at which the source can safely issue a subsequent SEND, before returning.
     * Hence, there will never be a need for an ACK.
     * @throws InvalidSystemConfigurationException if there are no configured IPs
     */
    protected final void upiSendBroadcast(
    ) throws InvalidSystemConfigurationException {
        InstructionProcessor ip = getBroadcastProcessor();
        ip.upiHandleInterrupt(this, true);
    }

    /**
     * For the subclass to invoke the process of sending a directed UPI interrupt.
     * We depart from the standard architecture in that we require the destination to fully handle the SEND,
     * at least to the point at which the source can safely issue a subsequent SEND, before returning.
     * @param upiIndex UPI index of the destination processor
     * @throws UPINotAssignedException if an unassigned UPI index was provided
     */
    protected final void upiSendDirected(
        int upiIndex
    ) throws UPINotAssignedException  {
        InventoryManager.getInstance().getProcessor(upiIndex).upiHandleInterrupt(this, false);
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

    //TODO do we need this?
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

    //TODO do we need this?
    /**
     * Convenience method for the above
     */
    static public void setStorageValue(
        final AbsoluteAddress absoluteAddress,
        final Word36 value
    ) throws AddressingExceptionInterrupt,
             AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        setStorageValue(absoluteAddress, value.getW());
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
