
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
    private ResourceRecord[] answerRRs;
    private ResourceRecord[] authorityRRs;

    // section sizes
    private final int headerSize = 12;
    private final int TypeSize = 2;
    private final int ClassSize = 2;
    private final int rrTTLSize = 4;
    private final int rrRDLength = 2;

    // current position in bytes array
    private int bytesArrayCursor = 0;

    public DNSQuery(byte[] dnsQueryBytes) {
        this.dnsQueryBytes = dnsQueryBytes;
        showBytesAsBits(dnsQueryBytes);

        // analyze dns query sections while moving the cursor respectively
        analyzeHeaderSection();
        analyzeQuestionSection();
        analyzeRRsSection();

        // move cursor to the first byte
        this.bytesArrayCursor = 0;
    }

    private void analyzeHeaderSection() {
        initFlags();
        initCounters();
        this.bytesArrayCursor += headerSize;
    }

    private void analyzeQuestionSection() {
        String questionName = readNameFromByte(this.bytesArrayCursor, true);
        byte[] questionTypeBytes = { dnsQueryBytes[this.bytesArrayCursor], dnsQueryBytes[this.bytesArrayCursor + 1] };
        int questionType = getNumberFromBytesArray(questionTypeBytes);
        this.bytesArrayCursor += TypeSize;
        this.bytesArrayCursor += ClassSize;
        this.question = new Question(questionName, questionType);
    }

    private void analyzeRRsSection() {
        // read RRs (answer + authorative) sections
        if (this.answersCounter > 0) {
            this.answerRRs = new ResourceRecord[this.answersCounter];
            initRRArray(this.answerRRs);
        } else if (this.authorativeCounter > 0) {
            this.authorityRRs = new ResourceRecord[this.authorativeCounter];
            initRRArray(this.authorityRRs);
        }
    }

    private void initRRArray(ResourceRecord[] rrArray) {
        for (int i = 0; i < rrArray.length; i++) {
            // RR name
            String rrName = readNameFromByte(this.bytesArrayCursor, true);
            // RR type
            byte[] rrTypeBytes = { dnsQueryBytes[this.bytesArrayCursor], dnsQueryBytes[this.bytesArrayCursor + 1] };
            int rrType = getNumberFromBytesArray(rrTypeBytes);
            this.bytesArrayCursor += TypeSize;
            // RR class & ttl
            this.bytesArrayCursor += ClassSize;
            this.bytesArrayCursor += rrTTLSize;
            // RR RDLENGTH
            byte[] rdLengthBytes = { dnsQueryBytes[this.bytesArrayCursor], dnsQueryBytes[this.bytesArrayCursor + 1] };
            int rdLength = getNumberFromBytesArray(rdLengthBytes);
            this.bytesArrayCursor += rrRDLength + rdLength;
            // construct new ResourceRecord
            rrArray[i] = new ResourceRecord(rrName, rrType);
        }
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

    private String readNameFromByte(int startingByteIdx, boolean advanceCursor) {

        StringBuilder result = new StringBuilder();

        int currentByteIdx = startingByteIdx;
        int bytesToRead = dnsQueryBytes[currentByteIdx];

        while (dnsQueryBytes[currentByteIdx] != 0) {

            boolean isPointer = !(getBitsSeqFromByte(this.dnsQueryBytes[currentByteIdx], 0, 2) == 0); // check if this
                                                                                                      // byte
                                                                                                      // represnts a
                                                                                                      // pointer
                                                                                                      // from
                                                                                                      // first 2 bits
            if (isPointer) {
                byte[] pointerBytes = { dnsQueryBytes[currentByteIdx], dnsQueryBytes[currentByteIdx + 1] };
                pointerBytes[0] &= 0x3f;
                int pointerByteIdx = getNumberFromBytesArray(pointerBytes);
                result.append(readNameFromByte(pointerByteIdx, false));
                currentByteIdx += 2;
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

        if (advanceCursor) {
            this.bytesArrayCursor += currentByteIdx - startingByteIdx + 1;
        }

        return result.toString();
    }

    // bytes helper methods
    private int getBitsSeqFromByte(byte inputByte, int position, int amount) {
        int andMask = 0;
        for (int i = 0; i < amount; i++) {
            andMask = (andMask << 1) + 1;
        }
        andMask = andMask << (8 - position - amount);
        return inputByte & andMask;
    }

    private int getNumberFromBytesArray(byte[] arr) {
        int result = 0;
        for (byte b : arr) {
            result = result << 8;
            result += b;
        }
        return result;
    }

    private void showBytesAsBits(byte[] dnsQueryBytes) {
        int counter = 0;
        for (byte b : dnsQueryBytes) {
            System.out.println(counter++);
            System.out.println(Integer.toBinaryString(b & 255 | 256).substring(1));
            if (counter > 100)
                break;
        }
    }
}
