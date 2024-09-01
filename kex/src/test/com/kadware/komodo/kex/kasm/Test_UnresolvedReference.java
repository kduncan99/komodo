/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.oldbaselib.FieldDescriptor;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_UnresolvedReference {

    @Test
    public void test1(
    ) {
        UnresolvedReference[] refs1 = {};
        UnresolvedReference[] expected = {};
        UnresolvedReference[] result = UnresolvedReference.coalesce(refs1);
        assertArrayEquals(expected, result);
    }

    @Test
    public void test2(
    ) {
        FieldDescriptor fd1 = new FieldDescriptor(0, 18);
        UnresolvedReference[] refs1 = {
            new UnresolvedReferenceToLabel(fd1, false, "FEE"),
            new UnresolvedReferenceToLabel(fd1, false, "FOO"),
            new UnresolvedReferenceToLabel(fd1, false, "FIE"),
            new UnresolvedReferenceToLabel(fd1, false, "FEE"),
            new UnresolvedReferenceToLabel(fd1, false, "FOO"),
            new UnresolvedReferenceToLabel(fd1, false, "FIE"),
        };

        UnresolvedReference[] expected = {
            new UnresolvedReferenceToLabel(fd1, false, "FEE"),
            new UnresolvedReferenceToLabel(fd1, false, "FEE"),
            new UnresolvedReferenceToLabel(fd1, false, "FOO"),
            new UnresolvedReferenceToLabel(fd1, false, "FOO"),
            new UnresolvedReferenceToLabel(fd1, false, "FIE"),
            new UnresolvedReferenceToLabel(fd1, false, "FIE"),
        };

        UnresolvedReference[] result = UnresolvedReference.coalesce(refs1);
        assertArrayEquals(expected, result);
    }

    @Test
    public void test3(
    ) {
        FieldDescriptor fd1 = new FieldDescriptor(0, 18);
        UnresolvedReference[] refs1 = {
            new UnresolvedReferenceToLabel(fd1, false, "FEE"),
            new UnresolvedReferenceToLabel(fd1, false, "FOO"),
            new UnresolvedReferenceToLabel(fd1, false, "FIE"),
            new UnresolvedReferenceToLabel(fd1, true, "FEE"),
            new UnresolvedReferenceToLabel(fd1, true, "FOO"),
            new UnresolvedReferenceToLabel(fd1, true, "FIE"),
        };

        UnresolvedReference[] expected = {};

        UnresolvedReference[] result = UnresolvedReference.coalesce(refs1);
        assertArrayEquals(expected, result);
    }

    @Test
    public void test4(
    ) {
        FieldDescriptor fd1 = new FieldDescriptor(0, 18);
        FieldDescriptor fd2 = new FieldDescriptor(0, 19);
        UnresolvedReference[] refs1 = {
            new UnresolvedReferenceToLabel(fd1, false, "FEE"),
            new UnresolvedReferenceToLabel(fd1, false, "FOO"),
            new UnresolvedReferenceToLabel(fd1, false, "FIE"),
            new UnresolvedReferenceToLocationCounter(fd1, true, 20),
            new UnresolvedReferenceToLabel(fd2, true, "FEE"),
            new UnresolvedReferenceToLabel(fd2, true, "FOO"),
            new UnresolvedReferenceToLabel(fd2, true, "FIE"),
            new UnresolvedReferenceToLocationCounter(fd2, false, 20),
        };

        UnresolvedReference[] expected = {
            new UnresolvedReferenceToLabel(fd1, false, "FEE"),
            new UnresolvedReferenceToLabel(fd1, false, "FOO"),
            new UnresolvedReferenceToLabel(fd1, false, "FIE"),
            new UnresolvedReferenceToLocationCounter(fd1, true, 20),
            new UnresolvedReferenceToLabel(fd2, true, "FEE"),
            new UnresolvedReferenceToLabel(fd2, true, "FOO"),
            new UnresolvedReferenceToLabel(fd2, true, "FIE"),
            new UnresolvedReferenceToLocationCounter(fd2, false, 20),
        };

        UnresolvedReference[] result = UnresolvedReference.coalesce(refs1);
        assertArrayEquals(expected, result);
    }
}
