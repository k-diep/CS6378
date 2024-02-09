import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket server_socket;

    //Constructor
    public Server(ServerSocket server_socket) {
        this.server_socket = server_socket;
    }
    
    //This metho
    public void runServer() {
        try {
            while (!server_socket.isClosed()){
                Socket socket = server_socket.accept();
                System.out.println("New Client connected" + socket.getInetAddress());
                ClientHandler client_handler = new ClientHandler(socket);

                Thread thread = new Thread(client_handler);
                thread.start();

            }
        }
        catch (IOException e){
            close_server_socket();
        }
    }

    public void close_server_socket(){
        try {
            if (server_socket != null) {
                server_socket.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws IOException{
        final int port_number = 1234;
        ServerSocket serverSocket = new ServerSocket(port_number);
        Server server = new Server(serverSocket);
        server.runServer();
        server.close_server_socket();
    }
    
}
