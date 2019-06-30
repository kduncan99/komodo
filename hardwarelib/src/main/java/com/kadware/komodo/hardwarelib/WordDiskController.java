/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.io.BufferedWriter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Models a disk controller with a word interface
 */
public class WordDiskController extends Controller {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(WordDiskController.class);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public WordDiskController(
        final String name,
        final short subsystemIdentifier
    ) {
        super(Controller.ControllerType.WordDisk, name, subsystemIdentifier);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  canConnect()
    //
    //  Checks to see whether this node is a valid descendant of the candidate node
    @Override
    public boolean canConnect(
        final Node candidateAncestor
    ) {
        //  We can connect only to WordChannelModules
        return false;//????return candidateAncestor instanceof SoftwareWordChannelModule;
    }

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
    }

    /**
     * NOP override
     */
    @Override
    public void initialize() {}

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * <p>
     * @param source the Node which is signalling us
     */
    @Override
    public void signal(
        final Node source
    ) {
        //  nothing to do
    }

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public void terminate() {}


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

}
