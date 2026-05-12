package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.UpdateUserDetailsRequest;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new user
     */
    public User createUser(String name, String mobileNumber, Boolean isVenueOwner) {
        // Check if user already exists using findByMobileNo
        User existingUser = userRepository.findByMobileNo(mobileNumber);
        if (existingUser != null) {
            throw new RuntimeException("User with mobile number " + mobileNumber + " already exists");
        }

        User user = new User(name, mobileNumber, isVenueOwner);
        return userRepository.save(user);
    }

    /**
     * Get user by mobile number
     */
    public User getUserByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNo(mobileNumber);
    }

    /**
     * Update user fields by mobile number. Name and city are optional; blank values are ignored by the repository.
     */
    public User updateUserDetails(UpdateUserDetailsRequest request) {
        if (request == null || request.getMobileNumber() == null || request.getMobileNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("mobileNumber is required");
        }
        String mobile = request.getMobileNumber().trim();
        String name = request.getName() != null ? request.getName().trim() : null;
        String city = request.getCity() != null ? request.getCity().trim() : null;

        User updated = userRepository.updateByMobileNo(mobile, name, city);
        if (updated == null) {
            throw new IllegalArgumentException("User not found for given mobile number");
        }
        return updated;
    }
}
