/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.kexec.apis.IFacilitiesServices;

public class FacilitiesServices implements IFacilitiesServices {

    private static final String LOG_SOURCE = FacilitiesManager.LOG_SOURCE;

    private final FacilitiesManager _mgr;

    public FacilitiesServices(FacilitiesManager manager) {
        _mgr = manager;
    }
}
