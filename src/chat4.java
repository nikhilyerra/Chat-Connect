import java.io.*;
import java.net.*;
import java.util.Scanner;

public class chat4 {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // Get user's name
            System.out.print("What is your name?: ");
            String name = scanner.nextLine();

            try (ServerSocket serverSocket = new ServerSocket(0)) {
                // Get random available port number
                int port = serverSocket.getLocalPort();
                System.out.println("Your port number is " + port);

                // Start a new thread for writing to the socket
                Thread writingThread = new Thread(() -> {
                    try {
                        // Get the port number of the peer to chat with
                        System.out.print("Please input the port number of the person you wish to have a conversation with: ");
                        int destinationPort = Integer.parseInt(scanner.nextLine());

                        // Connect to the destination's socket
                        try (Socket socket = new Socket("localhost", destinationPort)) {
                            System.out.println("Connected to destination on port " + destinationPort);
                            // Write to the peer's socket
                            PrintWriter SocketDataWriter = new PrintWriter(socket.getOutputStream(), true);
                            SocketDataWriter.println(name);

                            // Send and receive messages with the peer
                            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                                while (true) {
                                    // Get user's message
                                    String message = scanner.nextLine();
                                    // Send the message to the peer's socket
                                    
                                    SocketDataWriter.println(message);

                                    // Check if the message is a file transfer request
                                    if (message.startsWith("transfer ")) {
                                        // Get the filename from the message
                                        String filename = message.substring("transfer ".length());
                                        System.out.println("filename " + filename);
                                        
                                        
                                        // Send the file to the peer's socket
                                        sendFile(out, filename);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                writingThread.start();

                // Wait for a client to connect to the server's socket
                System.out.println("Waiting for connection!!!");
                Socket clientSocket = serverSocket.accept();
                //InetSocketAddress remoteAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();

                System.out.println("Connected to source on port " + clientSocket.getLocalPort());
                
                // Read messages from the client's socket
                try (BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    String message;
                    String client_name = clientReader.readLine();

                    while ((message = clientReader.readLine()) != null) {
                        // Print the message to the console
                        System.out.println(client_name + " : " +message);
                        // Check if the message is a file transfer request
                        if (message.startsWith("transfer ")) {
                        String[] parts = message.split(" ");
                        String command = parts[0];
                        String filename = parts[1];
                        System.out.println("command is" + command);
                        System.out.println("filename is" + filename);

                            // Receive the file from the client's socket
                            receiveFile(clientSocket, filename);
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendFile(DataOutputStream out, String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("File not found: " + filename);
            return;
        }
        System.out.println("Sending file: " + filename);

        // send file size to client
        try (FileInputStream fileIn = new FileInputStream(file)) {
            out.writeLong(file.length());

            // send file content to client
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();
        }
        System.out.println("File sent: " + filename);
    }

    private static void receiveFile(Socket clientSocket, String filename) throws IOException {
        System.out.println("Receiving file: " + filename);

        // receive file size from client
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        long fileSize = in.readLong();

        try (// receive file content from client
             FileOutputStream fileOut = new FileOutputStream("new_" + filename)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
        }
        System.out.println("File received: " + filename);
    }

}