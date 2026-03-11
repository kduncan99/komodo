/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.processors;

public abstract class Processor {

    private final String _upi;
    private final String _name;

    public Processor(
        final String upi,
        final String name
    ) {
        _upi = upi;
        _name = name;
    }

    public String getUpi() { return _upi; }
    public String getName() { return _name; }
}
