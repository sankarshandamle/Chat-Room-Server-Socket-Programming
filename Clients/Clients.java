/*
  Java implementation of the ChatRoom server.
  args[0] : username of the client
  args[1] : IP address of the server
  args[2] : Server port
  args[3] : DatagramSocket port for the client
*/
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.regex.*;

// Clients class
public class Clients
{
    public static int ServerPort = 0;
    public static DatagramSocket udpSocket = null;

    // helper functions
    public static String genPercentString (long percent) {
        String str = "";
        int i = 10;
        for (; i <= percent; i += 10) str += "=";
        str += ">";
        for (; i <= 100; i += 10) str += " ";
        return str;
    }

    public static void printScreen(String name) {
      System.out.println("---------------------------");
      System.out.println("** ChatRoom Service **");
      System.out.println("---------------------------");
      System.out.println("Commands available for Client: "+name);
      System.out.println("---------------------------");
      System.out.println("1. create chatroom *name*");
      System.out.println("2. join *chatroom*");
      System.out.println("3. leave");
      System.out.println("4. add *username*");
      System.out.println("5. list chatrooms");
      System.out.println("6. list users");
      System.out.println("7. reply \"text\"");
      System.out.println("8. reply fileName tcp/udp");
      System.out.println("\"exit\" to Quit");
      System.out.println("---------------------------");
    }

    public static void main(String args[]) throws UnknownHostException, IOException
    {
        BufferedReader scn = new BufferedReader(new InputStreamReader(System.in));

        // getting localhost ip and port
        InetAddress ip = InetAddress.getByName(args[1]);
        ServerPort = (int) Integer.parseInt(args[2]);

        // establish the connection
        Socket s = new Socket(ip, ServerPort);
        udpSocket = new DatagramSocket(Integer.parseInt(args[3])); // for UDP file transfer

        // obtaining input and out streams
        DataInputStream serverIn = new DataInputStream(s.getInputStream());
        DataOutputStream clientOut = new DataOutputStream(s.getOutputStream());

        // assigning the username and values
        System.out.println(serverIn.readUTF());
        clientOut.writeUTF(args[0]);
        System.out.println(serverIn.readUTF());
        clientOut.writeUTF(args[3]);
        printScreen(args[0]);
        System.out.print(">> ");

        // sendMessage thread
        Thread sendMessage = new Thread(new Runnable()
        {
            @Override
            public void run() {
              int print = 0; // local variable for print screen
              String name = args[0]; // local variable to store clinet's username
              String option = "";
                while (true) {
                    try {
                      // read the message to deliver.
                        String msg = scn.readLine();
                        // write on the output stream
                        clientOut.writeUTF(msg);
                        if (msg.equals("exit")) {
                          System.out.println("Bye....");
                          s.close();
                          clientOut.close();
                          serverIn.close();
                          break;
                        }
                        print = print + 1;
                        String[] tokens = msg.split(" ");
                        if (tokens[0].equals("reply")){
                          /* the following stub extracts the message to be deliverd as clientMsg */
                          Pattern matcherPattern = Pattern.compile("\"([^']*)\"");
                          Matcher matcher = matcherPattern.matcher(msg);
                          String clientMsg = "";
                          while(matcher.find()){
                            clientMsg = clientMsg + matcher.group();
                          }

                          /* stub ends */
                          if (clientMsg.equals("")) {
                            if (!tokens[2].equals("tcp") && !tokens[2].equals("udp")) {
                              System.out.println("Command synatically incorrect! Include tcp/udp and try again!");
                              break;
                            }
                            else {

                              File file = new File(tokens[1]); // get the file
                              if (!file.exists() && !file.isFile()) {
                                System.out.println("Client is saying File does not exist; try again!");
                                break;
                              }
                              // initialize variables required

                              long fileLength = file.length();
                              clientOut.writeLong(fileLength);
                              long read = 0;
                              long remaining = fileLength;
                              long totalRead = 0;
                              long percent = 0;
                              // now start sending the File
                              FileInputStream fis = new FileInputStream(tokens[1]);
                              byte[] buffer = new byte[(int)fileLength];
                              //byte[] buffer = file.getBytes();
                              //InetAddress host = InetAddress.getByName("localhost");
                              while ((read = fis.read(buffer)) > 0) {
                                if (tokens[2].equals("udp")) {
                                  DatagramPacket dp = new DatagramPacket(buffer, buffer.length, ip, 7000);
                                  udpSocket.send(dp);
                                }
                                else {
                                  clientOut.write(buffer, 0, (int)read);
                                }
                                totalRead += read;
                                remaining -= read;
                                percent = (totalRead*100)/fileLength;
                                System.out.print("Sending " + tokens[1] + " [" + genPercentString(percent) + "] " + percent + "%" + "\r");
                              } // while block

                              System.out.print("\n");
                              System.out.println("Sent file");
                              System.out.print(">> ");
                              fis.close();
                            } // inner else block
                          } // sending file
                          else {
                            //System.out.println("heyyyyyyyyyyyhere "+ clientMsg);
                          }
                        } // if block
                        //if (print >= 0) printScreen(name);
                        System.out.print(">> ");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // readMessage thread
        Thread readMessage = new Thread(new Runnable()
        {
            @Override
            public void run() {

                while (true) {
                    try {
                        // read the message sent to this client
                        String msg = serverIn.readUTF();
                        System.out.println(msg);
                        System.out.print(">> ");
                        String[] tokens = msg.split(" ");
                        if (tokens[0].equals("reply")){
                          /* the following stub extracts the message to be deliverd as clientMsg */
                          Pattern matcherPattern = Pattern.compile("\"([^']*)\"");
                          Matcher matcher = matcherPattern.matcher(msg);
                          String clientMsg = "";
                          while(matcher.find()){
                            clientMsg = clientMsg + matcher.group();
                          }

                          /* stub ends */
                          if (clientMsg.equals("")) {
                              // initialize variables required
                              long fileLength = serverIn.readLong();
                              long read = 0;
                              long remaining = fileLength;
                              long totalRead = 0;
                              long percent = 0;
                              // now start sending the File
                              FileOutputStream fos = new FileOutputStream(tokens[1]);
                              byte[] buffer = new byte[1024];
                              byte[] contents = new byte[1024];
                              DatagramPacket dp = new DatagramPacket(contents, contents.length);
                              while(remaining > 0) {
                                if (tokens[2].equals("udp")) {
                                  udpSocket.receive(dp);
                                  buffer = dp.getData();
                                  read = dp.getLength();
                                }
                                else {
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
                              fos.close();
                          } // sending file
                        } // if block
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        });

        sendMessage.start();
        readMessage.start();

    }
}
