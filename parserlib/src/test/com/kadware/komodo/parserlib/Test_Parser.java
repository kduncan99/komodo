/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.parserlib;

import com.kadware.komodo.parserlib.exceptions.ParserException;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class Test_Parser {

    @Test
    public void atEnd(
    ) throws ParserException {
        String input = "abcde";
        Parser p = new Parser(input);
        assertFalse(p.atEnd());
        p.skip(4);
        assertFalse(p.atEnd());
        p.skip(1);
        assertTrue(p.atEnd());
    }

    @Test
    public void next(
    ) throws ParserException {
        String input = "abcde";
        Parser p = new Parser(input);
        assertEquals(p.next(), 'a');
        assertEquals(p.next(), 'b');
        assertEquals(p.next(), 'c');
    }

    @Test
    public void parseInteger(
    ) throws ParserException {
        String input = "12345";
        Parser p = new Parser(input);
        assertEquals(12345, p.parseInteger());
    }

    @Test
    public void parseInteger_stopChars(
    ) throws ParserException {
        String input1 = "123x";
        String input2 = "456y";
        String input3 = "789z";
        String stopChars = "xyz";
        Parser p1 = new Parser(input1);
        assertEquals(123, p1.parseInteger(stopChars));
        Parser p2 = new Parser(input2);
        assertEquals(456, p2.parseInteger(stopChars));
        Parser p3 = new Parser(input3);
        assertEquals(789, p3.parseInteger(stopChars));
    }

    @Test
    public void parseLong(
    ) throws ParserException {
        String input = "12345";
        Parser p = new Parser(input);
        assertEquals(12345, p.parseLong());
    }

    @Test
    public void parseLong_stopChars(
    ) throws ParserException {
        String input1 = "123x";
        String input2 = "456y";
        String input3 = "789z";
        String stopChars = "xyz";
        Parser p1 = new Parser(input1);
        assertEquals(123, p1.parseLong(stopChars));
        Parser p2 = new Parser(input2);
        assertEquals(456, p2.parseLong(stopChars));
        Parser p3 = new Parser(input3);
        assertEquals(789, p3.parseLong(stopChars));
    }

    @Test
    public void parseSpecific(
    ) throws ParserException {
        String input = "valid";
        Parser p = new Parser(input);
        p.parseSpecificToken("valid");
    }

    @Test
    public void parseSpecific_Insensitive(
    ) throws ParserException {
        String input = "vAlId";
        Parser p = new Parser(input);
        p.parseSpecificTokenCaseInsensitive("valid");
    }

    @Test
    public void peek(
    ) throws ParserException {
        String input = "abcde";
        Parser p = new Parser(input);
        assertEquals(p.peek(), 'a');
        assertEquals(p.peek(), 'a');
        assertEquals(p.peek(), 'a');
    }

    @Test
    public void skipSpaces(
    ) throws ParserException {
        String input = "     x       ";
        Parser p = new Parser(input);
        assertFalse(p.atEnd());
        assertEquals(' ', p.peek());
        assertEquals(5, p.skipSpaces());
        assertFalse(p.atEnd());
        assertEquals('x', p.next());
        assertEquals(7, p.skipSpaces());
        assertTrue(p.atEnd());
    }

    @Test
    public void skipWhiteSpace(
    ) throws ParserException {
        String input = "     x       ";
        Parser p = new Parser(input);
        assertFalse(p.atEnd());
        assertEquals(' ', p.peek());
        assertEquals(5, p.skipWhiteSpace());
        assertFalse(p.atEnd());
        assertEquals('x', p.next());
        assertEquals(7, p.skipWhiteSpace());
        assertTrue(p.atEnd());
    }
}
