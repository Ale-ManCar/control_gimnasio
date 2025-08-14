package util;

import org.mindrot.jbcrypt.BCrypt;
public class SecurityUtil {
    /**
     * Cost factor for BCrypt hashing. Higher values increase security but also
     * the time required to generate the hash.
     */
    private static final int COST = 12;

    /**
     * Hashes the provided plain-text password using BCrypt with a random salt
     * and the predefined cost factor.
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(COST));
    }

    /**
     * Verifies that a plain-text password matches the stored BCrypt hash.
     */
    public static boolean verifyPassword(String password, String hashed) {
        if (password == null || hashed == null) {
            return false;
        }
        return BCrypt.checkpw(password, hashed);
    }
}