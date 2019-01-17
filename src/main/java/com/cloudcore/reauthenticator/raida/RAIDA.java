package com.cloudcore.reauthenticator.raida;

import com.cloudcore.reauthenticator.core.CloudCoin;
import com.cloudcore.reauthenticator.core.Config;
import com.cloudcore.reauthenticator.core.FileSystem;
import com.cloudcore.reauthenticator.utils.CoinUtils;
import com.cloudcore.reauthenticator.utils.SimpleLogger;
import com.cloudcore.reauthenticator.utils.Utils;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class RAIDA {


    /* Fields */

    public static SimpleLogger logger;

    public static RAIDA mainNetwork;
    public static RAIDA activeRAIDA;
    public static ArrayList<RAIDA> networks = new ArrayList<>();

    public Node[] nodes = new Node[Config.nodeCount];

    public MultiDetectRequest multiRequest;
    public ArrayList<CloudCoin> coins;

    public int networkNumber = 1;


    /* Constructors */

    private RAIDA() {
        for (int i = 0; i < Config.nodeCount; i++) {
            nodes[i] = new Node(i + 1);
        }
    }

    private RAIDA(Network network) {
        nodes = new Node[network.raida.length];
        this.networkNumber = network.nn;
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new Node(i + 1, network.raida[i]);
        }
    }

    // This method was introduced breaking the previously used Singleton pattern.
    // This was done in order to support multiple networks concurrently.
    // We can now have multiple RAIDA objects each containing different networks
    // RAIDA details are read from Directory URL first.
    // In case of failure, it falls back to a file on the file system
    public static ArrayList<RAIDA> instantiate() {
        String nodesJson = "";
        networks.clear();

        try {
            nodesJson = Utils.getHtmlFromURL(Config.URL_DIRECTORY);
        } catch (Exception e) {
            System.out.println(": " + e.getLocalizedMessage());
            e.printStackTrace();
            if (!Files.exists(Paths.get("directory.json"))) {
                System.out.println("RAIDA instantiation failed. No Directory found on server or local path");
                System.exit(-1);
                return null;
            }
            try {
                nodesJson = new String(Files.readAllBytes(Paths.get(Paths.get("").toAbsolutePath().toString()
                        + File.separator + "directory.json")));
            } catch (IOException e1) {
                System.out.println("| " + e.getLocalizedMessage());
                e1.printStackTrace();
            }
        }

        try {
            Gson gson = Utils.createGson();
            RAIDADirectory dir = gson.fromJson(nodesJson, RAIDADirectory.class);

            for (Network network : dir.networks) {
                System.out.println("Available Networks: " + network.raida[0].urls[0].url + " , " + network.nn);
                networks.add(RAIDA.getInstance(network));
            }
        } catch (Exception e) {
            System.out.println("RAIDA instantiation failed. No Directory found on server or local path");
            e.printStackTrace();
            System.exit(-1);
        }

        if (networks == null || networks.size() == 0) {
            System.out.println("RAIDA instantiation failed. No Directory found on server or local path");
            System.exit(-1);
            return null;
        }
        return networks;
    }

    // Return Main RAIDA Network populated with default Nodes Addresses(Network 1)
    public static RAIDA getInstance() {
        if (mainNetwork != null)
            return mainNetwork;
        else {
            mainNetwork = new RAIDA();
            return mainNetwork;
        }
    }

    public static RAIDA getInstance(Network network) {
        return new RAIDA(network);
    }

    public static CompletableFuture<Object> processNetworkCoins(int NetworkNumber, String account, String folderPath) {
        return processNetworkCoins(NetworkNumber, account, folderPath, true);
    }

    public static CompletableFuture<Object> processNetworkCoins(int NetworkNumber, String account, String folderPath, boolean ChangeANS) {
        System.out.println("Processing Network Coins...");
        return CompletableFuture.supplyAsync(() -> {
            if (folderPath.equals(FileSystem.RootPath + FileSystem.SuspectPath))
                FileSystem.moveFromImportToSuspectFolder(account);

            ArrayList<CloudCoin> folderCoins = FileSystem.loadFolderCoins(FileSystem.RootPath + FileSystem.BankPath);
            System.out.println(folderCoins.size() + " coins in " + FileSystem.RootPath + FileSystem.BankPath);
            ArrayList<CloudCoin> allCoins = new ArrayList<>();
            for (CloudCoin coin : folderCoins) {
                if (folderPath.equals(FileSystem.RootPath + FileSystem.BankPath)) {
                    if (CoinUtils.checkExpirationDate(coin))
                        allCoins.add(coin);
                }
                else if (NetworkNumber == coin.getNn())
                    allCoins.add(coin);
            }

            if (allCoins.size() == 0) {
                return null;
            }

            System.out.println("Getting network...");
            RAIDA raida = null;
            for (RAIDA network : RAIDA.networks) {
                if (network != null && NetworkNumber == network.networkNumber) {
                    raida = network;
                    break;
                }
            }

            if (raida == null)
                return null;

            // Process Coins in Lots of 200. Can be changed from Config File
            int LotCount = allCoins.size() / Config.multiDetectLoad;
            if (allCoins.size() % Config.multiDetectLoad > 0)
                LotCount++;

            int coinCount = 0;
            for (int i = 0; i < LotCount; i++) {
                ArrayList<CloudCoin> coins = new ArrayList<>();
                try { // Pick up to 200 Coins and send them to RAIDA
                    coins = new ArrayList<>(allCoins.subList(i * Config.multiDetectLoad, Math.min(allCoins.size(), 200)));
                    raida.coins = coins;
                } catch (Exception e) {
                    System.out.println(":" + e.getLocalizedMessage());
                    e.printStackTrace();
                }
                ArrayList<CompletableFuture<Node.MultiDetectResponse>> tasks = raida.getMultiDetectTasks(raida.coins, ChangeANS);
                try {
                    try {
                        System.out.println("Waiting for futures...");
                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
                    } catch (Exception e) {
                        System.out.println("RAIDA#PNC:" + e.getLocalizedMessage());
                    }

                    for (int j = 0; j < coins.size(); j++) {
                        CloudCoin coin = coins.get(j);
                        StringBuilder pownString = new StringBuilder();
                        coin.setPown("");
                        for (int k = 0; k < Config.nodeCount; k++) {
                            pownString.append(raida.nodes[k].multiResponse.responses[j].outcome, 0, 1);
                        }
                        coin.setPown(pownString.toString());
                        coinCount++;
                        if (!Config.KEEP_ANS)
                            CoinUtils.setAnsToPans(coin);

                        if (folderPath.equals(FileSystem.SuspectPath))
                            FileSystem.moveAndUpdateCoin(coin, FileSystem.RootPath + FileSystem.SuspectPath,
                                    FileSystem.RootPath + FileSystem.DetectedPath, ".stack");
                        else
                            FileSystem.updateCoin(coin);

                        updateLog("No. " + coinCount + ". Coin Detected. sn - " + coin.getSn() + ". Pass Count - " + CoinUtils.getPassCount(coin) +
                                ". Fail Count  - " + CoinUtils.getFailCount(coin) + ". Result - " + CoinUtils.getDetectionResult(coin) + "." + coin.getPown());
                        System.out.println("Coin Detected. sn - " + coin.getSn() + ". Pass Count - " + CoinUtils.getPassCount(coin) +
                                ". Fail Count  - " + CoinUtils.getFailCount(coin) + ". Result - " + CoinUtils.getDetectionResult(coin));
                    }
                } catch (Exception e) {
                    System.out.println("RAIDA#PNC: " + e.getLocalizedMessage());
                }
            }

            return null;
        });
    }

    public ArrayList<CompletableFuture<Node.MultiDetectResponse>> getMultiDetectTasks(ArrayList<CloudCoin> coins, boolean changeANs) {
        this.coins = coins;

        int[] nns = new int[coins.size()];
        int[] sns = new int[coins.size()];

        String[][] ans = new String[Config.nodeCount][];
        String[][] pans = new String[Config.nodeCount][];

        int[] dens = new int[coins.size()]; // Denominations
        ArrayList<CompletableFuture<Node.MultiDetectResponse>> detectTasks = new ArrayList<>(); // Stripe the coins

        for (int i = 0; i < coins.size(); i++) {
            CloudCoin coin = coins.get(i);
            if (changeANs)
                CoinUtils.generatePAN(coin);
            else
                CoinUtils.setAnsToPans(coin);
            nns[i] = coin.getNn();
            sns[i] = coin.getSn();
            dens[i] = CoinUtils.getDenomination(coin);
            System.out.println(coin.toString());
        }

        try {
        multiRequest = new MultiDetectRequest();
        multiRequest.timeout = Config.milliSecondsToTimeOut;
        for (int nodeNumber = 0; nodeNumber < Config.nodeCount; nodeNumber++) {
            ans[nodeNumber] = new String[coins.size()];
            pans[nodeNumber] = new String[coins.size()];

            for (int i = 0; i < coins.size(); i++) {
                ans[nodeNumber][i] = coins.get(i).getAn().get(nodeNumber);
                pans[nodeNumber][i] = coins.get(i).pan[nodeNumber];
            }
            multiRequest.an[nodeNumber] = ans[nodeNumber];
            multiRequest.pan[nodeNumber] = pans[nodeNumber];
            multiRequest.nn = nns;
            multiRequest.sn = sns;
            multiRequest.d = dens;
        }
        } catch (Exception e) {
            System.out.println("/0" + e.getLocalizedMessage());
            e.printStackTrace();
        }

        try {
            for (int nodeNumber = 0; nodeNumber < Config.nodeCount; nodeNumber++) {
                detectTasks.add(nodes[nodeNumber].MultiDetect());
            }
        } catch (Exception e) {
            System.out.println("/1" + e.getLocalizedMessage());
            e.printStackTrace();
        }

        return detectTasks;
    }

    public static void updateLog(String message) {
        System.out.println(message);
        SimpleLogger.writeLog(message.replaceAll(" ", ""), message);
    }
}
