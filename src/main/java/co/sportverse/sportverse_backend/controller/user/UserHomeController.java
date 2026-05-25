package co.sportverse.sportverse_backend.controller.user;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.AuthenticatedUser;
import co.sportverse.sportverse_backend.dto.home.UserHomeScreenDto;
import co.sportverse.sportverse_backend.security.AuthenticatedUserSupport;
import co.sportverse.sportverse_backend.service.UserHomeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Loads data for the in-app user home screen (personalized sections to be wired in {@link UserHomeService}).
 */
@RestController
@RequestMapping("/api/user/home")
public class UserHomeController {

    private static final Logger logger = LoggerFactory.getLogger(UserHomeController.class);

    @Autowired
    private UserHomeService userHomeService;

    @GetMapping
    public ResponseEntity<ApiResponse> getHome(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        String subject = AuthenticatedUserSupport.requireUserSubject(authenticatedUser);
        logger.info("GET /api/user/home - subject: {}", subject);

        UserHomeScreenDto payload = userHomeService.buildHomeScreen(subject);
        return ResponseEntity.ok(new ApiResponse(true, "Home screen loaded", payload));
    }
}
