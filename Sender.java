import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */
public interface Sender
{
//    Packet Info
    int MTU = 128;
    int IPHEAD = 20;
    int UDPHEAD = 8;
    int SEQ = 3;
//    SEQ Number Info
    int INIT_SEQ_NUM = 0;
    int SEQ_NUM_WIN = 2;
//    RTT Info
//    Time is measured in milliseconds.
    double INIT_EST_RTT = 100.00;
    double INIT_DEV_RTT = 0.00;
    double alpha = 0.125;
    double beta = 0.25;

    void sendMessage(String msgType) throws Exception;

    /*
	 * ---------------------------------------------------------------
	 * Method to calculate timeout interval based on estRTT and devRTT
	 * ---------------------------------------------------------------
	 */
    static int calcTimeoutInterval(double esimtatedRTT, double devRTT)
    {
        return ((int) (esimtatedRTT + 4 * devRTT));
    }

    /*
	 * ---------------------------------------------------------------------------------------------
	 * Method to calculate estRTT based on start, end, previous estRTT, and constants alpha and beta
	 * ---------------------------------------------------------------------------------------------
	 */
    static double calcEstimatedRTT(double startInMillis, double endInMillis, double estimatedRTT)
    {
        double sRTT = endInMillis - startInMillis;
        return  ((1 - alpha) * estimatedRTT) + (alpha * sRTT);
    }

    /*
	 * -----------------------------------------------------------------------------------------------------
	 * Method to calculate devRTT based on start, end, estRTT, previous devRTT, and constants alpha and beta
	 * -----------------------------------------------------------------------------------------------------
	 */
    static double calcDevRTT(double startInMillis, double endInMillis, double estimatedRTT, double devRTT)
    {
        double sRTT = endInMillis - startInMillis;
        return ((1 - beta) * devRTT) + (beta * Math.abs(sRTT - estimatedRTT));
    }

    /*
     * ------------------------------------------------------------------------------------
     * Method to create an ACK for a received packed based on its sequence number
     * Takes sequence number as parameter, and returns a String with the ACK flag raised,
     * the sequence number it's ACKing, and the message-terminating CRLFCRLF
     * ------------------------------------------------------------------------------------
     */
    static int getSeqNum(String packet)
    {
        Scanner seq = new Scanner(packet);
        seq.next();
        seq.next();
        return Integer.parseInt(seq.next());
    }

    /*
     * ---------------------------------------------------------------------------------------------
     * This method takes a message String as parameter, and calculates the number of packets it
     * will be divided into based on IPv4, UDP, and our own header data
     *
     * Method returns the number of packets as integer
     * ---------------------------------------------------------------------------------------------
     */
    static int getNumPackets(String msg) throws Exception
    {
        String header = InetAddress.getLocalHost() + " 0 ";
        int payloadSize = MTU - IPHEAD - UDPHEAD - SEQ - header.length();
        int numPackets = msg.length() / payloadSize;
        if(msg.length() % payloadSize != 0)
            numPackets++;

        System.out.println("num packets = " + numPackets);
        System.out.println("payloadSize = "+ payloadSize);
        System.out.println("msg size = " + msg.length());

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

        for(int i = 0; i < numPackets; i++,currentPacket++)
        {
            if(i == numPackets - 1)
                packets[i] = header + i % 2 + "\r\n" + msg.substring(i * payloadSize, msg.length());
            else
                packets[i] =  header + i % 2 + "\r\n" + msg.substring(i * payloadSize, (currentPacket * payloadSize));
        }
        return packets;
    }
}
