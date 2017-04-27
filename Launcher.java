import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */
public class Launcher
{

    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);
        String instanceType,serverHostname = "";
        int port;

        System.out.println("Welcome to the P2P application.");
        do
        {
            System.out.println("Is this machine a client [c] or a server [s]?");
            instanceType = scanner.next();
        } while (!(instanceType.equals("c") || instanceType.equals("s")));

        if(instanceType.equals("c"))
        {
            do
            {
                System.out.println("What is the hostname of the server?");
                serverHostname = scanner.next();
            } while (serverHostname.isEmpty());
        }

        do
        {
            System.out.println("What port number will be used for communication?");
            port = scanner.nextInt();
        } while (port == 0);

    }

}
