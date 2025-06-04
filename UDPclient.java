import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPclient {
    public static void main(String[] args) {
        // 验证命令行参数
        if (args.length != 3) {
            System.out.println("Usage: java UDPclient <host> <port> <filelist>");
            return;
        }
            
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileList = args[2];
        
        try {
            // 创建客户端Socket
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(host);
            
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
