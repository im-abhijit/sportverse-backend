package co.sportverse.sportverse_backend.exceptions;

/**
 * Raised when an authenticated principal cannot be resolved to an existing user.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
