package com.cardmaster.finance.util;

import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utilidades criptográficas nativas 100% compatibles con Node.js crypto y Web Crypto API.
 * Emplea PBKDF2 con HmacSHA256, 100,000 iteraciones y clave derivada de 256 bits (32 bytes).
 */
public class CryptoUtils {

    private static final int ITERATIONS = 100000;
    private static final int KEY_LENGTH = 256;

    public static String generateSaltHex(int lengthBytes) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[lengthBytes];
        random.nextBytes(salt);
        return bytesToHex(salt);
    }

    public static String hashPassword(String password, String saltHex) {
        if (password == null || saltHex == null) return null;
        try {
            byte[] saltBytes = hexToBytes(saltHex);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (Exception e) {
            try {
                // Fallback con bytes UTF-8 de la sal si la decodificación hex falla
                byte[] saltBytes = saltHex.getBytes("UTF-8");
                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] hash = skf.generateSecret(spec).getEncoded();
                return bytesToHex(hash);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static boolean verifyPassword(String password, String expectedHash, String saltHex) {
        if (password == null || expectedHash == null || saltHex == null) return false;
        String computedHash = hashPassword(password, saltHex);
        return expectedHash.equalsIgnoreCase(computedHash);
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return (hex != null) ? hex.getBytes() : new byte[0];
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}

