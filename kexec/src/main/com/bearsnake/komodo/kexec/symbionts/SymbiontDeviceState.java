/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

/**
 * Reflects internal symbiont-specific state of a particular symbiont device
 */
public enum SymbiontDeviceState {
    Quiesced,      // waiting for ready on input, or not handling any output
    PreRun,        // have not yet found @RUN card on input
    Loading,       // we are loading images into a READ$ file
    LoadingFile,   // we are loading images into a separate file for @FILE processing
}
