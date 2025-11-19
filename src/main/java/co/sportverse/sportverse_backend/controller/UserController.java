package co.sportverse.sportverse_backend.controller;

import co.sportverse.sportverse_backend.dto.ApiResponse;
import co.sportverse.sportverse_backend.dto.UpdateUserDetailsRequest;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {
        "https://sportverse.co.in",
        "http://localhost:8083"
})
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/update")
    public ResponseEntity<ApiResponse> updateUser(@RequestBody UpdateUserDetailsRequest request) {
        logger.info("POST /api/users/update - Updating user. mobileNumber: {}, name: {}, city: {}", 
                request.getMobileNumber(), request.getName(), request.getCity());
        try {
            if (request.getMobileNumber() == null || request.getMobileNumber().trim().isEmpty()) {
                logger.warn("POST /api/users/update - Validation failed: mobileNumber is required");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "mobileNumber is required"));
            }

            User updated = userService.updateUserByMobileNumber(
                    request.getMobileNumber().trim(),
                    request.getName().trim(),
                    request.getCity().trim()
            );

            if (updated == null) {
                logger.warn("POST /api/users/update - User not found. mobileNumber: {}", request.getMobileNumber());
                return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found for given mobile number"));
            }

            logger.info("POST /api/users/update - Successfully updated user. userId: {}, mobileNumber: {}", 
                    updated.getId(), updated.getPhone());
            return ResponseEntity.ok(new ApiResponse(true, "User updated successfully", updated));
        } catch (Exception e) {
            logger.error("POST /api/users/update - Error updating user. mobileNumber: {}", 
                    request.getMobileNumber(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error updating user: " + e.getMessage()));
        }
    }
}



