package il.ac.idc.cs.sinkhole;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class DNSRecursiveServer {

    // constants
    final int serverPortNumber = 5300;
    final int bufferSizeInBytes = 512;
    final int dnsDefaultPort = 53;
    final int maxNumOfIterations = 16;

    // class members
    private DatagramSocket clientSocket;
    private DatagramSocket serverSocket;
    private BlockListFilter blockListFilter;

    public DNSRecursiveServer() {
        try {
            this.clientSocket = new DatagramSocket();
            this.serverSocket = new DatagramSocket(serverPortNumber);
        } catch (SocketException e) {
            System.err.printf("error occurred while trying to open a new socket: %s\n", e);
        }
        this.blockListFilter = null;
    }

    public DNSRecursiveServer(String path) {
        this();
        this.blockListFilter = new BlockListFilter(path);
    }

    public void Run() {
        String welcomeMsg = String.format("DNSRecursiveServer is now listening on port %d", serverPortNumber);
        System.out.println(welcomeMsg);

        while (true) {
            // receive a new client packet and save the IP and port number
            DatagramPacket clientPacket = receivePacketFromClient();
            InetAddress clientIPAddress = clientPacket.getAddress();
            int clientPort = clientPacket.getPort();

            // init response related vars
            boolean finalResponseFound = false;
            DNSQuery responseDNSQuery = null;
            DatagramPacket serverPacket = null;

            // checks wether the clients DNS query is not in the blockList
            DNSQuery clientDNSQuery = new DNSQuery(clientPacket.getData());
            if (blockListFilter != null) {
                boolean isValidQuery = blockListFilter.isValidQuery(clientDNSQuery);
                if (!isValidQuery) {
                    responseDNSQuery = clientDNSQuery;
                    serverPacket = clientPacket;
                    responseDNSQuery.setFlag("RCODE", 3);
                    responseDNSQuery.setFlag("QR", 1); 
                    finalResponseFound = true;
                }
            }

            // init iterative process variables
            String nameServer = getRandomRootServerName(); // retrieve a random root dns name-server
            int countIterations = 0;
            try {
                while (countIterations < maxNumOfIterations && !finalResponseFound) {

                    // send the client's query to the next NS
                    byte[] DataBuffer = new byte[bufferSizeInBytes]; // a new clean buffer
                    clientPacket.setAddress(InetAddress.getByName(nameServer));
                    clientPacket.setPort(dnsDefaultPort);
                    clientSocket.send(clientPacket);

                    // receive response from the server
                    serverPacket = new DatagramPacket(DataBuffer, bufferSizeInBytes);
                    this.clientSocket.receive(serverPacket);

                    // init a new DNSQuery object from the response data (after removing redundant
                    // bytes)
                    byte[] trimmedData = Arrays.copyOfRange(serverPacket.getData(), 0, serverPacket.getLength());
                    responseDNSQuery = new DNSQuery(trimmedData);
                    int errorCode = responseDNSQuery.getReturnCodeFlag();

                    // domain name does not exist
                    if (errorCode == 3) {
                        finalResponseFound = true;
                    }

                    // no error condition
                    else if (errorCode == 0) {
                        // check if the response includes a final answer
                        if (responseDNSQuery.getAnswersCounter() > 0) {
                            finalResponseFound = true;
                        }

                        // check if the response includes authority answers
                        else if (responseDNSQuery.getAuthorityCounter() > 0) {
                            ResourceRecord rr = responseDNSQuery.getAuthorityRR(0); // get the first entity on AUTHORITY
                                                                                    // section
                            nameServer = rr.getRDData();
                        }
                    }
                }

                // return final response to the client
                if (finalResponseFound) {
                    responseDNSQuery.setFlag("AA", 0);
                    responseDNSQuery.setFlag("RA", 1);
                    serverPacket.setData(responseDNSQuery.getData());
                    serverPacket.setAddress(clientIPAddress);
                    serverPacket.setPort(clientPort);
                    serverSocket.send(serverPacket);
                    System.out.println("response sent!");
                }

            } catch (UnknownHostException e) {
                System.err.printf("an Unknown Host Exception occurred during the iterative process: %s\n", e);
            } catch (IOException e) {
                System.err.printf("IO error occurred during the iterative process: %s\n", e);
            } finally {
                countIterations++;
            }
        }
    }

    
    private DatagramPacket receivePacketFromClient() {
        byte[] DataBuffer = new byte[bufferSizeInBytes];
        DatagramPacket ClientPacket = new DatagramPacket(DataBuffer, DataBuffer.length);
        try {
            this.serverSocket.receive(ClientPacket);
        } catch (IOException e) {
            System.err.printf("IO error occurred while trying to receive a DatagramPacket from client: %s\n", e);
        }
        return ClientPacket;
    }

    private String getRandomRootServerName() {
        Random random = new Random();
        char randomRootServer = (char) ((int) 'a' + random.nextInt(13));
        return String.format("%c.root-servers.net", randomRootServer);
    }
}
