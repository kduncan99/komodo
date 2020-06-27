/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.baselib.exceptions.NotFoundException;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.FatalDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.ValueDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;
import com.kadware.komodo.kex.kasm.dictionary.EqufValue;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.dictionary.ValueType;
import java.math.BigInteger;
import java.util.Arrays;
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
     * Advances the next offset for the generated word map for the indicated location counter pool.
     * If the pool hasn't been created, it is created now.
     * Used for $RES directive
     */
    public void advance(
        final int lcIndex,
        final int count
    ) {
        obtainPool(lcIndex).advance(count);
    }

    /**
     * Generates a word (with possibly a form attached) for a given location counter index and offset,
     * and places it into the appropriate location counter pool within the given context.
     * Also associates it with the given text line.
     * @param textLine the top-level TextLine object which is responsible for generating this code
     * @param locale location of the text entity generating this word
     * @param lcIndex index of the location counter pool where-in the value is to be placed
     * @param value the integer/intrinsic value to be used
     */
    void generate(
        final TextLine textLine,
        final Locale locale,
        final int lcIndex,
        final IntegerValue value
    ) {
        obtainPool(lcIndex).generate(textLine, locale, value);
    }

    /**
     * Generates the multiple words for a given location counter index
     * and places them into the appropriate location counter pool.
     * Also associates it with the given text line.
     * @param textLine the top-level TextLine object which is responsible for generating this code
     * @param locale location of the text entity generating this word
     * @param lcIndex index of the location counter pool where-in the value is to be placed
     * @param values the values to be used
     */
    void generate(
        final TextLine textLine,
        final Locale locale,
        final int lcIndex,
        final long[] values
    ) {
        obtainPool(lcIndex).generate(textLine, locale, values);
    }

    /**
     * Generates a word with a form attached starting at the next generated-word offset, associating it with the given text line.
     *
     * The form describes 1 or more fields, the totality of which are expected to describe 36 bits.
     * The values parameter is an array with as many entities as there are fields in the form.
     * Each value must fit within the size of that value's respective field.
     * The overall integer portion of the generated value is the respective component integer values
     * shifted into their field positions, and or'd together.
     * The individual values should not have forms attached, but they may have undefined references.
     * All such undefined references are adjusted to match the field description of the particular
     * field to which the reference applies.
     * @param textLine the top-level TextLine object which is responsible for generating this code
     * @param locale location of the text entity generating this word
     * @param lcIndex index of the location counter pool where-in the value is to be placed
     * @param form form describing the fields for which values are specified in the values parameter
     * @param values array of component values, each with potential undefined refereces but no attached forms
     * @param assembler where we post diagnostics if any need to be generated
     */
    public void generate(
        final TextLine textLine,
        final Locale locale,
        final int lcIndex,
        final Form form,
        final IntegerValue[] values,
        final Assembler assembler
    ) {
        obtainPool(lcIndex).generate(textLine, locale, form, values, assembler);
    }

    /**
     * Generates a word with a form attached into the literal pool for the indicated lc index
     * and associates it with the given text line.
     *
     * The form describes 1 or more fields, the totality of which are expected to describe 36 bits.
     * The values parameter is an array with as many entities as there are fields in the form.
     * Each value must fit within the size of that value's respective field.
     * The overall integer portion of the generated value is the respective component integer values
     * shifted into their field positions, and or'd together.
     * The individual values should not have forms attached, but they may have undefined references.
     * All such undefined references are adjusted to match the field description of the particular
     * field to which the reference applies.
     * @param textLine the top-level TextLine object which is responsible for generating this code
     * @param locale location of the text entity generating this word
     * @param lcIndex index of the location counter pool where-in the value is to be placed
     * @param form form describing the fields for which values are specified in the values parameter
     * @param values array of component values, each with potential undefined refereces but no attached forms
     * @param assembler where we post diagnostics if any need to be generated
     * @return value indicating the location which applies to the word which was just generated...
     */
    public IntegerValue generateLiteral(
        final TextLine textLine,
        final Locale locale,
        final int lcIndex,
        final Form form,
        final IntegerValue[] values,
        final Assembler assembler
    ) {
        return obtainPool(lcIndex).generateLiteral(textLine, locale, form, values, assembler);
    }

    /**
     * Obtains a reference to the GeneratedPool corresponding to the given location counter index.
     * If such a pool does not exist, it is created.
     * @param lcIndex index of the desired pool
     * @return reference to the pool
     */
    public GeneratedPool obtainPool(
        final int lcIndex
    ) {
        GeneratedPool gp = get(lcIndex);
        if (gp == null) {
            gp = new GeneratedPool(lcIndex);
            put(lcIndex, gp);
        }
        return gp;
    }

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

        //  Go through the unresolved references for the word, and resolve all that can be resolved.
        //  Anything that cannot be, becomes an external reference.
        //  If we find any references to $EQUFs, we note them in a temporary table and otherwise
        //  resolved them to zero (for now).
        List<EqufValue> equfValues = new LinkedList<>();
        if (originalValue._references.length > 0) {
            BigInteger newDiscreteValue = originalValue._value.get();
            List<UnresolvedReference> newURefs = new LinkedList<>();
            for (UnresolvedReference uRef : originalValue._references) {
                if (uRef instanceof UnresolvedReferenceToLabel) {
                    UnresolvedReferenceToLabel lRef = (UnresolvedReferenceToLabel) uRef;
                    try {
                        Dictionary.ValueInfo vInfo = assembler.getDictionary().getValueInfo(lRef._label);
                        Value lookupValue = vInfo._value;
                        if (lookupValue.getType() == ValueType.Equf) {
                            EqufValue eqVal = (EqufValue) lookupValue;
                            if (lRef._isNegative) {
                                eqVal.invert(assembler);
                            }
                            equfValues.add(eqVal);
                        } else if (lookupValue.getType() == ValueType.Integer) {
                            IntegerValue lookupIntegerValue = (IntegerValue) lookupValue;
                            BigInteger addend = lookupIntegerValue._value.get();
                            if (lRef._isNegative) {
                                addend = addend.negate();
                            }

                            newDiscreteValue = newDiscreteValue.add(addend);
                            for (UnresolvedReference urSub : lookupIntegerValue._references) {
                                newURefs.add(urSub.copy(lRef._fieldDescriptor));
                            }
                        } else {
                            String msg = "Incorrect type for reference '%s'" + lRef._label;
                            assembler.appendDiagnostic(new ValueDiagnostic(originalValue._locale, msg));
                        }
                    } catch (NotFoundException ex) {
                        //  reference is still not found - propagate it
                        newURefs.add(uRef);
                    }
                } else if (uRef instanceof UnresolvedReferenceToLiteral) {
                    //  Any errors in resolving the reference are an internal error.
                    //  i.e., Should Never Happen.  Thus, producing a fatal exception with some debug info.
                    //  We calculate the offset from the location counter by adding the offset-from-literal-pool
                    //  to the offset-from-lc-of-literal-pool, and generate a URLC to replace the URLIT.
                    UnresolvedReferenceToLiteral litRef = (UnresolvedReferenceToLiteral) uRef;
                    GeneratedPool referredPool = get(litRef._locationCounterIndex);
                    if (referredPool == null) {
                        String msg = "Internal error resolving a reference to a literal";
                        assembler.appendDiagnostic(new FatalDiagnostic(null, msg));
                    } else {
                        //  Create an offset value which represents the actual offset from the start of the location counter.
                        //  Essentially, the literal pool offset (from the lc), added to the offset from the start of the lit pool.
                        //  Then we have to integrate it into the discrete value.
                        int offset = referredPool.getLiteralPoolOffset() + litRef._literalOffset;
                        IntegerValue offsetValue = new IntegerValue.Builder().setValue(offset).build();
                        IntegerValue ndValue = new IntegerValue.Builder().setValue(newDiscreteValue).build();
                        FieldDescriptor[] fds = { litRef._fieldDescriptor };
                        IntegerValue[] vals = { offsetValue };
                        IntegerValue.IntegrateResult integrateResult = IntegerValue.integrate(ndValue, fds, vals, null);
                        for (Diagnostic diag : integrateResult._diagnostics.getDiagnostics()) {
                            assembler.appendDiagnostic(diag);
                        }
                        newDiscreteValue = integrateResult._value._value.get();

                        //  Now create a new URtoLC to account for the location counter reference.
                        UnresolvedReference urNew = new UnresolvedReferenceToLocationCounter(litRef._fieldDescriptor,
                                                                                             litRef._isNegative,
                                                                                             litRef._locationCounterIndex);
                        newURefs.add(urNew);
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

        //  If there are any equf values, apply them now
        for (EqufValue ev : equfValues) {
            IntegerValue.IntegrateResult ir = IntegerValue.integrate(newValue,
                                                                     ev._form.getFieldDescriptors(),
                                                                     ev._values,
                                                                     ev._locale);
            for (Diagnostic diag : ir._diagnostics.getDiagnostics()) {
                assembler.appendDiagnostic(diag);
            }

            newValue = ir._value;
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
            pool.coalesceLiterals();
        }

        for (GeneratedPool pool : values()) {
            Iterator<Map.Entry<Integer, GeneratedWord>> gwIter = pool.getGeneratedWordsIterator();
            while (gwIter.hasNext()) {
                Map.Entry<Integer, GeneratedWord> entry = gwIter.next();
                GeneratedWord gw = entry.getValue();
                gw._value = resolveReferences(gw._value, assembler);
            }
        }
    }
}
