/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.baselib.FieldDescriptor;

/**
 * A subset of an IntegerValue - basically, the same thing but with no attached form.
 */
@SuppressWarnings("Duplicates")
class GeneratedWord {

    final TextLine _topLevelTextLine;
    final LineSpecifier _lineSpecifier;
    final int _locationCounterIndex;
    final int _locationCounterOffset;
    final IntegerValue _value;

    /**
     * Tracks the location of the line of source code which produced this object
     * @param topLevelTextLine reference to the TextLine object responsible for generating this word
     * @param lineSpecifier line specifier of the line of source code which directly produced this word
     * @param locationCounterIndex what pool contains this
     * @param locationCounterOffset where is this within that pool
     * @param value IntegerValue defining the the various field values and references for this word
     */
    GeneratedWord(
        final TextLine topLevelTextLine,
        final LineSpecifier lineSpecifier,
        final int locationCounterIndex,
        final int locationCounterOffset,
        final IntegerValue value
    ) {
        _lineSpecifier = lineSpecifier;
        _topLevelTextLine = topLevelTextLine;
        _locationCounterIndex = locationCounterIndex;
        _locationCounterOffset = locationCounterOffset;
        _value = value;
    }

    /**
     * Constructs a RelocatableWord object based upon the given IntegerValue.
     * Should be called after we've resolved all references local to the containing module.
     */
    RelocatableWord produceRelocatableWord() {
        return new RelocatableWord(_value._value, _value._references);
    }
}
