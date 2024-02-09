import java.time.Instant;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

public class NodeTimestamp implements Comparable<NodeTimestamp>{
    private Instant ts;
    private int value;

    //Constructor
    public NodeTimestamp(Instant ts, int value) {
        this.ts = ts;
        this.value = value;
    }
    
    //Getters
    public Instant getTimestamp(){
        return ts;
    }

    public int getValue(){
        return value;
    }

    //Setters (dont need)

    @Override
    public String toString(){
        return "Timestamp: " + ts + " -- Value: " + value;
    }

    // Override the comparison based on timestamp(ts) not int(value)
    @Override
    public int compareTo(NodeTimestamp otherNode) {
        return this.getTimestamp().compareTo(otherNode.getTimestamp());
        //throw new UnsupportedOperationException("Unimplemented method 'compareTo'");
    }
    
    public static void main(String[]args){
        //Testing Ignore
        PriorityQueue<NodeTimestamp> pq = new PriorityQueue<>();
        Instant instant1 = Instant.now();

        // wait 300ms
        try{
            TimeUnit.MILLISECONDS.sleep(300);
        }
        catch (InterruptedException e) {
            System.out.println("Interrupted while sleeping");
        }

        Instant instant2 = Instant.now();
        NodeTimestamp node1 = new NodeTimestamp(instant1, 50);
        NodeTimestamp node2 = new NodeTimestamp(instant2, 1);
        pq.add(node2);
        pq.add(node1);
        System.out.println(pq);
    }
}
