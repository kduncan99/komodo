/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import java.util.Arrays;

public enum ConsoleType {
    Communications(001, 'C', "COMMSG"),
    HardwareConfidence(002, 'H', "HWMSG"),
    InputOutput(004, 'I', "IOMSG"),
    System(010, 'S', "SYSMSG");

    private final int _bitMask;
    private final String _group;
    private final char _token;

    public final static int ALL =
        Communications._bitMask | HardwareConfidence._bitMask | InputOutput._bitMask | System._bitMask;

    ConsoleType(
        final int bitMask,
        final char token,
        final String group
    ) {
        _bitMask = bitMask;
        _group = group.toUpperCase();
        _token = Character.toUpperCase(token);
    }

    public int getBitMask() { return _bitMask; }
    public String getGroup() { return _group; }
    public char getToken() { return _token; }

    public static ConsoleType getByGroup(final String group) {
        return Arrays.stream(ConsoleType.values())
                     .filter(ct -> ct._group.equalsIgnoreCase(group))
                     .findFirst()
                     .orElse(null);
    }

    public static ConsoleType getByToken(final char token) {
        return Arrays.stream(ConsoleType.values())
                     .filter(ct -> ct._token == Character.toUpperCase(token))
                     .findFirst()
                     .orElse(null);
    }
}
