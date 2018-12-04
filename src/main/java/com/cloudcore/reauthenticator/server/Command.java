package com.cloudcore.reauthenticator.server;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Command {


    /* JSON Fields */

    @Expose
    @SerializedName("command")
    public String command;
    @Expose
    @SerializedName("account")
    public String account;

    public String filename;
}
