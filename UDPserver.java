import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPserver {
    public static void main(String[] args) {
        // 验证命令行参数
        if (args.length != 1) {
            System.out.println("Usage: java UDPserver <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);

        try {
            // 创建服务器Socket
            DatagramSocket serverSocket = new DatagramSocket(port);
            System.out.println("Server started on port " + port);
            
            // 主循环等待客户端请求
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                // 接收客户端请求
                serverSocket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Received: " + request);
                
                // 简单响应
                String response = "ECHO: " + request;
                byte[] responseData = response.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length, 
                    packet.getAddress(), packet.getPort());
                serverSocket.send(responsePacket);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
