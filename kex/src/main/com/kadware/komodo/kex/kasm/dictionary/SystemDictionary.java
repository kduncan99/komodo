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
import com.kadware.komodo.kex.kasm.directives.INCLUDEDirective;
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
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a top-level dictionary which contains all the system labels
 * This is a subclass so that we can pre-load the built-ins
 */
public class SystemDictionary extends Dictionary {

    private static final Map<String, Value> _initialValues = new TreeMap<>();
    static {
        //  register names - normally these come in via MAXR$, but every conceivable use of this assembler
        //  would include that proc, so it's easier just to put this stuff into the system dictionary at the outset.
        for (int rx = 0; rx < 16; ++rx) {
            _initialValues.put(String.format("X%d", rx), new IntegerValue.Builder().setValue(rx).build());
            _initialValues.put(String.format("EX%d", rx), new IntegerValue.Builder().setValue(rx).build());
            _initialValues.put(String.format("A%d", rx), new IntegerValue.Builder().setValue(rx + 12).build());
            _initialValues.put(String.format("EA%d", rx), new IntegerValue.Builder().setValue(rx + 12).build());
            _initialValues.put(String.format("R%d", rx), new IntegerValue.Builder().setValue(rx + 64).build());
            _initialValues.put(String.format("ER%d", rx), new IntegerValue.Builder().setValue(rx + 64).build());
        }

        for (int rx = 0; rx < 32; ++rx) {
            _initialValues.put(String.format("B%d", rx), new IntegerValue.Builder().setValue(rx).build());
        }

        //  partial-word designators
        _initialValues.put("W", new IntegerValue.Builder().setValue(0).build());
        _initialValues.put("H2", new IntegerValue.Builder().setValue(1).build());
        _initialValues.put("H1", new IntegerValue.Builder().setValue(2).build());
        _initialValues.put("XH2", new IntegerValue.Builder().setValue(3).build());
        _initialValues.put("XH1", new IntegerValue.Builder().setValue(4).build());
        _initialValues.put("Q2", new IntegerValue.Builder().setValue(4).build());
        _initialValues.put("T3", new IntegerValue.Builder().setValue(5).build());
        _initialValues.put("Q4", new IntegerValue.Builder().setValue(5).build());
        _initialValues.put("T2", new IntegerValue.Builder().setValue(6).build());
        _initialValues.put("Q3", new IntegerValue.Builder().setValue(6).build());
        _initialValues.put("T1", new IntegerValue.Builder().setValue(7).build());
        _initialValues.put("Q1", new IntegerValue.Builder().setValue(7).build());
        _initialValues.put("S6", new IntegerValue.Builder().setValue(8).build());
        _initialValues.put("S5", new IntegerValue.Builder().setValue(9).build());
        _initialValues.put("S4", new IntegerValue.Builder().setValue(10).build());
        _initialValues.put("S3", new IntegerValue.Builder().setValue(11).build());
        _initialValues.put("S2", new IntegerValue.Builder().setValue(12).build());
        _initialValues.put("S1", new IntegerValue.Builder().setValue(13).build());
        _initialValues.put("U", new IntegerValue.Builder().setValue(14).build());
        _initialValues.put("XU", new IntegerValue.Builder().setValue(15).build());

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
        _initialValues.put("$INCLUDE", new DirectiveValue(INCLUDEDirective.class));
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

        //  forms
        _initialValues.put("I$", new FormValue(Form.I$Form));
        _initialValues.put("EI$", new FormValue(Form.EI$Form));
        _initialValues.put("PF$FJAXHIU", new FormValue(Form.FJAXHIU$Form));
        _initialValues.put("PF$FJAXU", new FormValue(Form.FJAXU$Form));
        _initialValues.put("PF$FJAXHIBD", new FormValue(Form.FJAXHIBD$Form));
        _initialValues.put("PF$W", new FormValue(Form.W$Form));
    }

    /**
     * Constructor for top-level dictionary
     */
    public SystemDictionary(
    ) {
        for (Map.Entry<String, Value> entry : _initialValues.entrySet()) {
            addValue(0, entry.getKey(), null, entry.getValue());
        }
    }
}
