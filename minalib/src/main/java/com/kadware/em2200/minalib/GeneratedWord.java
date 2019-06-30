/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.IntegerValue;
import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.baselib.OnesComplement;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * During assembly, each component value of every generated word is placed in this container.
 * The component values represent various sub-fields of the word and appear in no particular order,
 * although each value is keyed by the field descriptor to which it pertains.
 */
@SuppressWarnings("Duplicates")
class GeneratedWord extends HashMap<FieldDescriptor, IntegerValue> {

    final TextLine _topLevelTextLine;
    final LineSpecifier _lineSpecifier;
    final int _locationCounterIndex;
    final int _locationCounterOffset;
    RelocatableWord36 _relocatableWord = null;

    /**
     * Tracks the location of the line of source code which produced this object
     * @param topLevelTextLine reference to the TextLine object responsible for generating this word
     * @param lineSpecifier line specifier of the line of source code which directly produced this word
     * @param locationCounterIndex what pool contains this
     * @param locationCounterOffset where is this within that pool
     */
    GeneratedWord(
        final TextLine topLevelTextLine,
        final LineSpecifier lineSpecifier,
        final int locationCounterIndex,
        final int locationCounterOffset
    ) {
        _lineSpecifier = lineSpecifier;
        _topLevelTextLine = topLevelTextLine;
        _locationCounterIndex = locationCounterIndex;
        _locationCounterOffset = locationCounterOffset;
    }

    /**
     * Constructs a composite RelocatableWord36 object based upon the various component field definitions.
     * Should be called after we've resolved all references local to the containing module.
     * If the word has already been constructed, don't do it twice...
     * @return composite word
     */
    RelocatableWord36 produceRelocatableWord36(
        final Diagnostics diagnostics
    ) {
        if (_relocatableWord == null) {
            long discreteValue = 0;
            List<UndefinedReference> relRefs = new LinkedList<>();
            for (Map.Entry<FieldDescriptor, IntegerValue> entry : entrySet()) {
                //  convert value from twos- to ones-complement, check for 36-bit truncation
                long fieldValue = entry.getValue()._value;
                OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
                OnesComplement.getOnesComplement36(fieldValue, ocr);
                boolean trunc = ocr._overflow;
                long value36 = ocr._result;

                FieldDescriptor fd = entry.getKey();
                long mask = (1L << fd._fieldSize) - 1;
                long maskedValue = value36 & mask;

                //  Check for field size truncation
                if (fieldValue > 0) {
                    trunc = (value36 != maskedValue);
                } else if (fieldValue < 0) {
                    trunc = ((mask | value36) != 0_777777_777777L);
                }

                if (trunc) {
                    diagnostics.append(new TruncationDiagnostic(new Locale(_lineSpecifier, 1),
                                                                fd._startingBit,
                                                                fd._startingBit + fd._fieldSize - 1));
                }

                int shiftCount = 36 - fd._startingBit - fd._fieldSize;
                discreteValue |= (maskedValue << shiftCount);

                //  Propagate any remaining external references
                for (UndefinedReference uRef : entry.getValue()._undefinedReferences) {
                    relRefs.add(uRef.copy(fd));
                }
            }

            _relocatableWord =
                new RelocatableWord36(discreteValue, relRefs.toArray(new UndefinedReference[0]));
        }

        return _relocatableWord;
    }
}
