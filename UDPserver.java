import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

    private static void handleFileTransfer(DatagramSocket socket, String filename, 
                                         InetAddress clientAddress, int clientPort) {
        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            long fileSize = file.length();
            System.out.println("Starting transfer of " + filename + " (" + fileSize + " bytes)");
        
            byte[] buffer = new byte[1000]; // 每次传输最多1000字节
            long bytesSent = 0;
        
            while (bytesSent < fileSize) {
                // 读取文件块
                int bytesToRead = (int) Math.min(buffer.length, fileSize - bytesSent);
                file.seek(bytesSent);
                int bytesRead = file.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) break;

                // 准备响应
                String response = String.format("FILE %s DATA %d %d ", filename, bytesSent, bytesSent + bytesRead - 1);
                byte[] responseData = new byte[response.length() + bytesRead];
                System.arraycopy(response.getBytes(), 0, responseData, 0, response.length());
                System.arraycopy(buffer, 0, responseData, response.length(), bytesRead);
            }
            System.out.println("\nTransfer completed: " + filename);
        } catch (IOException e) {
            System.err.println("Error transferring file: " + e.getMessage());
        }
    }
}