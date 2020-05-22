/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.kex.AbsoluteModule;

/**
 * Base class for result of a linkage
 */
public class LinkResult {

    public final int _errorCount;
    public final String _moduleName;
    public final AbsoluteModule _absoluteModule;
    public final BankDescriptor[] _bankDescriptors;
    public final Object _objectModule;

    /**
     * Constructor for the case where no content could be generated due to errors
     */
    public LinkResult(
        final int errorCount,
        final String moduleName
    ) {
        _errorCount = errorCount;
        _moduleName = moduleName;
        _absoluteModule = null;
        _bankDescriptors = null;
        _objectModule = null;
    }

    /**
     * Constructor for absolute linkage
     */
    public LinkResult(
        final int errorCount,
        final String moduleName,
        final BankDescriptor[] bankDescriptors,
        final AbsoluteModule absoluteModule
    ) {
        _errorCount = errorCount;
        _moduleName = moduleName;
        _absoluteModule = absoluteModule;
        _bankDescriptors = bankDescriptors;
        _objectModule = null;
    }

    /**
     * Constructor for object linkage
     */
    public LinkResult(
        final int errorCount,
        final String moduleName,
        final BankDescriptor[] bankDescriptors,
        final Object object
    ) {
        _errorCount = errorCount;
        _moduleName = moduleName;
        _absoluteModule = null;
        _bankDescriptors = bankDescriptors;
        _objectModule = object;
    }

    /**
     * Constructor for binary or multi-banked binary linkage
     */
    public LinkResult(
        final int errorCount,
        final String moduleName,
        final BankDescriptor[] bankDescriptors
    ) {
        _errorCount = errorCount;
        _moduleName = moduleName;
        _absoluteModule = null;
        _bankDescriptors = bankDescriptors;
        _objectModule = null;
    }
}
