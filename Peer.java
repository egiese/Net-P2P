import java.util.LinkedList;
import java.util.Scanner;

public class Peer
{
    int numEntries;
    LinkedList<String>[] hashMap;
    Scanner scan;
    
    public Peer(String inform)
    {
        numEntries = 107;
        hashMap = new LinkedList[numEntries];
        scan = new Scanner(inform);
        
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
    
    public void printHash()
    {
        for(int i = 0; i < numEntries; i++)
            for(String song : hashMap[i])
                System.out.println(song);
    }
    
    public String searchHash(String key)
    {
        String allFiles = "all_files";
        String files = "";
        
        if(key.equals(allFiles))
        {
            for(int i = 0; i < numEntries; i++)
            {
                for(String song : hashMap[i])
                    files += song + "\r\n";
            }
            return files;
        }
        else
            for(int i = 0; i < numEntries; i++)
                for(String song : hashMap[i])
                    if(song.contains(key))
                        return song;
                        
        return "File not found";
    }
}
