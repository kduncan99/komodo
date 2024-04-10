/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Credentials;
import com.kadware.komodo.configlib.HardwareConfiguration;
import com.kadware.komodo.configlib.ProcessorDefinition;
import com.kadware.komodo.hardwarelib.exceptions.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.EntryMessage;

/**
 * Creation and discarding of hardware things (i.e., anything extending Node) must occur via this manager.
 * This is a singleton.
 */
@SuppressWarnings("Duplicates")
public class InventoryManager {

    public static class Counters {
        final int _inputOutputProcessors;
        final int _instructionProcessors;
        final int _mainStorageProcessors;
        final int _systemProcessors;

        Counters(
            final int inputOutputProcessors,
            final int instructionProcessors,
            final int mainStorageProcessors,
            final int systemProcessors
        ) {
            _inputOutputProcessors = inputOutputProcessors;
            _instructionProcessors = instructionProcessors;
            _mainStorageProcessors = mainStorageProcessors;
            _systemProcessors = systemProcessors;
        }
    }

    //  The following are only useful for the create* routines.
    //  It is highly recommended that these be used for creation of processors, however that is not enforced.
    //  Client code can create any type of processor at any UPI index - we only enforce uniqueness of UPI index and name.
    private final static int MAX_INPUT_OUTPUT_PROCESSORS = 2;
    private final static int MAX_INSTRUCTION_PROCESSORS = 8;
    private final static int MAX_MAIN_STORAGE_PROCESSORS = 2;
    private final static int MAX_SYSTEM_PROCESSORS = 1;

    final static int FIRST_SYSTEM_PROCESSOR_UPI_INDEX = 0;
    final static int FIRST_MAIN_STORAGE_PROCESSOR_UPI_INDEX = FIRST_SYSTEM_PROCESSOR_UPI_INDEX + MAX_SYSTEM_PROCESSORS;
    final static int FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX = FIRST_MAIN_STORAGE_PROCESSOR_UPI_INDEX + MAX_MAIN_STORAGE_PROCESSORS;
    public final static int FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX = FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX + MAX_INPUT_OUTPUT_PROCESSORS;

    private final Map<Integer, Processor> _processors = new HashMap<>();
    private final List<ChannelModule> _channelModules = new LinkedList<>();
    private final List<Device> _devices = new LinkedList<>();

    private static InventoryManager _instance = null;

    private static final Logger LOGGER = LogManager.getLogger(InventoryManager.class.getSimpleName());


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
     * Adds an existing InputOutputProcessor to our inventory.
     * The existing IOP should not yet have been initialized.
     * Only for unit tests.
     * @param iop object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addInputOutputProcessor(
        final InputOutputProcessor iop
    ) throws NodeNameConflictException,
             UPIConflictException {
        if (_processors.get(iop._upiIndex) != null) {
            throw new UPIConflictException(iop._upiIndex);
        }

        for (Processor processor : _processors.values()) {
            if (iop._name.equalsIgnoreCase(processor._name)) {
                throw new NodeNameConflictException(iop._name);
            }
        }

        _processors.put(iop._upiIndex, iop);
        iop.initialize();
    }

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
        if (_processors.get(ip._upiIndex) != null) {
            throw new UPIConflictException(ip._upiIndex);
        }

        for (Processor processor : _processors.values()) {
            if (ip._name.equalsIgnoreCase(processor._name)) {
                throw new NodeNameConflictException(ip._name);
            }
        }

