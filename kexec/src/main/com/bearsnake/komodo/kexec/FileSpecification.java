/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.exec.Exec;

public class FileSpecification {

    public abstract static class Exception extends java.lang.Exception {}
    public static class InvalidQualifierException extends Exception {}
    public static class InvalidFilenameException extends Exception {}
    public static class InvalidFileCycleException extends Exception {}
    public static class InvalidReadKeyException extends Exception {}
    public static class InvalidWriteKeyException extends Exception {}

    // specified qualifier - if empty, there was an asterisk but no qualifier.
    // if null, there was neither a qualifier nor an asterisk.
    private final String _qualifier;

    // specified filename - this is the only item which is non-optional
    private final String _filename;

    private final FileCycleSpecification _fileCycleSpecification; // if null, no cycle was specified
    private final String _readKey; // if null, no read key was specified
    private final String _writeKey; // if null, no write key was specified

    public FileSpecification(
        final String qualifier,
        final String filename,
        final FileCycleSpecification fileCycle,
        final String readKey,
        final String writeKey
    ) {
        _qualifier = qualifier == null ? null : qualifier.toUpperCase();
        _filename = filename.toUpperCase();
        _fileCycleSpecification = fileCycle;
        _readKey = (readKey == null) || (readKey.isEmpty()) ? null : readKey.toUpperCase();
        _writeKey = (writeKey == null) || (writeKey.isEmpty()) ? null : writeKey.toUpperCase();
    }

    public FileSpecification(
        final String qualifier,
        final String filename
    ) {
        this(qualifier, filename, null, null, null);
    }

    public boolean couldBeInternalName() {
        return _qualifier == null && _fileCycleSpecification == null && _readKey == null && _writeKey == null;
    }

    public FileCycleSpecification getFileCycleSpecification() { return _fileCycleSpecification; }
    public String getFilename() { return _filename; }
    public String getQualifier() { return _qualifier; }
    public String getReadKey() { return _readKey; }
    public String getWriteKey() { return _writeKey; }
    public boolean hasFileCycleSpecification() { return _fileCycleSpecification != null; }
    public boolean hasQualifier() { return _qualifier != null; }

    /**
     * parses a FileSpecification object from the given text.
     * Format is:
     *      [[ qualifier ] '*' ] filename [ '(' cycle-spec ')' ] [ '/' [ read-key ] [ '/' [ write-key ] ] ]
     * cycle-spec is:
     *      [ abs-value ] | [ rel-spec ]
     * abs-value is a numeric string of digits, the value of which varies from 1 to 999 inclusive
     * rel-spec is
     *      [ '+' [ '0' | '1' ] | '-' rel-value ]
     * rel-value is a numeric string of digits, the value of which varies from 0 to 31 inclusive
     * @param parser Parser containing text to be parsed, positioned at the start of the text to be parsed
     * @return FileSpecification object if we have a valid filename and no syntax errors, null if no filename is found
     */
    public static FileSpecification parse(
        final Parser parser,
        final String cutSet
    ) throws FileSpecification.Exception {
        String qualifier = null;
        String filename;

        var token = parser.parseUntil("*" + cutSet);
        if (parser.peekNext() == '*') {
            if (!token.isEmpty() && !Exec.isValidQualifier(token)) {
                throw new InvalidQualifierException();
            }

            qualifier = token;
            parser.skipNext();

            filename = parser.parseUntil(cutSet);
        } else {
            filename = token;
        }

        if (filename.isEmpty()) {
            return null;
        } else if (!Exec.isValidFilename(filename)) {
            throw new InvalidFilenameException();
        }

        FileCycleSpecification fsSpec;
        try {
            fsSpec = FileCycleSpecification.parse(parser);
        } catch (Parser.SyntaxException ex) {
            throw new InvalidFileCycleException();
        }

        String readKey = null;
        String writeKey = null;
        if (parser.parseChar('/')) {
            readKey = parser.parseUntil(cutSet);
            if (readKey.isEmpty()) {
                readKey = null;
            } else if (!Exec.isValidReadWriteKey(readKey)) {
                throw new InvalidReadKeyException();
            }
            if (parser.parseChar('/')) {
                writeKey = parser.parseUntil(cutSet);
                if (writeKey.isEmpty()) {
                    writeKey = null;
                } else if (!Exec.isValidReadWriteKey(writeKey)) {
                    throw new InvalidWriteKeyException();
                }
            }
        }

        return new FileSpecification(qualifier, filename, fsSpec, readKey, writeKey);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        if (_qualifier != null) {
            sb.append(_qualifier).append('*');
        }
        sb.append(_filename);
        if (_fileCycleSpecification != null) {
            sb.append('(').append(_fileCycleSpecification.getCycle()).append(')');
        }
        if ((_readKey != null) || (_writeKey != null)) {
            sb.append("/");
            if (_readKey != null) {
                sb.append(_readKey);
            }
            if (_writeKey != null) {
                sb.append("/").append(_writeKey);
            }
        }

        return sb.toString();
    }
}
