package tool;

public class ByteUtils {

    public static String bytesToBit(byte[] bytes, int digit) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes)
            builder.append(String.format("%" + digit + "s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        return builder.toString();
    }

    public static boolean bitToBoolean(char c) {
        return c == '1';
    }

    public static int bitToInt(String bitString) {
        return Integer.parseInt(bitString, 2);
    }

    public static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) {
            value = (value << 8) + (b & 0xff);
        }
        return value;
    }

    public static Integer shortUnsignedAtOffset(byte[] bytes, int offset) {
        Integer lowerByte = (int) bytes[offset] & 0xFF;
        Integer upperByte = (int) bytes[offset + 1] & 0xFF; // // Interpret MSB as signed
        return (upperByte << 8) + lowerByte;
    }

    public static Integer bit24shortUnsignedAtOffset(byte[] bytes, int offset) {
        Integer lowerByte = (int) bytes[offset] & 0xFF;
        Integer middleByte = (int) bytes[offset + 1] & 0xFF; // // Interpret MSB as signed
        Integer upperByte = (int) bytes[offset + 2] & 0xFF;
        return (upperByte << 16) + (middleByte << 8) + lowerByte;
    }

}
