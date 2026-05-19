package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.UpdateUserDetailsRequest;
import co.sportverse.sportverse_backend.dto.UpdateUserProfileRequest;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.exceptions.UserNotFoundException;
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
    public User createUser(String name, String mobileNumber, Boolean isVenueOwner, String firstName, String lastName, String email) {
        // Check if user already exists using findByMobileNo
        User existingUser = userRepository.findByMobileNo(mobileNumber);
        if (existingUser != null) {
            throw new RuntimeException("User with mobile number " + mobileNumber + " already exists");
        }

        User user = new User(name, mobileNumber, isVenueOwner, firstName, lastName, email);
        return userRepository.save(user);
    }

    /**
     * Get user by mobile number
     */
    public User getUserByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNo(mobileNumber);
    }

    /**
     * Resolves {@link User} for JWT subject (normalized digits or stored phone shapes).
     *
     * @throws UserNotFoundException if no matching user exists
     */
    public User requireUserForJwtSubject(String jwtSubject) {
        if (jwtSubject == null || jwtSubject.isBlank()) {
            throw new IllegalArgumentException("Authenticated user identity is required");
        }
        User user = findUserByJwtSubject(jwtSubject.trim());
        if (user == null) {
            throw new UserNotFoundException("No user found for the authenticated identity");
        }
        return user;
    }

    /**
     * Try common {@code phone} field shapes stored at registration time.
     */
    public User findUserByJwtSubject(String jwtSubject) {
        if (jwtSubject == null || jwtSubject.isBlank()) {
            return null;
        }
        String raw = jwtSubject.trim();

        User u = userRepository.findByMobileNo(raw);
        if (u != null) {
            return u;
        }

        String norm = JwtService.normalizeIndianPhoneDigits(raw);
        if (!norm.isEmpty() && !norm.equals(raw)) {
            u = userRepository.findByMobileNo(norm);
            if (u != null) {
                return u;
            }
        }

        if (norm.length() == 10) {
            u = userRepository.findByMobileNo("+91" + norm);
            if (u != null) {
                return u;
            }
            u = userRepository.findByMobileNo("91" + norm);
            if (u != null) {
                return u;
            }
        }
        return null;
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

    /**
     * Updates {@code firstName}, {@code lastName}, {@code email} for the user resolved from the JWT subject.
     * Fields that are {@code null} in the request are left unchanged (must send at least one field).
     */
    public User updateProfileFromJwt(UpdateUserProfileRequest request, String jwtSubject) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getFirstName() == null && request.getLastName() == null && request.getEmail() == null) {
            throw new IllegalArgumentException("At least one of firstName, lastName, or email is required");
        }
        User resolved = requireUserForJwtSubject(jwtSubject);
        User updated = userRepository.updateProfileById(
                resolved.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail());
        if (updated == null) {
            throw new IllegalArgumentException("User could not be updated");
        }
        return updated;
    }
}
