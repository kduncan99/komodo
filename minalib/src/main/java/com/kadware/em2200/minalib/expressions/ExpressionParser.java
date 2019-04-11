/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.builtInFunctions.*;
import com.kadware.em2200.minalib.expressions.operators.*;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * Expression evaluator
 */
public class ExpressionParser {

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
     * <p>
     * @return
     */
    private boolean atEnd(
    ) {
        return _index >= _text.length();
    }

    /**
     * Generates a new Locale object to reflect the location of the text at _index
     * <p>
     * @return
     */
    private Locale getLocale(
    ) {
        return new Locale(_textLocale.getLineNumber(), _textLocale.getColumn() + _index);
    }

    /**
     * Retrieves the next character and advances the index
     * <p>
     * @return the character in text, indexed by _index
     */
    private char getNextChar(
    ) throws NotFoundException {
        if (atEnd()) {
            throw new NotFoundException("End of text");
        }
        return _text.charAt(_index++);
    }

    /**
     * Peek at the next character
     * <p>
     * @return the character in text, indexed by _index
     */
    private char nextChar(
    ) throws NotFoundException {
        if (atEnd()) {
            throw new NotFoundException("End of text");
        }
        return _text.charAt(_index);
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
     * <p>
     * @param token token to be parsed
     * @param caseSensitive true if we are sensitive to case, else false
     * <p>
     * @return true if token was found and skipped, else false
     */
    private boolean skipToken(
        final String token,
        final boolean caseSensitive
    ) {
        int remain = _text.length() - _index;
        if (remain >= token.length()) {
            for (int tx = 0; tx < token.length(); ++tx) {
                char ch1 = token.charAt(tx);
                char ch2 = _text.charAt(_index + tx);
                if (caseSensitive) {
                    ch1 = Character.toUpperCase(ch1);
                    ch2 = Character.toUpperCase(ch2);
                }
                if (ch1 != ch2) {
                    return false;
                }
            }

            _index += token.length();
            return true;
        }

        return false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private methods (possibly protected for unit test purposes)
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Parses an expression - starts at the current _index, and continues until we find something that doesn't
     * belong in the expression.
     * <p>
     * @param context Current assembler context
     * @param diagnostics Where we post any necessary diagnostics
     * <p>
     * @return a parsed Expression object
     * <p>
     * @throws ExpressionException if we detect an obvious error
     * @throws NotFoundException if we do not find such a structure
     */
    private Expression parseExpression(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException,
             NotFoundException {
        List<ExpressionItem> expItems = new LinkedList<>();

        boolean allowInfixOperator = false;
        boolean allowPostfixOperator = false;
        boolean allowPrefixOperator = true;
        boolean allowOperand = true;
        int prefixOperatorPrecedence = 0;       //  allow anything
        int postfixOperatorPrecedence = 99;     //  allow nothing

        while (!atEnd()) {
            if (allowInfixOperator) {
                try {
                    expItems.add(parseInfixOperator());
                    allowInfixOperator = false;
                    allowOperand = true;
                    allowPostfixOperator = false;
                    postfixOperatorPrecedence = 99;
                    allowPrefixOperator = true;
                    prefixOperatorPrecedence = 0;
                    continue;
                } catch (NotFoundException ex) {
                    //  skip
                }
            }

            if (allowOperand) {
                try {
                    expItems.add(parseOperand(context, diagnostics));
                    allowInfixOperator = true;
                    allowPostfixOperator = true;
                    postfixOperatorPrecedence = 0;
                    allowPrefixOperator = false;
                    prefixOperatorPrecedence = 99;
                    allowOperand = false;
                    continue;
                } catch (NotFoundException ex) {
                    //  skip
                }
            }

            if (allowPrefixOperator) {
                //????
            }

            if (allowPostfixOperator) {
                //????
            }

            //  we've found something we don't understand.
            //  If we haven't found anything yet, then we don't have an expression.
            //  If we *have* found something, this is the end of the expression.
            if (expItems.isEmpty()) {
                throw new NotFoundException();
            } else {
                //  end of expression is not allowed if we are expecting an operand
                if (allowOperand) {
                    diagnostics.append(new ErrorDiagnostic(getLocale(), "Incomplete expression"));
                    throw new ExpressionException();
                }
                break;
            }
        }

        return new Expression(expItems);
    }

//    /**
//     * Parses an expression group.
//     * Such a structure is formatted as:
//     *      '(' [ expression [ ',' expression ]* ')'
//     * These entities are used for function argument lists and possibly for other purposes.
//     * <p>
//     * @param context Current assembler context
//     * @param diagnostics Where we post any necessary diagnostics
//     * <p>
//     * @return an array of parsed Expression objects
//     * <p>
//     * @throws ExpressionException if we detect an obvious error
//     * @throws NotFoundException if we do not find such a structure
//     */
//    protected Expression[] parseExpressionGroup(
//        final Context context,
//        Diagnostics diagnostics
//    ) throws ExpressionException,
//             NotFoundException {
//        if (!skipToken("(", true)) {
//            throw new NotFoundException();
//        }
//
//        List<Expression> expressions = new LinkedList<>();
//        while (!skipToken(")", true)) {
//            try {
//                expressions.add(parseExpression(context, diagnostics));
//            } catch (NotFoundException ex) {
//                //  didn't find an expression, and we didn't find a closing paren either... something is wrong
//                diagnostics.append(new ErrorDiagnostic(getLocale(), "Syntax error"));
//                throw new ExpressionException();
//            }
//
//            if (skipToken(",", true)) {
//                continue;
//            }
//
//            if (nextChar() != ')') {
//                //  next char isn't a comma, nor a closing paren - again, something is wrong
//                diagnostics.append(new ErrorDiagnostic(getLocale(), "Syntax error"));
//                throw new ExpressionException();
//            }
//        }
//
//        return expressions.toArray(new Expression[expressions.size()]);
//    }

    /**
     * Parses a function specification from _index
     * <p>
     * @param context Current assembler context
     * @param diagnostics Where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws ExpressionException
     * @throws NotFoundException
     */
    protected FunctionItem parseFunction(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException,
             NotFoundException {
        //  We're looking for {label} '(' {expr}* ')'
        //  If we don't find the above combination, we bail with NotFoundException
        //  Otherwise, we begin parsing the putative and possibly empty parameter list.
        //  Each parameter is a sub-expression which... gets handled appropriately.

        Locale funcLocale = getLocale();
        int holdIndex = _index;
        String name = parseLabel(diagnostics);

        if (!skipToken("(", true)) {
            throw new NotFoundException();
        }

        List<Expression> argExpressions = new LinkedList<>();
        while (!skipToken(")", true)) {
            try {
                argExpressions.add(parseExpression(context, diagnostics));
            } catch (NotFoundException ex) {
                //  didn't find an expression, and we didn't find a closing paren either... something is wrong
                diagnostics.append(new ErrorDiagnostic(funcLocale, "Syntax error"));
                throw new ExpressionException();
            }

            if (skipToken(",", true)) {
                continue;
            }

            if (nextChar() != ')') {
                //  next char isn't a comma, nor a closing paren - again, something is wrong
                diagnostics.append(new ErrorDiagnostic(funcLocale, "Syntax error"));
                throw new ExpressionException();
            }
        }

        //  Is the label found in the dictionary?
        //  Function refs cannot be forward-referenced...
        Value value;
        try {
            value = context.getDictionary().getValue(name);
        } catch (NotFoundException ex) {
            diagnostics.append(new ErrorDiagnostic(funcLocale, String.format("Function %s not defined", name)));
            throw new ExpressionException();
        }

        //  Is it a built-in function?
        if (value.getType() == ValueType.BuiltInFunction) {
            try {
                Class<?>[] argTypes = { Locale.class, Expression[].class };
                Class<?> clazz = ((BuiltInFunctionValue)value).getClazz();
                Constructor<?> ctor = clazz.getConstructor(argTypes);
                Object[] ctorArgs = { funcLocale, argExpressions.toArray(new Expression[argExpressions.size()]) };
                BuiltInFunction bif = (BuiltInFunction)(ctor.newInstance(ctorArgs));
                return new BuiltInFunctionItem(bif);
            } catch (IllegalAccessException
                     | InstantiationException
                     | InvocationTargetException
                     | NoSuchMethodException ex) {
                throw new InternalErrorRuntimeException(String.format("Caught:%s in ExpressonParser.parseFunction()", ex));
            }
        }

        //  Is it a user function reference?
        if (value.getType() == ValueType.FuncName) {
            //????
        }

        diagnostics.append(new ErrorDiagnostic(getLocale(), String.format("%s is not defined as a function", name)));
        throw new ExpressionException();
    }

    /**
     * Parses a floating point literal value
     * <p>
     * @return
     * <p>
     * @throws ExpressionException if we find something wrong with the integer literal (presuming we found one)
     * @throws NotFoundException if we do not find anything which looks like an integer literal
     */
    protected OperandItem parseFloatingPointLiteral(
    ) throws ExpressionException,
             NotFoundException {
        //????
        throw new NotFoundException();
    }

    /**
     * If _index points to an infix operator, we construct an Operator object and return it.
     * <p>
     * @return Operator object
     * <p>
     * @throws NotFoundException if no post-fix operator was discovered
     */
    protected OperatorItem parseInfixOperator(
    ) throws NotFoundException {
        Locale locale = new Locale(_textLocale.getLineNumber(), _textLocale.getColumn() + _index);

        //  Be careful with ordering here... for example, look for '>=' before '>' so we don't get tripped up
        if (skipToken("==", true)) {
            return new OperatorItem(new NodeIdentityOperator(locale));
        } else if (skipToken("=/=", true)) {
            return new OperatorItem(new NodeNonIdentityOperator(locale));
        } else if (skipToken("<=", true)) {
            return new OperatorItem(new LessOrEqualOperator(locale));
        } else if (skipToken(">=", true)) {
            return new OperatorItem(new GreaterOrEqualOperator(locale));
        } else if (skipToken("<>", true)) {
            return new OperatorItem(new InequalityOperator(locale));
        } else if (skipToken("=", true)) {
            return new OperatorItem(new EqualityOperator(locale));
        } else if (skipToken("<", true)) {
            return new OperatorItem(new LessThanOperator(locale));
        } else if (skipToken(">", true)) {
            return new OperatorItem(new GreaterThanOperator(locale));
        } else if (skipToken("++", true)) {
            return new OperatorItem(new OrOperator(locale));
        } else if (skipToken("--", true)) {
            return new OperatorItem(new XorOperator(locale));
        } else if (skipToken("**", true)) {
            return new OperatorItem(new AndOperator(locale));
        } else if (skipToken("*/", true)) {
            return new OperatorItem(new ShiftOperator(locale));
        } else if (skipToken("+", true)) {
            return new OperatorItem(new AdditionOperator(locale));
        } else if (skipToken("-", true)) {
            return new OperatorItem(new SubtractionOperator(locale));
        } else if (skipToken("*", true)) {
            return new OperatorItem(new MultiplicationOperator(locale));
        } else if (skipToken("///", true)) {
            return new OperatorItem(new DivisionRemainderOperator(locale));
        } else if (skipToken("//", true)) {
            return new OperatorItem(new DivisionCoveredQuotientOperator(locale));
        } else if (skipToken("/", true)) {
            return new OperatorItem(new DivisionOperator(locale));
        } else if (skipToken(":", true)) {
            return new OperatorItem(new ConcatenationOperator(locale));
        }

        throw new NotFoundException();
    }

    /**
     * Parses an integer literal value
     * <p>
     * @param diagnostics
     * <p>
     * @return
     * <p>
     * @throws ExpressionException if we find something wrong with the integer literal (presuming we found one)
     * @throws NotFoundException if we do not find anything which looks like an integer literal
     */
    protected OperandItem parseIntegerLiteral(
        Diagnostics diagnostics
    ) throws ExpressionException,
             NotFoundException {
        if (atEnd() || !Character.isDigit(nextChar())) {
            throw new NotFoundException();
        }

        StringBuilder sb = new StringBuilder();
        int radix = 10;
        while (!atEnd() && Character.isDigit(nextChar())) {
            char ch = getNextChar();
            if ((radix == 8) && ((ch == '8') || (ch == '9'))) {
                diagnostics.append(new ErrorDiagnostic(getLocale(), "Invalid digit in octal literal"));
                throw new ExpressionException();
            }

            if ((ch == '0') && (sb.length() == 0)) {
                radix = 8;
            }
            sb.append(ch);
        }

        BigInteger bi = new BigInteger(sb.toString());
        OnesComplement.OnesComplement72Result ocResult = new OnesComplement.OnesComplement72Result();
        OnesComplement.getOnesComplement72(bi, ocResult);
        if (ocResult._overflow) {
            diagnostics.append(new TruncationDiagnostic(getLocale(), "Integer literal is too large"));
        }

        return new ValueItem(new IntegerValue.Builder().setValue(ocResult._result)
                                                       .build());
    }

    /**
     * Parses a label from _index
     * <p>
     * @param diagnostics
     * <p>
     * @return
     * <p>
     * @throws ExpressionException
     * @throws NotFoundException
     */
    protected String parseLabel(
        Diagnostics diagnostics
    ) throws ExpressionException,
             NotFoundException {
        //  Check first character - it must be acceptable as the first character of a label.
        if (atEnd()) {
            throw new NotFoundException();
        }

        char ch = nextChar();
        if (!Character.isAlphabetic(ch) && (ch != '$') && (ch != '_')) {
            throw new NotFoundException();
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
                diagnostics.append(new ErrorDiagnostic(getLocale(), "Label or Reference too long"));
            }

            skipNextChar();
            sb.append(ch);
        }

        return sb.toString();
    }

    /**
     * Parses a literal
     * <p>
     * @param context Current assembler context
     * @param diagnostics Where we post any necessary diagnostics
     * <p>
     * @return OperandItem representing the integer literal
     * <p>
     * @throws ExpressionException
     * @throws NotFoundException
     */
    protected OperandItem parseLiteral(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException,
             NotFoundException {
        try {
            return parseStringLiteral(context, diagnostics);
        } catch (NotFoundException ex) {
            //  keep going
        }

        //???? parse float literal here

        try {
            return parseIntegerLiteral(diagnostics);
        } catch (NotFoundException ex) {
            //  keep going
        }

        throw new NotFoundException();
    }

    /**
     * Parses anything which could be an operand
     * <p>
     * @param context Current assembler context
     * @param diagnostics Where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws ExpressionException
     * @throws NotFoundException
     */
    protected OperandItem parseOperand(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException,
             NotFoundException {
        try {
            return parseLiteral(context, diagnostics);
        } catch (NotFoundException ex) {
            //  keep going
        }

        try {
            return parseFunction(context, diagnostics);
        } catch (NotFoundException ex) {
            //  keep going
        }

        try {
            return parseReference();
        } catch (NotFoundException ex) {
            //  keep going
        }

        throw new NotFoundException();
    }

    /**
     * If _index points to a postfix operator, we construct an Operator object and return it.
     * Since StringValue doesn't like justification, we do not support L and R post-fix operators.
     * <p>
     * @return Operator object
     * <p>
     * @throws NotFoundException if no post-fix operator was discovered
     */
    protected OperatorItem parsePostfixOperator(
    ) throws NotFoundException {
        Locale locale = new Locale(_textLocale.getLineNumber(), _textLocale.getColumn() + _index);

        if (skipToken("S", false)) {
            return new OperatorItem(new SinglePrecisionOperator(locale));
        } else if (skipToken("D", false)) {
            return new OperatorItem(new DoublePrecisionOperator(locale));
        }

        throw new NotFoundException();
    }

    /**
     * If _index points to a prefix operator, we construct an Operator object and return it.
     * <p>
     * @return Operator object
     * <p>
     * @throws NotFoundException if no post-fix operator was discovered
     */
    protected OperatorItem parsePrefixOperator(
    ) throws NotFoundException {
        Locale locale = getLocale();

        if (skipToken("*", true)) {
            return new OperatorItem(new FlaggedOperator(locale));
        } else if (skipToken("+", true)) {
            return new OperatorItem(new PositiveOperator(locale));
        } else if (skipToken("-", true)) {
            return new OperatorItem(new NegativeOperator(locale));
        } else if (skipToken("\\", true)) {
            return new OperatorItem(new NotOperator(locale));
        }
        throw new NotFoundException();
    }

    protected OperandItem parseReference(
    ) throws ExpressionException,
             NotFoundException {
        //????
        throw new ExpressionException();//????
    }

    /**
     * Parses a string literal into an appropriate Value object
     * <p>
     * @param context
     * @param diagnostics
     * <p>
     * @return OperandItem object if we find a valid string literal.
     * <p>
     * @throws ExpressionException if there is an error in the formatting of the string literal
     * @throws NotFoundException if we do not find a string literal at all - this may NOT be an error
     */
    protected OperandItem parseStringLiteral(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException,
             NotFoundException {
        if (atEnd() || (nextChar() != '\'')) {
            throw new NotFoundException("");
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
            diagnostics.append(new QuoteDiagnostic(getLocale(), "Unterminated string literal"));
            throw new ExpressionException();
        }

        return new ValueItem(new StringValue.Builder().setValue(sb.toString())
                                                      .setCharacterMode(context.getCharacterMode())
                                                      .build());
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Parses the given text, within the given context, into the ExpressionItem list in an Expression object
     * <p>
     * @param context Current assembler context
     * @param diagnostics Where we post any necessary diagnostics
     * <p>
     * @return A properly formatted Expression object which can subsequently be evaluated
     * <p>
     * @throws ExpressionException if we run into a problem while we are working on a valid expressoin
     * @throws NotFoundException if we don't find anything that even looks like an expression
     */
    public Expression parse(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException,
             NotFoundException {
        _index = 0;

        Expression exp = parseExpression(context, diagnostics);
        if (!atEnd()) {
            //???? post some shit
            throw new ExpressionException();
        }

        return exp;
    }
}
