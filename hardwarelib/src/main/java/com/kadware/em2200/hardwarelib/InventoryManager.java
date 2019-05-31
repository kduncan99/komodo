/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.em2200.hardwarelib.misc.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Creation and discardation of hardware things (i.e., anything extending Node) must occur via this manager.
 * This is a singleton.
 */
@SuppressWarnings("Duplicates")
public class InventoryManager {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public final static int MAX_MAIN_STORAGE_PROCESSORS = 16;
    public final static int MAX_INPUT_OUTPUT_PROCESSORS = 8;
    public final static int MAX_INSTRUCTION_PROCESSORS = 8;

    public final static short FIRST_MAIN_STORAGE_PROCESSOR_UPI = 0;
    public final static short FIRST_INPUT_OUTPUT_PROCESSOR_UPI = FIRST_MAIN_STORAGE_PROCESSOR_UPI + MAX_MAIN_STORAGE_PROCESSORS;
    public final static short FIRST_INSTRUCTION_PROCESSOR_UPI = FIRST_INPUT_OUTPUT_PROCESSOR_UPI + MAX_INPUT_OUTPUT_PROCESSORS;

    public final static int MAIN_STORAGE_PROCESSOR_SIZE = 4 * 1024 * 1024;  //  4 MWords

    private final Map<Short, Processor> _processors = new HashMap<>();

    private static InventoryManager _instance = null;

