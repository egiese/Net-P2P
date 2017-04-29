import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/**
 * Created by Simon on 4/28/17.
 */
public class Server implements Sender, Receiver{

    private int portNumber;
    private DatagramSocket serverSocket;
    private ArrayList<Peer> peers;
    private String clientList;

    public Server(int portNumber) throws Exception
    {
        this.portNumber = portNumber;
        this.serverSocket = new DatagramSocket(portNumber);
        System.out.println(serverSocket.getLocalSocketAddress());

        System.out.println("UDP Server Port opened on port " + this.portNumber);
        peers = new ArrayList<Peer>();
    }

    public void serve() throws IOException {
        byte[] rcvData = new byte[1024];
        clientList = "";

        while (true)
        {
            DatagramPacket packet = new DatagramPacket(rcvData, rcvData.length);
            serverSocket.receive(packet);
            if(!(clientList.contains(packet.getAddress().toString())))
            {
                clientList += packet.getAddress().toString() + " ";
                new Thread(new Responder(packet)).start();
            }
        }
    }

    public void receiveMessage() throws Exception
    {

    }

    public void sendMessage(String msgType) throws Exception
    {

    }

    private static String genRespMsg(String msgType) throws Exception
    {
        return "";
    }

    class Responder implements Runnable
    {
        DatagramSocket socket = null;
        private ArrayList<String> incPackets;

        public Responder(DatagramPacket packet)
        {
            incPackets = new ArrayList<String>();
            try {
                this.socket = new DatagramSocket();
                System.out.println(socket.getLocalSocketAddress());
                this.socket.connect(packet.getAddress(), packet.getPort());
                System.out.println(socket.getRemoteSocketAddress());
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            byte[] rcvData = new byte[100];
            DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
            DatagramPacket sendPkt = null;

            while(true) {
                try {
                    socket.receive(rcvPkt);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Packet received on " + socket.getInetAddress().toString());
            }
        }
    }
}
