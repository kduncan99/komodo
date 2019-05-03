/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class which models a Procesor node
 */
@SuppressWarnings("Duplicates")
public abstract class Processor extends Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates the type of the processor
     */
    public static enum ProcessorType {
        None(0),
        InstructionProcessor(1),
        InputOutputProcessor(2),
        MainStorageProcessor(3);

        /**
         * Unique code for this particular ProcessorType
         */
        private final int _code;

        /**
         * Constructor
         * @param code code associated with the type
         */
        ProcessorType(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this ProcessorType
         * @return value
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a ProcessorType
         * @param code code of interest
         * @return the value
         */
        public static ProcessorType getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return InstructionProcessor;
                case 2:     return InputOutputProcessor;
                case 3:     return MainStorageProcessor;
                default:    return None;
            }
        }
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(Processor.class);

    private final ProcessorType _processorType;

    /**
     * Uniquely identifies each processor in the configuration, in a manner transparent to the configurating entity.
     * Our own requirement is that the UPI must be greater than zero, and less than or equal to 15.
     * This is due to absolute address resolution for MSPs.
     */
    private final short _upi;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public Processor(
        final ProcessorType processorType,
        final String name,
        final short upi
    ) {
        super(Node.Category.Processor, name);
        _processorType = processorType;
        _upi = upi;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * @return value
     */
    public ProcessorType getProcessorType(
    ) {
        return _processorType;
    }

    /**
     * Getter
     * @return value
     */
    public short getUPI(
    ) {
        return _upi;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes this Processor if/as necessary
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * @param source the Node which is signalling us
     */
    @Override
    public abstract void signal(
        final Node source
    );

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public abstract void terminate();


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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

}
