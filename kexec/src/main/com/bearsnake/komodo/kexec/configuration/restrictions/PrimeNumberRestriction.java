/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.PrimeNumberException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.IntegerValue;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

import java.util.LinkedList;
import java.util.List;

public class PrimeNumberRestriction implements Restriction {

    public PrimeNumberRestriction() {}

    private static final List<Integer> PRIME_NUMBERS = new LinkedList<>();
    static {
        buildPrimeNumberTable(04000);
    }

    private static void buildPrimeNumberTable(final int newLimit) {
        PRIME_NUMBERS.clear();

        var isPrime = new boolean[newLimit + 1];
        isPrime[0] = false;
        isPrime[1] = false;
        for (int tx = 2; tx < newLimit; tx++) {
            isPrime[tx] = true;
        }

        for (int tx = 2; tx <= newLimit; tx++) {
            if (isPrime[tx]) {
                for (int ty = tx * 2; ty <= newLimit; ty += tx) {
                    isPrime[ty] = false;
                }
            }
        }

        for (int tx = 2; tx <= newLimit; tx++) {
            if (isPrime[tx]) {
                PRIME_NUMBERS.add(tx);
            }
        }
    }

    private boolean isPrimeNumber(final int value) {
        if (value <= 1) {
            return false;
        }

        var checkLimit = Math.sqrt(value);
        var highest = PRIME_NUMBERS.getLast();
        if (checkLimit > highest) {
            buildPrimeNumberTable((int)checkLimit + 1);
        }

        return PRIME_NUMBERS.contains(value);
    }

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (value instanceof IntegerValue iv) {
            if (!isPrimeNumber((int)iv.getValue())) {
                throw new PrimeNumberException(value);
            }
        } else {
            throw new ValueTypeException(value, ValueType.INTEGER);
        }
    }
}
