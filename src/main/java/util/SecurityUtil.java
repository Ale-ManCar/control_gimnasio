package util;

import org.mindrot.jbcrypt.BCrypt;




public class SecurityUtil {
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
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