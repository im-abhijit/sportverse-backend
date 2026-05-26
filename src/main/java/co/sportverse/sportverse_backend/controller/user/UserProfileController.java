package co.sportverse.sportverse_backend.controller.user;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.AuthenticatedUser;
import co.sportverse.sportverse_backend.dto.UpdateUserProfileRequest;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.security.AuthenticatedUserSupport;
import co.sportverse.sportverse_backend.service.BookingService;
import co.sportverse.sportverse_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Authenticated profile APIs: JWT {@code subject} is resolved to a {@link User} (phone / normalized digits).
 */
@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        String userSubject = AuthenticatedUserSupport.requireUserSubject(authenticatedUser);
        User user = userService.requireUserForJwtSubject(userSubject);
        logger.info("GET /api/user/profile/me - subject: {}, userId: {}", userSubject, user.getId());
        return ResponseEntity.ok(new ApiResponse(true, "User retrieved successfully", user));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody UpdateUserProfileRequest request) {
        String userSubject = AuthenticatedUserSupport.requireUserSubject(authenticatedUser);
        User user = userService.updateProfileFromJwt(request, userSubject);
        logger.info("PATCH /api/user/profile/me - subject: {}, userId: {}", userSubject, user.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Profile updated successfully", user));
    }

    /**
     * Permanently removes the user document and bookings whose {@code userId} matches the JWT subject,
     * and attempts to release linked venue slots where possible.
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse> deleteMyAccount(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        String userSubject = AuthenticatedUserSupport.requireUserSubject(authenticatedUser);
        BookingService.AccountDeletionResult r = bookingService.deleteUserAccountForJwtSubject(userSubject);
        Map<String, Object> data = new HashMap<>();
        data.put("bookingsRemoved", r.bookingsRemoved());
        data.put("userRemoved", r.userRemoved());
        logger.info(
                "DELETE /api/user/profile/me - subject: {}, bookingsRemoved: {}, userRemoved: {}",
                userSubject,
                r.bookingsRemoved(),
                r.userRemoved());
        return ResponseEntity.ok(new ApiResponse(true, "Account deleted successfully", data));
    }
}
