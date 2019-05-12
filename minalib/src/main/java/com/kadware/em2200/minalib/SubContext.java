/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

/**
 * For sub-assemblies
 */
public class SubContext extends Context {

    private final Context _parent;

    public SubContext(
        final Context parent
    ) {
        super(parent._dictionary, parent._moduleName);
        _parent = parent;
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  special accessors - overriding the super class to access the parent object's values instead
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override public boolean getArithmeticFaultCompatibilityMode() { return _parent.getArithmeticFaultCompatibilityMode(); }
    @Override public boolean getArithmeticFaultNonInterruptMode() { return _parent.getArithmeticFaultNonInterruptMode(); }
    @Override public boolean getQuarterWordMode() { return _parent.getQuarterWordMode(); }
    @Override public boolean getThirdWordMode() { return _parent.getThirdWordMode(); }

    @Override public void setArithmeticFaultCompatibilityMode( final boolean value ) { _parent.setArithmeticFaultCompatibilityMode(value); }
    @Override public void setArithmeticFaultNonInterruptMode( final boolean value ) { _parent.setArithmeticFaultNonInterruptMode(value); }
    @Override public void setQuarterWordMode( final boolean value ) { _parent.setQuarterWordMode(value); }
    @Override public void setThirdWordMode( final boolean value ) { _parent.setThirdWordMode(value); }
}
