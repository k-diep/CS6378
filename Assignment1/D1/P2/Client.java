import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class Client {
    private Socket socket;
    private BufferedReader buffered_reader;
    private BufferedWriter buffered_writer;
    private String process_id;
    private FileInputStream file_read;
    private String file_name;
    private byte[] ByteBuffer;
    private String str_buffer;

    // Client Constructor
    public Client(Socket socket){
        try{
            this.socket = socket;
            this.buffered_writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.buffered_reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // The folder name is the ProcessID 
            this.process_id = Paths.get("").toAbsolutePath().getFileName().toString();
            // This Byte Buffer holds 100 bytes
            this.ByteBuffer = new byte[100];
            this.str_buffer = "";
        }
        catch (IOException e) {
            closeAll(socket, buffered_reader, buffered_writer);
        }
    }

    // Send to the message to the server.
    public void sendMessage(){
        try {
            // First we send the process id so the server can know which client is which.
            buffered_writer.write(process_id);
            buffered_writer.newLine();
            buffered_writer.flush();
            
            // Read the file from the same folder
            // The name of the file is the same as process ID 
            file_name = process_id + ".txt";
            //file_name = "F1.txt"; // TEMP, for testing 
            
            // This is the byte stream of the file.
            file_read = new FileInputStream(file_name);
            // Runs until ByteBuffer is empty (hits the end of the file)
            while((file_read.read(ByteBuffer)) != -1) {
                // Turns ByteBuffer into a String and send that string to the server
                String msg_to_send = new String(ByteBuffer, StandardCharsets.UTF_8);
                //System.out.println(msg_to_send);
                buffered_writer.write(msg_to_send);
                buffered_writer.newLine();
                buffered_writer.flush();
            }

            // Tell the server it is done sending messages
            // This is not the best way to do it since the file could technically 
            // have this message but simple to implement. And very unlikely. Could make this > 100bytes
            buffered_writer.write("DONE_FLAG");
            buffered_writer.newLine();
            buffered_writer.flush();
        }
        catch (IOException e){
            closeAll(socket, buffered_reader, buffered_writer);
        }
    }

    // Listen for messages from the server/clienthandler
    public void listenMessage(){          
        String receive_message;
        while (socket.isConnected()){
            try {
                receive_message = buffered_reader.readLine();

                // If the server is done sending messages, we then start writing to the server
                if(receive_message.equals("DONE_FLAG")){
                    FileWriter file_writer = new FileWriter(file_name);
                    System.out.println(str_buffer);
                    file_writer.write(str_buffer);
                    file_writer.close();
                    file_read = new FileInputStream(file_name);

                    buffered_writer.write("DONE_FLAG: TELL THE SERVER TO SHUT DOWN WHEN RECIEVED");
                    buffered_writer.newLine();
                    buffered_writer.flush();

                    closeAll(socket, buffered_reader, buffered_writer);
                    break;
                }
                else{
                    System.out.println("Start: " + receive_message);
                    str_buffer = str_buffer + receive_message;
                }
                //closeAll(socket, buffered_reader, buffered_writer);

            }
            catch (IOException e){
                closeAll(socket, buffered_reader, buffered_writer);
            }
        }
    }

    public void closeAll(Socket socket, BufferedReader buff_read, BufferedWriter buff_write) {
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

    public static void main(String[] args) throws IOException{
        Socket socket = new Socket("10.176.69.32", 1234);
        //Socket socket = new Socket("localhost", 1234);
        Client client = new Client(socket);
        client.sendMessage();
        client.listenMessage();
        
    }
        
    
}
