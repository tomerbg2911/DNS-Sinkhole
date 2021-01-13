
public class DNSQuery {

    private byte[] dnsQueryBytes;

    // flags
    private int isResponseFlag;
    private int recursionAvailableFlag;
    private int returnCodeFlag;

    // counters
    private int questionsCounter;
    private int answersCounter;
    private int authorativeCounter;

    public DNSQuery(byte[] dnsQueryBytes) {
        this.dnsQueryBytes = dnsQueryBytes;
        showBytesAsBits(dnsQueryBytes);
        initFlags();
        initCounters();
    }

    private void initFlags() {
        this.isResponseFlag = getBitsSeqFromByte(this.dnsQueryBytes[2], 0, 1);
        this.recursionAvailableFlag = getBitsSeqFromByte(this.dnsQueryBytes[3], 1, 1); // TODO - check if this is the one
                                                                                       // we need
        this.returnCodeFlag = getBitsSeqFromByte(this.dnsQueryBytes[3], 4, 4);
    }

    private void initCounters() {
        this.questionsCounter = (dnsQueryBytes[4] << 8) + dnsQueryBytes[5];
        this.answersCounter = (dnsQueryBytes[6] << 8) + dnsQueryBytes[7];
        this.authorativeCounter = (dnsQueryBytes[8] << 8) + dnsQueryBytes[9];
    }

    // static methods

    private static int getBitsSeqFromByte(byte inputByte, int position, int amount) {
        int andMask = 0;
        for (int i = 0; i < amount; i++) {
            andMask = (andMask << 1) + 1;
        }
        andMask = andMask << (8 - position - amount);
        return inputByte & andMask;
    }

    private static void showBytesAsBits(byte[] dnsQueryBytes) {
        int counter = 0;
        for (byte b : dnsQueryBytes) {
            System.out.println(counter++);
            System.out.println(Integer.toBinaryString(b & 255 | 256).substring(1));
            if (counter > 100)
                break;
        }
    }
}
