/*
  Java implementation of the ChatRoom server.
  args[0] : number of clients per ChatRoom
  args[1] : IP address of the server
  args[2] : Server port
*/
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.regex.*;

// Chat room Server class
public class ChatRoomServer
{
    // Vector to store active clients
    public static Vector<ClientHandlerToReceive> ar1 = new Vector<>();
    public static Vector<ClientHandlerToSend> ar2 = new Vector<>();
    // mapping for a client to a chat room
    public static HashMap <String,String> ChatRoomIndex = new HashMap<String, String>();
    public static HashMap <String,String> userToChat = new HashMap<String, String>();

    // global counter for clients
    public static int i = 0;
    // initialize UDP socket
    public static DatagramSocket udpSocket = null;
    // initialize the number of users per ChatRoom, the IP address and port
    public static int N = 0;
    public static InetAddress ip = null;
    public static int hostPort = 0;

    // helper functions
    // method to select command to execute
    public static int ChooseCommand(String var) {
      if (var.toLowerCase().equals("create")) {
        return 0;
      }
      else if (var.toLowerCase().equals("list")) {
        return 1;
      }
      else if (var.toLowerCase().equals("join")) {
        return 2;
      }
      else if (var.toLowerCase().equals("leave")) {
        return 3;
      }
      else if (var.toLowerCase().equals("add")) {
        return 4;
      }
      else if (var.toLowerCase().equals("reply")) {
        return 5;
      }
      else {
        return -1;
      }
    }

    public static void main(String[] args) throws IOException
    {
        // getting the values from the command line arguments
        N = (int) Integer.parseInt(args[0]); // setting the value of N
        ip = InetAddress.getByName(args[1]); // ip address of the server
        hostPort = (int) Integer.parseInt(args[2]); // server port

        // server is listening on port 1234
        ServerSocket ss = new ServerSocket(hostPort);
        udpSocket = new DatagramSocket(7000); // for UDP file transfer
        Socket s;

        String exit = "";

        // running infinite loop for getting
        // client request
        while (!exit.equals("bye"))
        {
            System.out.println("**  Welcome to the ChatRooms **");

            // Accept the incoming request
            s = ss.accept();

            System.out.println("New client request received : " + s);
            System.out.println("Server Busy...");
            // obtain input and output streams
            DataInputStream clientIn = new DataInputStream(s.getInputStream());
            DataOutputStream serverOut = new DataOutputStream(s.getOutputStream());

            serverOut.writeUTF("Getting the username...");
            String name = clientIn.readUTF();
            serverOut.writeUTF("Getting the Datagram Port...");
            int port = (int)Integer.parseInt(clientIn.readUTF());

            System.out.println("Creating a new handler for this client : "+name+"....");
            //serverOut.writeUTF("Creating a new handler for client : "+name+"....");

            // Create a new handler object for handling this request To receive
            ClientHandlerToReceive mtch1 = new ClientHandlerToReceive(s,name, clientIn, serverOut, port);

            // Create a new handler object for handling this request To sendMessage
            ClientHandlerToSend mtch2 = new ClientHandlerToSend(s,name, clientIn, serverOut, port);

            // Create a new Thread with this object.
            Thread t1 = new Thread(mtch1);
            Thread t2 = new Thread(mtch2);

            System.out.println("Adding this client to active client list");

            // add this client to active clients list
            ar1.add(mtch1); ar2.add(mtch2);

            // start the thread.
            t1.start(); t2.start();

            // increment i for new client.
            // i is used for debugging only, and not really required
            i++;

        }
    }
}

// ClientHandler class to receive data from clients
class ClientHandlerToReceive implements Runnable
{
    BufferedReader scn = new BufferedReader(new InputStreamReader(System.in));
    private String name;
    final DataInputStream clientIn;
    final DataOutputStream serverOut;
    Socket s;
    boolean isloggedin;
    int port;

    // constructor
    public ClientHandlerToReceive(Socket s, String name,
                            DataInputStream clientIn, DataOutputStream serverOut, int port) {
        this.clientIn = clientIn;
        this.serverOut = serverOut;
        this.name = name;
        this.s = s;
        this.isloggedin=true;
        this.port = port;
    }

    // helper functions
    private String Remove(String a, String item){
      String[] arr = a.split(",");
      List<String> list = new ArrayList<String>(Arrays.asList(arr));
      list.remove(item);
      arr = list.toArray(new String[0]);
      String arrNew = "";
      for (String str : arr) {
        arrNew = str + "," + arrNew;
      }
      return arrNew;
    } // remove block

