import java.util.Scanner;

/**
 * Created by Simon on 4/26/17.
 */
public class Launcher
{

    public static void main(String[] args) throws Exception
    {
        Scanner scanner = new Scanner(System.in);
        Scanner tcpScan = new Scanner(System.in);
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
                System.out.println("What should the " + instanceType + " do?\nInform and update [INUP]\nQuery [QUER]\nDownload file [DWNL]\nExit [EXIT]\nBreak control loop [STOP]");
                input = scanner.next().toUpperCase();
                switch (input) {
                    case "INUP":
                        test.sendMessage("INUP");
                        break;
                    case "QUER":
                        test.sendMessage("QUER");
                        break;
                    case "DWNL":
                        System.out.println("Enter host IP address:");
                        String hostIP = tcpScan.nextLine();
                        System.out.println("Enter filename, beginning with [\\], including extension:");
                        String filename = tcpScan.nextLine();
                        System.out.println("Enter target file's absolute path:");
                        String targetFP = tcpScan.nextLine();
                        System.out.println("Enter desired destination path:");
                        String destFP = tcpScan.nextLine();
                        test.startTCPClient("TCP Client", hostIP, port+1, targetFP + filename, destFP + filename);        
                        break;
                    case "EXIT":
                        test.sendMessage("EXIT");
                        break;
                }
            } while (!(input.equals("STOP")));
        }
    }

}
