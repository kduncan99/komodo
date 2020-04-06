/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.parserlib;

import com.kadware.komodo.parserlib.exceptions.DataFormatParserException;
import com.kadware.komodo.parserlib.exceptions.NotFoundParserException;
import com.kadware.komodo.parserlib.exceptions.OutOfDataParserException;
import com.kadware.komodo.parserlib.exceptions.StackEmptyParserException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Stack;

@SuppressWarnings("Duplicates")
public class Parser {

    private final char[] _text;
    private int _index;
    private Stack<Integer> _stack = new Stack<>();

    /**
     * Instantiates a parser with the given text, setting the index to zero
     * @param text text to be parsed
     */
    public Parser(
        final String text
    ) {
        _text = text.toCharArray();
        _index = 0;
    }

    /**
     * Instantiates a parser with the given text, setting the index to zero
     * @param bytes text to be parsed
     * @param charSet character set to be used
     */
    public Parser(
        final byte[] bytes,
        final Charset charSet
    ) {
        _text = new String(bytes, charSet).toCharArray();
        _index = 0;
    }

    /**
     * Instantiates a parser with the given text, setting the index to zero
     * @param inputStream contains text to be parsed
     * @param charSet character set to be used
     */
    public Parser(
        final InputStream inputStream,
        final Charset charSet
    ) {
        byte[] temp;
        try {
            temp = inputStream.readAllBytes();
        } catch (IOException ex) {
            temp = new byte[0];
        }

        _text = new String(temp, charSet).toCharArray();
        _index = 0;
    }

    /**
     * Indicates whether there are any more characters to be parsed
     * @return true if there is at least one more character, else false
     */
    public boolean atEnd() {
        return _index == _text.length;
    }

    /**
     * Retrieves the next character and advances the index
     * @return the next character
     * @throws OutOfDataParserException if there are no more characters to be parsed
     */
    public char next(
    ) throws OutOfDataParserException {
        if (!atEnd()) {
            return _text[_index++];
        } else {
            throw new OutOfDataParserException(_index);
        }
    }

    /**
     * Parses an integer value from the index.
     * Stops when any non-digit character is reached
     * @return integer parsed from the text
     * @throws NotFoundParserException If the first character parsed is not a digit
     * @throws OutOfDataParserException If there are no characters left to be parsed before we parse at least one digit
     */
    public int parseInteger(
    ) throws NotFoundParserException,
             OutOfDataParserException {
        if (atEnd()) {
            throw new OutOfDataParserException(_index);
        }

        StringBuilder sb = new StringBuilder();
        while (!atEnd() && Character.isDigit(peek())) {
            sb.append(next());
        }
        if (sb.length() == 0) {
            throw new NotFoundParserException(_index);
        }

        return Integer.parseInt(sb.toString());
    }

