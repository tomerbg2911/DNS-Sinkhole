import java.io.*;
import java.util.HashSet;
import java.util.Scanner;

public class BlockListFilter {

    private HashSet<String> blockList;

    public BlockListFilter(String path) {
        blockList = new HashSet<>();
        loadFileToBlockList(path);
    }

    public boolean isValidQuery(DNSQuery dnsQuery) {

        String requestedDomain = dnsQuery.getQuestion().name();
        return !blockList.contains(requestedDomain);

    }

    private void loadFileToBlockList(String path) {
        try {
            File blockListFile = new File(path);
            Scanner myReader = new Scanner(blockListFile);
            while (myReader.hasNextLine()) {
                String domain = myReader.nextLine();
                blockList.add(domain);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.err.printf("An error occurred. File %s not found", path);
        }
    }

}
