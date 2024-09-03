/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

import java.util.List;

public class Context {

    private enum Command {
        Display,
    }

    private Command _command = null;
    private boolean _displayASCII = false;
    private boolean _displayFieldata = false;
    private boolean _displayMetadata = false;
    private boolean _displayOctal = false;
    private boolean _displayUsage = false;
    private List<String> _packNames;

    private String _qualifier;
    private String _filename;
    private Integer _absoluteCycle;

    public boolean setup(final String[] args) {
        boolean err = false;
        int ax = 0;
        while (ax < args.length) {
            if (args[ax].startsWith("-")) {
                var opt = args[ax++];
                switch (opt) {
                    case "-a", "--ascii" -> _displayASCII = true;

                    case "-f", "--fieldata" -> _displayFieldata = true;

                    case "-h", "-?", "--help" -> _displayUsage = true;

                    case "-o", "--octal" -> _displayOctal = true;

                    case "-p", "--pack-file" -> {}

                    default -> {
                        System.err.println("ERROR:Unknown option: " + opt);
                        err = true;
                    }
                }
            } else {
                if (_qualifier != null) {
                    System.err.println("ERROR:Multiple qualifier/filenames provided");
                    err = true;
                    ax++;
                }


            }
        }

        if (_interpretLabel) {
            if (_wordAddress == null) {
                _wordAddress = 0L;
            } else if (_wordAddress != 0) {
                System.err.println("WARNING:Requested label interpretation of a non-label sector");
            }
        } else if (!_displayInfo && (_wordAddress == null)) {
            System.err.println("WARNING:No address specified - assuming 0");
            _wordAddress = 0L;
        }

        if (!_channelIO && (_wordAddress % 28 > 0)) {
            System.err.println("ERROR:Word address must be a multiple of 28 for direct disk IO");
            err = true;
        }

        if (_channelIO && _displayRaw) {
            System.err.println("ERROR:Cannot display raw bytes with channel IO");
            err = true;
        }

        if (_packName == null) {
            System.err.println("ERROR:Pack name not provided");
            err = true;
        }

        return !err;
    }

    public boolean displayASCII() { return _displayASCII; }
    public boolean displayFieldata() { return _displayFieldata; }
    public boolean displayInfo() { return _displayInfo; }
    public boolean displayRaw() { return _displayRaw; }
    public boolean displayUsage() { return _displayUsage; }
    public boolean doChannelIO() { return _channelIO; }
    public String getPackName() { return _packName; }
    public Long getWordAddress() { return _wordAddress; }
    public boolean interpretLabel() { return _interpretLabel; }

    public static void showUsage() {
        System.out.println("Manipulates files in the MFD of a disk pack (or set of disk packs) constituting fixed storage");
        System.out.println("usage:");
        System.out.println("  java -jar fileutil.jar [ options ] {command} {file_spec}");
        System.out.println("commands:");
        System.out.println("  DISPLAY - displays meta information and/or content for the specified file");
        System.out.println("file_spec:");
        System.out.println("  {qualifier} '*' {filename} [ '(' {absolute_cycle} ')' ]");
        System.out.println("options:");
        System.out.println("  -a, --ascii");
        System.out.println("     Output format includes ASCII");
        System.out.println("  -f, --fieldata");
        System.out.println("     Output format includes Fieldata");
        System.out.println("  -h, --help");
        System.out.println("  -o, --octal");
        System.out.println("     Output format includes Octal");
        System.out.println("  -p, --pack-file {pack_name}[,...]");
        System.out.println("     One or more host files which represent fixed disk packs");
        System.out.println("address:");
        System.out.println("  [0]n...n");
        System.out.println("  One or more digits (in octal or decimal) indicating the device-relative");
        System.out.println("  address of the sector to be displayed. A leading '0' indicates an octal value.");
    }
}
