package org.prowl.distribbs.utils;

import org.prowl.distribbs.DistriBBS;

/**
 * This class takes %TOKENS% that are used in the message.properties file and detokenizes them with the values from
 * various sources.  For example, %BBSNAME% is replaced with the name of the BBS.
 */
public class UnTokenize {

    public static String str(String tokenizedStringToDetokenise) {

        tokenizedStringToDetokenise = tokenizedStringToDetokenise.replace("%BBSCALLSIGN%", DistriBBS.INSTANCE.getMyCallNoSSID());

        return tokenizedStringToDetokenise;
    }


}
