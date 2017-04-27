import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */
interface Sender
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


    static int calcTimeoutInterval(double esimtatedRTT, double devRTT)
    {
        return ((int) (esimtatedRTT + 4 * devRTT));
    }

    static double calcEstimatedRTT(double startInMillis, double endInMillis, double estimatedRTT)
    {
        double sRTT = endInMillis - startInMillis;
        return  ((1 - alpha) * estimatedRTT) + (alpha * sRTT);
    }

    static double calcDevRTT(double startInMillis, double endInMillis, double estimatedRTT, double devRTT)
    {
        double sRTT = endInMillis - startInMillis;
        return ((1 - beta) * devRTT) + (beta * Math.abs(sRTT - estimatedRTT));
    }

    static int getSeqNum(String packet)
    {
        Scanner seq = new Scanner(packet);
        seq.next();
        seq.next();
        return Integer.parseInt(seq.next());
    }

    static int getNumPackets(String msg) throws Exception
    {
        String header = InetAddress.getLocalHost() + " 0 ";
        int payloadSize = MTU - IPHEAD - UDPHEAD - SEQ - header.length();
        int numPackets = (int) Math.ceil(msg.length() / payloadSize);
        return numPackets;
    }

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
