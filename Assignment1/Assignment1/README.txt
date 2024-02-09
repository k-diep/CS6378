Note:
- P3 is ran on dc01.utdallas.edu (This is our server)
- P2 is ran on dc02.utdallas.edu (Client #2)
- P1 is ran on dc03.utdallas.edu (Client #1)

Assumptions:
- Server is ran before both clients.
- P1 is executed before P2. (Processed concurrently)
- P1.txt and P2.txt are 300 bytes each
- P3.txt is empty

Execution:
For each process, go to the respective directo

- P1
cp P1_Copy.txt P1.txt (Optional)
y (Optional)
javac *.java
java Client

- P2
cp P2_Copy.txt P2.txt (Optional)
y (Optional)
javac *.java
java Client

- P3
javac *.java
java Server