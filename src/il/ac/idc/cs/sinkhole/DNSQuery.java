package il.ac.idc.cs.sinkhole;

public class DNSQuery {

    private byte[] dnsQueryBytes;

    // flags
    private int isResponseFlag;
    private int authorityAnswerFlag;
    private int recursionAvailableFlag;
    private int recursionDesiredFlag;
    private int returnCodeFlag;
    
    // counters
    private int questionsCounter;
    private int answersCounter;
    private int authorityCounter;
    
    // Question Section
    private Question question;
    
    // RRs
    private ResourceRecord[] answerRRs;
    private ResourceRecord[] authorityRRs;
    
    // section sizes
    private final int headerSize = 12;
    private final int typeSize = 2;
    private final int classSize = 2;
    private final int rrTTLSize = 4;
    private final int rrRDLength = 2;
    
    // current position in bytes array
    private int bytesArrayCursor = 0;
    
    // getters
    public byte[] getData() {
        return this.dnsQueryBytes;
    }
    
    public int getQuestionsCounter() {
        return this.questionsCounter;
    }
    
    public int getAnswersCounter() {
        return this.answersCounter;
    }

    public int getAuthorityCounter() {
        return this.authorityCounter;
    }
    
    public int getReturnCodeFlag() {
        return this.returnCodeFlag;
    }
    
    public Question getQuestion() {
        return this.question;
    }
    
    public ResourceRecord getAnswerRR(int idx) {
        return this.answerRRs[idx];
    }
    
    public ResourceRecord getAuthorityRR(int idx) {
        return this.authorityRRs[idx];
    }

    // setters
    public void setFlag(String flagName, int value) {
        byte newByte = 0;
        switch (flagName) {
            // header flags are on bytes 2-3 of the query
            case "QR":
                this.isResponseFlag = value;
                newByte = BytesOperations.setBitsSeqOnByte(this.dnsQueryBytes[2], 0, 1, value);
                setDnsQueryByte(2, newByte);
                break;
            case "RD":
                this.recursionDesiredFlag = value;
                newByte = BytesOperations.setBitsSeqOnByte(this.dnsQueryBytes[2], 7, 1, value);
                setDnsQueryByte(2, newByte);
                break;
            case "RA":
                this.recursionAvailableFlag = value;
                newByte = BytesOperations.setBitsSeqOnByte(this.dnsQueryBytes[3], 0, 1, value);
                setDnsQueryByte(3, newByte);
                break;
            case "AA":
                this.authorityAnswerFlag = value;
                newByte = BytesOperations.setBitsSeqOnByte(this.dnsQueryBytes[2], 5, 1, value);
                setDnsQueryByte(2, newByte);
                break;
            case "RCODE":
                this.returnCodeFlag = value;
                newByte = BytesOperations.setBitsSeqOnByte(this.dnsQueryBytes[3], 4, 4, value);
                setDnsQueryByte(3, newByte);
                break;
        }
    }

    private void setDnsQueryByte(int byteIdx, byte newByte) {
        this.dnsQueryBytes[byteIdx] = newByte;
    }

    // constructor
    public DNSQuery(byte[] dnsQueryBytes) {
        this.dnsQueryBytes = dnsQueryBytes;
        // BytesHelper.showBytesAsBits(dnsQueryBytes);
        
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
        int questionType = BytesOperations.getNumberFromBytesArray(questionTypeBytes);
        this.bytesArrayCursor += typeSize;
        this.bytesArrayCursor += classSize;
        this.question = new Question(questionName, questionType);
    }

    private void analyzeRRsSection() {
        // read RRs (answer + authority) sections
        if (this.answersCounter > 0) {
            this.answerRRs = new ResourceRecord[this.answersCounter];
            initRRArray(this.answerRRs);
        }
        if (this.authorityCounter > 0) {
            this.authorityRRs = new ResourceRecord[this.authorityCounter];
            initRRArray(this.authorityRRs);
        }
    }

