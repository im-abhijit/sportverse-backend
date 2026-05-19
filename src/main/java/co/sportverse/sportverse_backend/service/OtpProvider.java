package co.sportverse.sportverse_backend.service;

import org.springframework.stereotype.Service;

@Service
public interface OtpProvider {

    void sentOtp(String mobile);

    boolean verifyOtp(String mobile, String otp);
}
