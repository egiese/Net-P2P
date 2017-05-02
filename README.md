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
