/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

/**
 * This is part of the exec code.
 * It specifically manages information in the GENF file.
 * -------
 * The GenF File contains the following:
 *  general system information which should persist from one exec session to another, but is not configuration-related.
 *  input queue (backlog)
 *  output queue (both punch and print)
 * -------
 * Information in the GenF file is stored on sector boundaries. Each sector is of a specific type.
 * Type Description
 *   0    System information (this is the first sector)
 *          contains persisted system information, and links to the first input queue, output queue, and free tracks.
 *   1    Input queue track - contains input queue directory entries
 *   2    Output queue track - contains output queue directory entries
 * -------
 * System information sector format
 * Word 0: "*GENF*" in fieldata
 * Word 1,S1: type (0)
 * TODO what system info do we track here?
 * -------
 * Input queue sector
 * Word 0: "*GENF*" in fieldata
 * Word 1,S1: type (1)
 * Word 1,S2: source (symbiont name, site-id, etc)
 * Word 2:    symbiont source in fieldata if source==0
 * Word 3:    run-id for the run in fieldata
 * Word 5-6:  qualifier of file containing the content in fieldata ("SYS$")
 * Word 7-8:  filename of the file containing the content in fieldata ('READ$X' {run-id})
 * TODO
 * -------
 * Output queue sector
 * TODO see DATA STRUCTURES 12-25
 * -------
 * Unused sector
 * Word 0: "*GENF*" in fieldata
 * Word 1,S1: type (040)
 */
public class GenFileInterface {
}