    private void initRRArray(ResourceRecord[] rrArray) {
        for (int i = 0; i < rrArray.length; i++) {
            // RR name
            String rrName = readNameFromByte(this.bytesArrayCursor, true);
            // RR type
            byte[] rrTypeBytes = { dnsQueryBytes[this.bytesArrayCursor], dnsQueryBytes[this.bytesArrayCursor + 1] };
            int rrType = BytesOperations.getNumberFromBytesArray(rrTypeBytes);
            this.bytesArrayCursor += typeSize;
            // RR class & ttl
            this.bytesArrayCursor += classSize;
            this.bytesArrayCursor += rrTTLSize;
            // RR RDLENGTH
            byte[] rdLengthBytes = { dnsQueryBytes[this.bytesArrayCursor], dnsQueryBytes[this.bytesArrayCursor + 1] };
            int rdDataLength = BytesOperations.getNumberFromBytesArray(rdLengthBytes);
            this.bytesArrayCursor += rrRDLength;
            // RR RDDATA
            String rdData = "";
            if (rrType == 2) { // we try to decode the rdData only on NS type
                rdData = readNameFromByte(this.bytesArrayCursor, false);
            }
            this.bytesArrayCursor += rdDataLength;
            // construct new ResourceRecord
            rrArray[i] = new ResourceRecord(rrName, rrType, rdData);
        }
    }

    private void initFlags() {
        // header flags are on bytes 2-3 of the query
        this.isResponseFlag = BytesOperations.getBitsSeqFromByte(this.dnsQueryBytes[2], 0, 1);
        this.authorityAnswerFlag = BytesOperations.getBitsSeqFromByte(this.dnsQueryBytes[2], 5, 1);
        this.recursionDesiredFlag = BytesOperations.getBitsSeqFromByte(this.dnsQueryBytes[2], 7, 1);
        this.recursionAvailableFlag = BytesOperations.getBitsSeqFromByte(this.dnsQueryBytes[3], 0, 1);
        this.returnCodeFlag = BytesOperations.getBitsSeqFromByte(this.dnsQueryBytes[3], 4, 4);
    }

    private void initCounters() {
        // header counters are on bytes 4-9 of the query
        byte[] questionBytes = { dnsQueryBytes[4], dnsQueryBytes[5] };
        byte[] answerBytes = { dnsQueryBytes[6], dnsQueryBytes[7] };
        byte[] authorityBytes = { dnsQueryBytes[8], dnsQueryBytes[9] };

        // extract int values from bytes
        this.questionsCounter = BytesOperations.getNumberFromBytesArray(questionBytes);
        this.answersCounter = BytesOperations.getNumberFromBytesArray(answerBytes);
        this.authorityCounter = BytesOperations.getNumberFromBytesArray(authorityBytes);
    }

    private String readNameFromByte(int startingByteIdx, boolean advanceCursor) {
        StringBuilder result = new StringBuilder();
        int currentByteIdx = startingByteIdx;
        int bytesToRead = dnsQueryBytes[currentByteIdx];
        boolean readFromBytes = true;
        while (readFromBytes) {

            // check if this byte represents a pointer from first 2 bits
            boolean isPointer = !(BytesOperations.getBitsSeqFromByte(this.dnsQueryBytes[currentByteIdx], 0, 2) == 0);
            if (isPointer) {
                byte[] pointerBytes = { dnsQueryBytes[currentByteIdx], dnsQueryBytes[currentByteIdx + 1] };
                pointerBytes[0] = BytesOperations.setBitsSeqOnByte(pointerBytes[0], 0, 2, 0); // remove pointer '11' prefix
                int pointerByteIdx = BytesOperations.getNumberFromBytesArray(pointerBytes);
                result.append(readNameFromByte(pointerByteIdx, false));
                currentByteIdx += 2;
                readFromBytes = false;
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
                } else {
                    currentByteIdx++;
                    readFromBytes = false;
                }
            }
        }

        if (advanceCursor) {
            this.bytesArrayCursor += currentByteIdx - startingByteIdx;
        }

        return result.toString();
    }


}