    //???? Need some more general node things here
    //???? Need to decide if we do connect/disconnect here (I think we need to disconnect at least, on Node remove)


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor, instance getter, etc
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Instance getter
     * @return singleton instance getter
     */
    public static InventoryManager getInstance(
    ) {
        synchronized(InventoryManager.class) {
            if (_instance == null) {
                _instance = new InventoryManager();
            }
        }

        return _instance;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Adds an existing InstructionProcessor to our inventory.
     * The existing IP should not yet have been initialized.
     * Only for unit tests.
     * @param ip object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addInstructionProcessor(
        final InstructionProcessor ip
    ) throws NodeNameConflictException,
             UPIConflictException {
        if (_processors.get(ip.getUPI()) != null) {
            throw new UPIConflictException(ip.getUPI());
        }

        for (Processor processor : _processors.values()) {
            if (ip.getName().equalsIgnoreCase(processor.getName())) {
                throw new NodeNameConflictException(ip.getName());
            }
        }

        _processors.put(ip.getUPI(), ip);
        ip.initialize();
    }

    /**
     * Adds an existing StaticMainStorageProcessor to our inventory.
     * The existing MSP should not yet have been initialized.
     * Only for unit tests.
     * @param msp object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addMainStorageProcessor(
        final StaticMainStorageProcessor msp
    ) throws NodeNameConflictException,
             UPIConflictException {
        if (_processors.get(msp.getUPI()) != null) {
            throw new UPIConflictException(msp.getUPI());
        }

        for (Processor processor : _processors.values()) {
            if (msp.getName().equalsIgnoreCase(processor.getName())) {
                throw new NodeNameConflictException(msp.getName());
            }
        }

        _processors.put(msp.getUPI(), msp);
        msp.initialize();
    }

    /**
     * Creates a new StaticMainStorageProcessor with a unique name and UPI.
     * @return new processor object
     * @throws MaxNodesException if too many processors of this type have been created
     */
    public StaticMainStorageProcessor createMainStorageProcessor(
    ) throws MaxNodesException {
        short upi = FIRST_MAIN_STORAGE_PROCESSOR_UPI;
        for (int px = 0; px < MAX_MAIN_STORAGE_PROCESSORS; ++px, ++upi) {
            if (_processors.get(upi) == null) {
                String name = String.format("MSP%d", px);
                StaticMainStorageProcessor msp = new StaticMainStorageProcessor(name, upi, MAIN_STORAGE_PROCESSOR_SIZE);
                _processors.put(upi, msp);
                msp.initialize();
                return msp;
            }
        }

        throw new MaxNodesException(StaticMainStorageProcessor.class);
    }

    /**
     * Creates a new InputOutputProcessor with a unique name and UPI.
     * @return new processor object
     * @throws MaxNodesException if too many processors of this type have been created
     */
    public InputOutputProcessor createInputOutputProcessor(
    ) throws MaxNodesException {
        short upi = FIRST_INPUT_OUTPUT_PROCESSOR_UPI;
        for (int px = 0; px < MAX_INPUT_OUTPUT_PROCESSORS; ++px, ++upi) {
            if (_processors.get(upi) == null) {
                String name = String.format("IOP%d", px);
                InputOutputProcessor iop = new InputOutputProcessor(name, upi);
                _processors.put(upi, iop);
                iop.initialize();
                return iop;
            }
        }

        throw new MaxNodesException(InputOutputProcessor.class);
    }

    /**
     * Creates a new InstructionProcessor with a unique name and UPI.
     * @return new processor object
     * @throws MaxNodesException if too many processors of this type have been created
     */
    public InstructionProcessor createInstructionProcessor(
    ) throws MaxNodesException {
        short upi = FIRST_INSTRUCTION_PROCESSOR_UPI;
        for (int px = 0; px < MAX_INSTRUCTION_PROCESSORS; ++px, ++upi) {
            if (_processors.get(upi) == null) {
                String name = String.format("IP%d", px);
                InstructionProcessor ip = new InstructionProcessor(name, upi);
                _processors.put(upi, ip);
                ip.initialize();
                return ip;
            }
        }

        throw new MaxNodesException(InstructionProcessor.class);
    }

    /**
     * Deletes a particular processor from our inventory
     * @param upi UPI of the processor to be deleted
     * @throws UPINotAssignedException if no processor can be found
     */
    //???? Need to change this to deleteNode, or something
    public void deleteProcessor(
        final short upi
    ) throws UPINotAssignedException {
        Processor processor = _processors.get(upi);
        if (processor == null) {
            throw new UPINotAssignedException(upi);
        }

        processor.terminate();
        _processors.remove(upi);
    }

    /**
     * Retrieves a particular processor given its UPI
     * @param upi UPI of processor of interest
     * @return processor of interest
     * @throws UPINotAssignedException if no processor can be found
     */
    public Processor getProcessor(
        final short upi
    ) throws UPINotAssignedException {
        Processor processor = _processors.get(upi);
        if (processor == null) {
            throw new UPINotAssignedException(upi);
        }
        return processor;
    }

    /**
     * Retrieves a specific StaticMainStorageProcessor
     * @param upi UPI of processor of interest
     * @return processor of interest
     * @throws UPIProcessorTypeException if the UPI correspond to a processor not of the expected type
     * @throws UPINotAssignedException if no processor can be found
     */
    public StaticMainStorageProcessor getMainStorageProcessor(
        final short upi
    ) throws UPIProcessorTypeException,
             UPINotAssignedException {
        Processor processor = getProcessor(upi);
        if (processor instanceof StaticMainStorageProcessor) {
            return (StaticMainStorageProcessor)processor;
        } else {
            throw new UPIProcessorTypeException(upi, StaticMainStorageProcessor.class);
        }
    }

    /**
     * Retrieves a specific InputOutputProcessor
     * @param upi UPI of processor of interest
     * @return processor of interest
     * @throws UPIProcessorTypeException if the UPI correspond to a processor not of the expected type
     * @throws UPINotAssignedException if no processor can be found
     */
    public InputOutputProcessor getInputOutputProcessor(
        final short upi
    ) throws UPIProcessorTypeException,
             UPINotAssignedException {
        Processor processor = getProcessor(upi);
        if (processor instanceof InputOutputProcessor) {
            return (InputOutputProcessor)processor;
        } else {
            throw new UPIProcessorTypeException(upi, InputOutputProcessor.class);
        }
    }

    /**
     * Retrieves a specific InstructionProcessor
     * @param upi UPI of processor of interest
     * @return processor of interest
     * @throws UPIProcessorTypeException if the UPI correspond to a processor not of the expected type
     * @throws UPINotAssignedException if no processor can be found
     */
    public InstructionProcessor getInstructionProcessor(
        final short upi
    ) throws UPIProcessorTypeException,
             UPINotAssignedException {
        Processor processor = getProcessor(upi);
        if (processor instanceof InputOutputProcessor) {
            return (InstructionProcessor)processor;
        } else {
            throw new UPIProcessorTypeException(upi, InstructionProcessor.class);
        }
    }

    /**
     * Retrieves the value of a particular word of storage for a given MSP, as described by an AbsoluteAddress object
     * @param absoluteAddress MSP UPI and address of the word within the MSP
     * @return long integer value of the word retrieved
     * @throws AddressingExceptionInterrupt for an invalid MSP reference
     * @throws AddressLimitsException if the address is invalid
     * @throws UPIProcessorTypeException if the UPI is not for an MSP
     * @throws UPINotAssignedException if no processor exists for the given UPI
     */
    public long getStorageValue(
        final AbsoluteAddress absoluteAddress
    ) throws AddressingExceptionInterrupt,
             AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        StaticMainStorageProcessor msp = getMainStorageProcessor(absoluteAddress._upi);
        if ((absoluteAddress._offset < 0) || (absoluteAddress._offset >= MAIN_STORAGE_PROCESSOR_SIZE)) {
            throw new AddressLimitsException(absoluteAddress);
        }

        return msp.getStorage(absoluteAddress._segment).getValue(absoluteAddress._offset);
    }

