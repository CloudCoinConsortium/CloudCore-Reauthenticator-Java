package com.cloudcore.reauthenticator.utils;

import com.cloudcore.reauthenticator.core.CloudCoin;
import com.cloudcore.reauthenticator.core.Config;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.UUID;

public class CoinUtils {

    public static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("MM-yyyy")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();
    public static final DateTimeFormatter formatterSingleMonth = new DateTimeFormatterBuilder().appendPattern("M-yyyy")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();


    /* Methods */

    public static void calcExpirationDate(CloudCoin coin) {
        coin.setEd(calcExpirationDate());
    }
    public static String calcExpirationDate() {
        LocalDate expirationDate = LocalDate.now().plusYears(Config.EXPIRATION_YEARS);
        return (expirationDate.getMonthValue() + "-" + expirationDate.getYear());
    }

    public static boolean checkExpirationDate(CloudCoin coin) {
        LocalDate coinDate = null;
        try {
            if (7 == coin.getEd().length())
                coinDate = LocalDate.parse(coin.getEd(), formatter);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
        }
        try {
            if (6 == coin.getEd().length())
                coinDate = LocalDate.parse(coin.getEd(), formatterSingleMonth);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
        }

        if (coinDate == null)
            return false; // Cannot determine

        LocalDate currentDate = LocalDate.now();
        int yearsUntilUpdate = coinDate.getYear() - currentDate.getYear();

        if (yearsUntilUpdate > Config.REAUTHENTICATE_YEARS) // Doesn't need refreshed
            return false;
        else if (yearsUntilUpdate == Config.REAUTHENTICATE_YEARS) { // May need refreshed
            return coinDate.getMonthValue() <= currentDate.getMonthValue();
        }
        else
            return true; // Needs refreshed
    }

    /**
     * Returns a denomination describing the currency value of the CloudCoin.
     *
     * @param coin CloudCoin
     * @return 1, 5, 25, 100, 250, or 0 if the CloudCoin's serial number is invalid.
     */
    public static int getDenomination(CloudCoin coin) {
        int sn = coin.getSn();
        int nom;
        if (sn < 1)
            nom = 0;
        else if ((sn < 2097153))
            nom = 1;
        else if ((sn < 4194305))
            nom = 5;
        else if ((sn < 6291457))
            nom = 25;
        else if ((sn < 14680065))
            nom = 100;
        else if ((sn < 16777217))
            nom = 250;
        else
            nom = 0;

        return nom;
    }

    /**
     * Generates a name for the CloudCoin based on the denomination, Network Number, and Serial Number.
     * <br>
     * <br>Example: 25.1.6123456
     *
     * @return String a filename
     */
    public static String generateFilename(CloudCoin coin) {
        return getDenomination(coin) + ".CloudCoin." + coin.getNn() + "." + coin.getSn();
    }

    /**
     * Generates secure random GUIDs for pans. An example:
     * <ul>
     * <li>8d3eb063937164c789474f2a82c146d3</li>
     * </ul>
     * These Strings are hexadecimal and have a length of 32.
     */
    public static void generatePAN(CloudCoin coin) {
        //coin.pan = new String[Config.nodeCount];
        //Arrays.fill(coin.pan, "123456789abcdef00000000000000000");
        coin.pan = new String[Config.nodeCount];
        for (int i = 0; i < Config.nodeCount; i++) {
            SecureRandom random = new SecureRandom();
            byte[] cryptoRandomBuffer = random.generateSeed(16);

            UUID uuid = UUID.nameUUIDFromBytes(cryptoRandomBuffer);
            coin.pan[i] = uuid.toString().replace("-", "");
        }
    }

    public static int getPassCount(CloudCoin coin) {
        return Utils.charCount(coin.getPown(), 'p');
    }
    public static int getFailCount(CloudCoin coin) {
        return Utils.charCount(coin.getPown(), 'f');
    }
    public static String getDetectionResult(CloudCoin coin) {
        return (getPassCount(coin) >= Config.passCount) ? "Pass" : "Fail";
    }

    /**
     * Updates the Authenticity Numbers to the new Proposed Authenticity Numbers.
     */
    public static void setAnsToPans(CloudCoin coin) {
        for (int i = 0; (i < Config.nodeCount); i++) {
            coin.getAn().set(i, coin.pan[i]);
        }
    }
}
