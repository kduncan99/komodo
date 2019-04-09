/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

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

    private Context _context;
    private final List<ExpressionItem> _items = new LinkedList<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    public Expression(
        final List<ExpressionItem> items
    ) {
        _items.addAll(items);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public List<ExpressionItem> getItems(
    ) {
        return _items;
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Evaluates the (putative) expression in the given text
     * <p>
     * @param context under which we evaluate the given expression
     * @param diagnostics where we post any appropriate diagnostics
     * <p>
     * @return a Value object representing the final evaluated value of the exptression
     * <p>
     * @throws ExpressionException if the expression evaluation fails at any point
     */
    public Value evaluate(
        final Context context,
        final Diagnostics diagnostics
    ) throws ExpressionException {
        _context = context;
        Stack<Value> valueStack = new Stack<>();
        Stack<Operator> operatorStack = new Stack<>();

        for (ExpressionItem item : _items) {
            //  Take items off the item list...
            //  Operand items get resolved into values which are place on the value stack.
            //  Operator items get placed on the operator stack *after* all other operators
            //  on that stack, of equal or greater prcedence, are evaluated.
            if (item instanceof OperandItem) {
                OperandItem opItem = (OperandItem)item;
                Value value = opItem.resolve(context, diagnostics);
                valueStack.push(value);
            } else if (item instanceof OperatorItem) {
                OperatorItem opItem = (OperatorItem)item;
                Operator op = opItem.getOperator();
                while (!operatorStack.empty() && (operatorStack.peek().getPrecedence() >= op.getPrecedence())) {
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
