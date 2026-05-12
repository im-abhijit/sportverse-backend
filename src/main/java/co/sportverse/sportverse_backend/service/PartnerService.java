package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.ExpoTokenRequest;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    public void updateExpoToken(String partnerId, ExpoTokenRequest request) {
        if (partnerId == null || partnerId.trim().isEmpty()) {
            throw new IllegalArgumentException("partnerId is required");
        }
        if (request == null || request.getExpoToken() == null || request.getExpoToken().trim().isEmpty()) {
            throw new IllegalArgumentException("expoToken is required");
        }
        boolean updated = partnerRepository.addExpoToken(partnerId.trim(), request.getExpoToken().trim());
        if (!updated) {
            throw new IllegalArgumentException("Partner not found");
        }
    }
}
