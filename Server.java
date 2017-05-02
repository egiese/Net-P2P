import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Simon on 4/29/17.
 */
public class Server implements Sender, Receiver
{
    private int portNumber;
    private DatagramSocket serverSocket;
    private static List<Peer> peers;
    private ArrayList<ClientHandler> currClients;
    private Boolean slowMode;

    public Server(int portNumber, boolean slowMode) throws Exception
    {
        this.portNumber = portNumber;
        this.serverSocket = new DatagramSocket(portNumber);
        System.out.println("UDP Server opened on port " + this.portNumber);
        currClients = new ArrayList<ClientHandler>();
        peers = new ArrayList<Peer>();
        this.slowMode = slowMode;
    }

    public void serve() throws IOException
    {
        byte[] rcvData = new byte[1024];

        while(true)
        {
            DatagramPacket packet = new DatagramPacket(rcvData, rcvData.length);
            serverSocket.receive(packet);

            boolean temp = false;
            for(ClientHandler clients : currClients)
            {
                if(clients.clientIP.equals(packet.getAddress()))
                {
                    clients.queue.offer(packet);
                    temp = true;
                }
            }
            if(!temp)
            {
                BlockingQueue<DatagramPacket> queue = new LinkedBlockingQueue<DatagramPacket>();
                ClientHandler cH = new ClientHandler(packet.getAddress(), packet.getPort(), queue);
                new Thread(cH).start();
                queue.offer(packet);
                currClients.add(cH);
            }
        }
    }

    public static String parseMsg(String msg) throws Exception
    {
        Scanner scan = new Scanner(msg);
        String message = "";
        String method = scan.next();
        String hostInfo = scan.next();
        scan.nextLine();

        Scanner hostScan = new Scanner(hostInfo).useDelimiter("/");
        String hostname = hostScan.next();
        String IPAddress = "/" + hostScan.next();

        switch (method)
        {
            case "INUP":
                for(Iterator<Peer> iter = peers.iterator(); iter.hasNext();)
                {
                    Peer p = iter.next();
                    if(p.getIP().equals(IPAddress) || p.getHost().equals(hostname))
                        iter.remove();
                }
                peers.add(new Peer(hostInfo, msg));
                message += genRspMsg(method);
                break;
            case "QUER":
                String queryResponse = "";
                if(!peers.isEmpty())
                {
                    String queryName = scan.nextLine();
                    String search = null;

                    System.out.println("queryname = " + queryName);
                    for(Peer peer : peers)
                    {
                        if(!(search = peer.searchHash(queryName)).equals("File not found"))
                            queryResponse += search;
                    }

                    if(search.equals("File not found"))
                        queryResponse = "File not found\r\n";
                }
                message += genRspMsg(method) + queryResponse;
                break;
            case "EXIT":
                if(!peers.isEmpty())
                {
                    System.out.println("Leaving host is --- " + hostname + " " + IPAddress + "\n");

                    for(Iterator<Peer> iter = peers.iterator(); iter.hasNext();)
                    {
                        Peer p = iter.next();
                        if(p.getIP().equals(IPAddress) || p.getHost().equals(hostname))
                            iter.remove();
                    }
                }
                message += genRspMsg(method);
                break;
            default:
                message += genRspMsg(method);
                break;
        }
        return message + "\r\n\r\n";
    }

    public static String genRspMsg(String method) throws Exception
    {
        String msg = "";

        switch(method)
        {
            case "INUP":
                msg += "200 OK\r\nThank you for your contribution!\r\n";
                break;
            case "QUER":
                msg += "200 OK\r\n";
                break;
            case "EXIT":
                msg += "200 OK\r\nGoodbye!\r\n";
                break;
            default:
                msg += "400 ERROR\r\nRequest not understood - please try again.\r\n";
                break;
        }

//        msg += "\r\n\r\n\r\n";
        return msg;
    }

