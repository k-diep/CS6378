import java.util.ArrayList;

public class Quorum {
    
    public static ArrayList<ArrayList<Integer>> FindQuorum(int root){ 
        ArrayList<ArrayList<Integer>> returnQuorum = new ArrayList<ArrayList<Integer>>();
        int leftTree = 2*root;
        int rightTree = 2*root+1;

        // Base Case (Also Case 4)
        if((leftTree > 7) || (rightTree > 7)){
            ArrayList<Integer> newQuorum = new ArrayList<Integer>();
            newQuorum.add(root);
            returnQuorum.add(newQuorum);
            return returnQuorum; 
        }

        ArrayList<ArrayList<Integer>> leftQuorums = new ArrayList<ArrayList<Integer>>();
        ArrayList<ArrayList<Integer>> rightQuorums = new ArrayList<ArrayList<Integer>>();
        leftQuorums  = FindQuorum(leftTree);
        rightQuorums = FindQuorum(rightTree);

        // Case 3
        for(ArrayList<Integer> i : leftQuorums){
            for(ArrayList<Integer> j : rightQuorums){
                ArrayList<Integer> newQuorum = new ArrayList<Integer>();
                newQuorum.addAll(i);
                newQuorum.addAll(j);
                returnQuorum.add(newQuorum);
            }
        }

        // Case 1
        for(ArrayList<Integer> i : leftQuorums){
            ArrayList<Integer> newQuorum = new ArrayList<Integer>();
            newQuorum.add(root);
            newQuorum.addAll(i);
            returnQuorum.add(newQuorum);
        }

        // Case 2
        for(ArrayList<Integer> j : rightQuorums){
            ArrayList<Integer> newQuorum = new ArrayList<Integer>();
            newQuorum.add(root);
            newQuorum.addAll(j);
            returnQuorum.add(newQuorum);
        }
        
        return returnQuorum;
    }

    public static void PrintAllQuorum(ArrayList<ArrayList<Integer>> quorum){
        int count = 0;
        for (ArrayList<Integer> l : quorum){
            for (int i : l){
                System.out.print(i);
            }
            System.out.println();
            count++;

        }
        System.out.println("Count: " + count);
    }    

    public static void main(String[]args){
        //Testing
        int root = 1;
        
        ArrayList<ArrayList<Integer>> returnQuorum = new ArrayList<ArrayList<Integer>>();
        returnQuorum = FindQuorum(root);

        Quorum.PrintAllQuorum(returnQuorum);
        
        ArrayList<Integer> findQuorum = new ArrayList<Integer>();
        
        findQuorum.add(2);
        findQuorum.add(1);
        findQuorum.add(5);
        findQuorum.add(4);
        findQuorum.add(6);
        findQuorum.add(7);
        findQuorum.add(3);

        for(ArrayList<Integer> Quorum : returnQuorum){
            if (findQuorum.containsAll(Quorum)){
                System.out.println(Quorum);
            }
        }
        
        


    }
}