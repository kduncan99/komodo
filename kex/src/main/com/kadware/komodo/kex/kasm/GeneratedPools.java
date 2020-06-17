/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.exceptions.NotFoundException;
import com.kadware.komodo.kex.kasm.diagnostics.ValueDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.dictionary.ValueType;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Container for the various GeneratedPool objects which are created during an assembly
 */
public class GeneratedPools extends TreeMap<Integer, GeneratedPool> {

    /**
     * Resolves any lingering undefined references once initial assembly is complete, for one particular IntegerValue object.
     * These will be the forward-references we picked up along the way.
     * No point checking for loc ctr refs, those aren't resolved until link time.
     * @param originalValue an IntegerValue presumably containing undefined references which we can resolve
     * @param assembler the current controlling assembler (at this stage, it should be the main assembler)
     * @return a newly-generated IntegerValue which has all resolvable references satisfied and integrated into the base value.
     */
    private IntegerValue resolveReferences(
        final IntegerValue originalValue,
        final Assembler assembler
    ) {
        IntegerValue newValue = originalValue;
        if (originalValue._references.length > 0) {
            BigInteger newDiscreteValue = originalValue._value.get();
            List<UnresolvedReference> newURefs = new LinkedList<>();
            for (UnresolvedReference uRef : originalValue._references) {
                if (uRef instanceof UnresolvedReferenceToLabel) {
                    UnresolvedReferenceToLabel lRef = (UnresolvedReferenceToLabel) uRef;
                    try {
                        Dictionary.ValueInfo vInfo = assembler.getDictionary().getValueInfo(lRef._label);
                        Value lookupValue = vInfo._value;
                        if (lookupValue.getType() != ValueType.Integer) {
                            String msg = String.format("Reference '%s' does not resolve to an integer",
                                                       lRef._label);
                            assembler.appendDiagnostic(new ValueDiagnostic(originalValue._locale, msg));
                        } else {
                            IntegerValue lookupIntegerValue = (IntegerValue) lookupValue;
                            BigInteger addend = lookupIntegerValue._value.get();
                            if (lRef._isNegative) {
                                addend = addend.negate();
                            }
                            newDiscreteValue = newDiscreteValue.add(addend);
                            for (UnresolvedReference urSub : lookupIntegerValue._references) {
                                newURefs.add(urSub.copy(lRef._fieldDescriptor));
                            }
                        }
                    } catch (NotFoundException ex) {
                        //  reference is still not found - propagate it
                        newURefs.add(uRef);
                    }
                } else {
                    newURefs.add(uRef);
                }
            }

            newValue = new IntegerValue.Builder().setLocale(originalValue._locale)
                                                 .setValue(new DoubleWord36(newDiscreteValue))
                                                 .setForm(originalValue._form)
                                                 .setReferences(newURefs)
                                                 .build();
        }

        return newValue;
    }

    /**
     * Resolves all resolvable undefined references in this GeneratedPool object
     * @param assembler the controlling assembler (should be the main assembler)
     */
    public void resolveReferences(
        final Assembler assembler
    ) {
        for (GeneratedPool pool : values()) {
            Iterator<Map.Entry<Integer, GeneratedWord>> gwIter = pool.getGeneratedWordsIterator();
            while (gwIter.hasNext()) {
                Map.Entry<Integer, GeneratedWord> entry = gwIter.next();
                Integer lcOffset = entry.getKey();
                GeneratedWord gwOriginal = entry.getValue();
                IntegerValue originalValue = gwOriginal._value;
                IntegerValue newValue = resolveReferences(originalValue, assembler);
                if (!newValue.equals(originalValue)) {
                    pool.storeGeneratedWord(lcOffset, gwOriginal.copy(newValue));
                }
            }
        }
    }
}
