import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
    private ArrayList<Peer> peers;
    private String clientList;
    private ArrayList<ClientHandler> currClients;

    public Server(int portNumber) throws Exception
    {
        this.portNumber = portNumber;
        this.serverSocket = new DatagramSocket(portNumber);
        System.out.println("UDP Server opened on port " + this.portNumber);
        currClients = new ArrayList<ClientHandler>();
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
                    System.out.println(incPackets);
                    currClients.remove(this);
                    break;
                }
            }
        }
    }
}
