/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.minalib.Form;
import com.kadware.komodo.minalib.directives.*;
import com.kadware.komodo.minalib.expressions.builtInFunctions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a top-level dictionary which contains all the system labels
 * This is a subclass so that we can pre-load the built-ins
 */
public class SystemDictionary extends Dictionary {

    private static final Map<String, Value> _initialValues = new HashMap<>();
    static {
        for (int rx = 0; rx < 16; ++rx) {
            _initialValues.put(String.format("X%d", rx), new IntegerValue.Builder().setValue(rx).build());
            _initialValues.put(String.format("EX%d", rx), new IntegerValue.Builder().setValue(rx).build());
        }

        for (int rx = 0; rx < 16; ++rx) {
            _initialValues.put(String.format("A%d", rx), new IntegerValue.Builder().setValue(rx + 12).build());
            _initialValues.put(String.format("EA%d", rx), new IntegerValue.Builder().setValue(rx + 12).build());
        }

        for (int rx = 0; rx < 16; ++rx) {
            _initialValues.put(String.format("R%d", rx), new IntegerValue.Builder().setValue(rx + 64).build());
            _initialValues.put(String.format("ER%d", rx), new IntegerValue.Builder().setValue(rx + 64).build());
        }

        for (int rx = 0; rx < 32; ++rx) {
            _initialValues.put(String.format("B%d", rx), new IntegerValue.Builder().setValue(rx).build());
        }

        //  directives
        _initialValues.put("$ASCII", new DirectiveValue.Builder().setClass(ASCIIDirective.class).build());
        _initialValues.put("$BASIC", new DirectiveValue.Builder().setClass(BASICDirective.class).build());
        _initialValues.put("$EQU", new DirectiveValue.Builder().setClass(EQUDirective.class).build());
        _initialValues.put("$EQUF", new DirectiveValue.Builder().setClass(EQUFDirective.class).build());
        _initialValues.put("$EXTEND", new DirectiveValue.Builder().setClass(EXTENDDirective.class).build());
        _initialValues.put("$FDATA", new DirectiveValue.Builder().setClass(FDATADirective.class).build());
        _initialValues.put("$FORM", new DirectiveValue.Builder().setClass(FORMDirective.class).build());
        _initialValues.put("$GFORM", new DirectiveValue.Builder().setClass(GFORMDirective.class).build());
        _initialValues.put("$INFO", new DirectiveValue.Builder().setClass(INFODirective.class).build());
        _initialValues.put("$LIT", new DirectiveValue.Builder().setClass(LITDirective.class).build());
        _initialValues.put("$PROC", new DirectiveValue.Builder().setClass(PROCDirective.class).build());
        _initialValues.put("$RES", new DirectiveValue.Builder().setClass(RESDirective.class).build());

        //  built-in function names
        _initialValues.put("$CAS", new BuiltInFunctionValue.Builder().setClass(CASFunction.class).build());
        _initialValues.put("$CFS", new BuiltInFunctionValue.Builder().setClass(CFSFunction.class).build());
        _initialValues.put("$SL", new BuiltInFunctionValue.Builder().setClass(SLFunction.class).build());
        _initialValues.put("$SR", new BuiltInFunctionValue.Builder().setClass(SRFunction.class).build());
        _initialValues.put("$SS", new BuiltInFunctionValue.Builder().setClass(SSFunction.class).build());
        _initialValues.put("$TYPE", new BuiltInFunctionValue.Builder().setClass(TYPEFunction.class).build());

        //  built-in procs
        //TODO

        //  forms
        _initialValues.put("I$", new FormValue.Builder().setForm(Form.I$Form).build());
        _initialValues.put("EI$", new FormValue.Builder().setForm(Form.EI$Form).build());
        _initialValues.put("PF$FJAXHIU", new FormValue.Builder().setForm(Form.FJAXHIU$Form).build());
        _initialValues.put("PF$FJAXU", new FormValue.Builder().setForm(Form.FJAXU$Form).build());
        _initialValues.put("PF$FJAXHIBD", new FormValue.Builder().setForm(Form.FJAXHIBD$Form).build());
    }

    /**
     * Constructor for top-level dictionary
     */
    public SystemDictionary(
    ) {
        for (Map.Entry<String, Value> entry : _initialValues.entrySet()) {
            addValue(0, entry.getKey(), entry.getValue());
        }
    }
}
