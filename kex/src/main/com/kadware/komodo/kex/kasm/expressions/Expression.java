/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.expressions.items.*;
import com.kadware.komodo.kex.kasm.expressions.operators.Operator;
import com.kadware.komodo.kex.kasm.exceptions.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Expression evaluator
 */
public class Expression {

    final List<IExpressionItem> _items = new LinkedList<>();

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
     * @param assembler under which we evaluate the given expression
     * @return a Value object representing the final evaluated value of the exptression
     * @throws ExpressionException if the expression evaluation fails at any point
     */
    public Value evaluate(
        final Assembler assembler
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        Stack<Operator> operatorStack = new Stack<>();

        //  Special case - if there is an ExpressionGroupItem here and it has no other operand siblings,
        //  then it needs to become a case-2 e-g-item.
        ExpressionGroupItem egItem = null;
        IExpressionItem nonEgItem = null;
        for (IExpressionItem item : _items) {
            if (item instanceof ExpressionGroupItem) {
                egItem = (ExpressionGroupItem) item;
            } else if (item instanceof OperandItem) {
                nonEgItem = item;
            }
        }

        if ((egItem != null) && (nonEgItem == null)) {
            egItem.setIsSubExpression(false);
        }

        for (IExpressionItem item : _items) {
            //  Take items off the item list...
            //  Operand items get resolved into values which are place on the value stack.
            //  Operator items get placed on the operator stack *after* all other operators
            //  on that stack, of equal or greater prcedence, are evaluated.
            if (item instanceof OperandItem) {
                OperandItem opItem = (OperandItem)item;
                Value value = opItem.resolve(assembler);
                valueStack.push(value);
            } else if (item instanceof OperatorItem) {
                OperatorItem opItem = (OperatorItem)item;
                Operator op = opItem._operator;
                while ( !operatorStack.empty() && (operatorStack.peek().getPrecedence() >= op.getPrecedence()) ) {
                    Operator stackedOp = operatorStack.pop();
                    stackedOp.evaluate(assembler, valueStack);
                }
                operatorStack.push(op);
            }
        }

        //  There might be one or more operators left on the operator stack.
        //  Evaluate them.
        while (!operatorStack.empty()) {
            Operator op = operatorStack.pop();
            op.evaluate(assembler, valueStack);
        }

        //  There should now be exactly one value on the value stack.
        if (valueStack.size() != 1) {
            throw new RuntimeException("value stack does not have 1 item left at end of expression evaluation");
        }

        return valueStack.pop();
    }
}
