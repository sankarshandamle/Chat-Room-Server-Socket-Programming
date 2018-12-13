// A Java program for a Server
import java.net.*;
import java.io.*;

public class Server extends Thread {
    //initialize socket and input stream
    private static Socket socket = null;
    private static DatagramSocket udpSocket = null;
    private static ServerSocket server = null;
    private static DataInputStream clientIn =  null;
    private static BufferedReader stdIn = null;
    private static DataOutputStream serverOut = null;

    public Server () {
    }

    public static String genPercentString (long percent) {
        String str = "";
        int i = 10;
        for (; i <= percent; i += 10) str += "=";
        str += ">";
        for (; i <= 100; i += 10) str += " ";
        return str;
    }

    public void run() {
        try {
            // receiver side
            String line = "";

            // reads message from client until "bye" is sent
            while (!line.toLowerCase().equals("bye")) {
                try {
                    line = clientIn.readUTF();
                    System.out.print("\n");
                    System.out.println("Client : " + line);
                    String[] tokens = line.split(" ");
                    if (tokens[0].toLowerCase().equals("sending")) {
                        if (tokens.length != 3 || ! (tokens[2].equals("UDP") || tokens[2].equals("TCP"))) {
                            System.out.println("Error : Improper usage from Client\nPlease Ignore");
                            System.out.print(">> ");
                            continue;
                        }

                        long fileLength = clientIn.readLong();
                        if (fileLength == -1) {
                            System.out.println("Error : File does not exist on Client Side");
                            System.out.print(">> ");
                            continue;
                        }
                        
                        long read = 0;
                        long totalRead = 0;
                        long remaining = fileLength;
                        long percent = 0;

                        // DataInputStream dis = new DataInputStream(socket.getInputStream());
                		FileOutputStream fos = new FileOutputStream(tokens[1]);
                        byte[] buffer = new byte[1024];
                        byte[] contents = new byte[1024];

                        DatagramPacket dp = new DatagramPacket(contents, contents.length);
                        while(remaining > 0) {
                            if (tokens[2].equals("UDP")) {
                                udpSocket.receive(dp);
                                buffer = dp.getData();
                                read = dp.getLength();
                            } else {
                                read = clientIn.read(buffer);
                                read = Math.min(buffer.length, remaining);
                            }
                            if (read <= 0) break;
                            totalRead += read;
                            remaining -= read;
                            percent = (totalRead*100)/fileLength;
                			System.out.print("Receiving " + tokens[1] + " [" + genPercentString(percent) + "] " + percent + "%" + "\r");
                			fos.write(buffer, 0, (int)read);
                        }
                        System.out.print('\n');
                        System.out.println("Received File");
                        // fos.flush();
                        fos.close();
                        // dis.close();
                    }
                } catch(IOException i) {
                    System.out.println(i);
                }
                System.out.print(">> ");
            }
            System.out.println("Closing connection");

            // close connection
            socket.close();
            clientIn.close();
        } catch(IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) {

        // starts server and waits for a connection
        try {
            server = new ServerSocket(5000);
            udpSocket = new DatagramSocket(9000);

            System.out.println("Server started");

            System.out.println("Waiting for a client ...");

            socket = server.accept();
            System.out.println("Client accepted");

            // takes input from terminal
            stdIn = new BufferedReader(new InputStreamReader(System.in));
            // takes input from the client socket
            clientIn = new DataInputStream(socket.getInputStream());

            serverOut = new DataOutputStream(socket.getOutputStream());
            // serverOut.flush();
        } catch (IOException i) {
            System.out.println(i);
        }

        Server serverListener = new Server();
        serverListener.start();

        // string to read message from input
        String line = "";

        // keep reading until "bye" is input
        while (!line.toLowerCase().equals("bye")) {
            try {
                // sender side

                System.out.print(">> ");
                line = stdIn.readLine();
                serverOut.writeUTF(line);
                String[] tokens = line.split(" ");
                if (tokens[0].toLowerCase().equals("sending")) {
                    if (tokens.length != 3 || ! (tokens[2].equals("UDP") || tokens[2].equals("TCP"))) {
                        System.out.println("Error : Improper usage\nProper Usage : sending fileName TCP/UDP");
                        continue;
                    }

                    File file = new File(tokens[1]);

                    if (!file.exists() && !file.isFile()) {
                        long fileLength = -1;
                        serverOut.writeLong(fileLength);
                        System.out.println("The file does not exist");
                        continue;
                    }

                    long fileLength = file.length();
                    serverOut.writeLong(file.length());

                    long read = 0;
                    long remaining = fileLength;
                    long totalRead = 0;
                    long percent = 0;

                    FileInputStream fis = new FileInputStream(tokens[1]);
                    byte[] buffer = new byte[1024];

                    InetAddress host = InetAddress.getByName("localhost");

                    while ((read = fis.read(buffer)) > 0) {
                        if (tokens[2].equals("UDP")) {
                            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, host, 7000);
                            udpSocket.send(dp);
                        } else serverOut.write(buffer, 0, (int)read);
                        totalRead += read;
                        remaining -= read;
                        percent = (totalRead*100)/fileLength;
                        System.out.print("Sending " + tokens[1] + " [" + genPercentString(percent) + "] " + percent + "%" + "\r");
                    }

                    System.out.print("\n");
                    System.out.println("Sent file");

                    // serverOut.flush();

                    // dos.flush();
                    fis.close();
                    // dos.close();

                }
            } catch(IOException i) {
                System.out.println(i);
            }
        }

        // close the connection
        try {
            stdIn.close();
            serverOut.close();
            socket.close();
        } catch(IOException i) {
            System.out.println(i);
        }

    }
}
