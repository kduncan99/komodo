/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.kadware.em2200.baselib.types.Identifier;

/**
 * Base class which models a Controller node
 */
public abstract class Controller extends Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates the type of the controller
     */
    public static enum ControllerType {
        None(0),
        ByteDisk(1),        //  Disk Controller for byte interfaces
        Symbiont(2),        //  Symbiont Device or Controller
        Tape(3),            //  Tape Device or Controller
        WordDisk(4);        //  Disk controller for word interfaces

        /**
         * Unique code for this particular ControllerType
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        ControllerType(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this ControllerType
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a ControllerType
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static ControllerType getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return ByteDisk;
                case 2:     return Symbiont;
                case 3:     return Tape;
                case 4:     return WordDisk;
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

    private static final Logger LOGGER = LogManager.getLogger(Controller.class);

    private final ControllerType _controllerType;

    /**
     * Ties all devices and controllers in a logical subsystem together.
     * Used *very sparingly* by the Exec; to be established and verified by the wrapping executable (e.g., emssp).
     */
    private final short _subsystemIdentifier;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public Controller(
        final ControllerType controllerType,
        final String name,
        final short subsystemIdentifier
    ) {
        super(Node.Category.Controller, name);
        _controllerType = controllerType;
        _subsystemIdentifier = subsystemIdentifier;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public ControllerType getControllerType(
    ) {
        return _controllerType;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public short getSubsystemIdentifier(
    ) {
        return _subsystemIdentifier;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes this Controller if/as necessary
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * <p>
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
     * <p>
     * @param writer
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Type:%s  SubSysId:%s\n",
                                       _controllerType.toString(),
                                       _subsystemIdentifier));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Routes an IO described by the given IOInfo object to the child node identified by the node address.
     * <p>
     * @param nodeAddress
     * @param ioInfo
     */
    public void routeIo(
        final int nodeAddress,
        final Device.IOInfo ioInfo
    ) {
        Node deviceNode = _descendants.get(nodeAddress);
        if (deviceNode == null) {
            ioInfo.setStatus(Device.IOStatus.InvalidDeviceAddress);
            if (ioInfo.getSource() != null) {
                ioInfo.getSource().signal(this);
            }
        } else {
            ioInfo.setStatus(Device.IOStatus.InProgress);
            ((Device)deviceNode).handleIo(ioInfo);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

}
