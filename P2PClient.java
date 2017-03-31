import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class P2PClient
{
	final static int PORT = 5555;
	final static int MTU = 128;
	final static int IPHEAD = 20;
	final static int UDPHEAD = 8;
	final static int MSGHEAD = 3;
	final static int SEQ = 3;
	final static String serverHostname = "10.0.0.44";
	final static String clientHostname = "localhost";
	private static Scanner scan;
	private static DatagramSocket dgSkt;
	
	public static void main (String [] args) throws Exception
	{	
		InetAddress serverIP = InetAddress.getByName(serverHostname);
		
		dgSkt = new DatagramSocket();
		
		System.out.println("Attempting to send to " + serverIP + " via UDP");
		
		String message = genReqMsg(1);
		String[] packets = makePacket(message);
			
		
		for(String p : packets)
		{
			byte[] sendData = new byte[p.length()];
			System.out.println("sendData size = " + sendData.length);
			sendData = p.getBytes();
			DatagramPacket dgPkt = new DatagramPacket(sendData, sendData.length, serverIP, PORT);
			dgSkt.send(dgPkt);
		}
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------------------
	 * This method takes an integer parameter of flag, which will denote the message type to server
	 * Message always begins with the flag number and the client's InetAddress, followed by CRLF
	 * 
	 * Flag of 1 is for inform and update
	 * 		This portion scans a users shared file --- chosen by GUI later on --- and appends
	 * 		file name and file size (in bytes) of every file in the folder, followed by CRLF
	 * Flag of 2 is to query for file(s)
	 * Flag of 3 is for exit --- using "$$$$$$$$" as placeholder message for now
	 * 
	 * Method returns the overall message String
	 * ---------------------------------------------------------------------------------------------
	 */
	public static String genReqMsg(int flag) throws Exception
	{
		scan = new Scanner(System.in);
		String msg = flag + " " + InetAddress.getByName(clientHostname) + "\r\n";	
		
		/* Inform and update server with shared MP3s */
		if(flag == 1)
		{
			File folder = new File("C:/Users/Kookus/Documents/CCSU/Spring 2017/CS 490 - Networking/SharedFiles");
			File[] sharedFiles = folder.listFiles();
			
			for(File file : sharedFiles)
				msg += file.getName() + " " + file.length() + " " + file.getPath() + "\r\n";
		}
		
		/* Query server for one file or entire directory */
		else if(flag == 2)
		{
			System.out.println("Name of file to query? (Or type -showall to view entire directory");
			String input = scan.next();
			msg += input + "\r\n";
		}
		
		else if(flag == 3)
			msg += "$$$$$$$$";
		
		msg += "\r\n";
		
		return msg;
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------------------
	 * This method takes a message String as parameter, and calculates the number of packets it
	 * will be divided into based on IPv4, UDP, and our own header data
	 * 
	 * Method returns the number of packets as integer
	 * ---------------------------------------------------------------------------------------------
	 */
	public static int getNumPackets(String msg)
	{
		int payloadSize = MTU - IPHEAD - UDPHEAD - SEQ;
		int numPackets = msg.length() / payloadSize;
		if( (msg.length() % payloadSize) != 0)
			numPackets++;
		
		return numPackets;
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------------------
	 * This method takes a message String as a parameter, and returns an array of packets
	 * 
	 * Each packet will have a sequence number of 0 (even) or 1 (odd) followed by CRLF at the head
	 * 
	 * Method returns a String array containing each packet
	 * ---------------------------------------------------------------------------------------------
	 */
	public static String[] makePacket(String msg)
	{
		
		int payloadSize = MTU - IPHEAD - UDPHEAD - SEQ;
		int numPackets = getNumPackets(msg);
		int currentPacket = 1;
		
		String packets[] = new String[numPackets];

		for(int i = 0; i < numPackets; i++)
		{
			if(i == numPackets - 1)
				packets[i] = i%2 + "\r\n" + msg.substring(i * payloadSize, msg.length() );
			
			else
				packets[i] = i%2 + "\r\n" + msg.substring(i * payloadSize, (currentPacket * payloadSize) );
			
			currentPacket++;
		}
		
		return packets;
	}
}
