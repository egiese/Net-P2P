import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    public Server(int portNumber) throws Exception
    {
        this.portNumber = portNumber;
        this.serverSocket = new DatagramSocket(portNumber);
        System.out.println("UDP Server opened on port " + this.portNumber);
        currClients = new ArrayList<ClientHandler>();
        peers = new ArrayList<Peer>();
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
                ClientHandler cH = new ClientHandler(packet.getAddress(), queue);
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
                            queryResponse += search + "\r\n";
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
            case "EXIT":
                msg += "200 OK\r\nGoodbye!\r\n";
            default:
                msg += "400 ERROR\r\nRequest not understood - please try again.\r\n";
        }

//        msg += "\r\n\r\n\r\n";
        return msg;
    }

    private class ClientHandler implements Runnable
    {
        private InetAddress clientIP;
        private BlockingQueue<DatagramPacket> queue;
        private ArrayList<String> incPackets;

        public ClientHandler(InetAddress clientIP, BlockingQueue<DatagramPacket> queue)
        {
            this.clientIP = clientIP;
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

            while(true)
            {
                System.out.println("Waiting for packet from client " + clientIP.getHostAddress());
                try {
                    rcvPkt = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int rcvpktsize = rcvPkt.getLength();
                int clientPort = rcvPkt.getPort();
                String message = new String(rcvPkt.getData());
                Scanner scan = new Scanner(message);
                int headerLength = scan.nextLine().length() + 2;
                int sequenceNum = Sender.getSeqNum(message);

                if(currSeqNum != sequenceNum)
                {
                    System.out.println("Wrong sequence number.\nExpected " + currSeqNum + " got " + sequenceNum + ".");
                    //Resend previous ACK
                    System.out.println("Sending ACK for " + currSeqNum + " to " + clientIP + " on port " + clientPort);
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
                System.out.println("Sending ACK for " + sequenceNum + " to " + clientIP + " on port " + clientPort + "\n");
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

//                System.out.println(incPackets.get(incPackets.size() - 1));

                if(incPackets.toString().contains("\r\n\r\n\r\n"))
                {
                    totalMsg = Receiver.combinePackets(incPackets);
                    totalMsg = totalMsg.substring(0, totalMsg.length() - 1);
                    System.out.println("End of message.");
                    System.out.println("ArrayList total message: \n{\n" + totalMsg + "\n}");
                    System.out.println("ArrayList item size: " + totalMsg.length() + "\n");
                    String answer = null;
                    try {
                        answer = parseMsg(totalMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("SRV RESPONSE = \n{\n" + answer + "\n}\n");

                    currClients.remove(this);
                    break;
                }
            }
        }
    }
}