    /**
     * Parses an integer value from the index.
     * Stops when any of the stop characters are reached.
     * @param stopCharacters String containing the characters which stop the parser
     * @return integer parsed from the text
     * @throws DataFormatParserException If we reach a character which is neither a digit, nor in the stop characters
     * @throws NotFoundParserException If the first character parsed is not a digit
     * @throws OutOfDataParserException If there are no characters left to be parsed before we parse at least one digit
     */
    public int parseInteger(
        final CharSequence stopCharacters
    ) throws DataFormatParserException,
             NotFoundParserException,
             OutOfDataParserException {
        if (atEnd()) {
            throw new OutOfDataParserException(_index);
        }

        StringBuilder sb = new StringBuilder();
        int tx = _index;
        while (!atEnd()) {
            char ch = peek();
            if (Character.isDigit(ch)) {
                sb.append(next());
            } else {
                boolean found = false;
                for (int scx = 0; scx < stopCharacters.length(); ++scx) {
                    if (ch == stopCharacters.charAt(scx)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new DataFormatParserException(tx);
                } else {
                    break;
                }
            }
        }
        if (sb.length() == 0) {
            throw new NotFoundParserException(_index);
        }

        return Integer.parseInt(sb.toString());
    }

    /**
     * Parses an integer value from the index.
     * Stops when any non-digit character is reached
     * @return long integer parsed from the text
     * @throws NotFoundParserException If the first character parsed is not a digit
     * @throws OutOfDataParserException If there are no characters left to be parsed before we parse at least one digit
     */
    public long parseLong(
    ) throws NotFoundParserException,
             OutOfDataParserException {
        if (atEnd()) {
            throw new OutOfDataParserException(_index);
        }

        StringBuilder sb = new StringBuilder();
        while (!atEnd() && Character.isDigit(peek())) {
            sb.append(next());
        }
        if (sb.length() == 0) {
            throw new NotFoundParserException(_index);
        }

        return Long.parseLong(sb.toString());
    }

    /**
     * Parses an integer value from the index.
     * Stops when any of the stop characters are reached.
     * @param stopCharacters String containing the characters which stop the parser
     * @return long integer parsed from the text
     * @throws DataFormatParserException If we reach a character which is neither a digit, nor in the stop characters
     * @throws NotFoundParserException If the first character parsed is not a digit
     * @throws OutOfDataParserException If there are no characters left to be parsed before we parse at least one digit
     */
    public long parseLong(
        final CharSequence stopCharacters
    ) throws DataFormatParserException,
             NotFoundParserException,
             OutOfDataParserException {
        if (atEnd()) {
            throw new OutOfDataParserException(_index);
        }

        StringBuilder sb = new StringBuilder();
        int tx = _index;
        while (!atEnd()) {
            char ch = peek();
            if (Character.isDigit(ch)) {
                sb.append(next());
            } else {
                boolean found = false;
                for (int scx = 0; scx < stopCharacters.length(); ++scx) {
                    if (ch == stopCharacters.charAt(scx)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new DataFormatParserException(tx);
                } else {
                    break;
                }
            }
        }
        if (sb.length() == 0) {
            throw new NotFoundParserException(_index);
        }

        return Long.parseLong(sb.toString());
    }

    public void parseSpecificToken(
        final String sample
    ) throws NotFoundParserException,
             OutOfDataParserException {
        char[] sampleArray = sample.toCharArray();
        int tx = _index;
        for (int sx = 0; sx < sample.length(); ++sx, ++tx) {
            if (tx == _text.length) {
                throw new OutOfDataParserException(tx);
            } else if (sampleArray[sx] != _text[tx]) {
                throw new NotFoundParserException(tx);
            }
        }

        _index = tx;
    }

    public void parseSpecificTokenCaseInsensitive(
        final String sample
    ) throws NotFoundParserException,
             OutOfDataParserException {
        char[] sampleArray = sample.toLowerCase().toCharArray();
        int tx = _index;
        for (int sx = 0; sx < sample.length(); ++sx, ++tx) {
            if (tx == _text.length) {
                throw new OutOfDataParserException(tx);
            }

            char ch = _text[tx];
            if (Character.isUpperCase(ch)) {
                ch = Character.toLowerCase(ch);
            }

            if (sampleArray[sx] != ch) {
                throw new NotFoundParserException(tx);
            }
        }

        _index = tx;
    }

    public char peek(
    ) throws OutOfDataParserException {
        if (!atEnd()) {
            return _text[_index];
        } else {
            throw new OutOfDataParserException(_index);
        }
    }

    public void popIndex(
    ) throws StackEmptyParserException {
        if (_stack.isEmpty()) {
            throw new StackEmptyParserException(_index);
        } else {
            _index = _stack.pop();
        }
    }

    public void pushIndex() {
        _stack.add(_index);
    }

    public int remaining() {
        return _text.length - _index;
    }

    public void reset() {
        _index = 0;
    }

    public void skip(
        final int count
    ) throws OutOfDataParserException {
        if (count > remaining()) {
            throw new OutOfDataParserException(_index);
        }

        _index += count;
    }

    public int skipSpaces() {
        int count = 0;
        while (!atEnd() && Character.isSpaceChar(_text[_index])) {
            count++;
            _index++;
        }

        return count;
    }

    public int skipWhiteSpace() {
        int count = 0;
        while (!atEnd() && Character.isWhitespace(_text[_index])) {
            count++;
            _index++;
        }

        return count;
    }
}
