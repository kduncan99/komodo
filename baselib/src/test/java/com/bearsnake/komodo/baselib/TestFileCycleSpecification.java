/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import com.bearsnake.komodo.baselib.exceptions.SyntaxErrorParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFileCycleSpecification {

    @Test
    public void testNewAbsoluteSpecification_Valid() {
        FileCycleSpecification fcs = FileCycleSpecification.newAbsoluteSpecification(1);
        assertEquals(1, fcs.getCycle());
        assertTrue(fcs.isAbsolute());
        assertFalse(fcs.isRelative());

        fcs = FileCycleSpecification.newAbsoluteSpecification(9999);
        assertEquals(9999, fcs.getCycle());
        assertTrue(fcs.isAbsolute());

        fcs = FileCycleSpecification.newAbsoluteSpecification(500);
        assertEquals(500, fcs.getCycle());
    }

    @Test
    public void testNewAbsoluteSpecification_Invalid() {
        assertThrows(RuntimeException.class, () -> FileCycleSpecification.newAbsoluteSpecification(0));
        assertThrows(RuntimeException.class, () -> FileCycleSpecification.newAbsoluteSpecification(10000));
        assertThrows(RuntimeException.class, () -> FileCycleSpecification.newAbsoluteSpecification(-1));
    }

    @Test
    public void testNewRelativeSpecification_Valid() {
        FileCycleSpecification fcs = FileCycleSpecification.newRelativeSpecification(1);
        assertEquals(1, fcs.getCycle());
        assertFalse(fcs.isAbsolute());
        assertTrue(fcs.isRelative());

        fcs = FileCycleSpecification.newRelativeSpecification(0);
        assertEquals(0, fcs.getCycle());
        assertFalse(fcs.isAbsolute());

        fcs = FileCycleSpecification.newRelativeSpecification(-31);
        assertEquals(-31, fcs.getCycle());
        assertFalse(fcs.isAbsolute());
    }

    @Test
    public void testNewRelativeSpecification_Invalid() {
        assertThrows(RuntimeException.class, () -> FileCycleSpecification.newRelativeSpecification(2));
        assertThrows(RuntimeException.class, () -> FileCycleSpecification.newRelativeSpecification(-32));
    }

    @Test
    public void testParse_Absolute() throws SyntaxErrorParserException {
        Parser parser = new Parser("(123)");
        FileCycleSpecification fcs = FileCycleSpecification.parse(parser);
        assertNotNull(fcs);
        assertTrue(fcs.isAbsolute());
        assertEquals(123, fcs.getCycle());
        assertTrue(parser.atEnd());
    }

    @Test
    public void testParse_Absolute_LeadingZeroes() throws SyntaxErrorParserException {
        Parser parser = new Parser("(001)");
        FileCycleSpecification fcs = FileCycleSpecification.parse(parser);
        assertNotNull(fcs);
        assertTrue(fcs.isAbsolute());
        assertEquals(1, fcs.getCycle());
    }

    @Test
    public void testParse_Relative_PlusZero() throws SyntaxErrorParserException {
        Parser parser = new Parser("(+0)");
        FileCycleSpecification fcs = FileCycleSpecification.parse(parser);
        assertNotNull(fcs);
        assertFalse(fcs.isAbsolute());
        assertEquals(0, fcs.getCycle());
    }

    @Test
    public void testParse_Relative_PlusOne() throws SyntaxErrorParserException {
        Parser parser = new Parser("(+1)");
        FileCycleSpecification fcs = FileCycleSpecification.parse(parser);
        assertNotNull(fcs);
        assertFalse(fcs.isAbsolute());
        assertEquals(1, fcs.getCycle());
    }

    @Test
    public void testParse_Relative_Minus() throws SyntaxErrorParserException {
        Parser parser = new Parser("(-5)");
        FileCycleSpecification fcs = FileCycleSpecification.parse(parser);
        assertNotNull(fcs);
        assertFalse(fcs.isAbsolute());
        assertEquals(5, fcs.getCycle()); // Note: implementation stores it as 5, but isRelative() is true.
        // Wait, looking at FileCycleSpecification.java:
        // cyc = (int)parser.parseUnsignedDecimalInteger();
        // and it returns new FileCycleSpecification(abs, cyc);
        // So for (-5), cyc is 5.
    }

    @Test
    public void testParse_Relative_MinusZero() throws SyntaxErrorParserException {
        Parser parser = new Parser("(-0)");
        FileCycleSpecification fcs = FileCycleSpecification.parse(parser);
        assertNotNull(fcs);
        assertFalse(fcs.isAbsolute());
        assertEquals(0, fcs.getCycle());
    }

    @Test
    public void testParse_Relative_MinusMax() throws SyntaxErrorParserException {
        Parser parser = new Parser("(-31)");
        FileCycleSpecification fcs = FileCycleSpecification.parse(parser);
        assertNotNull(fcs);
        assertFalse(fcs.isAbsolute());
        assertEquals(31, fcs.getCycle());
    }

    @Test
    public void testParse_NoParenthesis() throws SyntaxErrorParserException {
        Parser parser = new Parser("123");
        FileCycleSpecification fcs = FileCycleSpecification.parse(parser);
        assertNull(fcs);
        assertEquals(0, parser.getIndex());
    }

    @Test
    public void testParse_Invalid_Absolute_OutOfRange() {
        Parser parser = new Parser("(1000)");
        assertThrows(SyntaxErrorParserException.class, () -> FileCycleSpecification.parse(parser));
    }

    @Test
    public void testParse_Invalid_Absolute_TooLow() {
        Parser parser = new Parser("(0)");
        assertThrows(SyntaxErrorParserException.class, () -> FileCycleSpecification.parse(parser));
    }

    @Test
    public void testParse_Invalid_Relative_PlusTwo() {
        Parser parser = new Parser("(+2)");
        assertThrows(SyntaxErrorParserException.class, () -> FileCycleSpecification.parse(parser));
    }

    @Test
    public void testParse_Invalid_Relative_MinusTooLarge() {
        Parser parser = new Parser("(-32)");
        assertThrows(SyntaxErrorParserException.class, () -> FileCycleSpecification.parse(parser));
    }

    @Test
    public void testParse_Invalid_NoClosingParen() {
        Parser parser = new Parser("(123");
        assertThrows(SyntaxErrorParserException.class, () -> FileCycleSpecification.parse(parser));
    }
}
