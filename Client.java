import com.sun.org.apache.regexp.internal.RE;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */

public class Client implements Sender, Receiver
{
    private int portNumber;
    private String serverHostname;
    private InetAddress serverIP;
    private DatagramSocket sendSocket;
    private DatagramSocket rcvSocket;
    private boolean slowMode;
    private TCPServer tcpSrv;
    private TCPClient tcpCli;

    public Client(int portNumber, String receiverHostname, boolean slowMode) throws Exception
    {
        this.slowMode = slowMode;
        this.portNumber = portNumber;
        this.serverHostname = receiverHostname;
        this.serverIP = InetAddress.getByName(this.serverHostname);
        this.tcpSrv = new TCPServer("TCP Server", portNumber + 1);
        tcpSrv.start();
        this.tcpCli = null;
    }

    public void sendMessage(String msgType) throws Exception
    {
        this.sendSocket = new DatagramSocket();
        this.slowMode = slowMode;
        byte[] rcvData = new byte[1024];

        System.out.println("Attempting to send to " + serverIP);

        String msg = genReqMsg(msgType);
        String[] packets = Sender.makePacket(msg);

        double estRTT = Sender.INIT_EST_RTT;
        double devRTT = Sender.INIT_DEV_RTT;
        devRTT = Sender.calcDevRTT(0.0, Sender.INIT_EST_RTT, estRTT, devRTT);
        estRTT = Sender.calcEstimatedRTT(0.0, Sender.INIT_EST_RTT, estRTT);
        int timeoutInterval = Sender.calcTimeoutInterval(estRTT, devRTT);

        int currSeqNum = Sender.INIT_SEQ_NUM;
        int packetCount = 0;

        // Attempt to send pakets via UDP
        for(String p : packets)
        {
            // Sending packets
            byte[] sendData = new byte[p.length()];
            sendData = p.getBytes();
            DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, serverIP, portNumber);

            if(slowMode)
            {
                Thread.sleep(4000);
            }

            try{
                sendSocket.send(sendPkt);
            }
            catch (SocketException e){
                System.out.println("Connection interrupted. Waiting for resumed connection.");
            }

            System.out.println("Sending following packet " + (packetCount + 1) + " of " + packets.length + " with sequence number " + Sender.getSeqNum(p) + ", current time out: " + estRTT + "\n{\n" + p + "\n}\n");
            System.out.println("Waiting for ACK...");

            // Receiving ACKs
            DatagramPacket rcvPkt;
            while(true)
            {
                // Start Timer
                sendSocket.setSoTimeout(timeoutInterval);
                double startTime = System.nanoTime() / 1000000;
                rcvPkt = new DatagramPacket(rcvData, rcvData.length);

                // Try to receive a packet
                try{
                    sendSocket.receive(rcvPkt);
                }
                // If timeout
                catch(SocketTimeoutException e){
                    double endTime = System.nanoTime() / 1000000;
                    // Recalculate timeout interval
                    devRTT = Sender.calcDevRTT(startTime, endTime, estRTT, devRTT);
                    estRTT = Sender.calcEstimatedRTT(startTime, endTime, estRTT);
                    timeoutInterval = Sender.calcTimeoutInterval(estRTT, devRTT);
                    System.out.println("Timeout! Resending following packet " + (packetCount + 1) + " of " + packets.length + " with sequence number " + Sender.getSeqNum(p) + ", current time out: " + estRTT + "\n{\n" + p + "\n}\n");
                    // Resend packet
                    try{
                        sendSocket.send(sendPkt);
                    }
                    catch (SocketException se){
                        System.out.println("Connection interrupted. Waiting for resumed connection.");
                    }
                    // Restart loop
                    continue;
                }
                double endTime = System.nanoTime() / 1000000;
                //Recalculate timeout interval
                devRTT = Sender.calcDevRTT(startTime, endTime, estRTT, devRTT);
                estRTT = Sender.calcEstimatedRTT(startTime, endTime, estRTT);
                timeoutInterval = Sender.calcTimeoutInterval(estRTT, devRTT);
                // Check new sequence number
                int newSeqNum = Sender.getSeqNum(new String(rcvPkt.getData()));
                System.out.println("ACK received! Sequence number " + newSeqNum);
                // If incorrect sequence number
                if(newSeqNum != currSeqNum)
                {
                    System.out.println("Wrong sequence number.\nExpected " + currSeqNum + " got " + newSeqNum + ".");
                    // Restart loop
                    continue;
                }
                // Turn off timer
                sendSocket.setSoTimeout(0);
                // Increment sequence number
                currSeqNum = (currSeqNum + 1) % Sender.SEQ_NUM_WIN;
                // End loop
                break;
            }

            String ACK = new String(rcvPkt.getData());
            int sequenceNum = Sender.getSeqNum(ACK);
            int rcvpktsize = rcvPkt.getLength();
            InetAddress clientIP = rcvPkt.getAddress();
            int clientPort = rcvPkt.getPort();
            String ack = new String(rcvPkt.getData());

            System.out.println("ACK = \n[\n" + ack + "\n]");
            System.out.println("Sequence number = " + sequenceNum);
            System.out.println("Sender IP address: " + clientIP);
            System.out.println("Sender port: " + clientPort);
            System.out.println("Packet size: " + rcvpktsize + "\n\n\n");
            packetCount++;
        }
        this.receiveMessage();
        sendSocket.close();
    }

    public void receiveMessage() throws Exception
    {
        this.rcvSocket = new DatagramSocket(portNumber);
        ArrayList<String> incPackets = new ArrayList<String>();
        byte[] rcvData = new byte[100];
        DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
        DatagramPacket sendPkt = null;

        int currSeqNum = Receiver.INIT_SEQ_NUM;
        int lastRcvSeqNum = Receiver.INIT_SEQ_NUM + 1;
        String totalMsg = "";

        while(true)
        {
            System.out.println("Waiting for packet from server " + serverIP.getHostAddress());
            rcvSocket.receive(rcvPkt);
            int rcvpktsize = rcvPkt.getLength();
            int serverPort = rcvPkt.getPort();
            String message = new String(rcvPkt.getData());
            Scanner scan = new Scanner(message);
            int headerLength = scan.nextLine().length() + 2;
            int sequenceNum = Sender.getSeqNum(message);
            System.out.println("ACK received! Sequence number " + sequenceNum);

            if(slowMode)
            {
                Thread.sleep(4000);
            }

            if(currSeqNum != sequenceNum)
            {
                System.out.println("Wrong sequence number.\nExpected " + currSeqNum + " got " + sequenceNum + ".");
                //Resend previous ACK
                System.out.println("Sending ACK with sequence number " + lastRcvSeqNum + " to " + serverIP + " on port " + serverPort);
                String ACK = null;
                ACK = InetAddress.getLocalHost() + Receiver.createACK(lastRcvSeqNum);
                byte[] sendData = new byte[ACK.length()];
                sendData = ACK.getBytes();
                sendPkt = new DatagramPacket(sendData, sendData.length, serverIP, serverPort);
                rcvSocket.send(sendPkt);
                continue;
            }

            //Incremement Sequence Number
            currSeqNum = (currSeqNum + 1) % Receiver.SEQ_NUM_WIN;
            incPackets.add(message.substring(headerLength, rcvpktsize));

            // ACKing Packets
            System.out.println("Sending ACK with sequence number " + sequenceNum + " to " + serverIP + " on port " + serverPort + "\n");
            String ACK = null;
            ACK = InetAddress.getLocalHost() + Receiver.createACK(sequenceNum);

            byte[] sendData = ACK.getBytes();
            sendPkt = new DatagramPacket(sendData, sendData.length, serverIP, serverPort);
            rcvSocket.send(sendPkt);

            if(incPackets.toString().contains("\r\n\r\n\r\n"))
            {
                totalMsg = Receiver.combinePackets(incPackets);
                totalMsg = totalMsg.substring(0, totalMsg.length() - 1);
                System.out.println("End of message.");
                System.out.println("ArrayList total message: \n{\n" + totalMsg + "\n}");
                System.out.println("ArrayList item size: " + totalMsg.length() + "\n");
                System.out.println("SRV RESPONSE = \n{\n" + totalMsg + "\n}\n");

                break;
            }
        }
        rcvSocket.close();
    }

    /*
	 * ---------------------------------------------------------------------------------------------
	 * This method takes a String parameter, which will denote the message type to server
	 * Message always begins with the flag number and the client's InetAddress, followed by CRLF
	 *
	 * "INUP" is for inform and update
	 * 		This portion scans a users shared file --- chosen by GUI later on --- and appends
	 * 		file name and file size (in bytes) of every file in the folder, followed by CRLF
	 * "QUER" is to query for file(s)
	 * "EXIT" is for exit --- using "$$$$$$$$" as placeholder message for now
	 *
	 * Method returns the overall message String
	 * ---------------------------------------------------------------------------------------------
	 */
    private static String genReqMsg(String msgType) throws Exception
    {
        Scanner scan = new Scanner(System.in);
        String msg = "";

        switch(msgType.toUpperCase())
        {
            case "INUP":
                msg += "INUP " + InetAddress.getLocalHost() + "\r\n";
                File folder;
                do
                {
                    System.out.println("Please enter the absolute path of the sharing folder.");
                    folder = new File(scan.nextLine());
                } while(!folder.isDirectory());
                File[] sharedFiles = folder.listFiles();

                for(File file : sharedFiles)
                    msg += file.getName() + " " + file.length() + " " + file.getPath() + "\r\n";

                break;
            case "QUER":
                msg += "QUER " + InetAddress.getLocalHost() + "\r\n";
                System.out.println("Name of file to query? Type -showall [-showall] to request entire directory.");
                String input = scan.next();
                msg += input + "\r\n";
                break;
            case "EXIT":
                msg += "EXIT " + InetAddress.getLocalHost() + "\r\n";
                msg += "$$$$$$$$\r\n";
                break;
        }
        msg += "\r\n\r\n";

        return msg;
    }
    
    public void startTCPClient(String name, String serverIP, int serverPort, String filepath, String destination)
    {
        System.out.println("Starting TCP Client...\n");
        this.tcpCli = new TCPClient(name, serverIP, serverPort, filepath, destination);
        tcpCli.start();
    }
    
    /**
    * Simple client connects sends a sentence periodically and outputs the
    * response. This is an adaption of the code provided by the Computer
    * Networking: A Top Down Approach book by Kurose and Ross
    *
    * @author Chad Williams
    * @additions from Mudassar and Elliot
    */
    public class TCPClient extends Thread {

        private int serverPort;
        private String serverIP;
        private String targetFile;
        private String storeLocation;

        public TCPClient(String name, String serverIP, int serverPort, String filepath, String destination) 
        {
            super(name);
            this.serverPort = serverPort;
            this.serverIP = serverIP;
            this.targetFile = filepath;
            this.storeLocation = destination;
        }

        /*
         * Code to request a file via TCP from a Transient Server (other client)
         * After a query, the user can inputs the file name, host IP, and filepath (from query),
         * and a new thread will begin to attempt to transfer the file
         */
        @Override
        public void run() 
        {
            Socket clientSocket = null;
            int filesize=999999999;
            int bytesRead;
            int currentTot = 0;

            // Attempt to open socket with the server and write to it
            try 
            {
                String srvResponse;
                System.out.println("CLIENT opening socket");
                clientSocket = new Socket(serverIP, serverPort);
                System.out.println("CLIENT connected to server");
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                System.out.println(this.getName() + ": sending '" + targetFile + "'");
                outToServer.writeBytes(targetFile + '\n');
                srvResponse = inFromServer.readLine();

                System.out.println(this.getName() + " received from server: " + srvResponse);     

                // Response of 200 means that file was found on the client
                if(srvResponse.equals("200 OK"))
                {
                    System.out.println("Attempting to download file!");

                    // Attempt to transfer the file via byteArray and OutputStreams
                    try
                    {
                        byte [] bytearray = new byte [filesize];
                        InputStream is = clientSocket.getInputStream();
                        FileOutputStream fos = new FileOutputStream(storeLocation);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        bytesRead = is.read(bytearray,0,bytearray.length);
                        currentTot = bytesRead;

                        do 
                        {
                                bytesRead = is.read(bytearray, currentTot, (bytearray.length-currentTot));
                            if(bytesRead >= 0) currentTot += bytesRead;
                        }
                        while(bytesRead > -1);

                        bos.write(bytearray, 0 , currentTot);
                        bos.flush();
                        bos.close();
                        System.out.println("Download complete!");
                    }
                    catch (Exception e)
                    {
                            System.out.println("transfer error");
                            e.printStackTrace();
                    }
                }
                // Otherwise, file not found, so return error
                else if(srvResponse.equals("404 NOT FOUND"))
                {
                    System.out.println("File not found - please a different file or host");
                }
                clientSocket.close();
                System.out.println(this.getName() + " closed connection to server");
            }
            catch (Exception e) 
            {
                e.printStackTrace();
                try 
                {
                    if (clientSocket != null) 
                    {
                            clientSocket.close();
                    }
                } 
                catch (Exception cse) 
                {
                        // ignore exception here
                }
            }
        }
    }
    
    
    /**
    * Simple TCP server thread that starts up and waits for TCP connections and
    * echos what is sent capitalized. This is an adaption of the code provided by
    * the Computer Networking: A Top Down Approach book by Kurose and Ross
    *
    * @author Chad Williams
    * @additions from Mudassar and Elliot
    */
    private class TCPServer extends Thread 
    {
        private int port;

        public TCPServer(String name, int port) 
        {
                super(name);
                this.port = port;
        }

        String clientQuery;
        String answer;

       /*
        * Code to transfer a file to a client making a request
        * The client sents the exact filepath through the socket, and the server
        * returns "200 OK" if the file exists, and "404 NOT FOUND" if not
        * After the message is sent, if the code was 200, then the server will
        * begin transferring the file.
        * Threading supports multiple clients connecting and transferring at the same time
        */
        public void run() 
        {
            ServerSocket serverSocket = null;

            // Try to open serversocket and begin to listen/accept connections from clients
            try 
            {
                serverSocket = new ServerSocket(this.port);

                while (true) 
                {
                    String clientResponse;
                    System.out.println("TCP SERVER accepting connections");
                    Socket clientConnectionSocket = serverSocket.accept();
                    System.out.println("TCP SERVER accepted connection (single threaded so others wait)");

                    // Read the incoming query from a client and extract the filepath
                    while (clientConnectionSocket.isConnected() && !clientConnectionSocket.isClosed()) 
                    {
                        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientConnectionSocket.getInputStream()));
                        DataOutputStream outToClient = new DataOutputStream(clientConnectionSocket.getOutputStream());
                        clientQuery = inFromClient.readLine();
                        answer = "500 PROBLEM";

                        // Check if the exact filepath exists on the client, then create msg
                        try
                        {
                            File f = new File(clientQuery);
                            if(f.exists())
                                    answer = "200 OK";
                            else
                                    answer = "404 NOT FOUND";
                        }
                        catch(Exception e)
                        {
                              System.out.println("File error");
                        }

                        // Note if this returns null it means the client closed the connection
                        if (clientQuery != null) 
                        {
                            System.out.println("TCP SERVER Received: " + clientQuery);
                            clientResponse = answer + '\n';
                            System.out.println("TCP SERVER responding: " + clientResponse);
                            outToClient.writeBytes(clientResponse);

                            // Attempt to transfer the file via byteArrays and OutputStreams
                            if(answer.equals("200 OK"))
                            {
                                Thread.sleep(3000);
                                System.out.println("Server transfering...");
                                try
                                {
                                    System.out.println("Client query = " + clientQuery);
                                    File transferFile = new File (clientQuery);	        		
                                    byte [] bytearray = new byte [(int)transferFile.length()];
                                    FileInputStream fin = new FileInputStream(transferFile);
                                    BufferedInputStream bin = new BufferedInputStream(fin);
                                    bin.read(bytearray,0,bytearray.length);
                                    OutputStream os = clientConnectionSocket.getOutputStream();
                                    System.out.println("Sending Files..");
                                    os.write(bytearray,0,bytearray.length);
                                    os.flush(); 
                                    clientConnectionSocket.close();
                                    bin.close();
                                    System.out.println("File transfer complete");
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                    System.out.println("Transfer error");
                                }
                            }
                        } 
                        else 
                        {
                            clientConnectionSocket.close();
                            System.out.println("TCP SERVER client connection closed");
                        }
                    }
                }
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
                {
                    try 
                    {
                            serverSocket.close();
                    } 
                    catch (IOException ioe) 
                    {
                            // ignore
                    }
                }
            }
        }
    }
}
