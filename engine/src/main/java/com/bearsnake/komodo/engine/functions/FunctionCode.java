package com.bearsnake.komodo.engine.functions;

public class FunctionCode {

    private final Integer _fField;
    private Integer _jField;
    private Integer _aField;

    public FunctionCode(final Integer fField) {
        _fField = fField;
        _jField = null;
        _aField = null;
    }

    public FunctionCode setAField(
        final Integer aField
    ) {
        _aField = aField;
        return this;
    }

    public FunctionCode setJField(
        final Integer jField
    ) {
        _jField = jField;
        return this;
    }

    public Integer getFField() { return _fField; }
    public Integer getJField() { return _jField; }
    public Integer getAField() { return _aField; }
}
