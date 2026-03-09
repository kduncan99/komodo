/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Nothing really different from the VirtualAddress class, but this is a specific hard-held register in the IP.
 * It's established as a subclass of Virtual Address so that we can access the statics in VA through
 * this class name.
 */
public class ProgramAddressRegister extends VirtualAddress {
}
