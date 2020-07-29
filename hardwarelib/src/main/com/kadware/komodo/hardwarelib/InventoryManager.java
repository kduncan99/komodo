/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Credentials;
import com.kadware.komodo.baselib.exceptions.NotFoundException;
import com.kadware.komodo.configlib.HardwareConfiguration;
import com.kadware.komodo.configlib.ProcessorDefinition;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.EntryMessage;

/**
 * Creation and discarding of hardware things (i.e., anything extending Node) must occur via this manager.
 * This is a singleton.
 *
 * A note about UPI communication (sends and acks):
 * This process is used primarily (maybe solely) for starting IOs on the IOPs, and for the IOPs to
 * indicate when an IO has completed. Nothing else. In order for this to work, a buffer address must be
 * passed to the IOP, and that address is passed through a mail slot, which is a small packet unique
 * for each combination of sender/receiver.
 *
 * Now, this address really only has to indicate *some* area of memory; the IOP will presume it is a
 * buffer of long[]s, but it could be any buffer anywhere, insofar as the IOP is concerned, and so far
 * as the SP and the IPs (which are the only units which initiate IO) are concerned.
 *
 * But there is a problem - in a properly functioning system, IOs are instigated by operating system
 * software, and that software must live within a world of virtual addresses which are translatable
 * to absolute addresses - and absolute addresses always (presumably) translate to segments and
 * offsets within one MSP or another.
 *
 * Which means, the IO buffer must be presented as an absolute address, which is the only thing which
 * is comprehensible to all processors as well as to the operating system.
 *
 * But... it gets very tricky dealing with mail slot management during configuration management.
 * Processors come and go, and potentially while the OS is running. This is manageable... but it gets
 * even more problematic when MSPs come and go - particularly whichever MSP contains the table of
 * mail slots.
 *
 * A similar problem exists with the configuration databank.
 *
 * So, the solution? A 'hidden' MSP, accessible via absolute addresses with UPI and segment index
 * set to zero. This MSP contains the configuration databank, and we will implement the mail slot
 * table within the configuration databank.
 *
 * Any processor can access the configuration databank via the absolute address with UPI 0,
 * segment index 0, at offset 0, and from there, derive the location of any particular mail slot.
 * Similarly, the operating system can base that bank, again using the indicated absolute address,
 * and get all the configuration information *and* the mail slot locations there-by.
 *
 * The actual format of the configuration data bank is defined in ConfigDataBank.java.
 */
@SuppressWarnings("Duplicates")
public class InventoryManager {

    public static class Counters {
        public final int _inputOutputProcessors;
        public final int _instructionProcessors;
        public final int _mainStorageProcessors;
        public final int _systemProcessors;

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

//    /**
//     * Simple pair of UPI indices -
//     * one representing a source for a SEND or ACK, the other representing the destination.
//     */
//    static class UPIIndexPair {
//        final int _sourceIndex;
//        final int _destinationIndex;
//
//        UPIIndexPair(
//            final int sourceIndex,
//            final int destinationIndex
//        ) {
//            _sourceIndex = sourceIndex;
//            _destinationIndex = destinationIndex;
//        }
//
//        @Override
//        public boolean equals(
//            final Object obj
//        ) {
//            return (obj instanceof UPIIndexPair)
//                   && (((UPIIndexPair) obj)._sourceIndex == _sourceIndex)
//                   && (((UPIIndexPair) obj)._destinationIndex == _destinationIndex);
//        }
//
//        @Override
//        public int hashCode() {
//            return (_sourceIndex << 16) | _destinationIndex;
//        }
//    }


    //  The following are only useful for the create* routines.
    //  It is highly recommended that these be used for creation of processors, however that is not enforced.
    //  Client code can create any type of processor at any UPI index - we only enforce uniqueness of UPI index and name.
    public final static int MAX_IOPS = 2;
    public final static int MAX_IPS = 8;
    public final static int MAX_MSPS = 4;
    public final static int MAX_SPS = 1;

