/**
 * Created by Simon on 4/26/17.
 */
interface Receiver
{
//    SEQ Number Info
    int INIT_SEQ_NUM = 0;
    int SEQ_NUM_WIN = 2;

    /*
	 * ------------------------------------------------------------------------------------
	 * Method to create an ACK for a received packed based on its sequence number
	 * Takes sequence number as parameter, and returns a String with the ACK flag raised,
	 * the sequence number it's ACKing, and the message-terminating CRLFCRLF
	 * ------------------------------------------------------------------------------------
	 */
    static String createACK(int sequenceNumber)
    {
        return " 1 " + sequenceNumber + "\r\n\r\n";
    }
}
