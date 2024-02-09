import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.time.Instant; 

public class Client {
    private ArrayList<Socket> socketList;
    private ArrayList<BufferedReader> buffered_reader = new ArrayList<BufferedReader>();
    private ArrayList<BufferedWriter> buffered_writer = new ArrayList<BufferedWriter>();
    private Socket shutdownSocket;
    private BufferedReader buffered_reader_shutdown;
    private BufferedWriter buffered_writer_shutdown;
    private String process_id;
    private long timeUnit;
    private int[] waitBeforeEnter;
    private ArrayList<ArrayList<Integer>> quorumList = new ArrayList<ArrayList<Integer>>();
    private List<Integer> quorumCheck = Collections.synchronizedList(new ArrayList<Integer>());
    private static int num_sent;
    private static int num_received;
    private static int num_critical;

    // Client Constructor
    public Client(ArrayList<Socket> socketList, Socket shutdownSocket){
        try{
            this.socketList = socketList;
            for (Socket socket : socketList){
                buffered_writer.add(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                buffered_reader.add(new BufferedReader(new InputStreamReader(socket.getInputStream())));
            }
            this.shutdownSocket = shutdownSocket;
            this.buffered_writer_shutdown = new BufferedWriter(new OutputStreamWriter(shutdownSocket.getOutputStream()));
            this.buffered_reader_shutdown = new BufferedReader(new InputStreamReader(shutdownSocket.getInputStream()));


            this.process_id = Paths.get("").toAbsolutePath().getFileName().toString().substring(1);
            this.timeUnit = 100; // in ms
            this.waitBeforeEnter = new int[]{5, 6, 7, 8, 9, 10};
            int root = 1;
            this.quorumList = Quorum.FindQuorum(root);
        }
        catch (IOException e) {
            closeAll(socketList, buffered_reader, buffered_writer);
        }
    }

    public int getNumSent (){
        return num_sent;
    }

    public int getNumReceive (){
        return num_received;
    }


    // When entering the critical section
    public void enterCriticalSection(){
        System.out.println("Entering " + process_id);
        Instant instant = Instant.now();
        System.out.println(instant);
        // Wait for 3 timeUnits
        try{
            TimeUnit.MILLISECONDS.sleep(3*timeUnit);
        }
        catch (InterruptedException e) {
            System.out.println("Interrupted while sleeping");
        }
        // Send Release message to all servers
        sendReleaseMessage();
        //Reset this counter
        System.out.println("Number of messages needed to get into critical section:" + num_critical);
        num_critical = 0;
        
    }

    // Send to the ClientID to all servers
    public void sendClientID(){
        try {
            for (BufferedWriter buff_write : buffered_writer){
                num_sent += 1;
                buff_write.write(process_id);
                buff_write.newLine();
                buff_write.flush();
            }
            // Send to shutdown server too
            buffered_writer_shutdown.write(process_id);
            buffered_writer_shutdown.newLine();
            buffered_writer_shutdown.flush();
        }
        catch (IOException e){
            closeAll(socketList, buffered_reader, buffered_writer);
        }
    }

    //Send RELEASE message to all server nodes
    public void sendReleaseMessage(){
        try {
            for (BufferedWriter buff_write : buffered_writer){
                num_sent += 1;
                num_critical += 1;
                //System.out.println("Release");
                buff_write.write(process_id + " : RELEASE");
                buff_write.newLine();
                buff_write.flush();
            }
        }
        catch (IOException e){
            closeAll(socketList, buffered_reader, buffered_writer);
        }
    }

    //Send "REQUEST" message to all server nodes
    public void sendRequestMessage(){
        try {
            // Wait 5 to 10 * waitTime before requesting
            // This gets the a random index from waitBeforeEnter
            System.out.println("Requesting");
            //System.out.println("Current quorumcheck while sending: " + quorumCheck);
            int randomWaitTimeIndex = new Random().nextInt(waitBeforeEnter.length);
            int randomWaitTime = waitBeforeEnter[randomWaitTimeIndex];
            try{
                TimeUnit.MILLISECONDS.sleep(randomWaitTime*timeUnit);
            }
            catch (InterruptedException e) {
                System.out.println("Interrupted while sleeping");
            }


            // Send Request to each Server Node.
            Instant ts = Instant.now();
            for (BufferedWriter buff_write : buffered_writer){
                num_sent += 1;
                num_critical += 1;
                buff_write.write(process_id + " : REQUEST : " + ts);
                buff_write.newLine();
                buff_write.flush();
            }
        }
        catch (IOException e){
            closeAll(socketList, buffered_reader, buffered_writer);
        }
    }

    // Send Shutdown message to S0 (shutdown server)
    public void sendShutdown(){
        try {
            System.out.println("Shutdown");
            buffered_writer_shutdown.write(process_id + " : SHUTDOWN");
            buffered_writer_shutdown.newLine();
            buffered_writer_shutdown.flush();
        }
        catch (IOException e){
            closeAll(socketList, buffered_reader, buffered_writer);
        }
    }

    // Send Shutdown message to ALL servers (beside S0)
    public void sendShutdownAll(){
        try {
            for (BufferedWriter buff_write : buffered_writer){
                buff_write.write(process_id + " : SHUTDOWN");
                buff_write.newLine();
                buff_write.flush();
            }
        }
        catch (IOException e){
            closeAll(socketList, buffered_reader, buffered_writer);
        }
    }

    public void waitForGrantMessage(){
        System.out.println("waiting for grant");

        loop:
        while(true){
            //System.out.println("Current quorumcheck while granted: " + quorumCheck);
            for(ArrayList<Integer> Quorum : quorumList){
                if (quorumCheck.containsAll(Quorum)){
                    //System.out.println("Current quorumcheck while granted: " + quorumCheck);
                    //System.out.println("Current quorum while granted: " + Quorum);
                    // Enter the critical section
                    enterCriticalSection();
                    quorumCheck.removeAll(quorumCheck);
                    //System.out.println("After delete quorum: " + quorumCheck);
                    break loop;

                }
            }
        }
        System.out.println("Loop Broken");
    }

    // Listen for messages from each server/clienthandler
    // This runs on a different thread meaning this runs concurrently to the rest of the program
    public void listenMessage(){
        // For each server (except shutdown server)
        for(int i = 0; i < socketList.size() ; i++){
            final Integer innerI = i;
            new Thread(new Runnable(){
                @Override
                public void run(){
                    String receiveMessage;
                    num_received += 1;
                    while (socketList.get(innerI).isConnected()){ //
                        try {
                            receiveMessage = buffered_reader.get(innerI).readLine();
                            System.out.println("Receive Message: " + receiveMessage);
                            String[] receiveMessageSplit = receiveMessage.split(" : ");
                            String messageType = receiveMessageSplit[1];
                            Integer serverId = Integer.parseInt(receiveMessageSplit[0]);
                            
                            if (messageType.equals("GRANT")){
                                num_critical += 1;
                                num_received += 1;
                                // System.out.println(receiveMessage);
                                // Add serverId to check array
                                quorumCheck.add(serverId);
                                //System.out.println("Quorum Check Current: " + quorumCheck);
                                //System.out.println(serverId + " added to quorumCheck.");
                            }
                        }
                        catch (IOException e){
                            closeAll(socketList, buffered_reader, buffered_writer);
                    
                        }
                    }
                }   
            }).start();
        }
    }

    public void listenMessageFromS0(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                String receiveMessage;
                while (shutdownSocket.isConnected()){
                    try {
                        receiveMessage = buffered_reader_shutdown.readLine();
                        if (receiveMessage.equals("SHUTDOWN")){
                            System.out.print("EXITING");
                            //if (process_id == "1") {
                                sendShutdownAll();
                            //}
                            System.exit(0);
                        }
                    }

                    catch (IOException e){
                        closeAll(socketList, buffered_reader, buffered_writer);
                
                    }
                }
            
            }
        }).start();
    }


    // Close all sockets
    public void closeAll(ArrayList<Socket> socket, ArrayList<BufferedReader> buff_read, ArrayList<BufferedWriter> buff_write) {
        try{
            for(int i = 0; i <=1 ; i++){
                if (buff_read != null){
                    buff_read.get(i).close();
                }
                if (buff_write != null){
                    buff_write.get(i).close();
                }
                if (socket != null){
                    socket.get(i).close();
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException{
        //Socket socket = new Socket("10.176.69.32", 1234);
        //Socket socket = new Socket("localhost", 1234);
        //Client client = new Client(socket);
        //client.sendMessage();
        //client.listenMessage();
        Socket socket0 = new Socket("localhost", 1230);
        Socket socket1 = new Socket("localhost", 1231);
        Socket socket2 = new Socket("localhost", 1232);
        Socket socket3 = new Socket("localhost", 1233);
        Socket socket4 = new Socket("localhost", 1234);
        Socket socket5 = new Socket("localhost", 1235);
        Socket socket6 = new Socket("localhost", 1236);
        Socket socket7 = new Socket("localhost", 1237);

        ArrayList<Socket> socketList = new ArrayList<Socket>();
        socketList.add(socket1);
        socketList.add(socket2);
        socketList.add(socket3);
        socketList.add(socket4);
        socketList.add(socket5);
        socketList.add(socket6);
        socketList.add(socket7);
        
        Client client = new Client(socketList, socket0);
        client.sendClientID();
        client.listenMessage();
        client.listenMessageFromS0();
        
        for(int i = 0; i < 20 ; i++){
            //System.out.println(i);
            long start = System.currentTimeMillis();
            client.sendRequestMessage();
            client.waitForGrantMessage();
            long finish = System.currentTimeMillis();
            long timeElapsed = finish-start;
            System.out.println("Elasped Time (ms):" + timeElapsed);
        }

        System.out.println("Messages Sent:" + client.getNumSent());
        System.out.println("Messages Received:" + client.getNumReceive());

        client.sendShutdown();   
    }
}
