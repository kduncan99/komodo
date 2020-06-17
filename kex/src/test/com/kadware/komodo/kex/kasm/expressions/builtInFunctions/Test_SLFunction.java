/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.builtInFunctions;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.CharacterMode;
import com.kadware.komodo.kex.kasm.LineSpecifier;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.StringValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
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
public class Test_SLFunction {

    @Test
    public void test(
    ) throws ExpressionException {

        List<ExpressionItem> items = new LinkedList<>();
        StringValue sv = new StringValue.Builder().setValue("Hello Stupid").setCharacterMode(CharacterMode.ASCII).build();
        items.add(new ValueItem(sv));

        Locale expLocale = new Locale(new LineSpecifier(0, 10), 11);
        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(expLocale, items);

        BuiltInFunction bif = new SLFunction(expLocale, expressions);

        Value result = bif.evaluate(new Assembler.Builder().build());
        IntegerValue expected = new IntegerValue.Builder().setValue(12).build();
        assertEquals(expected, result);
        assertEquals(expLocale, result._locale);
    }
}
