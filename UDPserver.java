import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPserver {
    public static void main(String[] args) {
        // Validate command line arguments
        if (args.length != 1) {
            System.out.println("Usage: java UDPserver <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);  // 修正为 args[0]

        try {
            // Create server socket
            DatagramSocket serverSocket = new DatagramSocket(port);
            System.out.println("Server started on port " + port);
            
            // Main loop to wait for client requests
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                // Receive client request
                serverSocket.receive(packet);
                
                // 调用 handleDownloadRequest 方法处理请求
                handleDownloadRequest(serverSocket, packet);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void handleDownloadRequest(DatagramSocket serverSocket, DatagramPacket packet) throws IOException {
        String request = new String(packet.getData(), 0, packet.getLength()).trim();
        String[] parts = request.split(" ");
    
        if (parts.length >= 2 && parts[0].equals("DOWNLOAD")) {
            String filename = parts[1];
            File file = new File(filename);
        
            // 准备响应
            String response;
            if (file.exists() && file.isFile()) {
                response = "OK " + filename + " SIZE " + file.length();
            } else {
                response = "ERR " + filename + " NOT_FOUND";
            }
        
            // 发送响应
            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                responseData, responseData.length,
                packet.getAddress(), packet.getPort());
            serverSocket.send(responsePacket);
        }
    }
}