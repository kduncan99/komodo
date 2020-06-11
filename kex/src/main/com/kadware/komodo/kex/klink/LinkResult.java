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
    public final LoadableBank[] _loadableBanks;
    public final Object _objectModule;
    public final ProgramStartInfo _programStartInfo;

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
        _loadableBanks = null;
        _objectModule = null;
        _programStartInfo = null;
    }

    /**
     * Constructor for absolute linkage
     */
    public LinkResult(
        final int errorCount,
        final String moduleName,
        final ProgramStartInfo programStartInfo,
        final LoadableBank[] loadableBanks,
        final AbsoluteModule absoluteModule
    ) {
        _errorCount = errorCount;
        _moduleName = moduleName;
        _absoluteModule = absoluteModule;
        _loadableBanks = loadableBanks;
        _objectModule = null;
        _programStartInfo = programStartInfo;
    }

    /**
     * Constructor for object linkage
     */
    public LinkResult(
        final int errorCount,
        final String moduleName,
        final ProgramStartInfo programStartInfo,
        final LoadableBank[] loadableBanks,
        final Object object
    ) {
        _errorCount = errorCount;
        _moduleName = moduleName;
        _absoluteModule = null;
        _loadableBanks = loadableBanks;
        _objectModule = object;
        _programStartInfo = programStartInfo;
    }

    /**
     * Constructor for binary or multi-banked binary linkage
     */
    public LinkResult(
        final int errorCount,
        final String moduleName,
        final ProgramStartInfo programStartInfo,
        final LoadableBank[] loadableBanks
    ) {
        _errorCount = errorCount;
        _moduleName = moduleName;
        _absoluteModule = null;
        _loadableBanks = loadableBanks;
        _objectModule = null;
        _programStartInfo = programStartInfo;
    }
}
