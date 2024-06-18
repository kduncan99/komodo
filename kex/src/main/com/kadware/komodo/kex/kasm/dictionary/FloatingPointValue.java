/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.TruncationDiagnostic;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;

/**
 * A Value which represents a floating point thing.
 * All floating point values are stored in mina as doubles, and will be reduced if necessary to generate single-word floats.
 */
@SuppressWarnings("Duplicates")
public class FloatingPointValue extends Value {

    public final FloatingPointComponents _value;

    private static final FloatingPointValue POSITIVE_ZERO = new Builder().setValue(FloatingPointComponents.COMP_POSITIVE_ZERO)
                                                                         .build();

    /**
     * constructor
     * @param locale - where this value was defined
     * @param flagged - leading asterisk
     * @param value - value encoded into a FloatingPointComponents object
     * @param precision - indicates single or double precision (or default)
     */
    private FloatingPointValue(
        final Locale locale,
        final boolean flagged,
        final FloatingPointComponents value,
        final ValuePrecision precision
    ) {
        super(locale, flagged, precision);
        _value = value;
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    @Override
    public int compareTo(
        final Object obj
    ) throws TypeException {
        if (obj instanceof FloatingPointValue) {
            FloatingPointValue fpObj = (FloatingPointValue)obj;
            return _value.compare(fpObj._value, 60);
        } else {
            throw new TypeException();
        }
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param locale new value for Locale
     * @param newFlagged new value
     * @return new object
     */
    @Override
    public Value copy(
        final Locale locale,
        final boolean newFlagged
    ) {
        return new FloatingPointValue(locale, newFlagged, _value, _precision);
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param locale new value for Locale
     * @param newPrecision new value for precision attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final Locale locale,
        final ValuePrecision newPrecision
    ) {
        return new FloatingPointValue(locale, _flagged, _value, newPrecision);
    }

    /**
     * Check for equality
     * @param obj comparison object
     * @return true if objects are equal
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        return (obj instanceof FloatingPointValue) && (_value == ((FloatingPointValue)obj)._value);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.FloatingPoint;
    }

    @Override
    public int hashCode() { return _value.hashCode(); }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        String prec = "";
        if (_precision == ValuePrecision.Single) { prec = "S"; }
        else if (_precision == ValuePrecision.Double) { prec = "D"; }

        return String.format("%s%s%s", _flagged ? "*" : "", _value.toString(), prec);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Stuff specific to FloatingPointValue
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Add two FloatingPointValue objects
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param assembler container of assembler in case we need to raise on
     * @return new IntegerValue object representing the sum of the two addends
     */
    public static FloatingPointValue add(
        final FloatingPointValue operand1,
        final FloatingPointValue operand2,
        final Locale locale,
        final Assembler assembler
    ) {
        try {
            FloatingPointComponents fpc = operand1._value.normalize().add(operand2._value.normalize());
            return new FloatingPointValue(locale, false, fpc, ValuePrecision.Default);
        } catch (CharacteristicOverflowException ex) {
            assembler.appendDiagnostic(new ErrorDiagnostic(locale, "Characteristic overflow"));
            return FloatingPointValue.POSITIVE_ZERO;
        } catch (CharacteristicUnderflowException ex) {
            assembler.appendDiagnostic(new ErrorDiagnostic(locale, "Characteristic underflow"));
            return FloatingPointValue.POSITIVE_ZERO;
        }
    }

    /**
     * Create a FloatingPointValue from an IntegerValue.
     */
    public static FloatingPointValue convertFromInteger(
        final Locale locale,
        final IntegerValue integerValue
    ) {
        if (integerValue._value.isPositiveZero()) {
            return new FloatingPointValue(locale,
                                          false,
                                          FloatingPointComponents.COMP_POSITIVE_ZERO,
                                          ValuePrecision.Default);
        } else if (integerValue._value.isNegativeZero()) {
            return new FloatingPointValue(locale,
                                          false,
                                          FloatingPointComponents.COMP_NEGATIVE_ZERO,
                                          ValuePrecision.Default);
        } else {
            FloatingPointComponents fpc = new FloatingPointComponents(integerValue._value.get());
            return new FloatingPointValue(locale,
                                          false,
                                          fpc,
                                          ValuePrecision.Default);
        }
    }

    /**
     * Performs floating point division.
     */
    public static FloatingPointValue divide(
        final FloatingPointValue dividend,
        final FloatingPointValue divisor,
        final Locale locale,
        final Assembler assembler
    ) {
        try {
            FloatingPointComponents fpc = dividend._value.divide(divisor._value);
            return new FloatingPointValue(locale,
                                          false,
                                          fpc,
                                          ValuePrecision.Default);
        } catch (CharacteristicOverflowException ex) {
            assembler.appendDiagnostic(new ErrorDiagnostic(locale, "Characteristic overflow"));
            return new FloatingPointValue(locale,
                                          false,
                                          FloatingPointComponents.COMP_POSITIVE_ZERO,
                                          ValuePrecision.Default);
        } catch (CharacteristicUnderflowException ex) {
            assembler.appendDiagnostic(new ErrorDiagnostic(locale, "Characteristic underflow"));
            return new FloatingPointValue(locale,
                                          false,
                                          FloatingPointComponents.COMP_POSITIVE_ZERO,
                                          ValuePrecision.Default);
        } catch (DivideByZeroException ex) {
            assembler.appendDiagnostic(new TruncationDiagnostic(locale, "Divide by zero"));
            return new FloatingPointValue(locale,
                                          false,
                                          FloatingPointComponents.COMP_POSITIVE_ZERO,
                                          ValuePrecision.Default);
        }
    }

    /**
     * Performs floating point multiplication
     */
    public static FloatingPointValue multiply(
        final FloatingPointValue dividend,
        final FloatingPointValue divisor,
        final Locale locale,
        final Assembler assembler
    ) {
        try {
            FloatingPointComponents fpc = dividend._value.multiply(divisor._value);
            return new FloatingPointValue(locale,
                                          false,
                                          fpc,
                                          ValuePrecision.Default);
        } catch (CharacteristicOverflowException ex) {
            assembler.appendDiagnostic(new ErrorDiagnostic(locale, "Characteristic overflow"));
            return new FloatingPointValue(locale,
                                          false,
                                          FloatingPointComponents.COMP_POSITIVE_ZERO,
                                          ValuePrecision.Default);
        } catch (CharacteristicUnderflowException ex) {
            assembler.appendDiagnostic(new ErrorDiagnostic(locale, "Characteristic underflow"));
            return new FloatingPointValue(locale,
                                          false,
                                          FloatingPointComponents.COMP_POSITIVE_ZERO, ValuePrecision.Default);
        }
    }

    /**
     * Produces the additive inverse of the given value
     */
    public static FloatingPointValue negate(
        final Locale locale,
        final FloatingPointValue value
    ) {
        FloatingPointComponents fpc = value._value.negate();
        return new FloatingPointValue(locale,
                                      false,
                                      fpc,
                                      value._precision);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Builder
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class Builder {

        boolean _flagged = false;
        Locale _locale = null;
        ValuePrecision _precision = ValuePrecision.Default;
        FloatingPointComponents _value = null;

        public Builder setFlagged(boolean value)                    { _flagged = value; return this; }
        public Builder setLocale(Locale value)                      { _locale = value; return this; }
        public Builder setPrecision(ValuePrecision value)           { _precision = value; return this; }
        public Builder setValue(FloatingPointComponents value)      { _value = value; return this; }

        public FloatingPointValue build(
        ) {
            if (_value == null) {
                throw new RuntimeException("Value not specified for FloatingPointValue builder");
            }

            return new FloatingPointValue(_locale, _flagged, _value, _precision);
        }
    }
}
