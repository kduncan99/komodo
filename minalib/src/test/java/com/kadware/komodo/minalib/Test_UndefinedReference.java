/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.baselib.FloatingPointComponents;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.operators.AdditionOperator;
import com.kadware.komodo.minalib.expressions.operators.Operator;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Stack;

import static org.junit.Assert.*;

public class Test_UndefinedReference {

    @Test
    public void test1(
    ) throws ExpressionException {
        UndefinedReference[] refs1 = {};
        UndefinedReference[] expected = {};
        UndefinedReference[] result = UndefinedReference.coalesce(refs1);
        assertArrayEquals(expected, result);
    }

    @Test
    public void test2(
    ) throws ExpressionException {
        FieldDescriptor fd1 = new FieldDescriptor(0, 18);
        UndefinedReference[] refs1 = {
            new UndefinedReferenceToLabel(fd1,false,"FEE"),
            new UndefinedReferenceToLabel(fd1,false,"FOO"),
            new UndefinedReferenceToLabel(fd1,false,"FIE"),
            new UndefinedReferenceToLabel(fd1,false,"FEE"),
            new UndefinedReferenceToLabel(fd1,false,"FOO"),
            new UndefinedReferenceToLabel(fd1,false,"FIE"),
        };

        UndefinedReference[] expected = {
            new UndefinedReferenceToLabel(fd1,false,"FEE"),
            new UndefinedReferenceToLabel(fd1,false,"FEE"),
            new UndefinedReferenceToLabel(fd1,false,"FOO"),
            new UndefinedReferenceToLabel(fd1,false,"FOO"),
            new UndefinedReferenceToLabel(fd1,false,"FIE"),
            new UndefinedReferenceToLabel(fd1,false,"FIE"),
        };

        UndefinedReference[] result = UndefinedReference.coalesce(refs1);
        assertArrayEquals(expected, result);
    }

    @Test
    public void test3(
    ) throws ExpressionException {
        FieldDescriptor fd1 = new FieldDescriptor(0, 18);
        UndefinedReference[] refs1 = {
            new UndefinedReferenceToLabel(fd1,false,"FEE"),
            new UndefinedReferenceToLabel(fd1,false,"FOO"),
            new UndefinedReferenceToLabel(fd1,false,"FIE"),
            new UndefinedReferenceToLabel(fd1,true,"FEE"),
            new UndefinedReferenceToLabel(fd1,true,"FOO"),
            new UndefinedReferenceToLabel(fd1,true,"FIE"),
        };

        UndefinedReference[] expected = {};

        UndefinedReference[] result = UndefinedReference.coalesce(refs1);
        assertArrayEquals(expected, result);
    }

    @Test
    public void test4(
    ) throws ExpressionException {
        FieldDescriptor fd1 = new FieldDescriptor(0, 18);
        FieldDescriptor fd2 = new FieldDescriptor(0, 19);
        UndefinedReference[] refs1 = {
            new UndefinedReferenceToLabel(fd1,false,"FEE"),
            new UndefinedReferenceToLabel(fd1,false,"FOO"),
            new UndefinedReferenceToLabel(fd1,false,"FIE"),
            new UndefinedReferenceToLocationCounter(fd1, true, 20),
            new UndefinedReferenceToLabel(fd2,true,"FEE"),
            new UndefinedReferenceToLabel(fd2,true,"FOO"),
            new UndefinedReferenceToLabel(fd2,true,"FIE"),
            new UndefinedReferenceToLocationCounter(fd2, false, 20),
        };

        UndefinedReference[] expected = {
            new UndefinedReferenceToLabel(fd1,false,"FEE"),
            new UndefinedReferenceToLabel(fd1,false,"FOO"),
            new UndefinedReferenceToLabel(fd1,false,"FIE"),
            new UndefinedReferenceToLocationCounter(fd1, true, 20),
            new UndefinedReferenceToLabel(fd2,true,"FEE"),
            new UndefinedReferenceToLabel(fd2,true,"FOO"),
            new UndefinedReferenceToLabel(fd2,true,"FIE"),
            new UndefinedReferenceToLocationCounter(fd2, false, 20),
        };

        UndefinedReference[] result = UndefinedReference.coalesce(refs1);
        assertArrayEquals(expected, result);
    }
}
