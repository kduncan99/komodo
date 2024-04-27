/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.builtInFunctions;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.CharacterMode;
import com.kadware.komodo.kex.kasm.LineSpecifier;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.items.ExpressionItem;
import com.kadware.komodo.kex.kasm.expressions.items.ValueItem;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the addition operator
 */
public class Test_CFSFunction {

    @Test
    public void test_single(
    ) throws ExpressionException {

        List<ExpressionItem> items = new LinkedList<>();
        IntegerValue iv = new IntegerValue.Builder().setValue(0_606162636465L).build();
        LineSpecifier ls01 = new LineSpecifier(0, 1);
        items.add(new ValueItem(iv));

        Locale expLocale = new Locale(new LineSpecifier(0, 10), 11);
        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(expLocale, items);

        BuiltInFunction bif = new CFSFunction(expLocale, expressions);

        Value result = bif.evaluate(new Assembler.Builder().build());
        StringValue expected = new StringValue.Builder().setValue("012345").setCharacterMode(CharacterMode.Fieldata).build();
        assertEquals(expected, result);
        assertEquals(expLocale, result._locale);
    }

    @Test
    public void test_double(
    ) throws ExpressionException {

        List<ExpressionItem> items = new LinkedList<>();
        DoubleWord36 dw36 = new DoubleWord36(0_050607101112L, 0_626364656667L);
        IntegerValue iv = new IntegerValue.Builder().setValue(dw36).setPrecision(ValuePrecision.Double).build();
        LineSpecifier ls01 = new LineSpecifier(0, 1);
        items.add(new ValueItem(iv));

        Locale expLocale = new Locale(new LineSpecifier(0, 10), 11);
        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(expLocale, items);

        BuiltInFunction bif = new CFSFunction(expLocale, expressions);

        Value result = bif.evaluate(new Assembler.Builder().build());
        StringValue expected = new StringValue.Builder().setValue(" ABCDE234567").setCharacterMode(CharacterMode.Fieldata).build();
        assertEquals(expected, result);
        assertEquals(expLocale, result._locale);
    }
}
