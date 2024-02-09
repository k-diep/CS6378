import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// ClientHandler class is part of the server.
public class ClientHandler implements Runnable{
    
    public static ArrayList<ClientHandler> client_handlers = new ArrayList<>();
    public static int client_shutdown;
    private Socket server_socket;
    private BufferedReader buffered_reader;
    private BufferedWriter buffered_writer;
    private String process_id;
    private StringWriter string_writer;
    private Boolean client_done;
    private FileInputStream file_read;
    private byte[] ByteBuffer;


    //Constructor
    public ClientHandler(Socket socket){ 
        try{
            this.server_socket = socket;
            this.buffered_writer = new BufferedWriter(new OutputStreamWriter(server_socket.getOutputStream()));
            this.buffered_reader = new BufferedReader(new InputStreamReader(server_socket.getInputStream()));
            // The processID is the same as the folder name. This is sent from the client
            this.process_id = buffered_reader.readLine();
            //System.out.println(process_id); 
            client_handlers.add(this);
            this.string_writer = new StringWriter();
            this.client_done = false;
            this.ByteBuffer = new byte[200];
            client_shutdown = 0;
        }
        catch (IOException e){
            closeAll(server_socket, buffered_reader, buffered_writer);
        }
    }

    // Getter methods for ClientHandler
    public Boolean getClientDone(){
        return client_done;
    }

    public StringWriter getStringWriter(){
        return string_writer;
    }

    // Each ClientHandler runs on a separate thread
    @Override
    public void run(){
        String messageFromClient;
        while (server_socket.isConnected()) {
            try {
                messageFromClient = buffered_reader.readLine();

                // Case when the client is done sending messages
                if (messageFromClient.equals("DONE_FLAG")){
                    this.client_done = true;
                    //System.out.println(messageFromClient);
                }
                // Case when the client is done receiving messages from the server 
                else if (messageFromClient.equals("DONE_FLAG: TELL THE SERVER TO SHUT DOWN WHEN RECIEVED")){
                    
                    client_shutdown += 1;
                    removeClientHandler();
                    System.out.println("Client_shutdown: " + client_shutdown);

                    // If both clients are done - Shut down server process
                    if (client_shutdown == 2){
                        System.out.println("HERE");
                        System.exit(0);
                    }
                
                    
                }
                else { 
                    string_writer.write(messageFromClient);
                    System.out.println(messageFromClient); 
                }

                // This entire if block checks if both clients are done sending messages and then write that message
                // to a file called "P3.txt" (Note: P3 is hard code-y)

                //Check if both clients are in client_handlers
                if(client_handlers.size() == 2){
                    // Check if both clients are done sending messages
                    if(client_handlers.get(0).getClientDone() && client_handlers.get(1).getClientDone()){
                        // We assume process 1 is before process 2
                        // Now write to the file 
                        String fileWrite;
                        fileWrite = client_handlers.get(0).getStringWriter().toString() + client_handlers.get(1).getStringWriter().toString();
                        FileWriter file_writer = new FileWriter("P3.txt");
                        file_writer.write(fileWrite);
                        file_writer.close();
                        file_read = new FileInputStream("P3.txt");

                        // Now we send the contents of "P3.txt" to all (in this case both) clients
                        while((file_read.read(ByteBuffer)) != -1) {
                            String msg_to_send = new String(ByteBuffer, StandardCharsets.UTF_8);
                            broadcastMessage(msg_to_send);
                        }
                        broadcastMessage("DONE_FLAG");
                        //closeAll(server_socket, buffered_reader, buffered_writer);
                    }
                }
            } 
            catch (IOException e){
                closeAll(server_socket, buffered_reader, buffered_writer);
                break;
            }
        }
    }

    // Server sends messages to all clients (broadcast)
    public void broadcastMessage(String messageToSend){
        for(ClientHandler clientHandler : client_handlers){
            try{
                clientHandler.buffered_writer.write(messageToSend);
                clientHandler.buffered_writer.newLine();
                clientHandler.buffered_writer.flush();
            } 
            catch (IOException e){
                closeAll(server_socket, buffered_reader, buffered_writer);
            }
        }
    }

    //remove from the ArrayList
    public void removeClientHandler() {
        client_handlers.remove(this);
    }

    //This method closes everything
    public void closeAll(Socket socket, BufferedReader buff_read, BufferedWriter buff_write) {
        removeClientHandler();
        System.out.println("Client " + process_id + "Closing");;
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