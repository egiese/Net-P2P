import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */
public class Launcher
{
    private Scanner scanner;
    private String instanceType,hostName;
    private int port;

    public static void main(String[] args)
    {
        Launcher launcher = new Launcher();
    }

    public Launcher() {
        scanner = new Scanner(System.in);
        System.out.println("Welcome to the P2P application.\nIs this machine a client [c] or a server [s]?");
        instanceType = scanner.next();
        System.out.println("What port number will be used for communication?");
        port = scanner.nextInt();
        System.out.println("What is this hostname of this machine?");
        hostName = scanner.next();
        System.out.println(instanceType + hostName + port);
    }
}
