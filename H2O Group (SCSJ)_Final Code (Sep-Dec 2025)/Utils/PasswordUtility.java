package com.name.ccf.Utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password security utility class.
 * Responsible for salting and hashing passwords using the BCrypt algorithm.
 * BCrypt is an industry-standard algorithm designed to be slow,
 * effectively protecting against brute-force attacks.
 */
public class PasswordUtility {

    // Default Work Factor. A higher value increases security but slows down computation.
    private static final int WORK_FACTOR = 12;

    public static String hashPassword(String plaintextPassword) {
        // BCrypt.hashpw automatically generates a random salt and embeds it in the hash result.
        return BCrypt.hashpw(plaintextPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean verifyPassword(String plaintextPassword, String hashedPassword) {
        // BCrypt.checkpw automatically extracts the salt and performs the comparison.
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            // Fail verification if the hash is null or not in the expected format.
            return false;
        }
        try {
            return BCrypt.checkpw(plaintextPassword, hashedPassword);
        } catch (Exception e) {
            // Handle potential exceptions from checkpw (e.g., malformed hash)
            return false;
        }
    }
}