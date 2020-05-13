/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.LineSpecifier;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Wraps an in-order container of Diagnostic objects, providing some simple getters to quickly analyze the content thereof.
 */
public class Diagnostics {

    private final List<Diagnostic> _diagnostics = new LinkedList<>();
    private final Map<Diagnostic.Level, Integer> _counters = new HashMap<>();

    public Diagnostics(
    ) {
        _counters.put(Diagnostic.Level.Addressability, 0);
        _counters.put(Diagnostic.Level.BaseRegister, 0);
        _counters.put(Diagnostic.Level.Directive, 0);
        _counters.put(Diagnostic.Level.Duplicate, 0);
        _counters.put(Diagnostic.Level.Error, 0);
        _counters.put(Diagnostic.Level.Fatal, 0);
        _counters.put(Diagnostic.Level.Form, 0);
        _counters.put(Diagnostic.Level.Go, 0);
        _counters.put(Diagnostic.Level.Level, 0);
        _counters.put(Diagnostic.Level.Quote, 0);
        _counters.put(Diagnostic.Level.Relocation, 0);
        _counters.put(Diagnostic.Level.Truncation, 0);
        _counters.put(Diagnostic.Level.UndefinedReference, 0);
        _counters.put(Diagnostic.Level.Value, 0);
        _counters.put(Diagnostic.Level.Warning, 0);
    }

    /**
     * Appends a Diagnostic object to our container, updating our counters as appropriate
     * @param diagnostic Diagnostic to be appended
     */
    public void append(
        final Diagnostic diagnostic
    ) {
        _diagnostics.add(diagnostic);
        Integer counter = _counters.get(diagnostic.getLevel());
        if (counter == null) {
            counter = 1;
        } else {
            ++counter;
        }
        _counters.put(diagnostic.getLevel(), counter);
    }

    /**
     * Appends all the Diagnostic objects from one container into this container, updating our counters as appropriate
     * @param diagnostics Diagnostics object which has individual Diagnostic objects to be appended
     */
    public void append(
        final Diagnostics diagnostics
    ) {
        for (Diagnostic diag : diagnostics._diagnostics) {
            append(diag);
        }
    }

    /**
     * Clears all the Diagnostic objects from this container.
     * Should not really be used, I think.
     */
    public void clear(
    ) {
        _diagnostics.clear();
    }

    /**
     * Getter
     * @return array of all diagnostics
     */
    public List<Diagnostic> getDiagnostics(
    ) {
        return _diagnostics;
    }

    /**
     * Getter
     * @param lineSpecifier indicates we only want those related to a particular line of text
     * @return array of diagnostics for the line number
     */
    public List<Diagnostic> getDiagnostics(
        final LineSpecifier lineSpecifier
        ) {
        List<Diagnostic> result = new LinkedList<>();
        for (Diagnostic d : _diagnostics) {
            if (d._locale.getLineSpecifier().equals(lineSpecifier)) {
                result.add(d);
            }
        }
        return result;
    }

    /**
     * Returns counters map
     */
    public Map<Diagnostic.Level, Integer> getCounters(
    ) {
        return _counters;
    }

    /**
     * Returns true if we have at leats one Error or Fatal level diagnostic
     */
    public boolean hasError() {
        for (Diagnostic d : _diagnostics) {
            if (d.isError()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if we have at least one Fatal level diagnostic
     */
    public boolean hasFatal() {
        return _counters.get(Diagnostic.Level.Fatal) > 0;
    }

    /**
     * Returns true if the container is empty
     */
    public boolean isEmpty() {
        return _diagnostics.isEmpty();
    }
}
