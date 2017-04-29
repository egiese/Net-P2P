import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;

/**
 * Created by Simon on 4/28/17.
 */
public class Server implements Sender, Receiver{

    private int portNumber;
    private DatagramSocket socket;
    private ArrayList<String> incPackets;
    private ArrayList<Peer> peers;
    private String clientList;

    public Server(int portNumber) throws Exception
    {
        this.portNumber = portNumber;
        this.socket = new DatagramSocket(portNumber);

        System.out.println("UDP Server Port opened on port " + this.portNumber);
        incPackets = new ArrayList<String>();
        peers = new ArrayList<Peer>();
    }

//    public void serve() throws IOException
//    {
//
//    }

    public void serve() throws IOException {
        byte[] rcvData = new byte[1024];
        clientList = "";

        while (true)
        {
            DatagramPacket packet = new DatagramPacket(rcvData, rcvData.length);
            socket.receive(packet);
            if(!(clientList.contains(packet.getAddress().toString())))
            {
                clientList += packet.getAddress().toString();
                new Thread(new Responder(packet)).start();
            }
        }
    }

    public void receiveMessage() throws Exception
    {
        byte[] rcvData = new byte[1024];
        DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
//        this.sendMessage();
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

        public Responder(DatagramPacket packet)
        {
            try {
                this.socket = new DatagramSocket();
                this.socket.connect(packet.getAddress(), portNumber);
            } catch (SocketException e) {
                System.out.println(packet.getAddress().toString());
                e.printStackTrace();
            }
        }

        public void run()
        {
            try {
                System.out.println("Made it!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
