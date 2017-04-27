import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */

public class Client implements Sender, Receiver
{
    int portNumber;
    String receiverHostname;
    InetAddress receiverIP;
    Scanner scan,seq;
    DatagramSocket sendSkt;
}
