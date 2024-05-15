/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.engine.exceptions.NotFoundEngineException;
import com.bearsnake.komodo.logger.LogManager;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Represents the General Register Set (GRS) for an Instruction Processor
 * The GRS is a table of 128 general registers.
 */
public class GeneralRegisterSet {

    private static final String LOG_SOURCE = "GRS";

    //  Indices of the defined registers indicating where they are found in the GRS.
    //  X0-X15, A0-A15, and R0-R15 are user registers.
    //  EX0-EX15, EA0-EA15, and ER0-ER15 are EXEC registers.
    //  Note the overlap in both user and EXEC registers, of X12-X15 and A0-A3.
    public static final int X0 = 0;
    public static final int X1 = 1;
    public static final int X2 = 2;
    public static final int X3 = 3;
    public static final int X4 = 4;
    public static final int X5 = 5;
    public static final int X6 = 6;
    public static final int X7 = 7;
    public static final int X8 = 8;
    public static final int X9 = 9;
    public static final int X10 = 10;
    public static final int X11 = 11;
    public static final int X12 = 12;
    public static final int X13 = 13;
    public static final int X14 = 14;
    public static final int X15 = 15;
    public static final int A0 = 12;
    public static final int A1 = 13;
    public static final int A2 = 14;
    public static final int A3 = 15;
    public static final int A4 = 16;
    public static final int A5 = 17;
    public static final int A6 = 18;
    public static final int A7 = 19;
    public static final int A8 = 20;
    public static final int A9 = 21;
    public static final int A10 = 22;
    public static final int A11 = 23;
    public static final int A12 = 24;
    public static final int A13 = 25;
    public static final int A14 = 26;
    public static final int A15 = 27;
    public static final int R0 = 64;
    public static final int R1 = 65;
    public static final int R2 = 66;
    public static final int R3 = 67;
    public static final int R4 = 68;
    public static final int R5 = 69;
    public static final int R6 = 70;
    public static final int R7 = 71;
    public static final int R8 = 72;
    public static final int R9 = 73;
    public static final int R10 = 74;
    public static final int R11 = 75;
    public static final int R12 = 76;
    public static final int R13 = 77;
    public static final int R14 = 78;
    public static final int R15 = 79;
    public static final int ER0 = 80;
    public static final int ER1 = 81;
    public static final int ER2 = 82;
    public static final int ER3 = 83;
    public static final int ER4 = 84;
    public static final int ER5 = 85;
    public static final int ER6 = 86;
    public static final int ER7 = 87;
    public static final int ER8 = 88;
    public static final int ER9 = 89;
    public static final int ER10 = 90;
    public static final int ER11 = 91;
    public static final int ER12 = 92;
    public static final int ER13 = 93;
    public static final int ER14 = 94;
    public static final int ER15 = 95;
    public static final int EX0 = 96;
    public static final int EX1 = 97;
    public static final int EX2 = 98;
    public static final int EX3 = 99;
    public static final int EX4 = 100;
    public static final int EX5 = 101;
    public static final int EX6 = 102;
    public static final int EX7 = 103;
    public static final int EX8 = 104;
    public static final int EX9 = 105;
    public static final int EX10 = 106;
    public static final int EX11 = 107;
    public static final int EX12 = 108;
    public static final int EX13 = 109;
    public static final int EX14 = 110;
    public static final int EX15 = 111;
    public static final int EA0 = 108;
    public static final int EA1 = 109;
    public static final int EA2 = 110;
    public static final int EA3 = 111;
    public static final int EA4 = 112;
    public static final int EA5 = 113;
    public static final int EA6 = 114;
    public static final int EA7 = 115;
    public static final int EA8 = 116;
    public static final int EA9 = 117;
    public static final int EA10 = 118;
    public static final int EA11 = 119;
    public static final int EA12 = 120;
    public static final int EA13 = 121;
    public static final int EA14 = 122;
    public static final int EA15 = 123;