    public final static int FIRST_SP_UPI_INDEX = 0;
    public final static int FIRST_MSP_UPI_INDEX = FIRST_SP_UPI_INDEX + MAX_SPS;     //  currently 1
    public final static int FIRST_IOP_UPI_INDEX = FIRST_MSP_UPI_INDEX + MAX_MSPS;   //  currently 5
    public final static int FIRST_IP_UPI_INDEX = FIRST_IOP_UPI_INDEX + MAX_IOPS;    //  currently 7

    public final static int LAST_SP_UPI_INDEX = FIRST_SP_UPI_INDEX + MAX_SPS - 1;
    public final static int LAST_MSP_UPI_INDEX = FIRST_MSP_UPI_INDEX + MAX_MSPS - 1;
    public final static int LAST_IOP_UPI_INDEX = FIRST_IOP_UPI_INDEX + MAX_IOPS - 1;
    public final static int LAST_IP_UPI_INDEX = FIRST_IP_UPI_INDEX + MAX_IPS - 1;

    private final Map<String, Node> _nodes = new HashMap<>();
    private final Map<Integer, Processor> _processors = new HashMap<>();

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
    //  Private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For non-Processor Nodes
     * @param node node being created or added
     */
    private void putNode(
        final Node node
    ) {
        _nodes.put(node._name.toUpperCase(), node);
    }

