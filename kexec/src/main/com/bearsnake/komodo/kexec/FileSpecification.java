/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.exec.Exec;

public class FileSpecification {

    // specified qualifier - if empty, there was an asterisk but no qualifier.
    // if null, there was neither a qualifier nor an asterisk.
    private final String _qualifier;

    // specified filename - this is the only item which is non-optional
    private final String _fileName;

    private final FileCycleSpecification _fileCycleSpec;
    private final String _readKey;
    private final String _writeKey;

    public FileSpecification(
        final String qualifier,
        final String fileName,
        final FileCycleSpecification fileCycle,
        final String readKey,
        final String writeKey
    ) {
        _qualifier = qualifier;
        _fileName = fileName;
        _fileCycleSpec = fileCycle;
        _readKey = readKey;
        _writeKey = writeKey;
    }

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
     * @throws Parser.SyntaxException if we have a spec in the input text, but it contains errors
     */
    public static FileSpecification parse(
        final Parser parser,
        final String cutSet
    ) throws Parser.SyntaxException {
        String qualifier = null;
        String filename;

        var token = parser.parseUntil("*" + cutSet);
        if (parser.peekNext() == '*') {
            if (!token.isEmpty() && !Exec.isValidQualifier(token)) {
                throw new Parser.SyntaxException("Invalid qualifier");
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
            throw new Parser.SyntaxException("Invalid filename");
        }

        FileCycleSpecification fsSpec = FileCycleSpecification.parse(parser);

        String readKey = null;
        String writeKey = null;
        if (parser.parseChar('/')) {
            readKey = parser.parseUntil(cutSet);
            if (readKey.isEmpty()) {
                readKey = null;
            } else if (!Exec.isValidReadWriteKey(readKey)) {
                throw new Parser.SyntaxException("Invalid read key");
            }
            if (parser.parseChar('/')) {
                writeKey = parser.parseUntil(cutSet);
                if (writeKey.isEmpty()) {
                    writeKey = null;
                } else if (!Exec.isValidReadWriteKey(writeKey)) {
                    throw new Parser.SyntaxException("Invalid write key");
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
        sb.append(_fileName);
        if (_fileCycleSpec != null) {
            sb.append('(').append(_fileCycleSpec.getCycle()).append(')');
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
