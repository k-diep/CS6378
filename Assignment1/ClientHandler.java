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
            //System.out.println(process_id); // DELETE
            client_handlers.add(this);
            this.string_writer = new StringWriter();
            this.client_done = false;
            this.ByteBuffer = new byte[200];
        }
        catch (IOException e){
            closeAll(server_socket, buffered_reader, buffered_writer);
        }
    }

    // Getter method for client_done
    public Boolean getClientDone(){
        return client_done;
    }

    public StringWriter getStringWriter(){
        return string_writer;
    }

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
                else { 
                    string_writer.write(messageFromClient);
                    System.out.println(string_writer.toString()); //DELETE
                }

                // This entire if block checks if both clients are done sending messages and then write that message
                // to a file called "P3.txt"

                //Check if both clients are in client_handlers
                if(client_handlers.size() == 2){
                    System.out.println(client_handlers.get(0).getClientDone());
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
                    }
                }
            } catch (IOException e){
                closeAll(server_socket, buffered_reader, buffered_writer);
                break;
            }
        }
    }

    // Server sends messages to all clients (broad)
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

    //This method closes 
    public void closeAll(Socket socket, BufferedReader buff_read, BufferedWriter buff_write) {
        removeClientHandler();
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