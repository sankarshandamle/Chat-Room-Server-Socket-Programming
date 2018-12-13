// A Java program for a Client
import java.net.*;
import java.io.*;

public class Client extends Thread {
    // initialize socket and input output streams
    private static Socket socket = null;
    private static DatagramSocket udpSocket = null;
    private static BufferedReader stdIn = null;
    private static DataInputStream serverIn = null;
    private static DataOutputStream clientOut = null;

    public Client () {

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

        // string to read message from input
        String line = "";

        // keep reading until "bye" is input
        while (!line.equals("bye")) {
            try {
                // sender side

                System.out.print(">> ");
                line = stdIn.readLine();
                clientOut.writeUTF(line);
                String[] tokens = line.split(" ");
                if (tokens[0].toLowerCase().equals("sending")) {
                    if (tokens.length != 3 || ! (tokens[2].equals("UDP") || tokens[2].equals("TCP"))) {
                        System.out.println("Error : Improper usage\nProper Usage : sending fileName TCP/UDP");
                        continue;
                    }

                    File file = new File(tokens[1]);
                    if (!file.exists() && !file.isFile()) {
                        long fileLength = -1;
                        clientOut.writeLong(fileLength);
                        System.out.println("The file does not exist");
                        continue;
                    }

                    long fileLength = file.length();
                    clientOut.writeLong(fileLength);

                    long read = 0;
                    long remaining = fileLength;
                    long totalRead = 0;
                    long percent = 0;

                    // DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    FileInputStream fis = new FileInputStream(tokens[1]);
                    byte[] buffer = new byte[1024];

                    InetAddress host = InetAddress.getByName("localhost");

                    while ((read = fis.read(buffer)) > 0) {
                        if (tokens[2].equals("UDP")) {
                            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, host, 9000);
                            udpSocket.send(dp);
                        } else clientOut.write(buffer, 0, (int)read);
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
            clientOut.close();
            socket.close();
        } catch(IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) {
        // establish a connection
        try {
            socket = new Socket("localhost", 5000);
            udpSocket = new DatagramSocket(7000);
            System.out.println("Connected");

            // takes input from terminal
            stdIn = new BufferedReader(new InputStreamReader(System.in));
            // takes input server socket
            serverIn = new DataInputStream(socket.getInputStream());

            // sends output to the socket
            clientOut = new DataOutputStream(socket.getOutputStream());
            // clientOut.flush();
        } catch(UnknownHostException u) {
            System.out.println(u);
        } catch(IOException i) {
            System.out.println(i);
        }

        Client clientSender = new Client();
        clientSender.start();

        try {

            String line = "";

            // reads message from server until "bye" is sent
            while (!line.equals("bye")) {
                try {
                    // receiver side

                    line = serverIn.readUTF();
                    System.out.print("\n");
                    System.out.println("Server : " + line);
                    String[] tokens = line.split(" ");
                    if (tokens[0].toLowerCase().equals("sending")) {
                        if (tokens.length != 3 || ! (tokens[2].equals("UDP") || tokens[2].equals("TCP"))) {
                            System.out.println("Error : Improper usage from Server\nPlease Ignore");
                            System.out.print(">> ");
                            continue;
                        }

                        long fileLength = serverIn.readLong();
                        if (fileLength == -1) {
                            System.out.println("Error : File does not exist on Server Side");
                            System.out.print(">> ");
                            continue;
                        }

                        long read = 0;
                        long totalRead = 0;
                        long remaining = fileLength;
                        long percent = 0;

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
                                read = serverIn.read(buffer);
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
            serverIn.close();
        } catch(IOException i) {
            System.out.println(i);
        }

    }
}