    /**
     * For processor nodes
     * @param processor processor being added
     */
    private void putProcessor(
        final Processor processor
    ) {
        putNode(processor);
        _processors.put(processor._upiIndex, processor);
        LOGGER.info(String.format("Processor %s inserted at upi %d", processor._name, processor._upiIndex));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Adds an existing InputOutputProcessor to our inventory.
     * The existing IOP should not yet have been initialized.
     * Only for unit tests.
     * @param processor object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addInputOutputProcessor(
        final InputOutputProcessor processor
    ) throws NodeNameConflictException,
             UPIConflictException,
             UPIInvalidException {
        EntryMessage em = LOGGER.traceEntry("name:{} upi:{}", processor._name, processor._upiIndex);

        synchronized (this) {
            int upix = processor._upiIndex;
            if ((upix < FIRST_IOP_UPI_INDEX) || (upix > LAST_IOP_UPI_INDEX)) {
                throw new UPIInvalidException(upix);
            }

            if (_processors.containsKey(upix)) {
                throw new UPIConflictException(upix);
            }

            if (_nodes.containsKey(processor._name.toUpperCase())) {
                throw new NodeNameConflictException(processor._name);
            }

            putProcessor(processor);
            processor.initialize();
        }

        LOGGER.traceExit(em);
    }

    /**
     * Adds an existing InstructionProcessor to our inventory.
     * The existing IP should not yet have been initialized.
     * Only for unit tests.
     * @param processor object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addInstructionProcessor(
        final InstructionProcessor processor
    ) throws NodeNameConflictException,
             UPIConflictException,
             UPIInvalidException {
        EntryMessage em = LOGGER.traceEntry("name:{} upi:{}", processor._name, processor._upiIndex);

        synchronized (this) {
            int upix = processor._upiIndex;
            if ((upix < FIRST_IP_UPI_INDEX) || (upix > LAST_IP_UPI_INDEX)) {
                throw new UPIInvalidException(upix);
            }

            if (_processors.containsKey(upix)) {
                throw new UPIConflictException(upix);
            }

            if (_nodes.containsKey(processor._name.toUpperCase())) {
                throw new NodeNameConflictException(processor._name);
            }

            putProcessor(processor);
            processor.initialize();
        }

        LOGGER.traceExit(em);
    }

    /**
     * Adds an existing MainStorageProcessor to our inventory.
     * The existing MSP should not yet have been initialized.
     * Only for unit tests.
     * @param processor object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addMainStorageProcessor(
        final MainStorageProcessor processor
    ) throws AddressingExceptionInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPIInvalidException {
        EntryMessage em = LOGGER.traceEntry("name:{} upi:{}", processor._name, processor._upiIndex);

        synchronized (this) {
            int upix = processor._upiIndex;
            if ((upix < FIRST_MSP_UPI_INDEX) || (upix > LAST_MSP_UPI_INDEX)) {
                throw new UPIInvalidException(upix);
            }

            if (_processors.containsKey(upix)) {
                throw new UPIConflictException(upix);
            }

            if (_nodes.containsKey(processor._name.toUpperCase())) {
                throw new NodeNameConflictException(processor._name);
            }

            putProcessor(processor);
            processor.initialize();
        }

        LOGGER.traceExit(em);
    }

    /**
     * Adds an already-created device to our configuration
     */
    public void addDevice(
        final Device device
    ) {
        putNode(device);
    }

    /**
     * Adds an existing SystemProcessor to our inventory.
     * The existing SP should not yet have been initialized.
     * Only for unit tests.
     * @param processor object to be added
     * @throws NodeNameConflictException if there is a conflict in node names
     * @throws UPIConflictException if there is a conflict in UPI
     */
    public void addSystemProcessor(
        final SystemProcessor processor
    ) throws NodeNameConflictException,
             UPIConflictException,
             UPIInvalidException {
        EntryMessage em = LOGGER.traceEntry("name:{} upi:{}", processor._name, processor._upiIndex);

        synchronized (this) {
            int upix = processor._upiIndex;
            if ((upix < FIRST_SP_UPI_INDEX) || (upix > LAST_SP_UPI_INDEX)) {
                throw new UPIInvalidException(upix);
            }

            if (_processors.containsKey(upix)) {
                throw new UPIConflictException(upix);
            }

            if (_nodes.containsKey(processor._name.toUpperCase())) {
                throw new NodeNameConflictException(processor._name);
            }

            putProcessor(processor);
            processor.initialize();
        }

        LOGGER.traceExit(em);
    }

    /**
     * Clears the configuration cleanly
     */
    public void clearConfiguration() {
        EntryMessage em = LOGGER.traceEntry();

        synchronized (this) {
            for (Node node: new HashSet<>(_nodes.values())) {
                if (node instanceof Device) {
                    deleteNode(node);
                }
            }

            for (Node node: new HashSet<>(_nodes.values())) {
                if (node instanceof ChannelModule) {
                    deleteNode(node);
                }
            }

            for (Node node : new HashSet<>(_nodes.values())) {
                deleteNode(node);
            }
        }

        LOGGER.traceExit(em);
    }

    /**
     * Creates a new channel module and attaches it to the given iop at the given address
     */
    public ChannelModule createChannelModule(
        final ChannelModule.ChannelModuleType type,
        final String name,
        final InputOutputProcessor iop,
        final int cmIndex
    ) throws CannotConnectException,
             ChannelModuleIndexConflictException,
             NodeNameConflictException {
        EntryMessage em = LOGGER.traceEntry("type:{} name:{} iop:{} cmIndex:{}", type, name, iop._name, cmIndex);

        ChannelModule chmod = null;
        synchronized (this) {
            if (_nodes.containsKey(name.toUpperCase())) {
                throw new NodeNameConflictException(name);
            }

            if (iop._descendants.containsKey(cmIndex)) {
                throw new ChannelModuleIndexConflictException(cmIndex);
            }

            chmod = switch (type) {
                case Byte -> new ByteChannelModule(name);
                case Word -> new WordChannelModule(name);
            };

            putNode(chmod);
            Node.connect(iop, cmIndex, chmod);
        }

        LOGGER.traceExit(em, chmod);
        return chmod;
    }

    /**
     * Creates a new InputOutputProcessor with a unique name and UPI.
     * @return new processor object
     * @throws MaxNodesException if too many processors of this type have been created
     */
    public InputOutputProcessor createInputOutputProcessor(
        final String name
    ) throws MaxNodesException {
        EntryMessage em = LOGGER.traceEntry("name:{}", name);

        int upiIndex = FIRST_IOP_UPI_INDEX;
        synchronized (this) {
            for (int px = 0; px < MAX_IOPS; ++px, ++upiIndex) {
                if (_processors.get(upiIndex) == null) {
                    InputOutputProcessor proc = new InputOutputProcessor(name, upiIndex);
                    putProcessor(proc);
                    proc.initialize();
                    LOGGER.traceExit(em, proc);
                    return proc;
                }
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
        EntryMessage em = LOGGER.traceEntry("name:{}", name);

        int upiIndex = FIRST_IP_UPI_INDEX;
        synchronized (this) {
            for (int px = 0; px < MAX_IPS; ++px, ++upiIndex) {
                if (_processors.get(upiIndex) == null) {
                    InstructionProcessor proc = new InstructionProcessor(name, upiIndex);
                    putProcessor(proc);
                    proc.initialize();
                    LOGGER.traceExit(em, proc);
                    return proc;
                }
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
        EntryMessage em = LOGGER.traceEntry("name:{}", name);

        int upiIndex = FIRST_MSP_UPI_INDEX;
        synchronized (this) {
            for (int px = 0; px < MAX_MSPS; ++px, ++upiIndex) {
                if (_processors.get(upiIndex) == null) {
                    MainStorageProcessor proc = new MainStorageProcessor(name, upiIndex, fixedStorageSize);
                    putProcessor(proc);
                    proc.initialize();
                    LOGGER.traceExit(em, proc);
                    return proc;
                }
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
    @SuppressWarnings("UnusedReturnValue")
    public SystemProcessor createSystemProcessor(
        final String name,
        final Integer httpPort,
        final Integer httpsPort,
        final Credentials credentials
    ) throws MaxNodesException {
        EntryMessage em = LOGGER.traceEntry("name:{}", name);

        int upiIndex = FIRST_SP_UPI_INDEX;
        synchronized (this) {
            for (int px = 0; px < MAX_SPS; ++px, ++upiIndex) {
                if (_processors.get(upiIndex) == null) {
                    SystemProcessor proc = new SystemProcessor(name, httpPort, httpsPort, credentials);
                    putProcessor(proc);
                    proc.initialize();
                    LOGGER.traceExit(em, proc);
                    return proc;
                }
            }
        }

        throw new MaxNodesException(SystemProcessor.class);
    }

    /**
     * Deletes a particular node from our inventory
     * @param name name of the node to be deleted
     * @throws NotFoundException if a node with the given name is not in our inventory
     */
    public void deleteNode(
        final String name
    ) throws NotFoundException {
        EntryMessage em = LOGGER.traceEntry("name:{}", name);
        Node node = _nodes.get(name.toUpperCase());
        if (node == null) {
            throw new NotFoundException(name);
        }
        deleteNode(node);
        LOGGER.traceExit(em);
    }

    /**
     * Deletes a particular node from our inventory
     * @param node node to be deleted
     */
    public void deleteNode(
        final Node node
    ) {
        EntryMessage em = LOGGER.traceEntry("name:{}", node._name);
        if (node instanceof Processor) {
            Processor proc = (Processor) node;
            if (proc instanceof InstructionProcessor) {
                InstructionProcessor ip = (InstructionProcessor) proc;
                ip.stop(InstructionProcessor.StopReason.Cleared, 0);
                while (!ip.isStopped()) {
                    Thread.onSpinWait();
                }
            }
            node.terminate();
            _processors.remove(proc._upiIndex);
        } else {
            node.terminate();
        }
        _nodes.remove(node._name.toUpperCase());

        LOGGER.traceExit(em);
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
            for (Node node : _nodes.values()) {
                if (node instanceof ChannelModule) {
                    node.dump(writer);
                }
            }

            writer.write("Devices ------------------------------------\n");
            for (Node node : _nodes.values()) {
                if (node instanceof Device) {
                    node.dump(writer);
                }
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
    public InstructionProcessor getInstructionProcessor(
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
    public MainStorageProcessor getMainStorageProcessor(
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
