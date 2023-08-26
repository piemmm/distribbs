package org.prowl.distribbs.config;


import org.prowl.distribbs.DistriBBS;

/**
 * List of configuration names used in the xml file.
 */
public enum Conf {

    // Enum list of configuration variables with their defaults
    callsign(""),
    monitor(false),


    // These settings are set per-interface
    uuid(""),
    beaconEvery(0),
    beaconText(""),

    // FBB compatible client system
    fbbListeningActive(true),
    fbbPreferredBBSCallsign("");

//    // APRS settings
//    aprsDecoingOverKISSEnabled(true),
//    connectToAPRSIServer(false),
//    aprsIServerHostname("aprs-cache.g0tai.net:14580"),
//
//    // MQTT settings
//    mqttPacketUploadEnabled(false),
//    mqttBrokerHostname(""),
//    mqttBrokerUsername(""),
//    mqttBrokerPassword(""),
//    mqttBrokerTopic("");

    public Object defaultSetting;

    Conf(Object defaultSetting) {
        this.defaultSetting = defaultSetting;
    }

    public String stringDefault() {
        return String.valueOf(defaultSetting);
    }

    public int intDefault() {
        return Integer.parseInt(String.valueOf(defaultSetting));
    }

    public boolean boolDefault() {
        return Boolean.parseBoolean(String.valueOf(defaultSetting));
    }

    /**
     * Create a default Net/ROM alias based on the callsign.
     *
     * @return
     */
    public static final String createDefaultNetromAlias() {
        if (DistriBBS.INSTANCE.getMyCall().length() > 0) {
            return DistriBBS.INSTANCE.getMyCallNoSSID().substring(DistriBBS.INSTANCE.getMyCallNoSSID().length() - 3) + "";
        } else {
            return "";
        }


    }

}
