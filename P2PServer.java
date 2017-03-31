
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class P2PServer
{
	final static int PORT = 5555;
	final static int CLI_PORT = 6666;
	final static String serverHostname = "localhost";
	final static String clientHostname = "localhost";
	private static Scanner scan;
	private static DatagramSocket srvSkt;
	private static DatagramSocket cliSkt;
	

	public static void main (String [] args) throws IOException
	{
		srvSkt = new DatagramSocket(PORT);
		
		
		System.out.println("UDP Server Port opened on port " + PORT);
		byte[] rcvData = new byte[1024];
		
		DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
		DatagramPacket sendPkt = null;
		
		while(true)
		{
			// Receiving packets //
			System.out.println("Waiting for packet on port " + PORT + "...\n");
			srvSkt.receive(rcvPkt);
			int rcvpktsize = rcvPkt.getLength();
			InetAddress clientIP = rcvPkt.getAddress();
			int clientPort = rcvPkt.getPort();
			String message = new String(rcvPkt.getData());
			int sequenceNum = getACK(message);
			
			
			// ACKing packets //
			System.out.println("Sending ACK for " + sequenceNum + " to " + clientIP + " on port " + clientPort + "\n");
			String ACK = createACK(sequenceNum);
			cliSkt = new DatagramSocket(CLI_PORT);
			byte[] sendData = new byte[ACK.length()];
			System.out.println("sendData size = " + sendData.length);
			sendData = ACK.getBytes();
			sendPkt = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
			cliSkt.send(sendPkt);
			
			
			System.out.println("sequence number = " + sequenceNum);
			System.out.println("rcvPkt: " + rcvPkt.getData());
			System.out.println("Sender IP address: " + clientIP);
			System.out.println("Sender port: " + clientPort);
			System.out.println("Packet size: " + rcvpktsize);

		}
	}

	public static int getACK(String packet)
	{
		return Character.getNumericValue(packet.charAt(0));
	}
	
	public static String createACK(int sequenceNumber)
	{
		String msg = "" + sequenceNumber;
		return msg;
	}
	
	
	
	
	
	
	public static String genRspMsg(int flag) throws Exception
	{
		String msg = flag + " " + InetAddress.getByName("localhost") + "\r\n";
		
		
		
		/* If sent message was inform and update */
		if(flag == 1)
		{
			File folder = new File("C:/Users/Kookus/Documents/CCSU/Spring 2017/CS 490 - Networking/SharedFiles");
			File[] sharedFiles = folder.listFiles();
			
			for(File f : sharedFiles)
				msg += f.getName() + " " + f.length() + " " + f.getPath() + "\r\n";
		}
		
		else if(flag == 2)
		{
			System.out.println("Name of file to query? (Or type -showall to view entire directory");
			String input = scan.next();
			msg += input + "\r\n";
		}
		
		msg += "\r\n";
		
		return msg;
	}
	
	
}
