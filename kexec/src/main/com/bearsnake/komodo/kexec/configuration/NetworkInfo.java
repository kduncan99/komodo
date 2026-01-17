/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

public class NetworkInfo {

    public final String _name;
    public final String _addressString;
    public final Integer _consolePort;
    public final Integer _demandPort;

    public NetworkInfo(final String name,
                       final String addressString,
                       final Integer consolePort,
                       final Integer demandPort) {
        _name = name;
        _addressString = addressString;
        _consolePort = consolePort;
        _demandPort = demandPort;
    }
}