    /**
     * Retrieves a particular word of storage for a given MSP, as described by an AbsoluteAddress object.
     * Since storage is kept as values and not objects, we have to create an object here.
     * @param absoluteAddress MSP UPI and address of the word within the MSP
     * @return Word36 object containing the value of the word retrieved
     * @throws AddressingExceptionInterrupt for an invalid MSP reference
     * @throws AddressLimitsException if the address is invalid
     * @throws UPIProcessorTypeException if the UPI is not for an MSP
     * @throws UPINotAssignedException if no processor exists for the given UPI
     */
    public Word36 getStorageWord(
        final AbsoluteAddress absoluteAddress
    ) throws AddressingExceptionInterrupt,
             AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        StaticMainStorageProcessor msp = getMainStorageProcessor(absoluteAddress._upi);
        if ((absoluteAddress._offset < 0) || (absoluteAddress._offset >= MAIN_STORAGE_PROCESSOR_SIZE)) {
            throw new AddressLimitsException(absoluteAddress);
        }

        return msp.getStorage(absoluteAddress._segment).getWord36(absoluteAddress._offset);
    }

    /**
     * Sets a value in main storage
     * @param absoluteAddress MSP UPI and address of the word within the MSP
     * @param value value to be stored
     * @throws AddressingExceptionInterrupt for an invalid MSP reference
     * @throws AddressLimitsException if the address is invalid
     * @throws UPIProcessorTypeException if the UPI is not for an MSP
     * @throws UPINotAssignedException if no processor exists for the given UPI
     */
    public void setStorageValue(
        final AbsoluteAddress absoluteAddress,
        final long value
    ) throws AddressingExceptionInterrupt,
             AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        StaticMainStorageProcessor msp = getMainStorageProcessor(absoluteAddress._upi);
        if ((absoluteAddress._offset < 0) || (absoluteAddress._offset >= MAIN_STORAGE_PROCESSOR_SIZE)) {
            throw new AddressLimitsException(absoluteAddress);
        }

        msp.getStorage(absoluteAddress._segment).setValue(absoluteAddress._offset, value);
    }

    /**
     * Sets a value in main storage
     * @param absoluteAddress MSP UPI and address of the word within the MSP
     * @param value value to be stored
     * @throws AddressingExceptionInterrupt for an invalid MSP reference
     * @throws AddressLimitsException if the address is invalid
     * @throws UPIProcessorTypeException if the UPI is not for an MSP
     * @throws UPINotAssignedException if no processor exists for the given UPI
     */
    public void setStorageValue(
        final AbsoluteAddress absoluteAddress,
        final Word36 value
    ) throws AddressingExceptionInterrupt,
             AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        StaticMainStorageProcessor msp = getMainStorageProcessor(absoluteAddress._upi);
        if ((absoluteAddress._offset < 0) || (absoluteAddress._offset >= MAIN_STORAGE_PROCESSOR_SIZE)) {
            throw new AddressLimitsException(absoluteAddress);
        }

        msp.getStorage(absoluteAddress._segment).setValue(absoluteAddress._offset, value.getW());
    }
}
