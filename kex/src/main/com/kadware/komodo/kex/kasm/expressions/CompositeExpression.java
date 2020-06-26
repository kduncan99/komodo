/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions;

import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Form;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.FatalDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.ValueDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.EqufValue;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;

/**
 * Represents a composite expression - that is, an expression composed of one or more subfields,
 * where-in the geometry of the subfields is defined by an attached form, and each subfield
 * is described by a discrete expression.
 *
 * This is necessary for supporting $EQUF references, where-in a reference to an $EQUF within
 * one subfield, may affect the resulting values of other subfields within the same
 * composite expression.
 *
 * An example:
 * E         $EQUF          010,X5,H2
 * ...
 *           LA             A0,BASE+E
 * Here, the reference to E is contained in the u-subfield, but will affect the j- and x- subfields
 * as well, producing the following effective instruction:
 *           LA,H2          A0,BASE+010,X5
 * If we merely evaluated expressions as independent entities, the reference to E in the u-subfield
 * would have no means of propogating its values into the other subfields which are repesented by
 * other independent expressions.
 *
 * By employing a composite expression, we can attach the EQUF reference to that expression, and
 * then apply the appropriate portions of that EQUF reference to the various discrete expressions
 * which comprise the component expression, at evaluation.
 *
 * This has the following implications:
 *  An EQUF label must be defined prior to reference - no forward references are allowed
 *  EQUF values must be integers, but may contain relocation items
 *  An EQUF reference must be either
 *      the only entity in the discrete expression in which it is contained
 *      the left-hand operand of a top-level least-precendent addition operator in the discrete expression
 *  The EQUF reference (and the addition operator if it exists) is sliced from the discrete expression
 *      at parse time, and attached to the composite expression which contains the discrete expression
 *      being parsed.
 *  We allow only one EQUF reference in a composite expression.
 */
public class CompositeExpression {

    final Expression[] _discreteExpressions;
    final EqufValue _equfValue;
    final Form _form;
    final Locale _locale;

    /**
     * constructor for expression with no EQUF attached
     */
    public CompositeExpression(
        final Locale locale,
        final Form form,
        final Expression[] discreteExpressions
    ) {
        _locale = locale;
        _form = form;
        _discreteExpressions = discreteExpressions;
        _equfValue = null;
    }

    /**
     * constructor for expression with EQUF attached
     */
    public CompositeExpression(
        final Locale locale,
        final Form form,
        final Expression[] discreteExpressions,
        final EqufValue equfValue
    ) {
        _locale = locale;
        _form = form;
        _discreteExpressions = discreteExpressions;
        _equfValue = equfValue;
    }

    /**
     * Evaluates the (putative) expression in the given component expressions,
     * then integrates them into a single integer value with appropriate field/relocation items.
     * @param assembler under which we evaluate the given expression
     * @return a Value object representing the final evaluated value of the exptression
     * @throws ExpressionException if the expression evaluation fails at any point
     */
    public Value evaluate(
        final Assembler assembler
    ) throws ExpressionException {
        FieldDescriptor[] fieldDescriptors = _form.getFieldDescriptors();
        if (fieldDescriptors.length != _discreteExpressions.length) {
            String msg = "Mismatch between field descriptor count and discrete expression count";
            assembler.appendDiagnostic(new FatalDiagnostic(_locale, msg));
            return IntegerValue.POSITIVE_ZERO;
        }

        if ((_equfValue != null) && (!_equfValue._form.equals(_form))) {
            String msg = "EQUF form does not match the expression form";
            assembler.appendDiagnostic(new ErrorDiagnostic(_locale, msg));
            return IntegerValue.POSITIVE_ZERO;
        }

        IntegerValue[] discreteValues = new IntegerValue[_discreteExpressions.length];
        for (int dx = 0; dx < _discreteExpressions.length; ++dx) {
            Value discreteResult = _discreteExpressions[dx].evaluate(assembler);
            if (discreteResult instanceof IntegerValue) {
                discreteValues[dx] = (IntegerValue) discreteResult;
            } else {
                String msg = String.format("Expression in subfield %d returning a non-integer value", dx);
                assembler.appendDiagnostic(new ValueDiagnostic(_locale, msg));
                discreteValues[dx] = IntegerValue.POSITIVE_ZERO;
            }
        }

        IntegerValue.IntegrateResult integrateResult =
            IntegerValue.integrate(IntegerValue.POSITIVE_ZERO, fieldDescriptors, discreteValues, _locale);
        for (Diagnostic diag : integrateResult._diagnostics.getDiagnostics()) {
            assembler.appendDiagnostic(diag);
        }

        return integrateResult._value;
    }
}
