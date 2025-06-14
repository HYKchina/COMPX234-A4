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

                System.out.print("Downloading: " + filename + " ");

                // 1. Send DOWNLOAD request
                String response = sendAndReceive(
                        clientSocket,
                        "DOWNLOAD " + filename,
                        serverAddress,
                        port,
                        MAX_RETRIES
                );

                if (response == null || response.startsWith("ERR")) {
                    System.out.println("\n! Failed: " +
                            (response == null ? "No response" : response));
                    continue;
                }

                // 2. Parse response: OK <filename> SIZE <size> PORT <port>
                String[] resParts = response.split(" ");
                long fileSize = Long.parseLong(resParts[3]);
                int dataPort = Integer.parseInt(resParts[5]);

                // 3. Open local file for writing
                try (RandomAccessFile localFile = new RandomAccessFile(filename, "rw")) {
                    localFile.setLength(fileSize);

                    long bytesReceived = 0;
                    int retryCount = 0;

                    // 4. Download in blocks
                    while (bytesReceived < fileSize && retryCount < MAX_RETRIES * 3) {
                        long start = bytesReceived;
                        long end = Math.min(start + BLOCK_SIZE - 1, fileSize - 1);

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

                        if (blockResponse == null || !blockResponse.contains("OK")) {
                            retryCount++;
                            System.out.print("x");
                            continue;
                        }

                        String[] blockParts = blockResponse.split("DATA ", 2);
                        if (blockParts.length < 2) {
                            retryCount++;
                            System.out.print("x");
                            continue;
                        }

                        byte[] fileData;
                        try {
                            fileData = Base64.getDecoder().decode(blockParts[1]);
                        } catch (IllegalArgumentException e) {
                            retryCount++;
                            System.out.print("x");
                            continue;
                        }

                        localFile.seek(start);
                        localFile.write(fileData);
                        bytesReceived = start + fileData.length;
                        retryCount = 0;
                        System.out.print(".");
                    }

                    // 5. Send CLOSE if done
                    if (bytesReceived == fileSize) {
                        sendAndReceive(clientSocket,
                                "FILE " + filename + " CLOSE",
                                serverAddress,
                                dataPort,
                                1); // Just one try
                        System.out.println(" ✓ (" + fileSize + " bytes)");
                    } else {
                        System.out.println("\n! Incomplete: " +
                                bytesReceived + "/" + fileSize + " bytes");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static String sendAndReceive(DatagramSocket socket, String message,
                                         InetAddress address, int port, int maxRetries) throws IOException {
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);

        byte[] receiveData = new byte[2048];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        int timeout = 1000;
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                socket.send(sendPacket);
                socket.setSoTimeout(timeout);
                socket.receive(receivePacket);
                return new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            } catch (SocketTimeoutException e) {
                attempts++;
                timeout *= 2;
                System.out.print("T"); // timeout symbol
            }
        }
        return null;
    }
}
