package util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class LicenseManager {

    private static final String LICENSE_FILE = "license.key";
    private static final String SECRET_KEY = "GymSysSec2025@";

    public static boolean validateLicense() {
        try {
            File licenseFile = new File(LICENSE_FILE);
            String currentId = HardwareUtil.getHardwareId();

            if (!licenseFile.exists()) {
                createLicense(currentId);
                return true;
            }

            String savedId = decrypt(new String(Files.readAllBytes(licenseFile.toPath())));
            return savedId.equals(currentId);

        } catch (Exception e) {
            System.err.println("Error validación: " + e.getMessage());
            return false;
        }
    }

    private static void createLicense(String hardwareId) throws Exception {
        Path path = Paths.get(LICENSE_FILE);
        Files.write(path, encrypt(hardwareId).getBytes());
    }

    private static String encrypt(String input) throws Exception {
        SecretKeySpec key = new SecretKeySpec(generateKey(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes()));
    }

    private static String decrypt(String input) throws Exception {
        SecretKeySpec key = new SecretKeySpec(generateKey(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.getDecoder().decode(input)));
    }

    private static byte[] generateKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        return sha.digest(SECRET_KEY.getBytes("UTF-8"));
    }

    public static String generateReactivationRequest() {
        String hardwareId = HardwareUtil.getHardwareId();
        return Base64.getEncoder().encodeToString(hardwareId.getBytes());
    }

    public static boolean applyReactivationCode(String code) {
        try {
            String hardwareId = new String(Base64.getDecoder().decode(code));
            createLicense(hardwareId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateActivationCode(String requestCode) {
        try {
            String hardwareId = new String(Base64.getDecoder().decode(requestCode));
            return encrypt(hardwareId);
        } catch (Exception e) {
            return "ERROR";
        }
    }
}