
public class DNSQuery {

    public byte[] dnsQueryBytes;

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
        BytesHelper.showBytesAsBits(dnsQueryBytes);

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
        int questionType = BytesHelper.getNumberFromBytesArray(questionTypeBytes);
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
            int rrType = BytesHelper.getNumberFromBytesArray(rrTypeBytes);
            this.bytesArrayCursor += TypeSize;
            // RR class & ttl
            this.bytesArrayCursor += ClassSize;
            this.bytesArrayCursor += rrTTLSize;
            // RR RDLENGTH
            byte[] rdLengthBytes = { dnsQueryBytes[this.bytesArrayCursor], dnsQueryBytes[this.bytesArrayCursor + 1] };
            int rdLength = BytesHelper.getNumberFromBytesArray(rdLengthBytes);
            this.bytesArrayCursor += rrRDLength + rdLength;
            // construct new ResourceRecord
            rrArray[i] = new ResourceRecord(rrName, rrType);
        }
    }

    private void initFlags() {
        this.isResponseFlag = BytesHelper.getBitsSeqFromByte(this.dnsQueryBytes[2], 0, 1);
        this.recursionDesiredFlag = BytesHelper.getBitsSeqFromByte(this.dnsQueryBytes[2], 7, 1);
        this.recursionAvailableFlag = BytesHelper.getBitsSeqFromByte(this.dnsQueryBytes[3], 0, 1);
        this.returnCodeFlag = BytesHelper.getBitsSeqFromByte(this.dnsQueryBytes[3], 4, 4);
    }

    private void initCounters() {
        // get relevant bytes
        byte[] questionBytes = { dnsQueryBytes[4], dnsQueryBytes[5] };
        byte[] answerBytes = { dnsQueryBytes[6], dnsQueryBytes[7] };
        byte[] authorativeBytes = { dnsQueryBytes[8], dnsQueryBytes[9] };

        // extract int values from bytes
        this.questionsCounter = BytesHelper.getNumberFromBytesArray(questionBytes);
        this.answersCounter = BytesHelper.getNumberFromBytesArray(answerBytes);
        this.authorativeCounter = BytesHelper.getNumberFromBytesArray(authorativeBytes);
    }

    private String readNameFromByte(int startingByteIdx, boolean advanceCursor) {

        StringBuilder result = new StringBuilder();

        int currentByteIdx = startingByteIdx;
        int bytesToRead = dnsQueryBytes[currentByteIdx];

        while (dnsQueryBytes[currentByteIdx] != 0) {

            boolean isPointer = !(BytesHelper.getBitsSeqFromByte(this.dnsQueryBytes[currentByteIdx], 0, 2) == 0); // check
                                                                                                                  // if
                                                                                                                  // this
            // byte
            // represnts a
            // pointer
            // from
            // first 2 bits
            if (isPointer) {
                byte[] pointerBytes = { dnsQueryBytes[currentByteIdx], dnsQueryBytes[currentByteIdx + 1] };
                pointerBytes[0] &= 0x3f;
                int pointerByteIdx = BytesHelper.getNumberFromBytesArray(pointerBytes);
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

    public void setFlag(String flagName, int value) {
        byte newByte = 0;
        switch (flagName) {
            case "QR":
                this.isResponseFlag = value;
                newByte = BytesHelper.setBitsSeqOnByte(this.dnsQueryBytes[2], 0, 1, value);
                setDnsQueryByte(2, newByte);
                break;
            case "RD":
                this.recursionDesiredFlag = value;
                newByte = BytesHelper.setBitsSeqOnByte(this.dnsQueryBytes[2], 7, 1, value);
                setDnsQueryByte(2, newByte);
                break;
        }
    }

    private void setDnsQueryByte(int byteIdx, byte newByte) {
        this.dnsQueryBytes[byteIdx] = newByte;
    }
}
