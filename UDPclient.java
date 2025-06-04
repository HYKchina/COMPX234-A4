
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
        
    }
}
