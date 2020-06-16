/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.LineSpecifier;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostics;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.items.ExpressionItem;
import com.kadware.komodo.kex.kasm.expressions.items.OperatorItem;
import com.kadware.komodo.kex.kasm.expressions.items.ValueItem;
import com.kadware.komodo.kex.kasm.expressions.operators.AdditionOperator;
import com.kadware.komodo.kex.kasm.expressions.operators.MultiplicationOperator;
import com.kadware.komodo.kex.kasm.expressions.operators.Operator;
import java.util.LinkedList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class Test_CompositeExpression {

    //TODO unit tests need created

//    @Test
//    public void evaluateSimpleValue(
//    ) throws ExpressionException {
//        Locale loc = new Locale(new LineSpecifier(0, 1), 1);
//
//        Value val = new IntegerValue.Builder().setLocale(loc).setValue(42).build();
//        List<ExpressionItem> items = new LinkedList<>();
//        items.add(new ValueItem(val));
//        Expression exp = new Expression(loc, items);
//
//        Assembler assembler = new Assembler.Builder().build();
//        Diagnostics diagnostics = new Diagnostics();
//        Value result = exp.evaluate(assembler);
//
//        assertTrue(diagnostics.isEmpty());
//        assertEquals(val, result);
//        assertEquals(loc, result._locale);
//    }
//
//    @Test
//    public void evaluateSimpleMath(
//    ) throws ExpressionException {
//        LineSpecifier ls = new LineSpecifier(0, 10);
//        Locale expLocale = new Locale(ls, 21);
//        Locale lhsLocale = new Locale(ls, 21);
//        Locale opLocale = new Locale(ls, 26);
//        Locale rhsLocale = new Locale(ls, 27);
//
//        Value addend1 = new IntegerValue.Builder().setLocale(lhsLocale).setValue(42).build();
//        Operator operator = new AdditionOperator(opLocale);
//        Value addend2 = new IntegerValue.Builder().setLocale(rhsLocale).setValue(112).build();
//        Value expected = new IntegerValue.Builder().setValue(154).build();
//
//        List<ExpressionItem> items = new LinkedList<>();
//        items.add(new ValueItem(addend1));
//        items.add(new OperatorItem(operator));
//        items.add(new ValueItem(addend2));
//        Expression exp = new Expression(expLocale, items);
//
//        Assembler assembler = new Assembler.Builder().build();
//        Diagnostics diagnostics = new Diagnostics();
//        Value result = exp.evaluate(assembler);
//
//        assertTrue(diagnostics.isEmpty());
//        assertEquals(expected, result);
//        assertEquals(opLocale, result._locale);
//    }
//
//    @Test
//    public void evaluateSimpleMathWithPrecedence(
//    ) throws ExpressionException {
//        LineSpecifier ls = new LineSpecifier(0, 10);
//        Locale expLocale = new Locale(ls, 21);
//        Locale operand1Locale = new Locale(ls, 21);
//        Locale operator1Locale = new Locale(ls, 26);
//        Locale operand2Locale = new Locale(ls, 27);
//        Locale operator2Locale = new Locale(ls, 32);
//        Locale operand3Locale = new Locale(ls, 33);
//
//        //  expression is 5 + 7 * 12...  it should be evaluated at 5 + (7 * 12) == 89
//        Value operand1 = new IntegerValue.Builder().setLocale(operand1Locale).setValue(5).build();
//        Operator operator1 = new AdditionOperator(operator1Locale);
//        Value operand2 = new IntegerValue.Builder().setLocale(operand2Locale).setValue(7).build();
//        Operator operator2 = new MultiplicationOperator(operator2Locale);
//        Value operand3 = new IntegerValue.Builder().setLocale(operand3Locale).setValue(12).build();
//        Value expected = new IntegerValue.Builder().setValue(89).build();
//
//        List<ExpressionItem> items = new LinkedList<>();
//        items.add(new ValueItem(operand1));
//        items.add(new OperatorItem(operator1));
//        items.add(new ValueItem(operand2));
//        items.add(new OperatorItem(operator2));
//        items.add(new ValueItem(operand3));
//        Expression exp = new Expression(expLocale, items);
//
//        Assembler assembler = new Assembler.Builder().build();
//        Diagnostics diagnostics = new Diagnostics();
//        Value result = exp.evaluate(assembler);
//
//        assertTrue(diagnostics.isEmpty());
//        assertEquals(expected, result);
//        assertEquals(operator1Locale, result._locale);
//    }
}
