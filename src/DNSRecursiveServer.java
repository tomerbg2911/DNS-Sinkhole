
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class DNSRecursiveServer {

    final int serverPortNumber = 5300;
    final int bufferSizeInBytes = 512;

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

        // init buffers
        byte[] receiveDataBuffer = new byte[bufferSizeInBytes];
        byte[] sendDataBuffer = new byte[bufferSizeInBytes];

        // server's loop
        while (true) {

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

            // init a new DNSQuery object for the client's request
            int receivedDataLength = receivePacketFromClient.getLength();
            DNSQuery clientDNSQuery = new DNSQuery(cutBytesBuffer(receiveDataBuffer, receivedDataLength));
            clientDNSQuery.setFlag("RD", 0); // disable Recursive Desired flag

            // retrieve a random root dns name-server
            Random random = new Random();
            char randomRootServer = (char) ((int) 'a' + random.nextInt(13));
            String nameServer = String.format("%c.root-servers.net", randomRootServer);
            System.out.println(randomRootServer);

            while (true) {
                try {
                    // clean receive data buffer
                    receiveDataBuffer = new byte[bufferSizeInBytes];

                    // send the client's query to the next name-server
                    DatagramPacket sendPacket = new DatagramPacket(clientDNSQuery.dnsQueryBytes,
                            clientDNSQuery.dnsQueryBytes.length, InetAddress.getByName(nameServer), 53);
                    clientSocket.send(sendPacket);

                    // recieve response from the server
                    DatagramPacket receivePacketFromServer = new DatagramPacket(receiveDataBuffer,
                            receiveDataBuffer.length);
                    this.clientSocket.receive(receivePacketFromServer);

                    // init a new DNSQuery object for the response
                    receivedDataLength = receivePacketFromServer.getLength();
                    DNSQuery responseDNSQuery = new DNSQuery(cutBytesBuffer(receiveDataBuffer, receivedDataLength));

                    if (responseDNSQuery.getAnswersCounter() > 0) {
                        responseDNSQuery.setFlag("AA", 0);
                        responseDNSQuery.setFlag("RA", 1);
                        sendDNSQuery(responseDNSQuery, IPAddress, port);
                        System.out.println("response sent!");
                        break;
                    }

                    else if (responseDNSQuery.getAuthorativeCounter() > 0) {
                        // randomally choose NS to be the next server
                        int randomNS = random.nextInt(responseDNSQuery.getAuthorativeCounter());
                        ResourceRecord rr = responseDNSQuery.getAuthoratyRR(randomNS);
                        nameServer = rr.getRDData();
                    }

                // TODO: make this more informative
                } catch (UnknownHostException e) {
                    System.err.printf("an UnknownHostException occured");
                } catch (IOException e) {
                    System.err.printf("an IOException occured");
                }
            }
        }
    }

    private void sendDNSQuery(DNSQuery dnsQuery, InetAddress IPAddress, int port) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(dnsQuery.dnsQueryBytes, dnsQuery.dnsQueryBytes.length, IPAddress,
                port);
        serverSocket.send(sendPacket);
    }

    private byte[] cutBytesBuffer(byte[] buffer, int dataLength) {
        return Arrays.copyOfRange(buffer, 0, dataLength + 1);
    }
}
