
import java.io.IOException;
import java.net.*;

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
            DatagramPacket receivePacket = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);
            try {
                this.serverSocket.receive(receivePacket);
            } catch (IOException e) {
                System.err.printf("Error occurred while trying to recieve a DatagramPacket");
            }

            DNSQuery dnsQuery = new DNSQuery(receiveDataBuffer);
            // String sentence = new String(Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()));
            // System.out.println("RECEIVED: " + sentence);
            // InetAddress IPAddress = receivePacket.getAddress();
            // int port = receivePacket.getPort();
            // String capitalizedSentence = sentence.toUpperCase();

            // sendDataBuffer = capitalizedSentence.getBytes();

            // DatagramPacket sendPacket =
            //         new DatagramPacket(sendDataBuffer, sendDataBuffer.length, IPAddress, port);
            // serverSocket.send(sendPacket);
        }
    }
}
