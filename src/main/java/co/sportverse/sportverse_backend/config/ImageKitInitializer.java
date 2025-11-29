package co.sportverse.sportverse_backend.config;


import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImageKitInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ImageKitInitializer.class);

    @Autowired
    private ImageKitConfig imageKitConfig;

    @PostConstruct
    public void initializeImageKit() {
        try {
            ImageKit imageKit = ImageKit.getInstance();
            Configuration config = new Configuration(
                    imageKitConfig.getPublicKey(),
                    imageKitConfig.getPrivateKey(),
                    imageKitConfig.getUrlEndpoint()
            );
            imageKit.setConfig(config);
            logger.info("ImageKit SDK initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize ImageKit SDK", e);
            throw new RuntimeException("ImageKit initialization failed", e);
        }
    }
}

