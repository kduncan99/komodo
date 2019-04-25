/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.operators.Operator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Expression evaluator
 */
public class Expression {

    public Context _context;
    public final List<IExpressionItem> _items = new LinkedList<>();

    /**
     * constructor
     * @param items list of ExpressionItems which comprise this expression
     */
    public Expression(
        final List<IExpressionItem> items
    ) {
        _items.addAll(items);
    }


    /**
     * Evaluates the (putative) expression in the given text
     * @param context under which we evaluate the given expression
     * @param diagnostics where we post any appropriate diagnostics
     * @return a Value object representing the final evaluated value of the exptression
     * @throws ExpressionException if the expression evaluation fails at any point
     */
    public Value evaluate(
        final Context context,
        final Diagnostics diagnostics
    ) throws ExpressionException {
        _context = context;
        Stack<Value> valueStack = new Stack<>();
        Stack<Operator> operatorStack = new Stack<>();

        for ( IExpressionItem item : _items ) {
            //  Take items off the item list...
            //  Operand items get resolved into values which are place on the value stack.
            //  Operator items get placed on the operator stack *after* all other operators
            //  on that stack, of equal or greater prcedence, are evaluated.
            if ( item instanceof OperandItem ) {
                OperandItem opItem = (OperandItem)item;
                Value value = opItem.resolve(context, diagnostics);
                valueStack.push(value);
            } else if ( item instanceof OperatorItem ) {
                OperatorItem opItem = (OperatorItem)item;
                Operator op = opItem._operator;
                while ( !operatorStack.empty() && (operatorStack.peek().getPrecedence() >= op.getPrecedence()) ) {
                    Operator stackedOp = operatorStack.pop();
                    stackedOp.evaluate(_context, valueStack, diagnostics);
                }
                operatorStack.push(op);
            }
        }

        //  There might be one or more operators left on the operator stack.
        //  Evaluate them.
        while (!operatorStack.empty()) {
            Operator op = operatorStack.pop();
            op.evaluate(_context, valueStack, diagnostics);
        }

        //  There should now be exactly one value on the value stack.
        if (valueStack.size() != 1) {
            throw new InternalErrorRuntimeException("value stack does not have 1 item left at end of expression evaluation");
        }

        return valueStack.pop();
    }
}
