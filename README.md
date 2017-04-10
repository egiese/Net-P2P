# Net-P2P
CS490 Networking - P2P Application

## How to Test
The main loops of the P2PClient and P2PServer files are meant to represent the main loops of the sender and receiver for our implementation of RDT 3.0, respectively.

1. Specify the name of the server in the source code of both the P2PClient and P2PServer source files by modifying the `serverHostname` variable. Be sure that these match.
2. Specify the full file-path of a folder containing files on your "client" machine in the "sharePath" variable in the P2PClient source code. The contents of this folder will be sent in a mock `INUP` request message to the "server" machine in order to test the protocol.
3. The client will generate the body of the `INUP` request and then divide it into packets. It will then attempt to send packets to the server using our implementation of RDT 3.0. It will state when it sends a packet, if a timeout event occurs, if an ACK is received, and if that ACK has the wrong sequence number. The sender will finish its loop once it has received an ACK for the final packet. It will also state the total size of the message sent to the server.
4. The server will continually wait to receive packets from the client. It will state when a packet is received, when the packet has the wrong sequence number, and when it is sending an ACK message. The server will extract the appplication layer data from each packet it receives and continually buffer it. Each time the buffer is updated, it will also list the amount of application data it has received so far. The loop will never terminate, but rather continue to wait for packets to be received. _Please note that this is not indicative of the final deliverable._
5. To verify that the entire message has been received, check that the sent message size on the client side matches the last value for the received file size on the server side.
