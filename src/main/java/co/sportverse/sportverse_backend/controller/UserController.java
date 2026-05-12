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
                request != null ? request.getMobileNumber() : null,
                request != null ? request.getName() : null,
                request != null ? request.getCity() : null);
        User updated = userService.updateUserDetails(request);
        logger.info("POST /api/users/update - Successfully updated user. userId: {}, mobileNumber: {}", 
                updated.getId(), updated.getPhone());
        return ResponseEntity.ok(new ApiResponse(true, "User updated successfully", updated));
    }
}



