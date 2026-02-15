/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import java.util.Arrays;
import java.util.LinkedList;

public enum ConsoleType {
    Communications('C', "COMMSG"),
    HardwareConfidence('H', "HWMSG"),
    InputOutput('I', "IOMSG"),
    System('S', "SYSMSG");

    private final String _group;
    private final char _token;

    public final static LinkedList<ConsoleType> ALL = new LinkedList<>();
    static {
        ALL.add(Communications);
        ALL.add(HardwareConfidence);
        ALL.add(InputOutput);
        ALL.add(System);
    }

    ConsoleType(
        final char token,
        final String group
    ) {
        _group = group.toUpperCase();
        _token = Character.toUpperCase(token);
    }

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