    //  User-displayable names of the various registers, in register index order
    public static final String[] NAMES = {
        "X0",    "X1",    "X2",    "X3",    "X4",    "X5",    "X6",    "X7",
        "X8",    "X9",    "X10",   "X11",   "A0",    "A1",    "A2",    "A3",
        "A4",    "A5",    "A6",    "A7",    "A8",    "A9",    "A10",   "A11",
        "A12",   "A13",   "A14",   "A15",   "UR0",   "UR1",   "UR2",   "UR3",
        "040",   "041",   "042",   "043",   "044",   "045",   "046",   "047",
        "050",   "051",   "052",   "053",   "054",   "055",   "056",   "057",
        "060",   "061",   "062",   "063",   "064",   "065",   "066",   "067",
        "070",   "071",   "072",   "073",   "074",   "075",   "076",   "077",
        "R0",    "R1",    "R2",    "R3",    "R4",    "R5",    "R6",    "R7",
        "R8",    "R9",    "R10",   "R11",   "R12",   "R13",   "R14",   "R15",
        "ER0",   "ER1",   "ER2",   "ER3",   "ER4",   "ER5",   "ER6",   "ER7",
        "ER8",   "ER9",   "ER10",  "ER11",  "ER12",  "ER13",  "ER14",  "ER15",
        "EX0",   "EX1",   "EX2",   "EX3",   "EX4",   "EX5",   "EX6",   "EX7",
        "EX8",   "EX9",   "EX10",  "EX11",  "EA0",   "EA1",   "EA2",   "EA3",
        "EA4",   "EA5",   "EA6",   "EA7",   "EA8",   "EA9",   "EA10",  "EA11",
        "EA12",  "EA13",  "EA14",  "EA15",  "0174",  "0175",  "0176",  "0177",
    };

    private final GeneralRegister[] _registers = new GeneralRegister[128];

    /**
     * Standard constructor
     */
    public GeneralRegisterSet(
    ) {
        for (int rx = 0; rx < 128; ++rx) {
            if ((rx >= X0) && (rx <= X15)) {
                _registers[rx] = new IndexRegister();
            } else if ((rx >= EX0) && (rx <= EX15)) {
                _registers[rx] = new IndexRegister();
            } else {
                _registers[rx] = new GeneralRegister();
            }
        }
    }

    /**
     * Given a string, we return the GRS address of the indicated register if it is in fact a register name
     * @param registerName for which we search
     * @return GRS address of the register
     * @throws NotFoundEngineException if the given name isn't a register name
     */
    public static int getGRSIndex(
        final String registerName
    ) throws NotFoundEngineException {
        for (int rx = 0; rx < NAMES.length; rx++) {
            if (registerName.equals( NAMES[rx] )) {
                return rx;
            }
        }

        throw new NotFoundEngineException(registerName);
    }

    /**
     * Retrieves a reference to a particular GeneralRegister object according to the given register index
     * @param registerIndex index of the requested register
     * @return reference as indicated above
     */
    public GeneralRegister getRegister(
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

        if ((registerIndex >= X0) && (registerIndex <= X15)) {
            _registers[registerIndex] = new IndexRegister(value);
        } else if ((registerIndex >= GeneralRegisterSet.EX0) && (registerIndex <= GeneralRegisterSet.EX15)) {
            _registers[registerIndex] = new IndexRegister(value);
        } else {
            _registers[registerIndex] = new GeneralRegister(value);
        }
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
                sb.append(String.format("  %5s:", NAMES[rx]));
                for (int ry = 0; ry < 8; ++ry) {
                    sb.append(String.format(" %012o", _registers[rx + ry].getW()));
                }
                sb.append("\n");
                writer.write(sb.toString());
            }
        } catch (IOException ex) {
            LogManager.logCatching(LOG_SOURCE, ex);
        }
    }
}
