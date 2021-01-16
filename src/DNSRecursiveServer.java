
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class DNSRecursiveServer {

    final int serverPortNumber = 5300;
    final int bufferSizeInBytes = 512;
    final int dnsPort = 53;
    final int maxNumOfIterations = 16;

    DatagramSocket clientSocket;
    DatagramSocket serverSocket;

    public DNSRecursiveServer() {
        try {
            this.clientSocket = new DatagramSocket();
            this.serverSocket = new DatagramSocket(serverPortNumber);
        } catch (SocketException e) {
            System.err.printf("Error occurred while trying to init a DatagramSocket");
        }
    }

    public void Run() {
        System.out.println(String.format("DNSRecursiveServer is now listening on port %d", serverPortNumber));

        while (true) {
            // init buffer
            byte[] receiveDataBuffer = new byte[bufferSizeInBytes];

            // receive a msg from a new client
            DatagramPacket receivePacketFromClient = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);
            try {
                this.serverSocket.receive(receivePacketFromClient);
            } catch (IOException e) {
                System.err.printf("Error occurred while trying to recieve a DatagramPacket");
            }

            // save client's IP and port number
            InetAddress IPAddress = receivePacketFromClient.getAddress();
            int port = receivePacketFromClient.getPort();

            // retrieve a random root dns name-server
            String nameServer = getRandomRootServerName();

            int countIterations = 0;
            boolean answerWasFound = false;
            while (countIterations < maxNumOfIterations && !answerWasFound) {
                try {
                    // send the client's query to the next NS
                    receiveDataBuffer = new byte[bufferSizeInBytes];
                    receivePacketFromClient.setAddress(InetAddress.getByName(nameServer));
                    receivePacketFromClient.setPort(dnsPort);

                    clientSocket.send(receivePacketFromClient);

                    // recieve response from the server
                    DatagramPacket receivePacketFromServer = new DatagramPacket(receiveDataBuffer, bufferSizeInBytes);
                    this.clientSocket.receive(receivePacketFromServer);

                    // init a new DNSQuery object from the response
                    DNSQuery responseDNSQuery = new DNSQuery(receivePacketFromServer.getData());
                    int errorCode = responseDNSQuery.getReturnCodeFlag();

                    // domain does not exist
                    if (errorCode == 3) {
                        answerWasFound = true;
                        responseDNSQuery.setFlag("AA", 0);
                        responseDNSQuery.setFlag("RA", 1);
                        sendDNSQuery(responseDNSQuery, IPAddress, port);
                        System.out.println("response sent!");
                    }

                    else if (errorCode == 0) {
                        // check if the response includes a final answer
                        if (responseDNSQuery.getAnswersCounter() > 0) {
                            answerWasFound = true;
                            responseDNSQuery.setFlag("AA", 0);
                            responseDNSQuery.setFlag("RA", 1);
                            sendDNSQuery(responseDNSQuery, IPAddress, port);
                            System.out.println("response sent!");
                        }

                        // check if the response includes authorative answers
                        else if (responseDNSQuery.getAuthorativeCounter() > 0) {
                            ResourceRecord rr = responseDNSQuery.getAuthoratyRR(0); // get the first entity on AUTHORITY
                                                                                    // section
                            nameServer = rr.getRDData();
                        }
                    }

                    else {
                        // delete thissss
                        System.out.println(errorCode);
                    }

                // TODO: make this more informative
                } catch (UnknownHostException e) {
                    System.err.printf("an UnknownHostException occured");
                } catch (IOException e) {
                    System.err.printf("an IOException occured");
                } finally {
                    countIterations++;
                }
            }
        }
    }

    private String getRandomRootServerName() {
        Random random = new Random();
        char randomRootServer = (char) ((int) 'a' + random.nextInt(13));
        return String.format("%c.root-servers.net", randomRootServer);
    }

    private void sendDNSQuery(DNSQuery dnsQuery, InetAddress IPAddress, int port) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(dnsQuery.dnsQueryBytes, dnsQuery.dnsQueryBytes.length, IPAddress,
                port);
        serverSocket.send(sendPacket);
    }
}
