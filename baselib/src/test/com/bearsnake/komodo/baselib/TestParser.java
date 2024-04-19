/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestParser {

    @Test
    public void testConstructor() {
        var p = new Parser("Hellow");
        assertEquals("Hellow", p.getText());
        assertEquals(0, p.getIndex());
        assertEquals(1, p.getTerminators().size());
    }

    @Test
    public void testSkipNext() {
        var p = new Parser("Hellow");
        p.skipNext();
        p.skipNext();
        assertEquals(2, p.getIndex());
    }

    @Test
    public void testParseRemaining() {
        var p = new Parser("Hellow");
        p.skipNext();
        p.skipNext();
        assertEquals("llow", p.parseRemaining());
    }

    @Test
    public void testParseUntil() {
        var p = new Parser("This is a string of text");
        assertEquals("Thi", p.parseUntil("sa "));
        assertEquals("s is a st", p.parseUntil("ngr"));
        assertEquals("ring of text", p.parseUntil("z"));
        assertEquals(24, p.getIndex());
        assertTrue(p.atEnd());
    }

    @Test
    public void testParseToken() {
        var p = new Parser("This is a string of text");
        assertFalse(p.parseToken("string"));
        p.parseUntil(" ");
        p.skipSpaces();
        assertTrue(p.parseToken("is"));
        assertEquals(7, p.getIndex());
    }

    @Test
    public void testParseChar() {
        var p = new Parser("abcd");
        assertFalse(p.parseChar('b'));
        p.skipNext();
        assertTrue(p.parseChar('b'));
        assertEquals(2, p.getIndex());
    }

    @Test
    public void testDecimalInteger() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("1234506789 blue");
        assertEquals(1234506789, p.parseUnsignedDecimalInteger());
    }

    @Test(expected = Parser.NotFoundException.class)
    public void testDecimalIntegerNotFound() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("red1234506789blue");
        p.parseUnsignedDecimalInteger();
    }

    @Test(expected = Parser.SyntaxException.class)
    public void testDecimalIntegerSyntax() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("1234506789blue");
        p.parseUnsignedDecimalInteger();
    }

    @Test
    public void testHexInteger_x() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("0x0135abf blue");
        assertEquals(0x135abf, p.parseUnsignedHexInteger());
    }

    @Test
    public void testHexInteger_X() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("0XDeadF00d11 blue");
        assertEquals(0xdeadf00d11L, p.parseUnsignedHexInteger());
    }

    @Test(expected = Parser.NotFoundException.class)
    public void testHexIntegerNotFound() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("1234");
        p.parseUnsignedHexInteger();
    }

    @Test(expected = Parser.SyntaxException.class)
    public void testHexIntegerSyntax() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("0xafg");
        p.parseUnsignedHexInteger();
    }

    @Test
    public void testOctalInteger() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("01234567 blue");
        assertEquals(01234567, p.parseUnsignedOctalInteger());
    }

    @Test(expected = Parser.NotFoundException.class)
    public void testOctalIntegerNotFound() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("1234");
        p.parseUnsignedOctalInteger();
    }

    @Test(expected = Parser.SyntaxException.class)
    public void testOctalIntegerSyntax() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("01234506789");
        p.parseUnsignedOctalInteger();
    }

    @Test
    public void testInteger_Decimal() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("5445899034555");
        assertEquals(5445899034555L, p.parseUnsignedInteger());
    }

    @Test
    public void testInteger_Hex() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("0xDEADfaceBeef");
        assertEquals(0xdeadfacebeefL, p.parseUnsignedInteger());
    }

    @Test
    public void testInteger_Octal() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("000000000770077554321");
        assertEquals(0770077554321L, p.parseUnsignedInteger());
    }

    @Test
    public void testIdentifier() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser(" ident:");
        p.skipSpaces();
        var ident = p.parseIdentifier(5, ",: ");
        assertEquals("ident", ident);
    }

    @Test(expected = Parser.NotFoundException.class)
    public void testIdentifierNotFound() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("   $ident:   ");
        p.skipSpaces();
        p.parseIdentifier(5, ",: ");
    }

    @Test(expected = Parser.SyntaxException.class)
    public void testIdentifierSyntaxBadChar() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("   ident$ifier:   ");
        p.skipSpaces();
        p.parseIdentifier(8, ",: ");
    }

    @Test(expected = Parser.SyntaxException.class)
    public void testIdentifierSyntaxTooLong() throws Parser.SyntaxException, Parser.NotFoundException {
        var p = new Parser("   identifier:   ");
        p.skipSpaces();
        p.parseIdentifier(8, ",: ");
    }
}
