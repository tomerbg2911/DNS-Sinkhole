
import java.io.IOException;
import java.net.*;
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
            DatagramPacket receivePacketFromClient = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);
            try {
                this.serverSocket.receive(receivePacketFromClient);
            } catch (IOException e) {
                System.err.printf("Error occurred while trying to recieve a DatagramPacket");
            }

            // save client's IP and port number
            InetAddress IPAddress = receivePacketFromClient.getAddress();
            int port = receivePacketFromClient.getPort();

            //
            DNSQuery clientDNSQuery = new DNSQuery(receiveDataBuffer);
            // clientDNSQuery.setFlag("RD", 0);

            // retrieve a random dns name-server
            Random random = new Random();
            char randomRootServer = (char) ((int) 'a' + random.nextInt(13));
            String nameServer = String.format("%c.root-servers.net", randomRootServer);

            while (true) {
                try {
                    // send the client's query to the next name server
                    DatagramPacket sendPacket = new DatagramPacket(clientDNSQuery.dnsQueryBytes, bufferSizeInBytes,
                            InetAddress.getByName(nameServer), 53);
                    clientSocket.send(sendPacket);

                    // recieve response from the server
                    DatagramPacket receivePacketFromServer = new DatagramPacket(receiveDataBuffer,
                            receiveDataBuffer.length);
                    this.clientSocket.receive(receivePacketFromServer);

                    DNSQuery responseDNSQuery = new DNSQuery(receiveDataBuffer);
                    int a = 5;




                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            // String sentence = new String(Arrays.copyOfRange(receivePacket.getData(), 0,
            // receivePacket.getLength()));
            // System.out.println("RECEIVED: " + sentence);
            // InetAddress IPAddress = receivePacket.getAddress();
            // int port = receivePacket.getPort();
            // String capitalizedSentence = sentence.toUpperCase();

            // sendDataBuffer = capitalizedSentence.getBytes();

            // DatagramPacket sendPacket =
            // new DatagramPacket(sendDataBuffer, sendDataBuffer.length, IPAddress, port);
            // serverSocket.send(sendPacket);
        }
    }
}
