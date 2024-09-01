/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.items;

import com.kadware.komodo.oldbaselib.FieldDescriptor;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.UnresolvedReference;
import com.kadware.komodo.kex.kasm.UnresolvedReferenceToLabel;
import com.kadware.komodo.kex.kasm.UnresolvedReferenceToLocationCounter;
import com.kadware.komodo.kex.kasm.dictionary.BuiltInFunctionValue;
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.NodeValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.expressions.builtInFunctions.BuiltInFunction;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.ValueDiagnostic;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Represents a dictionary reference within an expression.
 * This could be a reference to a simple value, to a node, or to a function.
 * It is generated from parsing something as follows:
 *      {identifier} [ '(' {expression_list} ')' ]
 * Where the expression list contains zero or more zero-delimited expressions.
 * Due to the overloading of synax across these potential reference types,
 * we don't actually know what the reference is until evaluation time, at which point
 * we poll the dictionary to figure out what the code is really asking for.
 */
public class ReferenceItem extends OperandItem {

    private final Expression[] _expressions;
    private final String _reference;

    /**
     * constructor
     * @param locale location of this entity
     * @param reference reference for this entity
     * @param expressions expressions defining the node selectors for a node reference or arguments for a function call.
     *                    Note that a null reference here means that no parenthesis were specified, which means
     *                      () we have a reference to a simple label, or
     *                      () a reference to the number of top-level members of a node, or
     *                      () a function which accepts zero or more arguments
     *                    while a reference to an empty array menas
     *                      () a reference to the number of top-level members of a node, or
     *                      () a function which accepts zero or more arguments
     */
    public ReferenceItem(
        final Locale locale,
        final String reference,
        final Expression[] expressions
    ) {
        super(locale);
        _reference = reference;
        _expressions = expressions;
    }


    /**
     * Evaluates the reference based on the dictionary
     */
    @Override
    public Value resolve(
        final Assembler assembler
    ) throws ExpressionException {
        try {
            //  Look up the reference in the dictionary.
            //  It must be a particular type of value, else we have an expression exception.
            Dictionary.ValueInfo vInfo = assembler.getDictionary().getValueInfo(_reference);
            Value v = vInfo._value;
            switch (v.getType()) {
                case Integer: {
                    //  If this has a location counter index associated with it, we don't want to resolve it yet
                    //  because we might need it for a linker special thing such as LBDICALL$.
                    //  If this is the case, then we produce an undefined reference instead of resolving...
                    IntegerValue iv = (IntegerValue) v;
                    for (UnresolvedReference ur : iv._references) {
                        if (ur instanceof UnresolvedReferenceToLocationCounter) {
                            UnresolvedReference[] refs = {
                                new UnresolvedReferenceToLabel(new FieldDescriptor(0, 36),
                                                               false,
                                                               _reference)
                            };
                            return new IntegerValue.Builder().setLocale(_locale)
                                                             .setValue(0)
                                                             .setReferences(refs)
                                                             .build();
                        }
                    }

                    //  Otherwise, resolve the label
                    return resolveLabel(assembler, v);
                }

                case FloatingPoint:
                case String:
                    return resolveLabel(assembler, v);

                case BuiltInFunction:
                    return resolveFunction(assembler, v);

                case Equf:
                    //  It's an EQUF, but we cannot resolve it here.
                    //  Turn it into an UnresolvedReference
                    UnresolvedReference[] refs = {
                        new UnresolvedReferenceToLabel(new FieldDescriptor(0, 36),
                                                       false,
                                                       _reference)
                    };
                    return new IntegerValue.Builder().setValue(0).setReferences(refs).build();

                case Node:
                    return resolveNode(assembler, v);

                case UserFunction:
                    //TODO

                default:
                    assembler.appendDiagnostic(new ValueDiagnostic(_locale, "Wrong value type referenced"));
                    throw new ExpressionException();
            }
        } catch (NotFoundException ex) {
            //  reference not found - create an IntegerValue with a value of zero
            //  and an attached positive UnresolvedReference.
            UnresolvedReference[] refs = {
                new UnresolvedReferenceToLabel(new FieldDescriptor(0, 36),
                                               false,
                                               _reference)
            };
            return new IntegerValue.Builder().setValue(0).setReferences(refs).build();
        }
    }

    /**
     * Resolves a value on the assumption it is a function call
     * @param assembler context of execution
     * @param value the value which serves as the source of the resolution
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something goes wrong with the process (we presume something has been posted to diagnostics)
     */
    private Value resolveFunction(
        final Assembler assembler,
        final Value value
    ) throws ExpressionException {
        try {
            Class<?>[] argTypes = { Locale.class, Expression[].class };
            Class<?> clazz = ((BuiltInFunctionValue) value)._class;
            Constructor<?> ctor = clazz.getConstructor(argTypes);
            Object[] ctorArgs = {
                _locale,
                _expressions == null ? new Expression[0] : _expressions
            };
            BuiltInFunction bif = (BuiltInFunction)(ctor.newInstance(ctorArgs));
            return bif.evaluate(assembler);
        } catch (IllegalAccessException
                | InstantiationException
                | InvocationTargetException
                | NoSuchMethodException ex) {
            throw new RuntimeException(String.format("Caught:%s in ExpressonParser.parseFunction()", ex));
        }
    }

    /**
     * Resolves a value on the assumption it is a simple label
     * @param assembler context of execution
     * @param value the value which serves as the source of the resolution
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something goes wrong with the process (we presume something has been posted to diagnostics)
     */
    private Value resolveLabel(
        final Assembler assembler,
        final Value value
    ) throws ExpressionException {
        if (_expressions != null) {
            assembler.appendDiagnostic(new ErrorDiagnostic(_locale,
                                                           "Attempt to apply node selectors to a non-node value"));
            throw new ExpressionException();
        }
        return value;
    }

    /**
     * Resolves a value on the assumption it is a node.
     * @param assembler context of execution
     * @param value the value which serves as the source of the resolution
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something goes wrong with the process (we presume something has been posted to diagnostics)
     */
    private Value resolveNode(
        final Assembler assembler,
        final Value value
    ) throws ExpressionException {
        Value currentValue = value;
        if (_expressions != null) {
            for (Expression exp : _expressions) {
                if (!(currentValue instanceof NodeValue)) {
                    assembler.appendDiagnostic(new ValueDiagnostic(_locale,
                                                                   "Arity of reference exceeds arity of node"));
                    throw new ExpressionException();
                }

                Value selectorValue = exp.evaluate(assembler);
                try {
                    currentValue = ((NodeValue) currentValue).getValue(selectorValue);
                } catch (NotFoundException ex) {
                    assembler.appendDiagnostic(new ValueDiagnostic(_locale, "Selector not found in node reference"));
                    throw new ExpressionException();
                }
            }
        }

        //  If we're still sitting on a NodeValue, return the number of child values.
        //  Otherwise, just return the node.
        if (currentValue instanceof NodeValue) {
            return new IntegerValue.Builder().setValue(((NodeValue) currentValue).getValueCount())
                                             .build();
        } else {
            return currentValue;
        }
    }
}
