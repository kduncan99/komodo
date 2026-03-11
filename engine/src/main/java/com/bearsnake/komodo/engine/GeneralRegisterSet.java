/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.engine.exceptions.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Represents the General Register Set (GRS) for an Instruction Processor
 * The GRS is a table of 128 general registers.
 */
public class GeneralRegisterSet {

    private static final Logger LOGGER = LogManager.getLogger(GeneralRegisterSet.class);

    private final long[] _registers = new long[128];

    /**
     * Given a string, we return the GRS address of the indicated register if it is in fact a register name
     * @param registerName for which we search
     * @return GRS address of the register
     * @throws NotFoundException if the given name isn't a register name
     */
    public static int getGRSIndex(
        final String registerName
    ) throws NotFoundException {
        for (int rx = 0; rx < Constants.GRS_REGISTER_NAMES.length; rx++) {
            if (registerName.equals(Constants.GRS_REGISTER_NAMES[rx] )) {
                return rx;
            }
        }

        throw new NotFoundException(registerName);
    }

    /**
     * Retrieves a reference to a particular GeneralRegister object according to the given register index
     * @param registerIndex index of the requested register
     * @return reference as indicated above
     */
    public long getRegisterValue(
        final int registerIndex
    ) {
        if ((registerIndex < 0) || (registerIndex >= 128)) {
            throw new RuntimeException(String.format("registerIndex=%d", registerIndex));
        }

        return _registers[registerIndex];
    }

    /**
     * Sets the value of a particular GeneralRegister to the value of the provided GeneralRegister value parameter
     * @param registerIndex indicates which GR is to be set
     * @param value contains the value to which the GR is to be set
     */
    public void setRegister(
        final int registerIndex,
        final long value
    ) {
        if ((registerIndex < 0) || (registerIndex >= 128)) {
            throw new RuntimeException(String.format("registerIndex=%d", registerIndex));
        }

        _registers[registerIndex] = value;
    }

    /**
     * Indicates whether the requested access is allowed for a particular register
     * @param registerIndex Indicates the register of interest
     * @param processorPrivilege Indicates the processor privilege for which the access is being made
     * @param writeAccess If true, indicates write access - otherwise, read access
     * @return true if access is allowed, else false
     */
    public static boolean isAccessAllowed(
        final int registerIndex,
        final int processorPrivilege,
        final boolean writeAccess
    ) {
        if (registerIndex < 040) {
            return true;
        } else if (registerIndex < 0100) {
            return false;
        } else if (registerIndex < 0120) {
            return true;
        } else {
            return ((writeAccess && (processorPrivilege == 0)) || (!writeAccess && (processorPrivilege <= 2)));
        }
    }

    /**
     * Writes the content of this GRS to the given buffered writer
     */
    public void log(
        final BufferedWriter writer
    ) {
        try {
            writer.write("General Register Set");
            for (int rx = 0; rx < 128; rx += 8) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  %5s:", Constants.GRS_REGISTER_NAMES[rx]));
                for (int ry = 0; ry < 8; ++ry) {
                    sb.append(String.format(" %012o", _registers[rx + ry]));
                }
                sb.append("\n");
                writer.write(sb.toString());
            }
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }
}