        _processors.put(ip._upiIndex, ip);
        ip.initialize();
    }

    /**
     * Adds an existing MainStorageProcessor to our inventory.
     * The existing MSP should not yet have been initialized.
     * Only for unit tests.
     * @param msp object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addMainStorageProcessor(
        final MainStorageProcessor msp
    ) throws NodeNameConflictException,
             UPIConflictException {
        if (_processors.get(msp._upiIndex) != null) {
            throw new UPIConflictException(msp._upiIndex);
        }

        for (Processor processor : _processors.values()) {
            if (msp._name.equalsIgnoreCase(processor._name)) {
                throw new NodeNameConflictException(msp._name);
            }
        }

        _processors.put(msp._upiIndex, msp);
        msp.initialize();
    }

    /**
     * Adds an existing SystemProcessor to our inventory.
     * The existing SP should not yet have been initialized.
     * Only for unit tests.
     * @param sp object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addSystemProcessor(
        final SystemProcessor sp
    ) throws NodeNameConflictException,
             UPIConflictException {
        if (_processors.get(sp._upiIndex) != null) {
            throw new UPIConflictException(sp._upiIndex);
        }

        for (Processor processor : _processors.values()) {
            if (sp._name.equalsIgnoreCase(processor._name)) {
                throw new NodeNameConflictException(sp._name);
            }
        }

        _processors.put(sp._upiIndex, sp);
        sp.initialize();
    }

    /**
     * Clears the configuration cleanly
     */
    public void clearConfiguration() {
        EntryMessage em = LOGGER.traceEntry("clearConfiguration()");
        for (Device device : _devices) {
            for (Node ancestor : device._ancestors) {
                Node.disconnect(ancestor, device);
            }

            device.clear();
            device.terminate();
        }
        _devices.clear();

        for (ChannelModule chmod : _channelModules) {
            for (Node ancestor : chmod._ancestors) {
                Node.disconnect(ancestor, chmod);
            }

            chmod.clear();
            chmod.terminate();
        }
        _channelModules.clear();

        for (Processor proc : _processors.values()) {
            proc.terminate();
        }
        _processors.clear();

        LOGGER.traceExit(em);
    }

    /**
     * Creates a new InputOutputProcessor with a unique name and UPI.
     * @return new processor object
     * @throws MaxNodesException if too many processors of this type have been created
     */
    public InputOutputProcessor createInputOutputProcessor(
        final String name
    ) throws MaxNodesException {
        int upiIndex = FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX;
        for (int px = 0; px < MAX_INPUT_OUTPUT_PROCESSORS; ++px, ++upiIndex) {
            if (_processors.get(upiIndex) == null) {
                InputOutputProcessor iop = new InputOutputProcessor(name, upiIndex);
                _processors.put(upiIndex, iop);
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
        final String name
    ) throws MaxNodesException {
        int upiIndex = FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX;
        for (int px = 0; px < MAX_INSTRUCTION_PROCESSORS; ++px, ++upiIndex) {
            if (_processors.get(upiIndex) == null) {
                InstructionProcessor ip = new InstructionProcessor(name, upiIndex);
                _processors.put(upiIndex, ip);
                ip.initialize();
                return ip;
            }
        }

        throw new MaxNodesException(InstructionProcessor.class);
    }

    /**
     * Creates a new MainStorageProcessor with a unique name and UPI.
     * @param name processor name
     * @param fixedStorageSize size, in words, of the fixed storage portion of the MSP
     * @return new processor object
     * @throws MaxNodesException if too many processors of this type have been created
     */
    public MainStorageProcessor createMainStorageProcessor(
        final String name,
        final int fixedStorageSize
    ) throws MaxNodesException {
        int upiIndex = FIRST_MAIN_STORAGE_PROCESSOR_UPI_INDEX;
        for (int px = 0; px < MAX_MAIN_STORAGE_PROCESSORS; ++px, ++upiIndex) {
            if (_processors.get(upiIndex) == null) {
                MainStorageProcessor msp = new MainStorageProcessor(name, upiIndex, fixedStorageSize);
                _processors.put(upiIndex, msp);
                msp.initialize();
                return msp;
            }
        }

        throw new MaxNodesException(MainStorageProcessor.class);
    }

    /**
     * Creates a new SystemProcessor with a unique name and UPI.
     * For SystemProcessors which have an HTTPSystemProcessorInterface (currently, that's all we have)
     * @param name name of the system processor
     * @param httpPort port on which to listen for nonsecure HTTP - null to disable
     * @param httpsPort port on which to listen for secure HTTP - null to disable
     * @return new processor object
     * @throws MaxNodesException if too many processors of this type have been created
     */
    public SystemProcessor createSystemProcessor(
        final String name,
        final Integer httpPort,
        final Integer httpsPort,
        final Credentials credentials
    ) throws MaxNodesException {
        int upiIndex = FIRST_SYSTEM_PROCESSOR_UPI_INDEX;
        for (int px = 0; px < MAX_SYSTEM_PROCESSORS; ++px, ++upiIndex) {
            if (_processors.get(upiIndex) == null) {
                SystemProcessor sp = new SystemProcessor(name, httpPort, httpsPort, credentials);
                _processors.put(upiIndex, sp);
                sp.initialize();
                return sp;
            }
        }

        throw new MaxNodesException(SystemProcessor.class);
    }

    /**
     * Deletes a particular processor from our inventory
     * @param upiIndex UPI of the processor to be deleted
     * @throws UPINotAssignedException if no processor can be found
     */
    public void deleteProcessor(
        final int upiIndex
    ) throws UPINotAssignedException {
        Processor processor = _processors.get(upiIndex);
        if (processor == null) {
            throw new UPINotAssignedException(upiIndex);
        }

        processor.terminate();
        _processors.remove(upiIndex);
    }

    /**
     * Generates a full hardware dump to the given output stream
     */
    public void dump(
        final BufferedWriter writer
    ) {
        try {
            writer.write(String.format("Komodo System Hardware Dump - %s\n", Instant.now().toString()));
            writer.write("Processors ---------------------------------\n");
            for (Processor p : _processors.values()) {
                p.dump(writer);
            }

            writer.write("Channel Modules ----------------------------\n");
            for (ChannelModule cm : _channelModules) {
                cm.dump(writer);
            }

            writer.write("Devices ------------------------------------\n");
            for (Device d : _devices) {
                d.dump(writer);
            }
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Tally number of each type of processor in the current configuration
     * @return Counters object
     */
    public Counters getCounters(
    ) {
        int iops = 0;
        int ips = 0;
        int msps = 0;
        int sps = 0;

        for (Processor processor : _processors.values()) {
            switch (processor._Type) {
                case InputOutputProcessor -> iops++;
                case InstructionProcessor -> ips++;
                case MainStorageProcessor -> msps++;
                case SystemProcessor -> sps++;
            }
        }

        return new Counters(iops, ips, msps, sps);
    }

    /**
     * Retrieves a specific InputOutputProcessor
     * @param upiIndex UPI of processor of interest
     * @return processor of interest
     * @throws UPIProcessorTypeException if the UPI correspond to a processor not of the expected type
     * @throws UPINotAssignedException if no processor can be found
     */
    public InputOutputProcessor getInputOutputProcessor(
        final int upiIndex
    ) throws UPIProcessorTypeException,
             UPINotAssignedException {
        Processor processor = getProcessor(upiIndex);
        if (processor instanceof InputOutputProcessor) {
            return (InputOutputProcessor) processor;
        } else {
            throw new UPIProcessorTypeException(upiIndex, InputOutputProcessor.class);
        }
    }

    /**
     * Retrieves a list of all currently-configured Input-Output Processors
     */
    List<InputOutputProcessor> getInputOutputProcessors() {
        List<InputOutputProcessor> result = new LinkedList<>();
        for (Processor processor : _processors.values()) {
            if (processor._Type == Processor.ProcessorType.InputOutputProcessor) {
                result.add((InputOutputProcessor) processor);
            }
        }
        return result;
    }

    /**
     * Retrieves a specific InstructionProcessor
     * @param upiIndex UPI of processor of interest
     * @return processor of interest
     * @throws UPIProcessorTypeException if the UPI correspond to a processor not of the expected type
     * @throws UPINotAssignedException if no processor can be found
     */
    InstructionProcessor getInstructionProcessor(
        final int upiIndex
    ) throws UPIProcessorTypeException,
             UPINotAssignedException {
        Processor processor = getProcessor(upiIndex);
        if (processor instanceof InstructionProcessor) {
            return (InstructionProcessor) processor;
        } else {
            throw new UPIProcessorTypeException(upiIndex, InstructionProcessor.class);
        }
    }

    /**
     * Retrieves a list of all currently-configured Instruction Processors
     */
    List<InstructionProcessor> getInstructionProcessors() {
        List<InstructionProcessor> result = new LinkedList<>();
        for (Processor processor : _processors.values()) {
            if (processor._Type == Processor.ProcessorType.InstructionProcessor) {
                result.add((InstructionProcessor) processor);
            }
        }
        return result;
    }

    /**
     * Retrieves a specific MainStorageProcessor
     * @param upiIndex UPI of processor of interest
     * @return processor of interest
     * @throws UPIProcessorTypeException if the UPI correspond to a processor not of the expected type
     * @throws UPINotAssignedException if no processor can be found
     */
    MainStorageProcessor getMainStorageProcessor(
        final int upiIndex
    ) throws UPIProcessorTypeException,
             UPINotAssignedException {
        Processor processor = getProcessor(upiIndex);
        if (processor instanceof MainStorageProcessor) {
            return (MainStorageProcessor) processor;
        } else {
            throw new UPIProcessorTypeException(upiIndex, MainStorageProcessor.class);
        }
    }

    /**
     * Retrieves a list of all currently-configured Main Storage Processors
     */
    List<MainStorageProcessor> getMainStorageProcessors() {
        List<MainStorageProcessor> result = new LinkedList<>();
        for (Processor processor : _processors.values()) {
            if (processor._Type == Processor.ProcessorType.MainStorageProcessor) {
                result.add((MainStorageProcessor) processor);
            }
        }
        return result;
    }

    /**
     * Retrieves a particular processor given its UPI
     * @param upiIndex UPI of processor of interest
     * @return processor of interest
     * @throws UPINotAssignedException if no processor can be found
     */
    Processor getProcessor(
        final int upiIndex
    ) throws UPINotAssignedException {
        Processor processor = _processors.get(upiIndex);
        if (processor == null) {
            throw new UPINotAssignedException(upiIndex);
        }
        return processor;
    }

    /**
     * Retrieves a list of all currently-configured Processors
     */
    List<Processor> getProcessors() {
        return new LinkedList<>(_processors.values());
    }

    /**
     * Retrieves a specific SystemProcessor
     * @param upiIndex UPI of processor of interest
     * @return processor of interest
     * @throws UPIProcessorTypeException if the UPI correspond to a processor not of the expected type
     * @throws UPINotAssignedException if no processor can be found
     */
    public SystemProcessor getSystemProcessor(
        final int upiIndex
    ) throws UPIProcessorTypeException,
             UPINotAssignedException {
        Processor processor = getProcessor(upiIndex);
        if (processor instanceof SystemProcessor) {
            return (SystemProcessor) processor;
        } else {
            throw new UPIProcessorTypeException(upiIndex, InstructionProcessor.class);
        }
    }

    /**
     * Retrieves a list of all currently-configured System Processors
     */
    List<SystemProcessor> getSystemProcessors() {
        List<SystemProcessor> result = new LinkedList<>();
        for (Processor processor : _processors.values()) {
            if (processor._Type == Processor.ProcessorType.SystemProcessor) {
                result.add((SystemProcessor) processor);
            }
        }
        return result;
    }

    /**
     * Loads the configuration from a HardwareConfiguration object
     * @param config hardware configuration object
     * @throws MaxNodesException if there are too many nodes of any particular category
     */
    public void importConfiguration(
        final HardwareConfiguration config
    ) throws MaxNodesException {
        clearConfiguration();
        for (ProcessorDefinition pd : config._processorDefinitions) {
            Processor.ProcessorType ptype = Processor.ProcessorType.valueOf(pd._processorType);
            switch (ptype) {
                case MainStorageProcessor -> createMainStorageProcessor(pd._nodeName, pd._fixedStorageSize);
                case InputOutputProcessor -> createInputOutputProcessor(pd._nodeName);
                case InstructionProcessor -> createInstructionProcessor(pd._nodeName);
                case SystemProcessor -> createSystemProcessor(pd._nodeName, pd._httpPort, pd._httpsPort, pd._adminCredentials);
            }
        }
    }
}
