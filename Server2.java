import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Simon on 4/29/17.
 */
public class Server2
{
    private int portNumber;
    private DatagramSocket serverSocket;
    private ArrayList<Peer> peers;
    private String clientList;
    private ArrayList<ClientHandler> currClients;

    public Server2(int portNumber) throws Exception
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

        public ClientHandler(InetAddress clientIP, BlockingQueue<DatagramPacket> queue)
        {
            this.clientIP = clientIP;
            this.queue = queue;
            System.out.println("New Thread!");
        }

        public void run()
        {
            while(true)
            {
                try {
                    System.out.println(queue.take().getData());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
