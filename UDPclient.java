import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Base64;

public class UDPclient {

    private static final int MAX_RETRIES = 5;
    private static final int BLOCK_SIZE = 1000;
    private static final int INITIAL_TIMEOUT = 1000;

    public static void main(String[] args) {
        // Validate command line arguments
        if (args.length != 3) {
            System.out.println("Usage: java UDPclient <host> <port> <filelist>");
            return;
        }
            
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String fileList = args[2];

        try (DatagramSocket clientSocket = new DatagramSocket();
         BufferedReader br = new BufferedReader(new FileReader(fileList))) {
        
        InetAddress serverAddress = InetAddress.getByName(host);
        String filename;
        
        while ((filename = br.readLine()) != null) {
            filename = filename.trim();
            if (filename.isEmpty()) continue;
            
            System.out.print("Starting download: " + filename + " ");
            
            // 1. 发送初始下载请求
            String response = sendAndReceive(
                clientSocket,
                "DOWNLOAD " + filename,
                serverAddress,
                port,
                MAX_RETRIES
            );
            
            // 2. 处理服务器响应
            if (response == null || response.startsWith("ERR")) {
                System.out.println("\n! Failed to initiate download: " + 
                    (response == null ? "No response" : response.split(" ")[2]));
                continue;
            }
            
            // 3. 解析文件信息
            String[] resParts = response.split(" ");
            long fileSize = Long.parseLong(resParts[3]);
            int dataPort = Integer.parseInt(resParts[5]);
            
            // 4. 创建本地文件
            try (RandomAccessFile localFile = new RandomAccessFile(filename, "rw")) {
                localFile.setLength(fileSize); // 预分配空间
                
                // 5. 分块下载
                long bytesReceived = 0;
                int retryCount = 0;
                
                while (bytesReceived < fileSize && retryCount < MAX_RETRIES * 3) {
                    long start = bytesReceived;
                    long end = Math.min(start + BLOCK_SIZE - 1, fileSize - 1);
                    
                    // 发送块请求
                    String blockRequest = String.format(
                        "FILE %s GET START %d END %d",
                        filename, start, end
                    );
                    
                    String blockResponse = sendAndReceive(
                        clientSocket,
                        blockRequest,
                        serverAddress,
                        dataPort,
                        MAX_RETRIES
                    );
                    
                    // 处理块响应
                    if (blockResponse == null || !blockResponse.contains("OK")) {
                        retryCount++;
                        System.out.print("x"); // 显示重试
                        continue;
                    }
                    
                    // 写入文件
                    String[] blockParts = blockResponse.split("DATA ");
                    byte[] fileData = Base64.getDecoder().decode(blockParts[1]);
                    localFile.seek(start);
                    localFile.write(fileData);
                    bytesReceived += fileData.length;
                    retryCount = 0; // 重置重试计数
                    
                    // 显示进度
                    System.out.print(".");
                }
                
                // 6. 完成处理
                if (bytesReceived == fileSize) {
                    // 发送关闭通知
                    sendAndReceive(clientSocket, 
                        "FILE " + filename + " CLOSE",
                        serverAddress, dataPort, 1);
                    System.out.println(" ✓ (" + fileSize + " bytes)");
                } else {
                    System.out.println("\n! Incomplete download (" + 
                        bytesReceived + "/" + fileSize + " bytes)");
                }
            }
        }
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
