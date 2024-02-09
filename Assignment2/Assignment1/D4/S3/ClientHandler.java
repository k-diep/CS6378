import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.PriorityQueue;

// ClientHandler class is part of the server.
public class ClientHandler implements Runnable{
    
    public static ArrayList<ClientHandler> client_handlers = new ArrayList<>();
    private static int num_sent;
    private static int num_received;
    private Socket server_socket;
    private BufferedReader buffered_reader;
    private BufferedWriter buffered_writer;
    private int client_id;
    private String server_id;
    private Boolean client_done;
    private static Integer locked;
    private static PriorityQueue<NodeTimestamp> pq;
    private static ArrayList<NodeTimestamp> nodeList;

    //Constructor
    public ClientHandler(Socket socket){ 
        try{
            this.server_socket = socket;
            this.buffered_writer = new BufferedWriter(new OutputStreamWriter(server_socket.getOutputStream()));
            this.buffered_reader = new BufferedReader(new InputStreamReader(server_socket.getInputStream()));

            // The client_id is the same as the folder name. This is sent from the client.
            this.client_id = Integer.parseInt(buffered_reader.readLine());
            //System.out.println("Client ID: " + client_id);

            // The server_id is based to the folder name.
            this.server_id = Paths.get("").toAbsolutePath().getFileName().toString().substring(1);
            //System.out.println("Server ID: " + server_id);

            client_handlers.add(this);
            this.client_done = false;
            if (locked == null){
                locked = -1;
            }
            if (pq == null){
                pq = new PriorityQueue<>();
            }
            if (nodeList == null){
                nodeList = new ArrayList<NodeTimestamp>();
            }
        }
        catch (IOException e){
            closeAll(server_socket, buffered_reader, buffered_writer);
        }
    }

    // Getter methods for ClientHandler
    public Boolean getClientDone(){
        return client_done;
    }

    public int getClientId(){
        return client_id;
    }

    // Each ClientHandler runs on a separate thread
    @Override
    public void run(){
        String messageFromClient;
        while (server_socket.isConnected()) {
            try {
                messageFromClient = buffered_reader.readLine();
                
                
                //DELETE LATER
                //System.out.println(messageFromClient);
                String[] messageFromClientSplit = messageFromClient.split(" : ");
                Integer clientNumber = Integer.parseInt(messageFromClientSplit[0]);
                String messageType  = messageFromClientSplit[1];
                

                //Request Messages
                if (messageType.equals("REQUEST")){
                    num_received += 1;
                    // Timestamp of the request
                    Instant messageTimestamp = Instant.parse(messageFromClientSplit[2]);

                    // If server is not locked (-1 will always mean unlocked)
                    //System.out.println("Locked : " + locked);
                    if (locked == -1){
                        // Change the lock to represent the clientNumber
                        locked = clientNumber;
                        String messageToSend = server_id + " : GRANT" ;
                        broadcastMessage(messageToSend, clientNumber);
                    }

                    //If server is locked
                    else {
                        NodeTimestamp nt = new NodeTimestamp(messageTimestamp, clientNumber);
                        pq.add(nt);
                        nodeList.add(nt);

                        //DELETE LATER
                        //System.out.println(pq);
                    }
                }


                else if (messageType.equals("RELEASE")){
                    num_received += 1;
                    // DELETE LATER
                    //System.out.println(clientNumber);
                    //System.out.println(messageFromClient);

                    // If it is locked by current client
                    //System.out.println("LOCKED: " + locked);
                    //System.out.println("Priority Queue: " + pq);
                    if (locked == clientNumber) {
                        //System.out.println(pq);
                        // If priority queue is empty, server becomes unlocked
                        if (pq.isEmpty()){
                            //System.out.println("Empty pq");
                            locked = -1;
                        }
                        // When priority queue is not empty
                        else {
                            
                            NodeTimestamp headNode = pq.poll();
                            //System.out.println("Headnode: "+ headNode);
                            int clientToGrant = headNode.getValue();
                            locked = clientToGrant;
                            String messageToSend = server_id + " : GRANT";
                            //System.out.println("NonEmpty pq: " + messageToSend);
                            broadcastMessage(messageToSend, clientToGrant);
                        }
                        //System.out.println(messageFromClient);
                    }

                    // If locked by current client
                    else{
                        //System.out.println(messageType);
                        //System.out.println("PQ Before Removing: " + pq);
                        for (NodeTimestamp nt : nodeList){
                            if (nt.getValue() == clientNumber){
                                //System.out.println("REMOVING: " + clientNumber);
                                nodeList.remove(nt);
                                pq.remove(nt);
                                break;
                            }
                        }
                        //System.out.println("PQ After Removing: " + pq);

                    }
                
                }
            
                else if (messageType.equals("SHUTDOWN")){
                    System.out.println("Total messages sent:" + num_sent);
                    System.out.println("Total messages received:" + num_received);
                    System.exit(0);
                }
            } 
            catch (IOException e){
                closeAll(server_socket, buffered_reader, buffered_writer);
                break;
            }
        }
    }

    // Server sends messages to clientNumber (broadcast)
    public void broadcastMessage(String messageToSend, int clientToSend){
        for(ClientHandler clientHandler : client_handlers){
            if(clientHandler.getClientId() == clientToSend){
                try{
                    //System.out.println(messageToSend);
                    num_sent += 1;
                    clientHandler.buffered_writer.write(messageToSend);
                    clientHandler.buffered_writer.newLine();
                    clientHandler.buffered_writer.flush();
                } 
                catch (IOException e){
                    closeAll(server_socket, buffered_reader, buffered_writer);
                }
            }
        }
    }

    //remove from the ArrayList ClientHandler
    public void removeClientHandler() {
        client_handlers.remove(this);
    }

    //This method closes everything
    public void closeAll(Socket socket, BufferedReader buff_read, BufferedWriter buff_write) {
        removeClientHandler();
        //System.out.println("Client " + client_id + ": Closing");
        try{
            if (buff_read != null){
                buff_read.close();
            }
            if (buff_write != null){
                buff_write.close();
            }
            if (socket != null){
                socket.close();
            }
        
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}