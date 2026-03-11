/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestAccessPermissions {

    @Test
    public void testConstants() {
        assertTrue(AccessPermissions.ALL.canEnter());
        assertTrue(AccessPermissions.ALL.canRead());
        assertTrue(AccessPermissions.ALL.canWrite());

        assertFalse(AccessPermissions.NONE.canEnter());
        assertFalse(AccessPermissions.NONE.canRead());
        assertFalse(AccessPermissions.NONE.canWrite());
    }

    @Test
    public void testDefaultConstructor() {
        AccessPermissions ap = new AccessPermissions();
        assertFalse(ap.canEnter());
        assertFalse(ap.canRead());
        assertFalse(ap.canWrite());
    }

    @Test
    public void testConstructorWithBooleans() {
        AccessPermissions ap = new AccessPermissions(true, false, true);
        assertTrue(ap.canEnter());
        assertFalse(ap.canRead());
        assertTrue(ap.canWrite());
    }

    @Test
    public void testConstructorWithSource() {
        AccessPermissions source = new AccessPermissions(true, false, true);
        AccessPermissions ap = new AccessPermissions(source);
        assertTrue(ap.canEnter());
        assertFalse(ap.canRead());
        assertTrue(ap.canWrite());
        assertEquals(source, ap);
        assertNotSame(source, ap);
    }

    @Test
    public void testConstructorWithInt() {
        AccessPermissions ap = new AccessPermissions(05); // 101 binary -> enter=true, read=false, write=true
        assertTrue(ap.canEnter());
        assertFalse(ap.canRead());
        assertTrue(ap.canWrite());
    }

    @Test
    public void testClear() {
        AccessPermissions ap = new AccessPermissions(true, true, true);
        AccessPermissions returned = ap.clear();
        assertSame(ap, returned);
        assertFalse(ap.canEnter());
        assertFalse(ap.canRead());
        assertFalse(ap.canWrite());
    }

    @Test
    public void testFromComposite() {
        AccessPermissions ap = new AccessPermissions();
        
        ap.fromComposite(0);
        assertFalse(ap.canEnter()); assertFalse(ap.canRead()); assertFalse(ap.canWrite());

        ap.fromComposite(1);
        assertFalse(ap.canEnter()); assertFalse(ap.canRead()); assertTrue(ap.canWrite());

        ap.fromComposite(2);
        assertFalse(ap.canEnter()); assertTrue(ap.canRead()); assertFalse(ap.canWrite());

        ap.fromComposite(3);
        assertFalse(ap.canEnter()); assertTrue(ap.canRead()); assertTrue(ap.canWrite());

        ap.fromComposite(4);
        assertTrue(ap.canEnter()); assertFalse(ap.canRead()); assertFalse(ap.canWrite());

        ap.fromComposite(5);
        assertTrue(ap.canEnter()); assertFalse(ap.canRead()); assertTrue(ap.canWrite());

        ap.fromComposite(6);
        assertTrue(ap.canEnter()); assertTrue(ap.canRead()); assertFalse(ap.canWrite());

        ap.fromComposite(7);
        assertTrue(ap.canEnter()); assertTrue(ap.canRead()); assertTrue(ap.canWrite());
    }

    @Test
    public void testToComposite() {
        assertEquals((byte)00, new AccessPermissions(false, false, false).toComposite());
        assertEquals((byte)01, new AccessPermissions(false, false, true).toComposite());
        assertEquals((byte)02, new AccessPermissions(false, true, false).toComposite());
        assertEquals((byte)03, new AccessPermissions(false, true, true).toComposite());
        assertEquals((byte)04, new AccessPermissions(true, false, false).toComposite());
        assertEquals((byte)05, new AccessPermissions(true, false, true).toComposite());
        assertEquals((byte)06, new AccessPermissions(true, true, false).toComposite());
        assertEquals((byte)07, new AccessPermissions(true, true, true).toComposite());
    }

    @Test
    public void testSetFromSource() {
        AccessPermissions source = new AccessPermissions(true, false, true);
        AccessPermissions ap = new AccessPermissions();
        AccessPermissions returned = ap.set(source);
        assertSame(ap, returned);
        assertTrue(ap.canEnter());
        assertFalse(ap.canRead());
        assertTrue(ap.canWrite());
    }

    @Test
    public void testFluentSetters() {
        AccessPermissions ap = new AccessPermissions();
        AccessPermissions returned = ap.setCanEnter(true)
                                        .setCanRead(true)
                                        .setCanWrite(true);
        assertSame(ap, returned);
        assertTrue(ap.canEnter());
        assertTrue(ap.canRead());
        assertTrue(ap.canWrite());

        ap.setCanEnter(false).setCanRead(false).setCanWrite(false);
        assertFalse(ap.canEnter());
        assertFalse(ap.canRead());
        assertFalse(ap.canWrite());
    }

    @Test
    public void testEqualsAndHashCode() {
        AccessPermissions ap1 = new AccessPermissions(true, false, true);
        AccessPermissions ap2 = new AccessPermissions(true, false, true);
        AccessPermissions ap3 = new AccessPermissions(false, true, false);
        
        assertEquals(ap1, ap2);
        assertNotEquals(ap1, ap3);
        assertNotEquals(ap1, "not an access permission object");
        assertNotEquals(ap1, null);

        assertEquals(ap1.hashCode(), ap2.hashCode());
        // Though not strictly required by the hashCode contract, different values in this case have different hashcodes
        assertNotEquals(ap1.hashCode(), ap3.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("+enter +read +write", new AccessPermissions(true, true, true).toString());
        assertEquals("-enter -read -write", new AccessPermissions(false, false, false).toString());
        assertEquals("+enter -read +write", new AccessPermissions(true, false, true).toString());
    }
}
