package com.cloudcore.reauthenticator;

import com.cloudcore.reauthenticator.core.FileSystem;
import com.cloudcore.reauthenticator.raida.RAIDA;
import com.cloudcore.reauthenticator.server.Command;
import com.cloudcore.reauthenticator.utils.SimpleLogger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.cloudcore.reauthenticator.raida.RAIDA.updateLog;

public class Main {

    public static int NetworkNumber = 1;

    public static void main(String[] args) {
        SimpleLogger.writeLog("ServantReauthenticatorStarted", "");
        singleRun = isSingleRun(args);
        if (args.length != 0 && Files.exists(Paths.get(args[0]))) {
            System.out.println("New root path: " + args[0]);
            FileSystem.changeRootPath(args[0]);
        }

        setup();
        updateLog("Loading Network Directory");
        SetupRAIDA();

        ArrayList<Command> commands = FileSystem.getCommands();
        if (commands.size() > 0)
            for (Command command : commands) {
                RAIDA.processNetworkCoins(NetworkNumber, command.account, FileSystem.BankPath);
                FileSystem.archiveCommand(command);
                exitIfSingleRun();
            }

        FolderWatcher watcher = new FolderWatcher(FileSystem.CommandsFolder);
        System.out.println("Watching for commands at " + FileSystem.CommandsFolder);
        while (true) {
            try {
                Thread.sleep(1000);

                if (watcher.newFileDetected()) {
                    commands = FileSystem.getCommands();
                    if (commands.size() > 0)
                        for (Command command : commands) {
                            RAIDA.processNetworkCoins(NetworkNumber, command.account, FileSystem.BankPath);
                            FileSystem.archiveCommand(command);
                            exitIfSingleRun();
                        }
                }
            } catch (Exception e) {
                System.out.println("Uncaught exception - " + e.getLocalizedMessage());
            }
        }
    }

    public static boolean singleRun = false;
    public static boolean isSingleRun(String[] args) {
        for (String arg : args)
            if (arg.equals("singleRun"))
                return true;
        return false;
    }
    public static void exitIfSingleRun() {
        if (singleRun)
            System.exit(0);
    }

    private static void setup() {
        FileSystem.createDirectories();
        RAIDA.getInstance();
    }
    public static void SetupRAIDA() {
        try
        {
            RAIDA.instantiate();
        }
        catch(Exception e)
        {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
            System.exit(1);
        }
        if (RAIDA.networks.size() == 0)
        {
            updateLog("No Valid Network found.Quitting!!");
            System.exit(1);
        }
        else
        {
            updateLog(RAIDA.networks.size() + " Networks found.");
            RAIDA raida = RAIDA.networks.get(0);
            for (RAIDA r : RAIDA.networks)
                if (NetworkNumber == r.networkNumber) {
                    raida = r;
                    break;
                }

            RAIDA.activeRAIDA = raida;
            if (raida == null) {
                updateLog("Selected Network Number not found. Quitting.");
                System.exit(0);
            }
        }
    }
}
