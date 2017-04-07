
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Scanner;

public class P2PServer
{
	final static int PORT = 5666;
	final static String serverHostname = "localhost";
	private static Scanner scan;
	private static DatagramSocket srvSkt;
	private static ArrayList<String> incPackets;
	private static ArrayList<Peer> peers;
	private static Scanner seq;
	

	public static void main (String [] args) throws IOException
	{
		srvSkt = new DatagramSocket(PORT);
		incPackets = new ArrayList<String>();
		peers = new ArrayList<Peer>();
		
		System.out.println("UDP Server Port opened on port " + PORT);
		byte[] rcvData = new byte[100];
		
		DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
		DatagramPacket sendPkt = null;
		
		
		while(true)
		{
			// Receiving packets //
			System.out.println("Waiting for packet on port " + PORT + "...\n");
			srvSkt.receive(rcvPkt);
			int rcvpktsize = rcvPkt.getLength();
			int clientPort = rcvPkt.getPort();
			InetAddress clientIP = rcvPkt.getAddress();
			String message = new String(rcvPkt.getData());
			scan = new Scanner(message);
			int headerLength = scan.nextLine().length() + 2;
			incPackets.add(message.substring(headerLength, rcvpktsize));
			int sequenceNum = getSeqNum(message);
			String totalMsg = combinePackets(incPackets);
			
			
			// ACKing packets //
			System.out.println("Sending ACK for " + sequenceNum + " to " + clientIP + " on port " + clientPort + "\n");
			String ACK = InetAddress.getLocalHost() + createACK(sequenceNum);
			byte[] sendData = new byte[ACK.length()];
			sendData = ACK.getBytes();
			sendPkt = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
			srvSkt.send(sendPkt);
			
			
		
			// Debugging & printing //
			//System.out.println("Sender message: \n{\n" + message + "\n}");
			//System.out.println("Packet size: " + rcvpktsize + "\n");
		
			System.out.println("ArrayList total message: \n{\n" + totalMsg + "\n}");
			System.out.println("arraylist item size: " + totalMsg.length() + "\n");
		}
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------------------
	 * Method to parse a message String (raw message data) into its components
	 * 
	 * Takes a String as a parameter, and performs and action based on the "method" field found in
	 * the message header
	 * 
	 * Headers include:
	 *   - INUP - server will add all following MP3 file information to a hash map of K/V :: host/MP3
	 *   - QUER - server will scan all hash maps for requested files
	 *   - EXIT - server will delete all values in hash map of sender's host name
	 *   
	 * If the method header was not understood, then the server will construct an error message
	 * ---------------------------------------------------------------------------------------------
	 */	
	public static String parseMsg(String msg) throws Exception
	{
		scan = new Scanner(msg);

		String method = scan.next();
		String hostInfo = scan.next();

		if(method.equals("INUP"))
		{
			//add all files to hash of (key, value) -> (hostname, song info)
			peers.add(new Peer(hostInfo, msg));
			return genRspMsg(method);
		}
		else if(method.equals("QUER"))
		{
			//scan entire hashmap, looking at first scan.next() String for song name

			String queryName = scan.nextLine();
			String queryResponse = "";
			String search;
			
			for(Peer peer : peers)
			{
				if(!(search = peer.searchHash(queryName)).equals("File not found"))
					queryResponse += search + "\r\n";
				
			}
			
			//do something here with the response message.
			return genRspMsg(method) + queryResponse;
		}
		else if(method.equals("EXIT"))
		{
			//check hash for host, delete all values
			Scanner hostScan = new Scanner(hostInfo).useDelimiter("/");
			String hostname = hostScan.next();
			String IPAddress = hostScan.next();
			int removeIndex = 0;

			for(Peer peer : peers)
			{
				if(peer.getHost().equals(hostname) && peer.getIP().equals(IPAddress))
				{
					peers.remove(removeIndex);
					break;
				}
				else
					removeIndex++;				
			}
		
			return genRspMsg(method);
		}
		else
		{
			//error//
			return genRspMsg("");
		}
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------------------
	 * Method to revert all incoming packets back to raw String message, which will be used for
	 * parsing afterward
	 * 
	 * Takes the list of all packets for a particular message, stored in an ArrayList of Strings
	 * as a parameter, and returns a message String
	 * ---------------------------------------------------------------------------------------------
	 */
	public static String combinePackets(ArrayList<String> packets)
	{
		String msg = "";
		
		for(String packet : packets)
			msg += packet;
		
		return msg;
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------------------
	 * Method to obtain the sequence number of a current packet
	 * 
	 * Takes the current packet String as a parameter and returns an sequence number integer
	 * 
	 * Sequence number is the first character of any packet coming through the socket
	 * ---------------------------------------------------------------------------------------------
	 */
	public static int getSeqNum(String packet)
	{
		seq = new Scanner(packet);
		seq.next();
		seq.next();
		
		return Integer.parseInt(seq.next());
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------------------
	 * Method to create an ACK String for an incoming packet
	 * 
	 * Takes a sequenceNumber integer of the current packet as a parameter and returns a String
	 * ---------------------------------------------------------------------------------------------
	 */
	public static String createACK(int sequenceNumber)
	{
		return " 1 " + sequenceNumber + "\r\n\r\n";
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------------------
	 * Method to generate a response message to an incoming Client message. Once the packets of the
	 * message have been combined, and the actual message has been parsed, the server must respond
	 * accordingly.
	 * 
	 * This method takes a String, "method," as a parameter, which is represents the method field
	 * of the Client message.
	 *   - INUP is inform and update - returns a standard message
	 *   - QUER is query - returns a list of all requested filenames, sizes, paths, and hosts
	 *   - EXIT is exit - returns a standard message
	 * 
	 * If the total message was parsed without errors, the server returns a 200 "OK" message
	 * If the message had an unrecognizable method field, the server responds with a 400 "ERROR"
	 * message.
	 * ---------------------------------------------------------------------------------------------
	 */
	public static String genRspMsg(String method) throws Exception
	{
		String msg = "";
		
		
		if(method.equals("INUP"))
		{
			msg += "200" + "OK" + "\r\n" + "Thank you for your contribution!\r\n\r\n";	
			
		}
		
		else if(method.equals("QUER"))
		{
			msg += "200" + "OK" + "\r\n";
			
		}
		
		else if(method.equals("EXIT"))
		{
			msg += "200" + "OK" + "\r\n" + "Goodbye!\r\n\r\n";
			
		}
		
		else
		{
			msg += "400" + "ERROR" + "\r\n" + "Request not understood - please try again\r\n\r\n";
			
		}
		
		return msg;
	}
}
