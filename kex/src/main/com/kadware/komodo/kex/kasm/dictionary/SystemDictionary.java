/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.Form;
import com.kadware.komodo.kex.kasm.directives.ASCIIDirective;
import com.kadware.komodo.kex.kasm.directives.BASICDirective;
import com.kadware.komodo.kex.kasm.directives.ENDDirective;
import com.kadware.komodo.kex.kasm.directives.EQUDirective;
import com.kadware.komodo.kex.kasm.directives.EQUFDirective;
import com.kadware.komodo.kex.kasm.directives.EXTENDDirective;
import com.kadware.komodo.kex.kasm.directives.FDATADirective;
import com.kadware.komodo.kex.kasm.directives.FORMDirective;
import com.kadware.komodo.kex.kasm.directives.GFORMDirective;
import com.kadware.komodo.kex.kasm.directives.INFODirective;
import com.kadware.komodo.kex.kasm.directives.LITDirective;
import com.kadware.komodo.kex.kasm.directives.PROCDirective;
import com.kadware.komodo.kex.kasm.directives.RESDirective;
import com.kadware.komodo.kex.kasm.expressions.builtInFunctions.CASFunction;
import com.kadware.komodo.kex.kasm.expressions.builtInFunctions.CFSFunction;
import com.kadware.komodo.kex.kasm.expressions.builtInFunctions.SLFunction;
import com.kadware.komodo.kex.kasm.expressions.builtInFunctions.SRFunction;
import com.kadware.komodo.kex.kasm.expressions.builtInFunctions.SSFunction;
import com.kadware.komodo.kex.kasm.expressions.builtInFunctions.TYPEFunction;
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
        _initialValues.put("$ASCII", new DirectiveValue(ASCIIDirective.class));
        _initialValues.put("$BASIC", new DirectiveValue(BASICDirective.class));
        _initialValues.put("$END", new DirectiveValue(ENDDirective.class));
        _initialValues.put("$EQU", new DirectiveValue(EQUDirective.class));
        _initialValues.put("$EQUF", new DirectiveValue(EQUFDirective.class));
        _initialValues.put("$EXTEND", new DirectiveValue(EXTENDDirective.class));
        _initialValues.put("$FDATA", new DirectiveValue(FDATADirective.class));
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
