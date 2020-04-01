/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

/**
 * Represents the components of a label field.
 * If either _lcIndex or _label is null, the corresponding component was not specified.
 * If _label is not null, _labelLevel will indicate the number of external signifiers (asterisks)
 * which follow the label.
 */
public class LabelFieldComponents {
    public final String _label;
    public final Integer _labelLevel;
    public final Locale _labelLocale;
    public final Integer _lcIndex;
    public final Locale _lcIndexLocale;

    public LabelFieldComponents(
            final Integer lcIndex,
            final Locale lcIndexLocale,
            final String label,
            final Integer labelLevel,
            final Locale labelLocale
    ) {
        _label = label;
        _labelLevel = labelLevel;
        _labelLocale = labelLocale;
        _lcIndex = lcIndex;
        _lcIndexLocale = lcIndexLocale;
    }
}
