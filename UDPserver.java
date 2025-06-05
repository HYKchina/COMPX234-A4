import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Base64;

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
        // Open the file in read-only mode using try-with-resources for automatic closure
        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            // Get the total size of the file
            long fileSize = file.length();
        
            // Initialize buffer for reading file chunks and prepare Base64 encoding
            byte[] buffer = new byte[1000];
            long bytesSent = 0;
            
            // Main transfer loop - continues until entire file is sent
            while (bytesSent < fileSize) {
                // Wait for client to request a specific chunk
                byte[] requestData = new byte[1024];
                DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length);
                socket.receive(requestPacket);
            
                // Parse client request
                String request = new String(requestPacket.getData(), 0, requestPacket.getLength()).trim();
                String[] parts = request.split(" ");
            
                // Check if request follows expected format: "FILE [filename] GET START x END y"
                if (parts.length == 5 && parts[0].equals("FILE") && parts[2].equals("GET")) {
                    // Extract requested byte range
                    long start = Long.parseLong(parts[4]);
                    long end = Long.parseLong(parts[6]);
                
                    // Read requested chunk from file
                    int length = (int)(end - start + 1);
                    file.seek(start);
                    file.read(buffer, 0, length);
                
                    // Encode binary data to Base64 string and adjust length
                    String base64Data = Base64.getEncoder().encodeToString(buffer).substring(0, length*4/3+4);
                
                    // Prepare and send response with file chunk
                    String response = String.format("FILE %s OK START %d END %d DATA %s", 
                        filename, start, end, base64Data);
                    byte[] responseData = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseData, responseData.length, clientAddress, clientPort);
                    socket.send(responsePacket);
                }
            }
        
            // Send final close confirmation to client
            String closeResponse = "FILE " + filename + " CLOSE_OK";
            byte[] closeData = closeResponse.getBytes();
            DatagramPacket closePacket = new DatagramPacket(
                closeData, closeData.length, clientAddress, clientPort);
            socket.send(closePacket);
        
        } catch (IOException e) {
            // Handle any IO exceptions during file transfer
            System.err.println("File transfer error: " + e.getMessage());
        }
    }    
}