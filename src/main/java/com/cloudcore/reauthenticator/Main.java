package com.cloudcore.reauthenticator;

import com.cloudcore.reauthenticator.core.FileSystem;
import com.cloudcore.reauthenticator.raida.RAIDA;
import com.cloudcore.reauthenticator.server.Command;
import com.cloudcore.reauthenticator.utils.SimpleLogger;

import java.util.ArrayList;

import static com.cloudcore.reauthenticator.raida.RAIDA.updateLog;

public class Main {

    public static int NetworkNumber = 1;

    public static void main(String[] args) {
        SimpleLogger.writeLog("ServantReauthenticatorStarted", "");
        ArrayList<Command> commands;
        try {
            setup();
            updateLog("Loading Network Directory");
            SetupRAIDA();

            FolderWatcher watcher = new FolderWatcher(FileSystem.CommandsFolder);
            boolean stop = false;

            commands = FileSystem.getCommands();
            if (commands.size() > 0)
                for (Command command : commands) {
                    FileSystem.createAccountDirectories(command.account);
                    RAIDA.processNetworkCoins(NetworkNumber, command.account, FileSystem.BankPath);
                    FileSystem.archiveCommand(command);
                }

            System.out.println("Watching folders at " + FileSystem.CommandsFolder + "...");

            while (!stop) {
                if (watcher.newFileDetected()) {
                    commands = FileSystem.getCommands();
                    if (commands.size() > 0)
                        for (Command command : commands) {
                            FileSystem.createAccountDirectories(command.account);
                            RAIDA.processNetworkCoins(NetworkNumber, command.account, FileSystem.BankPath);
                            FileSystem.archiveCommand(command);
                        }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Uncaught exception - " + e.getLocalizedMessage());
        }

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
        //networks[0]
    }
}
