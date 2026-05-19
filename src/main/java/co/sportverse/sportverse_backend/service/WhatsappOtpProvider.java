package co.sportverse.sportverse_backend.service;

import org.springframework.stereotype.Service;

@Service
public class WhatsappOtpProvider implements OtpProvider {
    @Override
    public void sentOtp(String mobile) {

    }

    @Override
    public boolean verifyOtp(String mobile, String otp) {
        return false;
    }
}
