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
            _initialValues.put(String.format("X%d", rx), new IntegerValue(rx));
            _initialValues.put(String.format("EX%d", rx), new IntegerValue(rx));
        }

        for (int rx = 0; rx < 16; ++rx) {
            _initialValues.put(String.format("A%d", rx), new IntegerValue(rx + 12));
            _initialValues.put(String.format("EA%d", rx), new IntegerValue(rx + 12));
        }

        for (int rx = 0; rx < 16; ++rx) {
            _initialValues.put(String.format("R%d", rx), new IntegerValue(rx + 64));
            _initialValues.put(String.format("ER%d", rx), new IntegerValue(rx + 64));
        }

        for (int rx = 0; rx < 32; ++rx) {
            _initialValues.put(String.format("B%d", rx), new IntegerValue(rx));
        }

        //  directives
        _initialValues.put("$BASIC", new DirectiveValue(BASICDirective.class));
        _initialValues.put("$EQU", new DirectiveValue(EQUDirective.class));
        _initialValues.put("$EQUF", new DirectiveValue(EQUFDirective.class));
        _initialValues.put("$EXTEND", new DirectiveValue(EXTENDDirective.class));
        _initialValues.put("$FORM", new DirectiveValue(FORMDirective.class));
        _initialValues.put("$GFORM", new DirectiveValue(GFORMDirective.class));
        _initialValues.put("$INFO", new DirectiveValue(INFODirective.class));
        _initialValues.put("$LIT", new DirectiveValue(LITDirective.class));
        _initialValues.put("$PROC", new DirectiveValue(PROCDirective.class));
        _initialValues.put("$RES", new DirectiveValue(RESDirective.class));

        //  built-in function names
        _initialValues.put("$CAS", new BuiltInFunctionValue(CASFunction.class));
        _initialValues.put("$CFS", new BuiltInFunctionValue(CFSFunction.class));
        _initialValues.put("$SL", new BuiltInFunctionValue(SLFunction.class));
        _initialValues.put("$SR", new BuiltInFunctionValue(SRFunction.class));
        _initialValues.put("$SS", new BuiltInFunctionValue(SSFunction.class));
        _initialValues.put("$TYPE", new BuiltInFunctionValue(TYPEFunction.class));

        //  built-in procs
        //TODO

        //  forms
        _initialValues.put("I$", new FormValue(Form.I$Form));
        _initialValues.put("EI$", new FormValue(Form.EI$Form));
        _initialValues.put("PF$FJAXHIU", new FormValue(Form.FJAXHIU$Form));
        _initialValues.put("PF$FJAXU", new FormValue(Form.FJAXU$Form));
        _initialValues.put("PF$FJAXHIBD", new FormValue(Form.FJAXHIBD$Form));
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
