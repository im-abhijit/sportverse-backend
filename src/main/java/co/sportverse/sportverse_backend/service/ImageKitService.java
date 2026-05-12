package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.config.ImageKitConfig;
import io.imagekit.sdk.ImageKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ImageKitService {

    @Autowired
    private ImageKitConfig imageKitConfig;

    public Map<String, String> createUploadAuthenticationParameters() {
        Map<String, String> response = ImageKit.getInstance()
                .getAuthenticationParameters(null, (System.currentTimeMillis() / 1000) + 300L);
        response.put("publicKey", imageKitConfig.getPublicKey());
        response.put("urlEndpoint", imageKitConfig.getUrlEndpoint());
        return response;
    }
}
