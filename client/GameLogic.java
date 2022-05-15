import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class GameLogic {
    public static int game_id;

    public static int height;
    public static int width;

    public static int nb_ghosts;
    public static int nb_ghosts_left;

    public static String broadcast_ip;
    public static String broadcast_port;

    public static PlayerInfo[] players;
    public static PlayerInfo this_player;


    public static void receiveWelcomeMessage() throws IncorrectMessageException, IOException {
        // receive WELCO
        byte[] welco = new byte[39];
        int res = Client.tcp_socket_reader.read(welco, 0, 39);

        if (res != 39) {
            Client.logIncorrectLengthMessage("WELCO", 39, res);
        }
        String welco_str = Arrays.toString(welco);
        if (!welco_str.startsWith("WELCO")) {
            Client.logIncorrectHeader("WELCO", welco_str);
        }

        // extract the info
        game_id = Client.getShortFromByte(welco[7]);
        height = Client.getIntFromLittleEndian(welco, 9);
        width = Client.getIntFromLittleEndian(welco, 12);
        nb_ghosts = Client.getShortFromByte(welco[13]);
        nb_ghosts_left = nb_ghosts;
        broadcast_ip = welco_str.substring(17, 32);
        broadcast_port = welco_str.substring(32, 36);

        Client.LOGGER.info(String.format("Received WELCO message : broadcast ip : %s, broadcast port : %s" +
                        "\nGame id : %d, height : %d, width %d, nb ghosts %d\n",
                broadcast_ip, broadcast_port, game_id, height, width, nb_ghosts));

        // read player's position
        byte[] posit = new byte[25];
        res = Client.tcp_socket_reader.read(posit, 0, 25);

        if (res != 25) {
            Client.logIncorrectLengthMessage("POSIT", 25, res);
        }
        String posit_str = Arrays.toString(posit);
        if (!posit_str.startsWith("POSIT")) {
            Client.logIncorrectHeader("POSIT", posit_str);
        }

        // extract the info
        String x_str = posit_str.substring(16, 19);
        String y_str = posit_str.substring(20, 23);
        int x = Integer.parseInt(x_str);
        int y = Integer.parseInt(y_str);

        Client.LOGGER.info(String.format("Initial player position : x=%d, y=%d\n", x, y));

        // ask for all the player's info
        players = get_players();
        for (int i = 0; i<4; i++) {
            if (players.length > i && players[i].x == x && players[i].y == y) {
                this_player = players[i];
            }
        }
    }


    //********************
    // MOVEMENTS
    //*******************
    // we made 4 different function instead of one big with boolean to
    // indicate direction because it is easier to manipulate from the UI
    // side (not getting the booleans mixed up)

    /**
     *
     * @param d the number of steps to take
     * @return the new position of the player after the move
     */
    public static int[] moveUp(int d) throws IOException, IncorrectMessageException {
        send_movement_and_log(d, "UPMOV");
        receiveMOVE_answer();
        return new int[]{this_player.x, this_player.y};
    }

    public static int[] moveDown(int d) throws IOException, IncorrectMessageException {
        send_movement_and_log(d, "DOMOV");
        receiveMOVE_answer();
        return new int[]{this_player.x, this_player.y};
    }

    public static int[] moveLeft(int d) throws IOException, IncorrectMessageException {
        send_movement_and_log(d, "LEMOV");
        receiveMOVE_answer();
        return new int[]{this_player.x, this_player.y};
    }

    public static int[] moveRight(int d) throws IOException, IncorrectMessageException {
        send_movement_and_log(d, "RIMOV");
        receiveMOVE_answer();
        return new int[]{this_player.x, this_player.y};
    }

    /**
     * Receives the MOVE! or MOVEF answers
     * Updates x and y with the new position
     * Doesn't update the score if MOVEF (this will be done when the UDP message arrives)
     */
    public static void receiveMOVE_answer() throws IncorrectMessageException, IOException {
        byte[] header = new byte[5];
        int res = Client.tcp_socket_reader.read(header, 0, 5);

        if (res != 5) {
            Client.logIncorrectLengthMessage("MOVE! or MOVEF", 5, res);
        }

        String header_str = Arrays.toString(header);

        if (header_str.equals("MOVE!")) {
            read_new_coordinates();
            Client.LOGGER.info(String.format("Read MOVE! answer. New position x=%d, y=%d\n", this_player.x, this_player.y));
        } else if (header_str.equals("MOVEF")) {
            // the score is handled when receiving the UDP message
            Client.LOGGER.info(String.format("Read MOVE! answer. New position x=%d, y=%d\n", this_player.x, this_player.y));
            res = Client.tcp_socket_reader.read(null, 0, 4); // discard the extra 4 bytes for the score
        } else {
            Client.logIncorrectHeader("MOVE! or MOVEF", header_str);
        }
    }

    public static void read_new_coordinates() throws IOException, IncorrectMessageException {
        byte[] rest_of_message = new byte[11];
        int res = Client.tcp_socket_reader.read(rest_of_message, 0, 11);

        if (res != 11) {
            Client.logIncorrectLengthMessage("MOVE! or MOVEF", 11, res);
        }
        String message = Arrays.toString(rest_of_message);

        this_player.x = Integer.parseInt(message.substring(1, 4));
        this_player.y = Integer.parseInt(message.substring(5, 8));
    }


    public static void send_movement_and_log(int d, String context) throws IOException {
        String message = String.format("%s %d***", context, d);
        byte[] buf = message.getBytes(StandardCharsets.UTF_8);

        Client.tcp_socket_writer.write(buf);
        Client.tcp_socket_writer.flush();
        Client.LOGGER.info(String.format("Sent %s for %d steps, initial position x=%d, y=%d\n",
                context, d, this_player.x, this_player.y));
    }

    //********************
    // UDP reception
    //********************
    /* you need :
    - score
    - caught a ghost
    - received a UDP personnal message
    - received a multicast message
    - received ENDGA
    - received GOBYE
    Then you will need to trigger the UI so that it
    displays the changes
     */

    //**********************
    // Sending messages
    //**********************

    /**
     * Sends a personal message (SEND? header)
     * @param message       the message to send, <200 chars, doesn't contain *** or +++
     * @param recipient_id  the id of the player, == 8 chars
     * @return true if the message was sent, false otherwise
     */
    public static boolean send_personal_message(String message, String recipient_id) {
        return false;
    }

    /**
     * Sends a message to all users (MALL? header)
     * @param message   the message to send, <200 chars, doesn't contain *** or +++
     */
    public static void send_general_message(String message) {

    }


    //****************
    // OTHER MESSAGES
    //***************

    public static void i_quit() {
        // send the IQUIT message and read GOBYE, then kill the client
        // the server should kill the connection ? but check in the server
        // code to be sure. If not, make the server close the sockets, it
        // is much better
    }

    // similar to getPlayersForGame in PregameLogic, but
    // you need to put additional info.
    // i made you a little class underneath for that, but there is
    // maybe another better way for that
    public static PlayerInfo[] get_players() {

        return null;
    }

    public class PlayerInfo {
        String id;
        int x;
        int y;
        int score;

        public PlayerInfo(String id, int x, int y, int score) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.score = score;
        }
    }

}
