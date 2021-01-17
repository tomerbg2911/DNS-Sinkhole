
public class BytesOperations {
    public static int getBitsSeqFromByte(byte inputByte, int position, int amount) {
        int andMask = 0;
        for (int i = 0; i < amount; i++) {
            andMask = (andMask << 1) + 1;
        }
        andMask = andMask << (8 - position - amount);
        int result = inputByte & andMask;
        return result >> (8 - position - amount);
    }

    public static byte setBitsSeqOnByte(byte inputByte, int position, int amount, int value) {
        // move new value bits to the right idx
        value = value << (8 - position - amount);
        // create AND mask
        int andMask = 0;
        for (int i = 0; i < amount; i++) {
            andMask = (andMask << 1) + 1;
        }
        andMask = andMask << (8 - position - amount);
        // get new byte value
        // based on value = (value & ~mask) | (newvalue & mask)
        byte newValue = (byte) ((int) inputByte & ~andMask | (value & andMask));
        
        return newValue;
    }

    public static int getNumberFromBytesArray(byte[] arr) {
        int result = 0;
        for (byte b : arr) {
            result = result << 8;
            result += (int)b & 0xff; // treating the byte as unsigned
        }
        return result;
    }

    public static void showBytesAsBits(byte[] dnsQueryBytes) {
        int counter = 0;
        for (byte b : dnsQueryBytes) {
            System.out.println(counter++);
            System.out.println(Integer.toBinaryString(b & 255 | 256).substring(1));
            if (counter > 100)
                break;
        }
    }
}
