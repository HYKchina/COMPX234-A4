import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

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

    private static String sendDownloadRequest(DatagramSocket socket, String filename, 
                                       InetAddress serverAddress, int serverPort) throws IOException {
        String request = "DOWNLOAD " + filename;
        byte[] sendData = request.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        socket.send(sendPacket);
    
        // 设置超时
        socket.setSoTimeout(5000); // 5秒超时
    
        // 接收响应
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            socket.receive(receivePacket);
            return new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout while waiting for server response");
            return null;
        }
    }

    private static String sendAndReceive(DatagramSocket socket, String message, 
                                   InetAddress address, int port, int maxRetries) throws IOException {
        // Convert message to bytes and create send packet
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        
        // Prepare receive buffer and packet
        byte[] receiveData = new byte[2048];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        // Initial timeout is 1 second
        int timeout = 1000; // Initial timeout of 1 second
        int attempts = 0;
    
        // Retry loop up to maxRetries times
        while (attempts < maxRetries) {
            try {
                // Send the packet
                socket.send(sendPacket);
                // Set socket timeout
                socket.setSoTimeout(timeout);
            
                // Wait for response
                socket.receive(receivePacket);
                // Return trimmed response string
                return new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            } catch (SocketTimeoutException e) {
                // Increment attempt counter on timeout
                attempts++;
                System.out.println("Timeout, retry " + attempts + " of " + maxRetries);
                timeout *= 2; // Exponential backoff for next attempt
            }
        }
        // Return null if all retries exhausted
        return null; // All retries failed
    }
}
