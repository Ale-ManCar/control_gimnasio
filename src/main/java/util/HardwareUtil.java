package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.UUID;

public class HardwareUtil {

    public static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (network.isVirtual() || !network.isUp() || network.isLoopback()) continue;

                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("Error MAC: " + e.getMessage());
        }
        return "NO_MAC";
    }

    public static String getDiskId() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Process process;

            if (os.contains("win")) {
                process = Runtime.getRuntime().exec(
                        new String[]{"cmd", "/c", "wmic diskdrive get serialnumber"}
                );
            } else if (os.contains("linux")) {
                process = Runtime.getRuntime().exec(
                        new String[]{"bash", "-c", "sudo hdparm -I /dev/sda | grep 'Serial Number'"}
                );
            } else {
                process = Runtime.getRuntime().exec(
                        new String[]{"bash", "-c", "system_profiler SPHardwareDataType | grep 'Serial'"}
                );
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    output.append(line.replaceAll("[^a-zA-Z0-9]", ""));
                }
            }

            if (output.length() > 0) {
                return output.toString();
            }
        } catch (Exception e) {
            System.err.println("Error disco: " + e.getMessage());
        }
        return "NO_DISK";
    }

    public static String getHardwareId() {
        try {
            String baseId = getMacAddress() + "_" + getDiskId();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(baseId.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString().substring(0, 32);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}