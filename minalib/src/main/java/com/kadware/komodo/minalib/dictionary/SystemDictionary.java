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
            _initialValues.put(String.format("X%d", rx), new IntegerValue(false, rx, null));
            _initialValues.put(String.format("EX%d", rx), new IntegerValue(false, rx, null));
        }

        for (int rx = 0; rx < 16; ++rx) {
            _initialValues.put(String.format("A%d", rx), new IntegerValue(false, rx + 12, null));
            _initialValues.put(String.format("EA%d", rx), new IntegerValue(false, rx + 12, null));
        }

        for (int rx = 0; rx < 16; ++rx) {
            _initialValues.put(String.format("R%d", rx), new IntegerValue(false, rx + 64, null));
            _initialValues.put(String.format("ER%d", rx), new IntegerValue(false, rx + 64, null));
        }

        for (int rx = 0; rx < 32; ++rx) {
            _initialValues.put(String.format("B%d", rx), new IntegerValue(false, rx, null));
        }

        //  directives
        _initialValues.put("$BASIC", new DirectiveValue(BASICDirective.class));
        _initialValues.put("$EQU", new DirectiveValue(EQUDirective.class));
        _initialValues.put("$EXTEND", new DirectiveValue(EXTENDDirective.class));
        _initialValues.put("$GFORM", new DirectiveValue(GFORMDirective.class));
        _initialValues.put("$INFO", new DirectiveValue(INFODirective.class));
        _initialValues.put("$LIT", new DirectiveValue(LITDirective.class));
        _initialValues.put("$PROC", new DirectiveValue(PROCDirective.class));
        _initialValues.put("$RES", new DirectiveValue(RESDirective.class));

        //  built-in function names
        _initialValues.put("$BDI", new BuiltInFunctionValue(BDIFunction.class));
        _initialValues.put("$CAS", new BuiltInFunctionValue(CASFunction.class));
        _initialValues.put("$CFS", new BuiltInFunctionValue(CFSFunction.class));
        _initialValues.put("$SL", new BuiltInFunctionValue(SLFunction.class));
        _initialValues.put("$SR", new BuiltInFunctionValue(SRFunction.class));
        _initialValues.put("$SS", new BuiltInFunctionValue(SSFunction.class));
        _initialValues.put("$TYPE", new BuiltInFunctionValue(TYPEFunction.class));

        //  built-in procs
        //TODO

        //  forms
        int[] ifjaxhiu = { 6, 4, 4, 4, 1, 1, 16 };
        Value vfjaxhiu = new FormValue(false, new Form(ifjaxhiu));
        _initialValues.put("PF$FJAXHIU", vfjaxhiu);

        int[] ifjaxu = { 6, 4, 4, 4, 18 };
        Value vfjaxu = new FormValue(false, new Form(ifjaxu));
        _initialValues.put("PF$FJAXU", vfjaxu);

        int[] ifjaxhibd = { 6, 4, 4, 4, 1, 1, 4, 12 };
        Value vfjaxhibd = new FormValue(false, new Form(ifjaxhibd));
        _initialValues.put("PF$FJAXHIBD", vfjaxhibd);
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
