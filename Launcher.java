import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */
public class Launcher
{

    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the P2P application.\nIs this machine a client [c] or a server [s]?");
        String instanceType = scanner.next();
        System.out.println("What port number will be used for communication?");
        int port = scanner.nextInt();
        System.out.println("What is this hostname of this machine?");
        String hostName = scanner.next();
    }

}
