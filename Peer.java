import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;

public class Peer
{
    private static int numEntries;
    private LinkedList<String>[] hashMap;
    private Scanner scan;
    private Scanner hostScan;
    private Scanner songScan;
    private String hostname;
    private String IPAddress;
	
    
    public Peer(String hostInfo, String inform)
    {
    	numEntries = 107;
    	hostScan = new Scanner(hostInfo).useDelimiter("/");
    	scan = new Scanner(inform);
    	// Flush scanner
    	scan.nextLine();
    	hostname = hostScan.next();
    	IPAddress = "/" + hostScan.next();
        hashMap = new LinkedList[numEntries];
        
        for(int i = 0; i < numEntries; i++)
            hashMap[i] = new LinkedList<String>();
        
        while(scan.hasNext())
        {
            String songInfo = scan.nextLine();
            int songHash = Math.abs(songInfo.hashCode());
            int index = songHash % numEntries;
            
            hashMap[index].add(songInfo);
        }
    }
    
    public String getHost()
    {
    	return hostname;
    }
    
    public String getIP()
    {
    	return IPAddress;
    }
    
    public void printHash()
    {
        for(int i = 0; i < numEntries; i++)
            for(String song : hashMap[i])
                System.out.println(song);
    }
    
    public String searchHash(String query)
    {
        String allFiles = "-showall";
        String files = "";
        
        if(query.equals(allFiles))
        {
            for(int i = 0; i < numEntries; i++)
            {
                for(String song : hashMap[i])
                    files += song + "\r\n";
            }
            return files;
        }
        else
        {
            for(int i = 0; i < numEntries; i++)
            {
            	try
            	{
	                for(String song : hashMap[i])
	                {
	                	songScan = new Scanner(song);
	                	String fileName = songScan.next();
	                    if(fileName.contains(query))
	                    	return song;
	    			}
            	}
	            catch(Exception e)
            	{
	            	System.out.println("");
            	}
            }
        }
        
        return "File not found";
    }
}
