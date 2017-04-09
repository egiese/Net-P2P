
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.net.*;

public class P2PClient
{
	final static int PORT = 5666;
	final static int CLI_PORT = 6666;
	final static int MTU = 128;
	final static int IPHEAD = 20;
	final static int UDPHEAD = 8;
	final static int SEQ = 3;
	final static int APPHEADER = 5;
	final static double INIT_EST_RTT = 100.0; //Note, RTT is measured in milliseconds.
	final static double INIT_DEV_RTT = 0.0;
	final static double alpha = 0.125;
	final static double beta = 0.25;
	final static int INIT_SEQ_NUM = 0;
	final static int SEQ_NUM_WIN = 2;
	final static String serverHostname = "localhost";
	final static String sharePath = "C:/Users/Kookus/Documents/CCSU/Spring 2017/CS 490 - Networking/SharedFiles";

	private static Scanner scan;
	private static DatagramSocket sendSkt;
	private static Scanner seq;

	public static void main (String [] args) throws Exception
	{
		InetAddress serverIP = InetAddress.getByName(serverHostname);

		sendSkt = new DatagramSocket();
		byte[] rcvData = new byte[1024];



		System.out.println("Attempting to send to " + serverIP + " via UDP");

		String message = genReqMsg(1);
		String[] packets = makePacket(message);

		double estRTT = INIT_EST_RTT;
		double devRTT = INIT_DEV_RTT;
		devRTT = calcDevRTT(0.0, INIT_EST_RTT, estRTT, devRTT);
		estRTT = calcEstimatedRTT(0.0, INIT_EST_RTT, estRTT);
		int timeoutInterval = calcTimeoutInterval(estRTT, devRTT);

		int currSeqNum = INIT_SEQ_NUM;

		for(String p : packets)
		{

			// Sending packets //
			byte[] sendData = new byte[p.length()];
			sendData = p.getBytes();
			DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, serverIP, PORT);
			sendSkt.send(sendPkt);
			
			System.out.println("Waiting for ACK...");
           		DatagramPacket rcvPkt;

            while(true)
            {
                // Receiving ACKs //
                // Start Timer
                sendSkt.setSoTimeout(timeoutInterval);
                double startTime = System.nanoTime() / 1000000;
                rcvPkt = new DatagramPacket(rcvData, rcvData.length);
                try
                {
                    sendSkt.receive(rcvPkt);
                }
                // If timeout
                catch(SocketTimeoutException e)
                {
                    double endTime = System.nanoTime() / 1000000;
                    System.out.println("Timeout!");
                    // Recalculate timeout interval
                    devRTT = calcDevRTT(startTime, endTime, estRTT, devRTT);
                    estRTT = calcEstimatedRTT(startTime, endTime, estRTT);
                    timeoutInterval = calcTimeoutInterval(estRTT, devRTT);
                    // Resend packet
                    sendSkt.send(sendPkt);
                    // Restart loop
                    continue;
                }
                double endTime = System.nanoTime() / 1000000;
                System.out.println("ACK received!");
                // Recalculate timeout interval
                devRTT = calcDevRTT(startTime, endTime, estRTT, devRTT);
                estRTT = calcEstimatedRTT(startTime, endTime, estRTT);
                timeoutInterval = calcTimeoutInterval(estRTT, devRTT);
				// Check new seq number
				int newSeqNum = getSeqNum(new String(rcvPkt.getData()));
				// If incorrect sequence number
				if(newSeqNum != currSeqNum)
				{
					System.out.println("Wrong sequence number.\nExpected " + currSeqNum + " got " + newSeqNum + ".");
                // Resend packet
					sendSkt.send(sendPkt);
                // Restart loop
					continue;
				}
                // Turn off timer
                sendSkt.setSoTimeout(0);
				//Incrememnt sequence number
				currSeqNum = (currSeqNum + 1) % SEQ_NUM_WIN;
                // End loop
                break;
            }

           		String ACK = new String(rcvPkt.getData());
           		int sequenceNum = getSeqNum(ACK);
			int rcvpktsize = rcvPkt.getLength();
			InetAddress clientIP = rcvPkt.getAddress();
			int clientPort = rcvPkt.getPort();
            		String ack = new String(rcvPkt.getData());

			System.out.println("ACK = " + ack);
			System.out.println("sequence number = " + sequenceNum);
			System.out.println("Sender IP address: " + clientIP);
			System.out.println("Sender port: " + clientPort);
			System.out.println("Packet size: " + rcvpktsize + "\n\n\n");
		}
	}

	public static int calcTimeoutInterval(double estimatedRTT, double devRTT)
	{
		int timeout = (int) (estimatedRTT + 4 * devRTT);
		return timeout;
	}

	public static double calcEstimatedRTT(double startInMillis, double endInMillis, double estimatedRTT)
	{
		double sRTT = endInMillis - startInMillis;
		double eRTT = ((1 - alpha) * estimatedRTT) + (alpha * sRTT);
		return eRTT;
	}

	public static double calcDevRTT(double startInMillis, double endInMillis, double estimatedRTT, double devRTT)
	{
		double sRTT = endInMillis - startInMillis;
		double dRTT = ((1 - beta) * devRTT) + (beta * Math.abs(sRTT - estimatedRTT));
		return dRTT;
	}

	public static int getSeqNum(String packet)
	{
		seq = new Scanner(packet);
		seq.next();
		seq.next();

		return Integer.parseInt(seq.next());
	}

	public static String createACK(int sequenceNumber)
	{
		return " 1 " + sequenceNumber + "\r\n\r\n";
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
		String msg = "";

		/* Inform and update server with shared MP3s */
		if(flag == 1)
		{
			msg += "INUP" + " " + InetAddress.getLocalHost() + "\r\n";
			File folder = new File(sharePath);
			File[] sharedFiles = folder.listFiles();

			for(File file : sharedFiles)
				msg += file.getName() + " " + file.length() + " " + file.getPath() + "\r\n";
		}

		/* Query server for one file or entire directory */
		else if(flag == 2)
		{
			msg += "QUER" + " " + InetAddress.getLocalHost() + "\r\n";
			System.out.println("Name of file to query? (Or type -showall to view entire directory");
			String input = scan.next();
			msg += input + "\r\n";
		}

		else if(flag == 3)
		{
			msg += "EXIT" + " " + InetAddress.getLocalHost() + "\r\n";
			msg += "$$$$$$$$";
		}

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
	public static int getNumPackets(String msg) throws Exception
	{
		String header = InetAddress.getLocalHost() + " 0 ";
		int payloadSize = MTU - IPHEAD - UDPHEAD - SEQ - header.length();
		int numPackets = msg.length() / payloadSize;

		System.out.println("num packets = " + numPackets);
		System.out.println("payloadSize = " + payloadSize);
		System.out.println("msg size = " + msg.length());


		if( (msg.length() % payloadSize) != 0)
			numPackets++;

		System.out.println("num packets = " + numPackets + "\n\n");

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
	public static String[] makePacket(String msg) throws Exception
	{
		String header = InetAddress.getLocalHost() + " 0 ";
		int payloadSize = MTU - IPHEAD - UDPHEAD - SEQ - header.length();
		int numPackets = getNumPackets(msg);
		int currentPacket = 1;

		String packets[] = new String[numPackets];

		for(int i = 0; i < numPackets; i++)
		{
			if(i == numPackets - 1)
				packets[i] = header + i%2 + "\r\n" + msg.substring(i * payloadSize, msg.length() );

			else
				packets[i] = header + i%2 + "\r\n" + msg.substring(i * payloadSize, (currentPacket * payloadSize) );

			currentPacket++;
		}

		return packets;
	}
}
