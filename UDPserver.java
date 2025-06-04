

public class UDPserver {
    public static void main(String[] args) {
        // 验证命令行参数
        if (args.length != 1) {
            System.out.println("Usage: java UDPserver <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
    }
}
