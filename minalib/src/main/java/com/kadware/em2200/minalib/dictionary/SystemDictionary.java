/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.expressions.builtInFunctions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a top-level dictionary which contains all the system labels
 * This is a subclass so that we can pre-load the built-ins
 */
public class SystemDictionary extends Dictionary {

    private static final Map<String, Value> _initialValues = new HashMap<>();
    static {
        //  registers TODO move these to AXR$... maybe
        for (int rx = 0; rx < 15; ++rx) {
            _initialValues.put(String.format("X%d", rx), new IntegerValue(false, rx, null));
            _initialValues.put(String.format("EX%d", rx), new IntegerValue(false, rx, null));
        }

        for (int rx = 0; rx < 15; ++rx) {
            _initialValues.put(String.format("A%d", rx), new IntegerValue(false, rx + 12, null));
            _initialValues.put(String.format("EA%d", rx), new IntegerValue(false, rx + 12, null));
        }

        for (int rx = 0; rx < 15; ++rx) {
            _initialValues.put(String.format("R%d", rx), new IntegerValue(false, rx + 64, null));
            _initialValues.put(String.format("ER%d", rx), new IntegerValue(false, rx + 64, null));
        }

        for (int rx = 0; rx < 31; ++rx) {
            _initialValues.put(String.format("B%d", rx), new IntegerValue(false, rx, null));
        }

        //TODO should be put directives in here?

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
