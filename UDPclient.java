import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPclient {
    public static void main(String[] args) {
        // Validate command line arguments
        if (args.length != 3) {
            System.out.println("Usage: java UDPclient <host> <port> <filelist>");
            return;
        }
            
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileList = args[2];
        
        try {
            // Create client socket
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(host);

            // Send test message
            String testMessage = "TEST MESSAGE";
            byte[] sendData = testMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, serverAddress, port);
            clientSocket.send(sendPacket);

            // Receive response
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Server response: " + response);
            
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
