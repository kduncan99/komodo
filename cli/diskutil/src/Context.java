/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

public class Context {

    private boolean _channelIO = false;
    private boolean _displayASCII = false;
    private boolean _displayFieldata = false;
    private boolean _displayInfo = false;
    private boolean _displayRaw = false;
    private boolean _displayUsage = false;
    private boolean _interpretLabel = false;
    private String _packName;
    private Long _wordAddress = null;

    public boolean setup(final String[] args) {
        boolean err = false;
        int ax = 0;
        while (ax < args.length) {
            if (args[ax].startsWith("-")) {
                var opt = args[ax++];
                switch (opt) {
                    case "-a", "-ascii" -> _displayASCII = true;

                    case "-ch", "-channel" -> _channelIO = true;

                    case "-f", "-fieldata" -> _displayFieldata = true;

                    case "-h", "-?", "--help" -> _displayUsage = true;

                    case "-i", "-info" -> _displayInfo = true;

                    case "-l", "-label" -> _interpretLabel = true;

                    case "-r", "-raw" -> _displayRaw = true;

                    case "-s", "-sector" -> {
                        if (ax == args.length) {
                            System.err.println("ERROR:Missing sector address value");
                            err = true;
                        }

                        try {
                            var str = args[ax++];
                            var value = Long.parseLong(str, str.startsWith("0") ? 8 : 10);
                            if (_wordAddress != null) {
                                System.err.println("ERROR:Address specified more than once");
                                err = true;
                                break;
                            }
                            _wordAddress = value * 28;
                        } catch (NumberFormatException e) {
                            System.err.println("ERROR:Invalid sector address value");
                            err = true;
                        }
                    }

                    case "-t", "-track" -> {
                        if (ax == args.length) {
                            System.err.println("ERROR:Missing track address value");
                            err = true;
                        }

                        try {
                            var str = args[ax++];
                            var value = Long.parseLong(str, str.startsWith("0") ? 8 : 10);
                            if (_wordAddress != null) {
                                System.err.println("ERROR:Address specified more than once");
                                err = true;
                                break;
                            }
                            _wordAddress = value * 1792;
                        } catch (NumberFormatException e) {
                            System.err.println("ERROR:Invalid track address value");
                            err = true;
                        }
                    }

                    case "-w", "-word" -> {
                        if (ax == args.length) {
                            System.err.println("ERROR:Missing word address value");
                            err = true;
                        }

                        try {
                            var str = args[ax++];
                            var value = Long.parseLong(str, str.startsWith("0") ? 8 : 10);
                            if (_wordAddress != null) {
                                System.err.println("ERROR:Address specified more than once");
                                err = true;
                                break;
                            }
                            _wordAddress = value;
                        } catch (NumberFormatException e) {
                            System.err.println("ERROR:Invalid word address value");
                            err = true;
                        }
                    }

                    default -> {
                        System.err.println("ERROR:Unknown option: " + opt);
                        err = true;
                    }
                }
            } else {
                if (_packName != null) {
                    System.err.println("ERROR:Multiple pack names provided");
                    err = true;
                    ax++;
                }

                _packName = args[ax++];
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
        System.out.println("Displays individual sectors of a virtual disk pack used with a FileSystemDiskDevice.");
        System.out.println("usage:");
        System.out.println("  java -jar cli.jar [ options ] {pack_name}");
        System.out.println("options:");
        System.out.println("  -a, --ascii");
        System.out.println("     Output format includes ASCII");
        System.out.println("  -ch, --channel");
        System.out.println("     Perform IO using via DiskChannel instead of direct to FileSystemDiskDevice.");
        System.out.println("  -f, --fieldata");
        System.out.println("     Output format includes Fieldata");
        System.out.println("  -h, --help");
        System.out.println("  -i, --info");
        System.out.println("     Display disk pack info");
        System.out.println("  -l, --label");
        System.out.println("     Display and interpret disk label (sector 0)");
        System.out.println("  -r, --raw");
        System.out.println("     Output includes raw byte data");
        System.out.println("  -s, --sector {address}");
        System.out.println("     Display the sector indicated by the device-relative sector address");
        System.out.println("  -t, --track {address}");
        System.out.println("     Display the sector indicated by the device-relative track address");
        System.out.println("  -w, --word {address}");
        System.out.println("     Display the sector indicated by the device-relative word address");
        System.out.println("address:");
        System.out.println("  [0]n...n");
        System.out.println("  One or more digits (in octal or decimal) indicating the device-relative");
        System.out.println("  address of the sector to be displayed. A leading '0' indicates an octal value.");
    }
}
