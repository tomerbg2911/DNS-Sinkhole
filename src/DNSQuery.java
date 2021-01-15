
public class DNSQuery {

    private byte[] dnsQueryBytes;

    // flags
    private int isResponseFlag;
    private int recursionAvailableFlag;
    private int recursionDesiredFlag;
    private int returnCodeFlag;

    // counters
    private int questionsCounter;
    private int answersCounter;
    private int authorativeCounter;

    // RRs
    private Question question;

    // section sizes
    private int headerSize = 12;
    private int questionSize;

    // current position in bytes arry
    private int bytesArryCursor = 0;
    

    public DNSQuery(byte[] dnsQueryBytes) {
        this.dnsQueryBytes = dnsQueryBytes;
        showBytesAsBits(dnsQueryBytes);
        initFlags();
        initCounters();

        System.out.println(readMsgFromByte(12));

    }

    private void initRRs() {
        

    }

    private String readMsgFromByte(int byteIdx) {

        StringBuilder result = new StringBuilder();

        int currentByteIdx = byteIdx;
        int bytesToRead = dnsQueryBytes[currentByteIdx];

        while (dnsQueryBytes[currentByteIdx] != 0) {

            boolean isPointer = !(getBitsSeqFromByte(this.dnsQueryBytes[byteIdx], 0, 2) == 0); // check if this byte
                                                                                               // represnts a pointer
                                                                                               // from
                                                                                               // first 2 bits
            if (isPointer) {
                byte[] pointerBytes = { dnsQueryBytes[currentByteIdx], dnsQueryBytes[currentByteIdx + 1] };
                pointerBytes[0] &= 0x3f;
                int pointerByteIdx = getNumberFromBytesArray(pointerBytes);
                result.append(readMsgFromByte(pointerByteIdx));
            } else {
                bytesToRead = dnsQueryBytes[currentByteIdx];
                currentByteIdx++;
                // append next label
                for (int i = 0; i < bytesToRead; i++) {
                    char c = (char) dnsQueryBytes[currentByteIdx];
                    result.append(c);
                    currentByteIdx++;
                }
                if (dnsQueryBytes[currentByteIdx] != 0) {
                    result.append('.');
                }
            }
        }

        return result.toString();
    }

    private void initFlags() {
        this.isResponseFlag = getBitsSeqFromByte(this.dnsQueryBytes[2], 0, 1);
        this.recursionDesiredFlag = getBitsSeqFromByte(this.dnsQueryBytes[2], 7, 1);
        this.recursionAvailableFlag = getBitsSeqFromByte(this.dnsQueryBytes[3], 0, 1);
        this.returnCodeFlag = getBitsSeqFromByte(this.dnsQueryBytes[3], 4, 4);
    }

    private void initCounters() {
        // get relevant bytes
        byte[] questionBytes = { dnsQueryBytes[4], dnsQueryBytes[5] };
        byte[] answerBytes = { dnsQueryBytes[6], dnsQueryBytes[7] };
        byte[] authorativeBytes = { dnsQueryBytes[8], dnsQueryBytes[9] };

        // extract int values from bytes
        this.questionsCounter = getNumberFromBytesArray(questionBytes);
        this.answersCounter = getNumberFromBytesArray(answerBytes);
        this.authorativeCounter = getNumberFromBytesArray(authorativeBytes);
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

    private static int getNumberFromBytesArray(byte[] arr) {
        int result = 0;
        for (byte b : arr) {
            result = result << 8;
            result += b;
        }
        return result;
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
