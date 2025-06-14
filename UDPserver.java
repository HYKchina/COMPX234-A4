import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Base64;
import java.util.Random;
import java.util.Arrays;

public class UDPserver {
    public static void main(String[] args) {
        // Validate command line arguments
        if (args.length != 1) {
            System.out.println("Usage: java UDPserver <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);

                String request = new String(packet.getData(), 0, packet.getLength()).trim();
                String[] parts = request.split(" ");

                if (parts.length >= 2 && parts[0].equals("DOWNLOAD")) {
                    String filename = parts[1];
                    File file = new File(filename);

                    if (file.exists() && file.isFile()) {
                        int dataPort = 50000 + new Random().nextInt(1000);

                        // Send OK response using the main server socket
                        String response = String.format("OK %s SIZE %d PORT %d", filename, file.length(), dataPort);
                        byte[] responseData = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length,
                            packet.getAddress(), packet.getPort());
                        serverSocket.send(responsePacket); // ✅ 使用主 socket 回复

                        // Start new thread to handle file transfer
                        new Thread(() -> {
                            try (DatagramSocket dataSocket = new DatagramSocket(dataPort)) {
                                System.out.println("Sending file " + filename + " to " +
                                        packet.getAddress().getHostAddress() + ":" + dataPort);
                                handleFileTransfer(dataSocket, filename, packet.getAddress());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();

                    } else {
                        String response = "ERR " + filename + " NOT_FOUND";
                        byte[] responseData = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length,
                            packet.getAddress(), packet.getPort());
                        serverSocket.send(responsePacket);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void handleFileTransfer(DatagramSocket socket, String filename,
                                           InetAddress clientAddress) {
        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            long fileSize = file.length();

            while (true) {
                byte[] requestData = new byte[1024];
                DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length);
                socket.receive(requestPacket);

                String request = new String(requestPacket.getData(), 0, requestPacket.getLength()).trim();
                String[] parts = request.split(" ");

                if (parts.length == 3 && parts[2].equals("CLOSE")) {
                    String closeResponse = "FILE " + filename + " CLOSE_OK";
                    byte[] closeData = closeResponse.getBytes();
                    DatagramPacket closePacket = new DatagramPacket(
                        closeData, closeData.length,
                        requestPacket.getAddress(), requestPacket.getPort());
                    socket.send(closePacket);
                    System.out.println("Finished sending file " + filename);
                    break;
                }

                if (parts.length == 7 && parts[0].equals("FILE") && parts[2].equals("GET")) {
                    long start = Long.parseLong(parts[4]);
                    long end = Long.parseLong(parts[6]);
                    int length = (int) (end - start + 1);

                    byte[] fileData = new byte[length];
                    file.seek(start);
                    int bytesRead = file.read(fileData);
                    if (bytesRead < length) {
                        fileData = Arrays.copyOf(fileData, bytesRead);
                    }

                    String base64Data = Base64.getEncoder().encodeToString(fileData);
                    String response = String.format("FILE %s OK START %d END %d DATA %s",
                            filename, start, start + fileData.length - 1, base64Data);
                    byte[] responseData = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseData, responseData.length,
                        requestPacket.getAddress(), requestPacket.getPort());
                    socket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            System.err.println("File transfer error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
