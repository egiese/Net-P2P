import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */
public class Launcher
{

    public static void main(String[] args) throws Exception
    {
        Scanner scanner = new Scanner(System.in);
        String instanceType,serverHostname,slow;
        instanceType = serverHostname = slow = "";
        int port;
        Boolean slowMode = false;

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

        do
        {
            System.out.println("Operate in slow mode? [yes / no]");
            slow = scanner.next();
        } while (!(slow.equals("yes") || slow.equals("no")));

        switch (slow)
        {
            case "yes":
                slowMode = true;
                break;
            case "no":
                slowMode = false;
                break;
        }


        if(instanceType.equals("server"))
        {
            Server server = new Server(port, slowMode);
            server.serve();
        }
        else {
            String input;
            Client test = new Client(port, serverHostname, slowMode);
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
            } while (!(input.equals("STOP")));
        }
    }

}
