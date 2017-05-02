# Net-P2P
CS490 Networking - P2P Application

Simon Yawin, Elliot Giese, & Mudassar Shaikh


## How to Compile
The main control of the program is located in the  `Launcher.java` file. When run, this program will walk the user through a series of options in order to create an instance of a Client or Server. The best way to do this is by running the `.jar` file in the `/final` directory from the command line, but it can be run by compiling all of the files in the project and running the `main` loop in `Launcher.java`.

_Please note that, because of the way this program uses and binds to sockets, two instances of this program_ cannot _run on the same machine, as one of the instances will fail to bind to the application port and crash._

## How to run
Upon launching the program, the user will be asked to specify if this instance is a client or a server. From there, the user will specify the hostname of the server (client only), the port that will be used for communication (this must be the same for all instances that wish to communicate with each other), and whether or not the instance will run in "slow mode".

### Slow Mode
Slow mode is a run of the instance that introduces a 4 second delay before the instance sends a packet/ACK. This is used to simulate packet loss and demonstrate that the application can recover and dynamically adjust its timeout window.

## System Documentation
TCP Client will request a file via TCP from the Transient Server.  Initially the Transient Server will listen to and wait for any connections from clients on a certain port. The user will send out a query and they can input the name of the file, ip address of the host, and the path of the file.  Client sends path of file to server.  If the server responds back with a 200 message that means that the file was found on the Transient Server and attempt to transfer the file.  Threading gives the ability for multiple clients to connect and transfer at the same time.  If the server responds back with a 400 message this means that the file was not found and the user will have to select another file or another client and the connection to the server will be closed.  This would return a NULL.

The java.net.Socket class represents a socket, and the java.net.ServerSocket class provides a mechanism for the server program to listen for clients and establish connections with them.
The following steps occur when establishing a TCP connection between two computers using sockets-     The server instantiates a ServerSocket object, denoting which port number communication is to occur on.    The server invokes the accept() method of the ServerSocket class. This method waits until a client connects to the server on the given port.  After the server is waiting, a client instantiates a Socket object, specifying the server name and the port number to connect to.  The constructor of the Socket class attempts to connect the client to the specified server and the port number. If communication is established, the client now has a Socket object capable of communicating with the server.   On the server side, the accept() method returns a reference to a new socket on the server that is connected to the client's socket.  After the connections are established, communication can occur using I/O streams. Each socket has both an OutputStream and an InputStream. The client's OutputStream is connected to the server's InputStream, and the client's InputStream is connected to the server's OutputStream.
The java.net.ServerSocket class is used by server applications to obtain a port and listen for client requests.  One of the constructors is below:

        public ServerSocket(int port) throws IOException: Attempts to create a server socket bound to the specified port. An exception occurs if the port is already bound by another application

Sender.java

the calcTimeoutInterval method is created to calculate the timeout interval based on estimated RTT and DevRTT.  Estimated RTT is added to 4 \*devRTT

calcEstimatedRTT method to calculate estRTT based on start, end, previous estRTT with alpha and beta as constants

calcDevRTT method to calculate devRTT based on the start, end, estRTT, previous devRTT, with constants Alpha, and Beta.

getSeqNum method to create an ACK for a received packet based on its seq number.  It takes the seq number as a parameter, and returns a string with the ACK flag, message terminating CRLFCRLF

getNumPackets method returns number of packets as an integer.  Method takes a message string as a parameter, and calculates the number of packets it will be divided into based on udp, and our header data

makepacket method has a string as a parameter and returns an array of packets.  Each packet will have a seq number of 0 or 1 followed by CRLF(terminate)
