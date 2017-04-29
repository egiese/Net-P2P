import java.io.File;
import java.net.*;
import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */

public class Client implements Sender, Receiver
{
    private int portNumber;
    private String serverHostname;
    private InetAddress serverIP;
    private DatagramSocket socket;

    public Client(int portNumber, String receiverHostname) throws Exception
    {
        this.portNumber = portNumber;
        this.serverHostname = receiverHostname;
        this.serverIP = InetAddress.getByName(this.serverHostname);
        this.socket = new DatagramSocket();
    }

    public void sendMessage(String msgType) throws Exception
    {
        byte[] rcvData = new byte[1024];

        System.out.println("Attempting to send to " + serverIP);

        String msg = genReqMsg(msgType);
        String[] packets = Sender.makePacket(msg);

        double estRTT = Sender.INIT_EST_RTT;
        double devRTT = Sender.INIT_DEV_RTT;
        devRTT = Sender.calcDevRTT(0.0, Sender.INIT_EST_RTT, estRTT, devRTT);
        estRTT = Sender.calcEstimatedRTT(0.0, Sender.INIT_EST_RTT, estRTT);
        int timeoutInterval = Sender.calcTimeoutInterval(estRTT, devRTT);

        int currSeqNum = Sender.INIT_SEQ_NUM;

        // Attempt to send pakets via UDP
        for(String p : packets)
        {
            // Sending packets
            byte[] sendData = new byte[p.length()];
            sendData = p.getBytes();
            DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, serverIP, portNumber);

            try{
                socket.send(sendPkt);
            }
            catch (SocketException e){
                System.out.println("Connection interrupted. Waiting for resumed connection.");
            }

            System.out.println("Sending following packet with sequence number " + Sender.getSeqNum(p) + "\n{\n" + p + "\n}\n");
            System.out.println("Waiting for ACK...");

            // Receiving ACKs
            DatagramPacket rcvPkt;
            while(true)
            {
                // Start Timer
                socket.setSoTimeout(timeoutInterval);
                double startTime = System.nanoTime() / 1000000;
                rcvPkt = new DatagramPacket(rcvData, rcvData.length);

                // Try to receive a packet
                try{
                    socket.receive(rcvPkt);
                }
                // If timeout
                catch(SocketTimeoutException e){
                    double endTime = System.nanoTime() / 1000000;
                    System.out.println("Timeout!");
                    // Recalculate timeout interval
                    devRTT = Sender.calcDevRTT(startTime, endTime, estRTT, devRTT);
                    estRTT = Sender.calcEstimatedRTT(startTime, endTime, estRTT);
                    timeoutInterval = Sender.calcTimeoutInterval(estRTT, devRTT);
                    // Resend packet
                    try{
                        socket.send(sendPkt);
                    }
                    catch (SocketException se){
                        System.out.println("Connection interrupted. Waiting for resumed connection.");
                    }
                    // Restart loop
                    continue;
                }
                double endTime = System.nanoTime() / 1000000;
                System.out.println("ACK received!");
                //Recalculate timeout interval
                devRTT = Sender.calcDevRTT(startTime, endTime, estRTT, devRTT);
                estRTT = Sender.calcEstimatedRTT(startTime, endTime, estRTT);
                timeoutInterval = Sender.calcTimeoutInterval(estRTT, devRTT);
                // Check new sequence number
                int newSeqNum = Sender.getSeqNum(new String(rcvPkt.getData()));
                // If incorrect sequence number
                if(newSeqNum != currSeqNum)
                {
                    System.out.println("Wrong sequence number.\nExpected " + currSeqNum + " got " + newSeqNum + ".");
                    // Resend packet
                    try{
                        socket.send(sendPkt);
                    }
                    catch (SocketException se){
                        System.out.println("Connection interrupted. Waiting for resumed connection.");
                    }
                    // Restart loop
                    continue;
                }
                // Turn off timer
                socket.setSoTimeout(0);
                // Increment sequence number
                currSeqNum = (currSeqNum + 1) % Sender.SEQ_NUM_WIN;
                // End loop
                break;
            }

            String ACK = new String(rcvPkt.getData());
            int sequenceNum = Sender.getSeqNum(ACK);
            int rcvpktsize = rcvPkt.getLength();
            InetAddress clientIP = rcvPkt.getAddress();
            int clientPort = rcvPkt.getPort();
            String ack = new String(rcvPkt.getData());

            System.out.println("ACK = \n[\n" + ack + "\n]");
            System.out.println("Sequence number = " + sequenceNum);
            System.out.println("Sender IP address: " + clientIP);
            System.out.println("Sender port: " + clientPort);
            System.out.println("Packet size: " + rcvpktsize + "\n\n\n");
        }
//        this.receiveMessage();
    }

    /*
	 * ---------------------------------------------------------------------------------------------
	 * This method takes a String parameter, which will denote the message type to server
	 * Message always begins with the flag number and the client's InetAddress, followed by CRLF
	 *
	 * "INUP" is for inform and update
	 * 		This portion scans a users shared file --- chosen by GUI later on --- and appends
	 * 		file name and file size (in bytes) of every file in the folder, followed by CRLF
	 * "QUER" is to query for file(s)
	 * "EXIT" is for exit --- using "$$$$$$$$" as placeholder message for now
	 *
	 * Method returns the overall message String
	 * ---------------------------------------------------------------------------------------------
	 */
    private static String genReqMsg(String msgType) throws Exception
    {
        Scanner scan = new Scanner(System.in);
        String msg = "";

        switch(msgType.toUpperCase())
        {
            case "INUP":
                msg += "INUP " + InetAddress.getLocalHost() + "\r\n";
                System.out.println("Please enter the absolute path of the sharing folder.");
                File folder = new File(scan.next());
                File[] sharedFiles = folder.listFiles();

                for(File file : sharedFiles)
                    msg += file.getName() + " " + file.length() + " " + file.getPath() + "\r\n";
                break;
            case "QUER":
                msg += "QUER " + InetAddress.getLocalHost() + "\r\n";
                System.out.println("Name of file to query? (Or type -showall to view entire directory");
                String input = scan.next();
                msg += input + "\r\n";
                break;
            case "EXIT":
                msg += "EXIT " + InetAddress.getLocalHost() + "\r\n";
                msg += "$$$$$$$$";
                break;
        }
        msg += "\r\n";

        return msg;
    }
}
