/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.kexec.configuration.Configuration;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.logger.Level;
import com.bearsnake.komodo.logger.LogManager;
import com.bearsnake.komodo.logger.TimestampedFileLogger;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static class Context {
        public String _configFileName = null;
        public boolean[] _jumpKeys = new boolean[36];
        public boolean _helpFlagSet = false;
    }

    private static void showHelp() {
        System.out.println("Usage:");
        System.out.println("  kexec [-h] [-c config_file] [-jk jump_key,...]");
        System.out.println("If config_file is not specified, system defaults will be used,");
        System.out.println("  and disk packs and tape reels will be searched for in the current directory.");
        System.out.println("jump_key values are integers from 1 to 36 - the recognized values are:");
        System.out.println("  jk1:  Force display of Modify Config message to allow facilities keyins prior to boot");
        System.out.println("  jk2:  Perform partial manual dump prior to booting the exec");
        System.out.println("  jk3:  Disable auto-recovery - when the exec stops, this application will terminate");
        System.out.println("  jk4:  Reload system libraries from system library tape");
        System.out.println("  jk6:  Performs full dump in conjunction with jk2");
        System.out.println("  jk7:  Solicits TIP initialization;");
        System.out.println("          solicits pack recovery/initialization during recovery");
        System.out.println("  jk9:  On initial boot, prevents recovery of backlog and print queues");
        System.out.println("  jk13: On initial boot, initializes mass storage - requires jk4");
    }

    private static void showUsage() {
        System.out.println("Usage:");
        System.out.println("  kexec [-h] [-cf config_file] [-jk jump_key,...]");
    }

    private static Context processArgs(String[] args) {
        var context = new Context();

        int ax = 0;
        while (ax < args.length) {
            var sw = args[ax++];
            switch (sw.toLowerCase()) {
                case "-cf" -> {
                    if (ax == args.length) {
                        System.err.println("no argument specified for -c switch");
                        return null;
                    }
                    context._configFileName = args[ax++];
                }

                case "-h" -> context._helpFlagSet = true;

                case "-jk" -> {
                    if (ax == args.length) {
                        System.err.println("no argument specified for -jk switch");
                        return null;
                    }
                    var split = args[ax++].split(",");
                    for (var key : split) {
                        try {
                            var jk = Integer.parseInt(key);
                            context._jumpKeys[jk - 1] = true;
                            System.out.printf("Setting jk %d\n", jk);
                        } catch (NumberFormatException ex) {
                            System.err.printf("invalid jump key %s\n", key);
                            return null;
                        }
                    }
                }

                default -> {
                    System.err.printf("Switch %s not recognized\n", sw);
                    return null;
                }
            }
        }

        return context;
    }

    public static void run(final Context context,
                           final Configuration config) {
        Exec e = new Exec(context._jumpKeys);
        try {
            e.setConfiguration(config);
            e.initialize();
        } catch (KExecException ex) {
            LogManager.logFatal("Exec", "Initialization error %v", ex.toString());
            return;
        }

        int session = 0;
        while (true) {
            if (e.isJumpKeySet(2)) {
                System.out.println("::Performing pre-boot system dump...");
                e.dump(e.isJumpKeySet(6));
            }

            System.out.printf("::Starting KEXEC session %d...\n", session);
            try {
                boolean recovery = session > 0;
                e.boot(recovery, session);
            } catch (KExecException ex) {
                System.out.printf("::Cannot boot exec:%s\n", ex.getMessage());
                e.dump(true);
                break;
            }

            while (e.isRunning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // nothing to do here
                }
            }

            System.out.printf("::System error %03o (%s) terminated session %03o\n",
                              e.getStopCode().getCode(),
                              e.getStopCode(),
                              session);

            if (e.isJumpKeySet(3)) {
                System.out.println("::Auto-recovery inhibited - producing final post-mortem dump...");
                e.dump(e.isJumpKeySet(6));
                break;
            } else if (!e.isRecoveryBootAllowed()) {
                System.out.println("::Exec recovery is not possible - producing post-mortem dump...");
                e.dump(e.isJumpKeySet(6));
                break;
            }

            System.out.println("::Recovering system...");
            session++;
        }

        e.close();
    }

    public static void main(String[] args) {
        var context = processArgs(args);
        if (context == null) {
            showUsage();
            return;
        } else if (context._helpFlagSet) {
            showHelp();
            return;
        }

        var cfg = new Configuration();
        if (context._configFileName != null) {
            try {
                if (!cfg.updateFromFile(context._configFileName)) {
                    return;
                }
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                return;
            }
        }

        try {
            LogManager.clear();
            var logger = new TimestampedFileLogger("kexec");
            logger.setLevel(Level.All);
            LogManager.register(logger);
            LogManager.setGlobalLevel(Level.All);//TODO configurable
        } catch (FileNotFoundException ex) {
            System.err.printf("Cannot open log file:%s\n", ex);
        }

        run(context, cfg);
        LogManager.close();
        System.out.println("MAIN DONE");//TODO remove
    }
}
