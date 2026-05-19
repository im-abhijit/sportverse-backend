package co.sportverse.sportverse_backend.enums;

public enum OtpChannel {
    SMS,WHATSAPP;

    public static OtpChannel getChannel(String channel) {
        if (channel.equalsIgnoreCase("whatsapp")) {
            return WHATSAPP;
        }
        return SMS;
    }
}
