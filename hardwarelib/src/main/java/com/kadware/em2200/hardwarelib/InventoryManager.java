/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import java.util.HashMap;
import java.util.Map;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.misc.*;

/**
 * Creation and discardation of hardware things (i.e., anything extending Node) must occur via this manager.
 * This is a singleton.
 */
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

    final Map<Short, Processor> _processors = new HashMap<>();

    private static InventoryManager _instance = null;

    //???? Need some more general node things here
    //???? Need to decide if we do connect/disconnect here (I think we need to disconnect at least, on Node remove)


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor, instance getter, etc
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     */
    public InventoryManager(
    ) {
    }

    /**
     * Instance getter
     * <p>
     * @return
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
    //  Private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Adds an existing InstructionProcessor to our inventory.
     * The existing IP should not yet have been initialized.
     * <p>
     * @param ip
     * <p>
     * @throws NodeNameConflictException
     * @throws UPIConflictException
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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new MainStorageProcessor with a unique name and UPI.
     * <p>
     * @return
     * <p>
     * @throws MaxNodesException
     */
    public MainStorageProcessor createMainStorageProcessor(
    ) throws MaxNodesException {
        short upi = FIRST_MAIN_STORAGE_PROCESSOR_UPI;
        for (int px = 0; px < MAX_MAIN_STORAGE_PROCESSORS; ++px, ++upi) {
            if (_processors.get(upi) == null) {
                String name = String.format("MSP%d", px);
                MainStorageProcessor msp = new MainStorageProcessor(name, upi, MAIN_STORAGE_PROCESSOR_SIZE);
                _processors.put(upi, msp);
                msp.initialize();
                return msp;
            }
        }

        throw new MaxNodesException(MainStorageProcessor.class);
    }

    /**
     * Creates a new InputOutputProcessor with a unique name and UPI.
     * <p>
     * @return
     * <p>
     * @throws MaxNodesException
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
     * <p>
     * @return
     * <p>
     * @throws MaxNodesException
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
     * <p>
     * @param upi UPI of the processor to be deleted
     * <p>
     * @throws UPINotAssignedException
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
     * <p>
     * @param upi
     * <p>
     * @return
     * <p>
     * @throws UPINotAssignedException
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
     * Retrieves a specific MainStorageProcessor
     * <p>
     * @param upi
     * <p>
     * @return
     * <p>
     * @throws UPIProcessorTypeException
     * @throws UPINotAssignedException
     */
    public MainStorageProcessor getMainStorageProcessor(
        final short upi
    ) throws UPIProcessorTypeException,
             UPINotAssignedException {
        Processor processor = getProcessor(upi);
        if (processor instanceof MainStorageProcessor) {
            return (MainStorageProcessor)processor;
        } else {
            throw new UPIProcessorTypeException(upi, MainStorageProcessor.class);
        }
    }

    /**
     * Retrieves a specific InputOutputProcessor
     * <p>
     * @param upi
     * <p>
     * @return
     * <p>
     * @throws UPIProcessorTypeException
     * @throws UPINotAssignedException
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
     * <p>
     * @param upi
     * <p>
     * @return
     * <p>
     * @throws UPIProcessorTypeException
     * @throws UPINotAssignedException
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
     * <p>
     * @param absoluteAddress
     * <p>
     * @return
     * <p>
     * @throws AddressLimitsException
     * @throws UPIProcessorTypeException
     * @throws UPINotAssignedException
     */
    public long getStorageValue(
        final AbsoluteAddress absoluteAddress
    ) throws AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        MainStorageProcessor msp = getMainStorageProcessor(absoluteAddress._upi);
        if ((absoluteAddress._offset < 0) || (absoluteAddress._offset >= MAIN_STORAGE_PROCESSOR_SIZE)) {
            throw new AddressLimitsException(absoluteAddress);
        }

        return msp.getStorage().getValue(absoluteAddress._offset);
    }

    /**
     * Retrieves a particular word of storage for a given MSP, as described by an AbsoluteAddress object.
     * Since storage is kept as values and not objects, we have to create an object here.
     * <p>
     * @param absoluteAddress
     * <p>
     * @return
     * <p>
     * @throws AddressLimitsException
     * @throws UPIProcessorTypeException
     * @throws UPINotAssignedException
     */
    public Word36 getStorageWord(
        final AbsoluteAddress absoluteAddress
    ) throws AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        MainStorageProcessor msp = getMainStorageProcessor(absoluteAddress._upi);
        if ((absoluteAddress._offset < 0) || (absoluteAddress._offset >= MAIN_STORAGE_PROCESSOR_SIZE)) {
            throw new AddressLimitsException(absoluteAddress);
        }

        return msp.getStorage().getWord36(absoluteAddress._offset);
    }

    /**
     * Sets a value in main storage
     * <p>
     * @param absoluteAddress
     * @param value
     * <p>
     * @throws AddressLimitsException
     * @throws UPIProcessorTypeException
     * @throws UPINotAssignedException
     */
    public void setStorageValue(
        final AbsoluteAddress absoluteAddress,
        final long value
    ) throws AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        MainStorageProcessor msp = getMainStorageProcessor(absoluteAddress._upi);
        if ((absoluteAddress._offset < 0) || (absoluteAddress._offset >= MAIN_STORAGE_PROCESSOR_SIZE)) {
            throw new AddressLimitsException(absoluteAddress);
        }

        msp.getStorage().setValue(absoluteAddress._offset, value);
    }

    /**
     * Sets a value in main storage
     * <p>
     * @param absoluteAddress
     * @param value
     * <p>
     * @throws AddressLimitsException
     * @throws UPIProcessorTypeException
     * @throws UPINotAssignedException
     */
    public void setStorageValue(
        final AbsoluteAddress absoluteAddress,
        final Word36 value
    ) throws AddressLimitsException,
             UPIProcessorTypeException,
             UPINotAssignedException {
        MainStorageProcessor msp = getMainStorageProcessor(absoluteAddress._upi);
        if ((absoluteAddress._offset < 0) || (absoluteAddress._offset >= MAIN_STORAGE_PROCESSOR_SIZE)) {
            throw new AddressLimitsException(absoluteAddress);
        }

        msp.getStorage().setValue(absoluteAddress._offset, value.getW());
    }
}
