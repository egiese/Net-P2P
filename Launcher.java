import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */
public class Launcher
{

    public static void main(String[] args) throws Exception
    {
        Scanner scanner = new Scanner(System.in);
        String instanceType,serverHostname = "";
        int port;

        System.out.println("Welcome to the P2P application.");
        do
        {
            System.out.println("Is this machine a client [client] or a server [server]?");
            instanceType = scanner.next().toLowerCase();
        } while (!(instanceType.equals("client") || instanceType.equals("server")));

        if(instanceType.equals("client"))
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

//        TODO Create instance of either Sender or Server using given information.
//        TODO Loop until "exit".
//        TODO Send commands to objects.
        String input;
        Client test = new Client(port, serverHostname);
        do {
            System.out.println("What should the " + instanceType + " do?\nInform and update [INUP]\nQuery [QUER]\nExit [EXIT]\nBreak control loop [STOP]");
            input = scanner.next().toUpperCase();
            switch (input) {
                case "INUP":
                    test.sendMessage("INUP");
                    break;
                case "QUER":
                    test.sendMessage("QUER");
                    break;
                case "EXIT":
                    test.sendMessage("EXIT");
                    break;
            }
        } while(!(input.equals("STOP")));
    }

}
