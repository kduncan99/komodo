/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

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

        //  Initialize directive names
        //????
    }
}