    private class ClientHandler implements Runnable
    {
        private InetAddress clientIP;
        private int clientPort;
        private BlockingQueue<DatagramPacket> queue;
        private ArrayList<String> incPackets;

        public ClientHandler(InetAddress clientIP, int clientPort ,BlockingQueue<DatagramPacket> queue)
        {
            this.clientIP = clientIP;
            this.clientPort = clientPort;
            this.queue = queue;
            this.incPackets = new ArrayList<String>();
            System.out.println("New Thread!");
        }

        public void run()
        {
            byte[] rcvData = new byte[100];
            DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
            DatagramPacket sendPkt = null;

            int currSeqNum = Receiver.INIT_SEQ_NUM;
            String totalMsg = "";
            String answer = null;

            while(true)
            {
                System.out.println("Waiting for packet from client " + clientIP.getHostAddress());
                try {
                    rcvPkt = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                queue.clear();
                int rcvpktsize = rcvPkt.getLength();
                String message = new String(rcvPkt.getData());
                Scanner scan = new Scanner(message);
                int headerLength = scan.nextLine().length() + 2;
                int sequenceNum = Sender.getSeqNum(message);
                System.out.println("Packet received! Sequence number " + sequenceNum);

                if(slowMode)
                {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if(currSeqNum != sequenceNum)
                {
                    System.out.println("Wrong sequence number.\nExpected " + currSeqNum + " got " + sequenceNum + ".");
                    //Resend previous ACK
                    System.out.println("Sending ACK with sequence number " + currSeqNum + " to " + clientIP + " on port " + clientPort);
                    String ACK = null;
                    try {
                        ACK = InetAddress.getLocalHost() + Receiver.createACK(currSeqNum);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    byte[] sendData = new byte[ACK.length()];
                    sendData = ACK.getBytes();
                    sendPkt = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
                    try {
                        serverSocket.send(sendPkt);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // Increment Sequence Number
                currSeqNum = (currSeqNum + 1) % Receiver.SEQ_NUM_WIN;
                incPackets.add(message.substring(headerLength, rcvpktsize));

                // ACKing Packets
                System.out.println("Sending ACK with sequence number " + sequenceNum + " to " + clientIP + " on port " + clientPort + "\n");
                String ACK = null;
                try {
                    ACK = InetAddress.getLocalHost() + Receiver.createACK(sequenceNum);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                byte[] sendData = ACK.getBytes();
                sendPkt = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
                try {
                    serverSocket.send(sendPkt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(incPackets.toString().contains("\r\n\r\n\r\n"))
                {
                    totalMsg = Receiver.combinePackets(incPackets);
                    totalMsg = totalMsg.substring(0, totalMsg.length() - 1);
                    System.out.println("End of message.");
                    System.out.println("ArrayList total message: \n{\n" + totalMsg + "\n}");
                    System.out.println("ArrayList item size: " + totalMsg.length() + "\n");
                    try {
                        answer = parseMsg(totalMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("SRV RESPONSE = \n{\n" + answer + "\n}\n");

                    break;
                }
            }

            try {
                this.sendMessage(answer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            currClients.remove(this);
        }

        public void sendMessage(String msg) throws Exception
        {
            DatagramSocket clientSocket = new DatagramSocket();
            byte[] rcvData = new byte[1024];
            String packets[] = Sender.makePacket(msg);

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
                DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, clientIP, portNumber);

                if(slowMode)
                {
                    Thread.sleep(4000);
                }

                try{
                    clientSocket.send(sendPkt);
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
                    clientSocket.setSoTimeout(timeoutInterval);
                    double startTime = System.nanoTime() / 1000000;
                    rcvPkt = new DatagramPacket(rcvData, rcvData.length);

                    // Try to receive a packet
                    try{
                        clientSocket.receive(rcvPkt);
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
                            clientSocket.send(sendPkt);
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
                    clientSocket.setSoTimeout(0);
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
            clientSocket.close();
        }
    }
}
