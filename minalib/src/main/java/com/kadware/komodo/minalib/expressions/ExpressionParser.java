/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions;

import com.kadware.komodo.baselib.FloatingPointComponents;
import com.kadware.komodo.baselib.exceptions.CharacteristicOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristicUnderflowException;
import com.kadware.komodo.baselib.exceptions.DivideByZeroException;
import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.expressions.items.*;
import com.kadware.komodo.minalib.expressions.operators.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expression evaluator
 */
@SuppressWarnings("Duplicates")
public class ExpressionParser {

    private static final Pattern FLOATING_POINT_PATTERN = Pattern.compile("\\d+\\.\\d+");

    private final String _text;
    private final Locale _textLocale;
    private int _index;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    public ExpressionParser(
        final String text,
        final Locale textLocale
    ) {
        _text = text;
        _textLocale = textLocale;
        _index = 0;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private basic-functionality methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Are we at the end of the expression text?
     * @return true if we've reached the end of the text, else false
     */
    private boolean atEnd(
    ) {
        return _index >= _text.length();
    }

    /**
     * Generates a new Locale object to reflect the location of the text at _index
     * @return generated locale object
     */
    private Locale getLocale(
    ) {
        return new Locale(_textLocale.getLineSpecifier(), _textLocale.getColumn() + _index);
    }

    /**
     * Retrieves the next character and advances the index
     * @return the character in text, indexed by _index, else 0
     */
    private char getNextChar(
    ) {
        return atEnd() ? 0 : _text.charAt(_index++);
    }

    /**
     * Peek at the next character
     * @return the character in text, indexed by _index unless we're at the end, then 0.
     */
    private char nextChar(
    ) {
        return atEnd() ? 0 : _text.charAt(_index);
    }

    /**
     * Skips the next character in text, as indexed by _index.
     * Does nothing if we are atEnd().
     * Usually used (conditionally) after nextChar().
     */
    private void skipNextChar(
    ) {
        if (!atEnd()) {
            ++_index;
        }
    }

    /**
     * Skips a fixed token if it exists in the source code at _index
     * @param token token to be parsed
     * @return true if token was found and skipped, else false
     */
    private boolean skipToken(
        final String token
    ) {
        int remain = _text.length() - _index;
        if (remain >= token.length()) {
            for (int tx = 0; tx < token.length(); ++tx) {
                char ch1 = Character.toUpperCase(token.charAt(tx));
                char ch2 = Character.toUpperCase(_text.charAt(_index + tx));
                if (ch1 != ch2) {
                    return false;
                }
            }

            _index += token.length();
            return true;
        }

        return false;
    }

    /**
     * Skips past whitespace - not usually a thing, except inside grouping symbols
     */
    private void skipWhitespace(
    ) {
        while (!atEnd() && (_text.charAt(_index) == ' ')) {
            ++_index;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private methods (possibly protected for unit minalib purposes)
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Parses an expression - starts at the current _index, and continues until we find something that doesn't
     * belong in the expression.
     * @param context Current assembler context
     * @return a parsed Expression object if we found one, else null
     * @throws ExpressionException if we detect an obvious error
     */
    private Expression parseExpression(
        final Context context
    ) throws ExpressionException {
        List<IExpressionItem> expItems = new LinkedList<>();

        boolean allowInfixOperator = false;
        boolean allowPostfixOperator = false;
        boolean allowPrefixOperator = true;
        boolean allowOperand = true;

        skipWhitespace();
        while (!atEnd()) {
            if (allowInfixOperator) {
                IExpressionItem item = parseInfixOperator();
                if (item != null) {
                    expItems.add(item);
                    allowInfixOperator = false;
                    allowOperand = true;
                    allowPostfixOperator = false;
                    allowPrefixOperator = true;
                    skipWhitespace();
                    continue;
                }
            }

            if (allowOperand) {
                IExpressionItem item = parseOperand(context);
                if (item != null) {
                    expItems.add(item);
                    allowInfixOperator = true;
                    allowPostfixOperator = true;
                    allowPrefixOperator = false;
                    allowOperand = false;
                    skipWhitespace();
                    continue;
                }
            }

            if (allowPrefixOperator) {
                IExpressionItem item = parsePrefixOperator();
                if (item != null) {
                    expItems.add(item);
                    skipWhitespace();
                    continue;
                }
            }

            if (allowPostfixOperator) {
                IExpressionItem item = parsePostfixOperator();
                if (item != null) {
                    expItems.add(item);
                    skipWhitespace();
                    continue;
                }
            }

            //  we've found something we don't understand.
            //  If we haven't found anything yet, then we don't have an expression.
            //  If we *have* found something, this is the end of the expression.
            if (expItems.isEmpty()) {
                return null;
            } else {
                //  end of expression is not allowed if we are expecting an operand
                if (allowOperand) {
                    context.appendDiagnostic(new ErrorDiagnostic(getLocale(), "Incomplete expression"));
                    throw new ExpressionException();
                }
                break;
            }
        }

        return new Expression(expItems);
    }

    /**
     * Parses an expression group.
     * Such a structure is formatted as:
     *      '(' [ expression [ ',' expression ]* ')'
     * @param context Current assembler context
     * @return an array of parsed Expression objects if we found a group, else null
     * @throws ExpressionException if we detect an obvious error
     */
    private Expression[] parseExpressionGroup(
        final Context context
    ) throws ExpressionException {
        if (!skipToken("(")) {
            return null;
        }

        List<Expression> expressions = new LinkedList<>();
        while (!skipToken(")")) {
            Expression exp = parseExpression(context);
            if (exp == null) {
                //  didn't find an expression, and we didn't find a closing paren either... something is wrong
                context.appendDiagnostic(new ErrorDiagnostic(getLocale(), "Syntax error"));
                throw new ExpressionException();
            }

            expressions.add(exp);
            skipWhitespace();
            if (skipToken(",")) {
                continue;
            }

            skipWhitespace();
            if (nextChar() != ')') {
                //  next char isn't a comma, nor a closing paren - again, something is wrong
                context.appendDiagnostic(new ErrorDiagnostic(getLocale(), "Syntax error"));
                throw new ExpressionException();
            }
        }

        return expressions.toArray(new Expression[0]);
    }

    /**
     * Parses a floating point literal value.
     * Format is:
     *   [0-9]+ '.' [0-9]+
     *   Which is to say, a decimal point with at least one digit on both sides
     * @return floating point value OperandItem if found, else null
     */
    private OperandItem parseFloatingPointLiteral(
        final Context context
    ) throws ExpressionException {
        if (atEnd() || !Character.isDigit(nextChar())) {
            return null;
        }

        Matcher m = FLOATING_POINT_PATTERN.matcher(_text.substring(_index));
        if (!m.lookingAt()) {
            return null;
        }

        try {
            double value = Double.parseDouble(_text.substring(_index, _index + m.end()));
            _index += m.end();
            FloatingPointComponents fpc = new FloatingPointComponents(value);
            FloatingPointValue fpValue = new FloatingPointValue.Builder().setValue(fpc).build();
            return new ValueItem(getLocale(), fpValue);
        } catch (NumberFormatException ex) {
            context.appendDiagnostic(new ErrorDiagnostic(getLocale(), "Invalid floating point literal"));
            throw new ExpressionException();
        }
//        int oldIndex = _index;
//        double value = 0.0;
//        while (!atEnd() && Character.isDigit(nextChar())) {
//            char ch = getNextChar();
//            value = (value * 10) + (ch - '0');
//        }
//
//        if (atEnd() || (getNextChar() != '.')) {
//            _index = oldIndex;
//            return null;
//        }
//
//        boolean hasFractionalDigits = false;
//        double divisor = 10.0;
//        while (!atEnd() && Character.isDigit(nextChar())) {
//            char ch = getNextChar();
//            value += ((double)(ch - '0') / divisor);
//            divisor *= 10;
//            hasFractionalDigits = true;
//        }
//
//        if (!hasFractionalDigits) {
//            _index = oldIndex;
//            return null;
//        }
    }

    /**
     * If _index points to an infix operator, we construct an Operator object and return it.
     * @return Operator object if found, else null
     */
    private OperatorItem parseInfixOperator(
    ) {
        Locale locale = getLocale();

        //  Be careful with ordering here... for example, look for '>=' before '>' so we don't get tripped up
        if (skipToken("==")) {
            return new OperatorItem(new NodeIdentityOperator(locale));
        } else if (skipToken("=/=")) {
            return new OperatorItem(new NodeNonIdentityOperator(locale));
        } else if (skipToken("<=")) {
            return new OperatorItem(new LessOrEqualOperator(locale));
        } else if (skipToken(">=")) {
            return new OperatorItem(new GreaterOrEqualOperator(locale));
        } else if (skipToken("<>")) {
            return new OperatorItem(new InequalityOperator(locale));
        } else if (skipToken("=")) {
            return new OperatorItem(new EqualityOperator(locale));
        } else if (skipToken("<")) {
            return new OperatorItem(new LessThanOperator(locale));
        } else if (skipToken(">")) {
            return new OperatorItem(new GreaterThanOperator(locale));
        } else if (skipToken("++")) {
            return new OperatorItem(new OrOperator(locale));
        } else if (skipToken("--")) {
            return new OperatorItem(new XorOperator(locale));
        } else if (skipToken("**")) {
            return new OperatorItem(new AndOperator(locale));
        } else if (skipToken("*/")) {
            return new OperatorItem(new ShiftOperator(locale));
        } else if (skipToken("+")) {
            return new OperatorItem(new AdditionOperator(locale));
        } else if (skipToken("-")) {
            return new OperatorItem(new SubtractionOperator(locale));
        } else if (skipToken("*")) {
            return new OperatorItem(new MultiplicationOperator(locale));
        } else if (skipToken("///")) {
            return new OperatorItem(new DivisionRemainderOperator(locale));
        } else if (skipToken("//")) {
            return new OperatorItem(new DivisionCoveredQuotientOperator(locale));
        } else if (skipToken("/")) {
            return new OperatorItem(new DivisionOperator(locale));
        } else if (skipToken(":")) {
            return new OperatorItem(new ConcatenationOperator(locale));
        }

        return null;
    }

    /**
     * Parses an integer literal value
     * @param context assembler context
     * @return integer literal OperandItem if found, else null
     * @throws ExpressionException if we find something wrong with the integer literal (presuming we found one)
     */
    private OperandItem parseIntegerLiteral(
        final Context context
    ) throws ExpressionException {
        if (atEnd() || !Character.isDigit(nextChar())) {
            return null;
        }

        long value = 0;
        int radix = 10;
        int digits = 0;
        while (!atEnd() && Character.isDigit(nextChar())) {
            char ch = getNextChar();
            if ((radix == 8) && ((ch == '8') || (ch == '9'))) {
                context.appendDiagnostic(new ErrorDiagnostic(getLocale(), "Invalid digit in octal literal"));
                throw new ExpressionException();
            }

            if ((ch == '0') && (digits == 0)) {
                radix = 8;
            }

            value = (value * radix) + (ch - '0');
            ++digits;
        }

        return new ValueItem(getLocale(), new IntegerValue.Builder().setValue(value).build());
    }

    /**
     * Parses a label or reference (okay, a label would be a reference) from _index
     * @param context assembler context
     * @return the label if found, else null
     */
    String parseLabel(
        final Context context
    ) {
        //  Check first character - it must be acceptable as the first character of a label.
        if (atEnd()) {
            return null;
        }

        char ch = nextChar();
        if (!Character.isAlphabetic(ch) && (ch != '$') && (ch != '_')) {
            return null;
        }

        //  So, we *might* have a label - parse through until we get to the end of the label.
        //  I don't think there's anything we could find at this point, which isn't at least
        //  an *attempt* at a label, so we'll flag too-many-characters as an expression exception.
        StringBuilder sb = new StringBuilder();
        sb.append(ch);
        skipNextChar();
        while (!atEnd()) {
            ch = nextChar();
            if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) && (ch != '$') && (ch != '_')) {
                break;
            }

            if (sb.length() == 12) {
                context.appendDiagnostic(new ErrorDiagnostic(getLocale(), "Label or Reference too long"));
            }

            skipNextChar();
            sb.append(ch);
        }

        return sb.toString();
    }

    /**
     * Parses a literal
     * @param context Current assembler context
     * @return OperandItem representing the literal if found, else null
     * @throws ExpressionException if we find something wrong with the integer literal (presuming we found one)
     */
    private OperandItem parseLiteral(
        final Context context
    ) throws ExpressionException {
        OperandItem opItem = parseStringLiteral(context);
        if (opItem == null) {
            opItem = parseFloatingPointLiteral(context);
        }
        if (opItem == null) {
            opItem = parseIntegerLiteral(context);
        }

        return opItem;
    }

    /**
     * Parses anything which could be an operand
     * @param context Current assembler context
     * @return parsed OperandItem if found, else null
     * @throws ExpressionException if we find something wrong with the integer literal (presuming we found one)
     */
    private OperandItem parseOperand(
        final Context context
    ) throws ExpressionException {
        Expression[] expGroup = parseExpressionGroup(context);
        if (expGroup != null) {
            return new ExpressionGroupItem(_textLocale, expGroup);
        }

        OperandItem opItem = parseLiteral(context);
        if (opItem == null) {
            opItem = parseReference(context);
        }

        return opItem;
    }

    /**
     * If _index points to a postfix operator, we construct an Operator object and return it.
     * @return Operator object if found, else null
     */
    private OperatorItem parsePostfixOperator(
    ) {
        Locale locale = getLocale();

        if (skipToken("L")) {
            return new OperatorItem(new LeftJustificationOperator(locale));
        } else if (skipToken("R")) {
            return new OperatorItem(new RightJustificationOperator(locale));
        } else if (skipToken("D")) {
            return new OperatorItem(new DoublePrecisionOperator(locale));
        } else if (skipToken("S")) {
            return new OperatorItem(new SinglePrecisionOperator(locale));
        }

        return null;
    }

    /**
     * If _index points to a prefix operator, we construct an Operator object and return it.
     * @return Operator object if found, else null
     */
    private OperatorItem parsePrefixOperator(
    ) {
        Locale locale = getLocale();

        if (skipToken("*")) {
            return new OperatorItem(new FlaggedOperator(locale));
        } else if (skipToken("+")) {
            return new OperatorItem(new PositiveOperator(locale));
        } else if (skipToken("-")) {
            return new OperatorItem(new NegativeOperator(locale));
        } else if (skipToken("\\")) {
            return new OperatorItem(new NotOperator(locale));
        }

        return null;
    }

    /**
     * Parses a reference to a label, node, or function.
     * Because they are all formatted as such:
     *      {identifier} [ '(' {expression_list} ')' ]
     * we don't know at this time which is which - that can only be determined from the dictionary,
     * when the value is to be resolved.
     * @param context Current assembler context
     * @return newly created ReferenceItem object
     * @throws ExpressionException if something is syntactically wrong
     */
    ReferenceItem parseReference(
        final Context context
    ) throws ExpressionException {
        String nodeName = parseLabel(context);
        if (nodeName == null) {
            return null;
        }

        //  Parse the expression group (if there is one)... there doesn't have to be one.
        Expression[] argExpressions = parseExpressionGroup(context);
        return new ReferenceItem(new Locale(_textLocale.getLineSpecifier(), _textLocale.getColumn() + _index),
                                 nodeName,
                                 argExpressions);
    }

    /**
     * Parses a string literal into an appropriate Value object
     * @param context context
     * @return OperandItem object if we find a valid string literal, null if we don't find any string literal
     * @throws ExpressionException if there is an error in the formatting of the string literal
     */
    private OperandItem parseStringLiteral(
        final Context context
    ) throws ExpressionException {
        if (atEnd() || (nextChar() != '\'')) {
            return null;
        }

        skipNextChar();
        StringBuilder sb = new StringBuilder();
        boolean terminated = false;
        while (!atEnd()) {
            char ch = getNextChar();
            if (ch == '\'') {
                if (!atEnd() && (nextChar() == '\'')) {
                    //  two single quotes - makes one quote in the string
                    sb.append(ch);
                    skipNextChar();
                } else {
                    terminated = true;
                    break;
                }
            } else {
                sb.append(ch);
            }
        }

        //  Did we hit atEnd() before we found a terminating quote?
        if (!terminated) {
            context.appendDiagnostic(new QuoteDiagnostic(getLocale(), "Unterminated string literal"));
            throw new ExpressionException();
        }

        return new ValueItem(getLocale(), new StringValue.Builder().setValue(sb.toString()).build());
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Parses the given text, within the given context, into the IExpressionItem list in an Expression object
     * @param context Current assembler context
     * @return A properly formatted Expression object which can subsequently be evaluated
     * @throws ExpressionException if we run into a problem while we are working on a valid expressoin
     */
    public Expression parse(
        final Context context
    ) throws ExpressionException {
        _index = 0;

        return parseExpression(context);
    }
}
