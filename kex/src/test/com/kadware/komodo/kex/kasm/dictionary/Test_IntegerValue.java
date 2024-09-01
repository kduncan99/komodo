/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.oldbaselib.FieldDescriptor;
import com.kadware.komodo.kex.kasm.Form;
import com.kadware.komodo.kex.kasm.LineSpecifier;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.UnresolvedReference;
import com.kadware.komodo.kex.kasm.UnresolvedReferenceToLocationCounter;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class Test_IntegerValue {

    @Test
    public void integrate_good() {
        FieldDescriptor fdur1 = new FieldDescriptor(18, 18);
        UnresolvedReference[] initialRefs = {
            new UnresolvedReferenceToLocationCounter(fdur1, false, 25)
        };
        IntegerValue initial = new IntegerValue.Builder().setValue(0_111111_222222L)
                                                         .setReferences(initialRefs)
                                                         .setForm(Form.EI$Form)
                                                         .setFlagged(true)
                                                         .build();

        FieldDescriptor fdur2 = new FieldDescriptor(30, 6);
        UnresolvedReference[] refs1 = {
            new UnresolvedReferenceToLocationCounter(fdur2, false, 13)
        };
        FieldDescriptor compFD1 = new FieldDescriptor(12, 6);
        IntegerValue compValue1 = new IntegerValue.Builder().setValue(025).setReferences(refs1).build();
        FieldDescriptor compFD2 = new FieldDescriptor(21, 12);
        IntegerValue compValue2 = new IntegerValue.Builder().setValue(0_1111).build();
        FieldDescriptor compFD3 = new FieldDescriptor(1, 1);
        IntegerValue compValue3 = new IntegerValue.Builder().setValue(1).build();

        FieldDescriptor[] compFDs = { compFD1, compFD2, compFD3 };
        IntegerValue[] compValues = { compValue1, compValue2, compValue3 };

        Locale intLocale = new Locale(new LineSpecifier(2, 25), 30);
        IntegerValue.IntegrateResult result = IntegerValue.integrate(initial, compFDs ,compValues, intLocale);

        for (Diagnostic d : result._diagnostics.getDiagnostics()) {
            System.out.println(d.getMessage());
        }

        assertTrue(result._diagnostics.isEmpty());
        assertEquals(intLocale, result._value._locale);
        assertEquals(Form.EI$Form, result._value._form);
        assertFalse(result._value._flagged);
        assertEquals(ValuePrecision.Default, result._value._precision);
        assertEquals(0_311136_233332L, result._value._value.get().longValue());
        UnresolvedReference[] urs = result._value._references;
        assertEquals(2, urs.length);
        assertTrue(urs[0] instanceof UnresolvedReferenceToLocationCounter);
        assertTrue(urs[1] instanceof UnresolvedReferenceToLocationCounter);
        UnresolvedReferenceToLocationCounter urlc0 = (UnresolvedReferenceToLocationCounter) urs[0];
        UnresolvedReferenceToLocationCounter urlc1 = (UnresolvedReferenceToLocationCounter) urs[1];
        assertEquals(25, urlc0._locationCounterIndex);
        assertEquals(fdur1, urlc0._fieldDescriptor);
        assertFalse(urlc0._isNegative);
        assertEquals(13, urlc1._locationCounterIndex);
        assertEquals(compFD1, urlc1._fieldDescriptor);
        assertFalse(urlc1._isNegative);
    }

    @Test
    public void integrate_good_negativeValues() {
        FieldDescriptor fdur1 = new FieldDescriptor(18, 18);
        UnresolvedReference[] initialRefs = {
            new UnresolvedReferenceToLocationCounter(fdur1, false, 25)
        };
        IntegerValue initial = new IntegerValue.Builder().setValue(0_000776_010770L)
                                                         .setReferences(initialRefs)
                                                         .setForm(Form.EI$Form)
                                                         .setFlagged(true)
                                                         .build();

        FieldDescriptor compFD1 = new FieldDescriptor(9, 9);
        DoubleWord36 dw1 = new DoubleWord36(0L, 05L);
        IntegerValue compValue1 = new IntegerValue.Builder().setValue(dw1).build();

        FieldDescriptor compFD2 = new FieldDescriptor(18, 9);
        DoubleWord36 dw2 = new DoubleWord36(0_777777_777777L, 0_777777_777774L);
        IntegerValue compValue2 = new IntegerValue.Builder().setValue(dw2).build();

        FieldDescriptor compFD3 = new FieldDescriptor(27, 9);
        DoubleWord36 dw3 = new DoubleWord36(0_777777_777777L, 0_777777_777775L);
        IntegerValue compValue3 = new IntegerValue.Builder().setValue(dw3).build();

        FieldDescriptor[] compFDs = { compFD1, compFD2, compFD3 };
        IntegerValue[] compValues = { compValue1, compValue2, compValue3 };

        Locale intLocale = new Locale(new LineSpecifier(2, 25), 30);
        IntegerValue.IntegrateResult result = IntegerValue.integrate(initial, compFDs ,compValues, intLocale);

        for (Diagnostic d : result._diagnostics.getDiagnostics()) {
            System.out.println(d.getMessage());
        }

        assertTrue(result._diagnostics.isEmpty());
        assertEquals(intLocale, result._value._locale);
        assertEquals(Form.EI$Form, result._value._form);
        assertFalse(result._value._flagged);
        assertEquals(ValuePrecision.Default, result._value._precision);
        assertEquals(0_000004_005766L, result._value._value.get().longValue());
    }

    //  TODO need some negative test cases
}
