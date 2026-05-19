package co.sportverse.sportverse_backend.service.factory;

import co.sportverse.sportverse_backend.enums.OtpChannel;
import co.sportverse.sportverse_backend.service.OtpProvider;
import co.sportverse.sportverse_backend.service.SMSOtpProvider;
import co.sportverse.sportverse_backend.service.WhatsappOtpProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OtpProviderFactory {

    private final Map<OtpChannel, OtpProvider> otpProviders;

    OtpProviderFactory(WhatsappOtpProvider whatsappOtpProvider, SMSOtpProvider smsOtpProvider) {
            otpProviders = Map.of(OtpChannel.SMS, smsOtpProvider, OtpChannel.WHATSAPP, whatsappOtpProvider);
    }

    public OtpProvider getOtpProvider(OtpChannel otpChannel) {
        return otpProviders.get(otpChannel);
    }

}
