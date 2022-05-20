import javax.swing.*;
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
    public static int broadcast_port;

    public static PlayerInfo[] players;
    public static PlayerInfo this_player;

    public static GamePanel game_panel;

    public static void receiveWelcomeMessage() throws IncorrectMessageException, IOException, InterruptedException {
        // receive WELCO
        byte[] welco = new byte[39];
        int res = Client.tcp_socket_reader.read(welco, 0, 39);

        if (res != 39) {
            Client.logIncorrectLengthMessage("WELCO", 39, res);
        }
        String welco_str = new String(welco);
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
        broadcast_port = Integer.parseInt(welco_str.substring(31, 35));

        Client.LOGGER.info(String.format("Received WELCO message : broadcast ip : %s, broadcast port : %d" +
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
        get_players();
        for (int i = 0; i < 4; i++) {
            if (players.length > i && players[i].x == x && players[i].y == y) {
                this_player = players[i];
            }
        }

        // make the UDP services
        make_udp_threads();

        // make the UI
        game_panel = new GamePanel(width, height, x, y);
        Client.ui.set_game_panel(game_panel);

    }

    public static void make_udp_threads() throws InterruptedException {
        UDPListeningService udp_service = new UDPListeningService(broadcast_ip, broadcast_port,
                game_panel);
        MulticastListeningService multicast_service = new MulticastListeningService(broadcast_ip,
                broadcast_port, udp_service, game_panel);

        Thread t = new Thread(udp_service);
        Thread t2 = new Thread(multicast_service);

        t.join();
        t2.join();
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
    public static int[] moveUp(int d) {
        try {
            send_movement_and_log(d, "UPMOV");
            receiveMOVE_answer();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return new int[]{this_player.x, this_player.y};
    }

    public static int[] moveDown(int d) {
        try {
        send_movement_and_log(d, "DOMOV");
        receiveMOVE_answer();
        } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
    }
        return new int[]{this_player.x, this_player.y};
    }

    public static int[] moveLeft(int d) {
        try {
        send_movement_and_log(d, "LEMOV");
        receiveMOVE_answer();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return new int[]{this_player.x, this_player.y};
    }

    public static int[] moveRight(int d) {
        try {
        send_movement_and_log(d, "RIMOV");
        receiveMOVE_answer();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
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
        // send the message
        String mess = String.format("SEND? %s %s***", recipient_id, message);
        byte[] buf = message.getBytes(StandardCharsets.UTF_8);

        try {
            Client.tcp_socket_writer.write(buf);
            Client.tcp_socket_writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        Client.LOGGER.info("Sent SEND? message");

        // read the server's answer
        buf = new byte[8];
        try {
            int res = Client.tcp_socket_reader.read(buf, 0, 8);
            String answer = new String(buf);
            if (answer.equals("SEND!***")) {
                Client.LOGGER.info("Message was sent !");
                return true;
            } else if (answer.equals("NSEND***")) {
                Client.LOGGER.info("Message couldn't be send !");
                return false;
            } else {
                if (res != 8) {
                    Client.logIncorrectLengthMessage("SEND! or NSEND", 8, res);
                } else {
                    Client.logIncorrectHeader("SEND! or NSEND", answer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return false; // never executed : if error, program terminates,
                    // otherwise returns true or false in the second try block
    }

    /**
     * Sends a message to all users (MALL? header)
     * @param message   the message to send, <200 chars, doesn't contain *** or +++
     */
    public static void send_general_message(String message) {
        String mess = String.format("MALL? %s***", message);
        byte[] buf = message.getBytes(StandardCharsets.UTF_8);

        try {
            Client.tcp_socket_writer.write(buf);
            Client.tcp_socket_writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        Client.LOGGER.info("Sent MALL? message");
    }


    //****************
    // OTHER MESSAGES
    //***************

    public static void i_quit() {
        // send message
        byte[] buf = "IQUIT***".getBytes(StandardCharsets.UTF_8);
        try {
            Client.tcp_socket_writer.write(buf);
            Client.tcp_socket_writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        Client.LOGGER.info("Sent IQUIT");

        // no need to read the answer, the server can only acknowledge
        // since we are going to terminate the entire program soon,
        // no need to close all the sockets and reader/writer
    }

    public static void get_players() {
        send_glis();
        Client.LOGGER.info("Sent GLIS? message");

        int nb_players = read_first_glis_answer();
        Client.LOGGER.info(String.format("GLIS! : %d players", nb_players));

        players = new PlayerInfo[nb_players];
        for (int i = 0; i<nb_players; i++) {
            players[i] = read_player_info();
        }

    }

    public static void send_glis() {
        byte[] buf = "GLIS?***".getBytes(StandardCharsets.UTF_8);

        try {
            Client.tcp_socket_writer.write(buf);
            Client.tcp_socket_writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static int read_first_glis_answer() {
        byte[] buf = new byte[10];

        int nb = 0;
        try {
            int res = Client.tcp_socket_reader.read(buf, 0, 10);
            if (res != 10) {
                Client.logIncorrectLengthMessage("GLIS!", 10, res);
            } else if (! new String(buf).startsWith("GLIS!")) {
                Client.logIncorrectHeader("GLIS!", new String(buf));
            } else {
                nb = Client.getShortFromByte(buf[6]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return nb;
    }

    public static PlayerInfo read_player_info() {
        byte[] buf = new byte[30];

        // read the message
        String answer = "";
        try {
            int res = Client.tcp_socket_reader.read(buf, 0 , 30);
            answer = new String(buf);

            if (res != 30) {
                Client.logIncorrectLengthMessage("GPLYR", 30, res);
            } else if (!answer.startsWith("GPLYR")) {
                Client.logIncorrectHeader("GPLYR", answer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // extract the info
        String id = answer.substring(6, 14);
        int x = Integer.parseInt(answer.substring(15, 18));
        int y = Integer.parseInt(answer.substring(19, 22));
        int score = Integer.parseInt(answer.substring(23, 27));

        return new PlayerInfo(id, x, y, score);
    }
}
