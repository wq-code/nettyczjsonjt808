package net.map591.utils;

public class ByteUtils {
    public static String bytesToHexWithSpace(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF)); // %02X 自动转大写，加空格
        }
        return sb.toString().trim(); // 去掉末尾多余空格
    }
}