    public static String genPercentString (long percent) {
        String str = "";
        int i = 10;
        for (; i <= percent; i += 10) str += "=";
        str += ">";
        for (; i <= 100; i += 10) str += " ";
        return str;
    }

    // function to broadcast file to every user in a ChatRoom
    public void sendToUsers(ClientHandlerToReceive mc, String fileName, String method) {
      try {
        File file = new File(fileName); // getting the file
        for (ClientHandlerToReceive mc_all : ChatRoomServer.ar1 ) {
          if (ChatRoomServer.userToChat.get(mc_all.name).equals(ChatRoomServer.userToChat.get(mc.name)) && mc != mc_all) {
            long fileLength = file.length();
            mc_all.serverOut.writeUTF("reply "+fileName+" "+method);
            mc_all.serverOut.writeLong(file.length());

            long read = 0;
            long remaining = fileLength;
            long totalRead = 0;
            long percent = 0;

            FileInputStream fis = new FileInputStream(fileName);
            byte[] buffer = new byte[(int)fileLength];

            InetAddress host = InetAddress.getByName("localhost");

            while ((read = fis.read(buffer)) > 0) {
              if (method.equals("udp")) {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length, host, mc_all.port);
                ChatRoomServer.udpSocket.send(dp);
              }
              else mc_all.serverOut.write(buffer, 0, (int)read);
              totalRead += read;
              remaining -= read;
              percent = (totalRead*100)/fileLength;
              System.out.print("Sending " + fileName + " [" + genPercentString(percent) + "] " + percent + "%" + "\r");
              }

              System.out.print("\n");
              System.out.println("Sent file to Client: "+mc_all.name);
              fis.close();
          } // if block
        } // for block
      } // try block
      catch (IOException e){
        e.printStackTrace();
      }
    } // sendToUsers block

    @Override
    public void run() {
        String received;
        while (true)
        {
            try
            {
                // receive the string
                received = clientIn.readUTF();

                System.out.println(this.name + " says: " + received);

                if(received.equals("exit")){
                    this.isloggedin=false;
                    this.s.close();
                    this.clientIn.close();
                    this.serverOut.close();
                    break;
                }

                // break the string into message and recipient participating
                // search for the recipient in the connected devices list.
                // ar1 is the vector storing client of active users
                for (ClientHandlerToReceive mc : ChatRoomServer.ar1)
                {
                    // if the recipient is found, write on its
                    // output stream
                    if (mc.name.equals(this.name) && mc.isloggedin==true)
                    {
                        mc.serverOut.writeUTF("Processing: "+mc.name+" : "+received);
                        String[] tokens = received.split(" "); // parsing the command
                        int choose = ChatRoomServer.ChooseCommand(tokens[0]);   // variable to choose which command to execute
                        switch (choose) {
                          case 0: // define the create command
                            int f = 0; // flag for error handling
                            for (String key : ChatRoomServer.userToChat.keySet()) {
                              if (key.equals(this.name) && !ChatRoomServer.userToChat.get(key).equals("Removed") ) {
                                mc.serverOut.writeUTF("User already associated with a ChatRoom!");
                                f = 1;
                                break;
                              }
                            }
                            if ( f == 1 ) break;
                            String tempArray = mc.name;
                            ChatRoomServer.ChatRoomIndex.put(tokens[2],tempArray);
                            mc.serverOut.writeUTF("Congratulations client: "+mc.name+" has now created and joined ChatRoom: "+tokens[2]);
                            System.out.println("ChatRoom: "+tokens[2]+" has been added!.\n Total ChatRooms available : "+ChatRoomServer.ChatRoomIndex.size());
                            //ChatRoomServer.noChatRooms = ChatRoomServer.noChatRooms + 1; // increment the counter for number of Chatrooms
                            ChatRoomServer.userToChat.put(mc.name,tokens[2]);
                            break;
                          case 1: // defining the list ChatRooms and list users command command
                            if (tokens[1].equals("chatrooms")) {
                              String data = "";
                              for (String key : ChatRoomServer.ChatRoomIndex.keySet()) {
                                data = data + "\n" + key;
                              }
                              serverOut.writeUTF("The current available Chatrooms are: "+data);
                            }
                            else if (tokens[1].equals("users")) {
                              this.serverOut.writeUTF("The users associated with the ChatRoom: " + ChatRoomServer.userToChat.get(this.name) + "are : " + ChatRoomServer.ChatRoomIndex.get(ChatRoomServer.userToChat.get(this.name)));
                            }
                            else {
                              this.serverOut.writeUTF("Invalid Command!");
                            }
                            break;
                          case 2: // defining the join command
                            int count = 0; // local count for number of clients in the chatroom in consdiration
                            for (String key : ChatRoomServer.userToChat.keySet()) {
                              if (ChatRoomServer.userToChat.get(key).equals(tokens[1])) {
                                count = count + 1;
                              }
                            }
                            if (count == ChatRoomServer.N) {
                              serverOut.writeUTF("Limit Exceeded!");
                              break;
                            }
                            tempArray = ChatRoomServer.ChatRoomIndex.get(tokens[1]) + "," + mc.name;
                            ChatRoomServer.ChatRoomIndex.put(tokens[1],tempArray);
                            ChatRoomServer.userToChat.put(mc.name,tokens[1]);
                            // display the update list of participating clients to everyone in the chatroom
                            for (ClientHandlerToReceive mc_all : ChatRoomServer.ar1){
                              if (ChatRoomServer.userToChat.get(mc_all.name).equals(ChatRoomServer.userToChat.get(mc.name))){
                                mc_all.serverOut.writeUTF("Congratulations client: "+mc.name+" has now joined your ChatRoom: "+tokens[1]+"\nThe updated clients for Chatroom " + tokens[1] +" are: "+tempArray);
                              }
                            }
                            break;
                          case 3: // defining the leave command
                            String currChat = ChatRoomServer.userToChat.get(this.name); // getting clients ChatRoom
                            ChatRoomServer.userToChat.put(this.name,"Removed"); // removing the user associated with his chatroom
                            tempArray = Remove(ChatRoomServer.ChatRoomIndex.get(currChat),this.name); // deleting from the list of clients of that array
                            ChatRoomServer.ChatRoomIndex.put(currChat,tempArray); // update the number of clients in that ChatRoom
                            if (tempArray.equals("")) {
                              ChatRoomServer.ChatRoomIndex.remove(currChat);
                              this.serverOut.writeUTF("ChatRoom deleted!");
                              break;
                            }
                            // display the update list of participating clients to everyone in the chatroom
                            for (ClientHandlerToReceive mc_all : ChatRoomServer.ar1){
                              if (ChatRoomServer.userToChat.get(mc_all.name).equals(currChat)){
                                mc_all.serverOut.writeUTF("Client: "+mc.name+" has now left your ChatRoom: "+currChat+"\nThe updated clients for Chatroom " + currChat +" are: "+tempArray);
                              }
                            }
                            break;
                          case 4: // defining the join command
                            int flag = 0; // to test for user's existence
                            for (ClientHandlerToReceive mc_all : ChatRoomServer.ar1) {
                              if (mc_all.name.equals(tokens[1])) {
                                flag = 1;
                              }
                            }
                            if (flag == 0) {
                              mc.serverOut.writeUTF("User does not exit!");
                              break;
                            }
                            for (String key : ChatRoomServer.userToChat.keySet()) {
                              if (ChatRoomServer.userToChat.get(key).equals(ChatRoomServer.userToChat.get(this.name)) && key.equals(tokens[1])) {
                                mc.serverOut.writeUTF("User already associated with this ChatRoom!");
                                flag = 2;
                                break;
                              }
                              if (!ChatRoomServer.userToChat.get(key).equals("Removed") && key.equals(tokens[1])) {
                                mc.serverOut.writeUTF("User is associated with another chatroom!");
                                flag = 2;
                                break;
                              }

                            }
                            if (flag == 2) break;
                            ChatRoomServer.userToChat.put(tokens[1],ChatRoomServer.userToChat.get(this.name));
                            tempArray = ChatRoomServer.ChatRoomIndex.get(ChatRoomServer.userToChat.get(this.name));
                            tempArray = tempArray + "," + tokens[1];
                            // display the update list of participating clients to everyone in the chatroom
                            for (ClientHandlerToReceive mc_all : ChatRoomServer.ar1){
                              if (ChatRoomServer.userToChat.get(mc_all.name).equals(ChatRoomServer.userToChat.get(this.name))){
                                System.out.println(mc_all.name);
                                mc_all.serverOut.writeUTF("Client: "+tokens[1]+" has been added to your ChatRoom: "+ChatRoomServer.userToChat.get(this.name)+"\nThe updated clients for Chatroom " + ChatRoomServer.userToChat.get(this.name) +" are: "+tempArray);
                              }
                            }
                            break;
                          case 5: // defining the reply command
                            /* the following stub extracts the message to be deliverd as clientMsg */
                            Pattern matcherPattern = Pattern.compile("\"([^']*)\"");
                            Matcher matcher = matcherPattern.matcher(received);
                            String clientMsg = "";
                            while(matcher.find()){
                              clientMsg = clientMsg + matcher.group();
                            }
                            /* stub ends */
                            if (clientMsg.equals("")) { // this indicates the client is sending a file
                              if (!tokens[2].equals("tcp") && !tokens[2].equals("udp")) {
                                //mc.serverOut.writeUTF("Command synatically incorrect! Include tcp/udp and try again!");
                                //break;
                              }
                              else {
                                // initialize variables required
                                long fileLength = mc.clientIn.readLong();
                                long read = 0;
                                long totalRead = 0;
                                long remaining = fileLength;
                                long percent = 0;

                                FileOutputStream fos = new FileOutputStream(tokens[1]);
                                byte[] buffer = new byte[1024];
                                byte[] contents = new byte[1024];
                                byte[] emptyArray = new byte[0];

                                DatagramPacket dp = new DatagramPacket(contents, contents.length);
                                while(remaining > 0) {

                                if (tokens[2].equals("udp")) {
                                  ChatRoomServer.udpSocket.receive(dp);
                                  buffer = new byte[dp.getLength()];
                                  buffer = dp.getData();
                                  read = dp.getLength();
                                }
                                else {
                                  read = mc.clientIn.read(buffer);
                                  read = Math.min(buffer.length, remaining);
                                }

                                if (read <= 0 || dp.getLength() <= 0) break;
                                if (read > remaining) System.out.println("hahhah");
                                totalRead += read;
                                remaining -= read;
                                percent = (totalRead*100)/fileLength;
                                System.out.print("Receiving " + tokens[1] + " [" + genPercentString(percent) + "] " + percent + "%" + "\r");
                                fos.write(buffer, 0, (int)read);
                              }
                              System.out.print('\n');
                              System.out.println("Received File");
                              System.out.println("Broadcasting it to ALL users in the ChatRoom : "+ChatRoomServer.userToChat.get(mc.name));

                              fos.close();
                              } // inner else block
                              // now the server has received the file it will send it to
                              // everyone associated with this users ChatRoom
                              // but NOT the user itself
                              sendToUsers(mc,tokens[1],tokens[2]);
                            } // bigger if block
                            else { // otherwise just broadcast the message to the ChatRoom
                              currChat = ChatRoomServer.userToChat.get(this.name); // getting clients ChatRoom
                              // broadcasting the message to everyone in the chatroom
                              for (ClientHandlerToReceive mc_all : ChatRoomServer.ar1){
                                if (ChatRoomServer.userToChat.get(mc_all.name).equals(currChat) && mc != mc_all){
                                  System.out.println(mc_all.name);
                                  mc_all.serverOut.writeUTF("Client: "+mc.name+" "+clientMsg);
                                }
                              }
                            }
                            break;
                          default :
                            mc.serverOut.writeUTF("Nothing to Process");
                            System.out.println("Succesfully executed!");
                            break;
                    }
                        break;
                    }
                }
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
        try
        {
            // closing resources
            this.clientIn.close();
            this.serverOut.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}


// ClientHandler class to send data to clients
class ClientHandlerToSend implements Runnable
{
    Scanner scn = new Scanner(System.in);
    private String name;
    final DataInputStream clientIn;
    final DataOutputStream serverOut;
    Socket s;
    boolean isloggedin;
    int port;

    // constructor
    public ClientHandlerToSend(Socket s, String name,
                            DataInputStream clientIn, DataOutputStream serverOut, int port) {
        this.clientIn = clientIn;
        this.serverOut = serverOut;
        this.name = name;
        this.s = s;
        this.isloggedin=true;
        this.port = port;
    }

    @Override
    public void run() {

        String received;
        String exit = "";
        while (!exit.equals("bye"))
        {
            try
            {
                String sendData = scn.nextLine();
                for (ClientHandlerToSend mc_all : ChatRoomServer.ar2) {
                  mc_all.serverOut.writeUTF(sendData);
                }
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
        try
        {
            // closing resources
            this.clientIn.close();
            this.serverOut.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
