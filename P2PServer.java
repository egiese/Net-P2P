
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class P2PServer
{
	public static void main (String [] args) throws IOException
	{
		final int PORT = 5555;
		
		DatagramSocket srvSkt = new DatagramSocket(PORT);
		
		System.out.println("UDP Server Port opened on port " + PORT);
		byte[] rcvData = new byte[1024];
		
		DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
		
		
		while(true)
		{
			System.out.println("Waiting for packet on port " + PORT + "...\n");
			srvSkt.receive(rcvPkt);
			
			int rcvpktsize = rcvPkt.getLength();
			InetAddress ip = rcvPkt.getAddress();
			int clientPort = rcvPkt.getPort();
			String message = new String(rcvPkt.getData());
			
			System.out.println("rcvPkt: " + rcvPkt.getData());
			System.out.println("Sender IP address: " + ip);
			System.out.println("Sender port: " + clientPort);
			System.out.println("Packet size: " + rcvpktsize);
			System.out.println("Sender message: {\n" + message + "}\n");
			System.out.println("Message size: " + message.length());
		}
	
		
	}
}
