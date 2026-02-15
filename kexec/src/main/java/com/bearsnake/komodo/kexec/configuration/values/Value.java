/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.values;

public abstract class Value {

    public abstract boolean equals(Object obj);
    public abstract int hashCode();
    public abstract ValueType getValueType();
    public abstract String toString();
}
