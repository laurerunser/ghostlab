public class GameLogic {
    public static int game_id;

    public static int height;
    public static int width;

    // current position of the player
    public static int x;
    public static int y;

    public static int nb_ghosts;
    public static int nb_ghosts_left;

    public static String broadcast_ip;
    public static String broadcasting_port;


    public static void receiveWelcomeMessage() {
        // set all the variables above with the info
        // contained in the message WELCO and POSIT messages
    }


    //********************
    // MOVEMENTS
    //*******************
    /**
     *
     * @param d the number of steps to take
     * @return the new position of the player after the move
     */
    public static int[] moveUp(int d) {
        return null;
    }

    public static int[] moveDown(int d) {
        return null;
    }

    public static int[] moveLeft(int d) {
        return null;
    }

    public static int[] moveRight(int d) {
        return null;
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
