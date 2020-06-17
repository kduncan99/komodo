/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.builtInFunctions;

import com.kadware.komodo.baselib.DoubleWord36;
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
import static org.junit.Assert.*;

/**
 * Unit tests for the addition operator
 */
public class Test_CASFunction {

    @Test
    public void test_single(
    ) throws ExpressionException {

        List<ExpressionItem> items = new LinkedList<>();

        Locale expLocale = new Locale(new LineSpecifier(0, 1), 11);
        IntegerValue iv = new IntegerValue.Builder().setLocale(expLocale).setValue(0_060_061_062_063L).build();
        items.add(new ValueItem(iv));

        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(expLocale, items);

        BuiltInFunction bif = new CASFunction(expLocale, expressions);

        Value result = bif.evaluate(new Assembler.Builder().build());
        StringValue expected = new StringValue.Builder().setValue("0123").setCharacterMode(CharacterMode.ASCII).build();
        assertEquals(expected, result);
        assertEquals(expLocale, result._locale);
    }

    @Test
    public void test_double(
    ) throws ExpressionException {

        List<ExpressionItem> items = new LinkedList<>();

        Locale expLocale = new Locale(new LineSpecifier(0, 10), 11);
        DoubleWord36 dw36 = new DoubleWord36(0_060061062063L, 0_064065066067L);
        IntegerValue iv = new IntegerValue.Builder().setLocale(expLocale)
                                                    .setValue(dw36)
                                                    .setPrecision(ValuePrecision.Double)
                                                    .build();
        items.add(new ValueItem(iv));

        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(expLocale, items);

        BuiltInFunction bif = new CASFunction(expLocale, expressions);

        Value result = bif.evaluate(new Assembler.Builder().build());
        StringValue expected = new StringValue.Builder().setValue("01234567").setCharacterMode(CharacterMode.ASCII).build();
        assertEquals(expected, result);
        assertEquals(expLocale, result._locale);
    }
}
