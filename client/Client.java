import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

public class Client {
    public final static Logger LOGGER = Logger.getGlobal();

    public static int serverTCPPort = 3490;
    public static String serverName = "lulu";
    public static int playerUDPPort = 4444;

    public static DataInputStream tcp_socket_reader;
    public static DataOutputStream tcp_socket_writer;
    public static Socket tcpSocket;

    // TODO add timeout : if server disconnects or takes too long to respond

    // TODO : add a `sent_start` boolean to be activate when sending the start message.
    // => then any time we try to send a message, check it is OK
    // otherwise, send a new exception (to be caught by the UI to display an error message)
    // + log that we tried to send but wasn't allowed

    public static void main(String[] args) throws IOException, IncorrectMessageException {
        // get connected to the server
        try {
            InetAddress serveur = InetAddress.getByName(serverName);
            tcpSocket = new Socket(serveur, serverTCPPort);

            tcp_socket_reader = new DataInputStream(tcpSocket.getInputStream());
            tcp_socket_writer = new DataOutputStream(tcpSocket.getOutputStream());

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.warning("Can't connect to server\n");
            System.exit(1);
        }
        LOGGER.info("TCP connection established\n");

        // start UI
        javax.swing.SwingUtilities.invokeLater(
                () -> {
                    Ui v = null;
                    try {
                        v = new Ui();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(0);
                    }
                    v.setVisible(true);
                }
        );

//        readNbPlayersAnswer();
//        getAllGamesAndNbOfPlayers();
//        getMazeSizeForGame((short) 2);
//        getMazeSizeForGame((short) 100);
//        createGame("TESTtest");
//        registerToGame((short) 3, "TESTtest");
//        registerToGame((short) 1, "TESTtest");
//        unregisterFromGame();
//        registerToGame((short) 2, "TESTtest");
//        getPlayersForGame((short) 3);
//        getPlayersForGame((short) 1);
//        getPlayersForGame((short) 100);
    }


    /**
     * Converts a byte into a short, if it was originally an uint_8 number
     * (one byte, unsigned).
     * The & 0xFF makes the conversion (otherwise it would be treated as a signed byte)
     *
     * @param b the byte to convert
     * @return the converted short
     */
    public static short getShortFromByte(byte b) {
        return (short) (b & 0xFF); // to convert
    }

    /**
     * Converts a short into 1 byte, unsigned (uint_8 equivalent)
     *
     * @param i the number to convert
     * @return the converted byte
     */
    public static byte getByteFromShort(short i) {
        return (byte) (i & 0xff);
    }

    /**
     * Logs a length error. To be used when the received message is not the right length
     *
     * @param context  a string to explain where the error occurred. Usually the header of the received message
     * @param expected the expected number of bytes
     * @param received the received number of bytes
     * @throws IncorrectMessageException because the message doesn't respect the protocol
     */
    public static void logIncorrectLengthMessage(String context, int expected, int received) throws IncorrectMessageException {
        LOGGER.warning(String.format("ERROR : Reading a %s message, expected %d bytes but only received %d\n",
                context, expected, received));
        throw new IncorrectMessageException("Incorrect message length.");
    }

    /**
     * Logs an error. To be used when the received message doesn't start with the correct header
     *
     * @param expected the expected header
     * @param received the received header
     * @throws IncorrectMessageException because the message doesn't follow protocol
     */
    public static void logIncorrectHeader(String expected, String received) throws IncorrectMessageException {
        LOGGER.warning(String.format("ERROR : Expected message [%s] from " +
                "server, but received message starting with [%s]\n", expected, received));
        throw new IncorrectMessageException(String.format("Wrong header receiving %s message.", expected));
    }
}
