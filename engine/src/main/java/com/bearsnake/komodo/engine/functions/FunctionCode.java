/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

public class FunctionCode {

    private final Integer _fField;
    private Integer _jField;
    private Integer _aField;
    private int _processorPrivilege;

    public FunctionCode(final Integer fField) {
        _fField = fField & 0_077;
        _jField = null;
        _aField = null;

        // default PP - user level
        _processorPrivilege = 3;
    }

    public FunctionCode setAField(
        final Integer aField
    ) {
        _aField = aField & 0_017;
        return this;
    }

    public FunctionCode setJField(
        final Integer jField
    ) {
        _jField = jField & 0_017;
        return this;
    }

    public FunctionCode setProcessorPrivilege(
        final int processorPrivilege
    ) {
        _processorPrivilege = processorPrivilege & 0x03;
        return this;
    }

    public Integer getAField() { return _aField; }
    public Integer getFField() { return _fField; }
    public Integer getJField() { return _jField; }
    public int getProcessorPrivilege() { return _processorPrivilege; }
}
