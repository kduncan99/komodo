/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.minalib.Form;
import com.kadware.em2200.minalib.expressions.builtInFunctions.*;

/**
 * Represents a main-level (i.e., level 1) dictionary.
 * This is a subclass so that we can pre-load the built-ins
 */
public class MainLevelDictionary extends Dictionary {

    /**
     * Constructor for top-level dictionary
     * <p>
     * @param globalDictionary reference to the global-level (level 0) dictionary
     */
    public MainLevelDictionary(
        final Dictionary globalDictionary
    ) {
        super(globalDictionary);
        initialize();
    }

    private void initialize(
    ) {
        //  Initialize built-in function names
        addValue(0, "$CAS", new BuiltInFunctionValue(CASFunction.class));
        addValue(0, "$CFS", new BuiltInFunctionValue(CFSFunction.class));
        addValue(0, "$SL", new BuiltInFunctionValue(SLFunction.class));
        addValue(0, "$SR", new BuiltInFunctionValue(SRFunction.class));
        addValue(0, "$SS", new BuiltInFunctionValue(SSFunction.class));
        addValue(0, "$TYPE", new BuiltInFunctionValue(TYPEFunction.class));

        //  Initialize Forms
        int[] ifjaxhiu = { 6, 4, 4, 4, 1, 1, 16 };
        Value vfjaxhiu = new IntegerValue.Builder().setForm(new Form(ifjaxhiu)).build();
        addValue(0, "PF$FJAXHIU", vfjaxhiu);

        int[] ifjaxu = { 6, 4, 4, 4, 18 };
        Value vfjaxu = new IntegerValue.Builder().setForm(new Form(ifjaxu)).build();
        addValue(0, "PF$FJAXU", vfjaxhiu);

        int[] ifjaxhibd = { 6, 4, 4, 4, 1, 1, 4, 12 };
        Value vfjaxhibd = new IntegerValue.Builder().setForm(new Form(ifjaxhibd)).build();
        addValue(0, "PF$FJAXHIBD", vfjaxhibd);

        //  Initialize directive names
        //????
    }
}
