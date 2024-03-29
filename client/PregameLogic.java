import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PregameLogic {

    /**
     * Sends the [GAME?...] message and reads the answer
     *
     * @return an array filled with the number of players for each available game
     * value will be -1 if the server doesn't mention the game
     * (e.g. bc it doesn't exist or bc it has already been created)
     * @throws IOException               if there is a problem with the socket
     * @throws IncorrectMessageException if the server's messages don't follow protocol
     */
    public static int[] getAllGamesAndNbOfPlayers() throws IOException, IncorrectMessageException {
        // make the message
        byte[] message = "GAME?***".getBytes(StandardCharsets.UTF_8);

        // send the message
        Client.tcp_socket_writer.write(message);
        Client.tcp_socket_writer.flush();
        Client.LOGGER.info("Sent [GAMES?] message\n");

        // read the server's answer and return the result
        return readNbPlayersAnswer();
    }

    /**
     * Reads the [GAME...] and [OGAME...] messages from the server
     *
     * @return an array filled with the number of players for each available game
     * *          value will be -1 if the server doesn't mention the game
     * *          (e.g. bc it doesn't exist or bc it has already been created)
     * @throws IOException               if there is a problem with the socket
     * @throws IncorrectMessageException if the server messages don't follow protocol.
     *                                   We are testing :
     *                                   -> if the [GAMES] or [OGAME] headers are at the beginning of the message
     *                                   -> if the length of the message is correct
     *                                   Other problems with the message are not taken into account
     */
    public static int[] readNbPlayersAnswer() throws IOException, IncorrectMessageException {
        Client.LOGGER.info("Reading the server's answer to [GAMES?] request\n");
        // create an array and fill it with -1
        int[] games = new int[256];
        Arrays.fill(games, -1);

        // read the first [GAMES] message and check it has the right length
        byte[] first_message = new byte[10];
        int res = Client.tcp_socket_reader.read(first_message, 0, 10);
        if (res != 10) {
            Client.logIncorrectLengthMessage("GAMES", 10, res);
        }

        // check that the message starts with "GAMES "
        String msgHeader = new String(first_message, 0, 5);
        if (!msgHeader.equals("GAMES")) {
            Client.logIncorrectHeader("GAMES", msgHeader);
        }

        // read the total number of available games
        short nb_games = Client.getShortFromByte(first_message[6]);
        Client.LOGGER.info(String.format("Server says %d games are available\n", nb_games));

        // read the nb of players for each game
        for (int i = 0; i < nb_games; i++) {
            // read the message from the tcp reader and check it has the right length
            byte[] message = new byte[12]; // to store the [OGAME] message
            res = Client.tcp_socket_reader.read(message, 0, 12);
            if (res != 12) {
                Client.logIncorrectLengthMessage("OGAME", 12, res);
            }

            // check that the header is correct
            String header = new String(message);
            if (!header.startsWith("OGAME")) {
                Client.logIncorrectHeader("OGAME", header.substring(0, 5));
            }
            // read the id of the message from the byte array
            short id = Client.getShortFromByte(message[6]);
            // read the number of players from the byte array
            games[id] = Client.getShortFromByte(message[8]);

            Client.LOGGER.info(String.format("Game id : %d has %d players\n", id, games[id]));
        }

        Client.LOGGER.info("Done reading server answer to [GAME?]\n");
        return games;
    }

    /**
     * Asks the server to create a new game
     *
     * @param id the pseudo of the user. Exactly 8 chars
     * @return the number of the game created, or -1 if the server says error
     */
    public static int createGame(String id) {
        // make the message
        String m = "NEWPL " + id + " " + String.format("%04d", Client.playerUDPPort) + "***";
        System.out.println(m.length());
        byte[] message = m.getBytes(StandardCharsets.UTF_8);

        // send the message
        try {
            Client.tcp_socket_writer.write(message);
            Client.tcp_socket_writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        Client.LOGGER.info(String.format("Sent [NEWPL] message with id = %s and port = %d\n", id, Client.playerUDPPort));

        // receive the server's answer and return result
        int res = 0;
        try {
            res = receiveREGOKorREGNO();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return res;
    }

    /**
     * Receives a [REGOK] or [REGNO] answer from the server
     *
     * @return the id of the game the user registered to if REGOK , or -1 if REGNO
     * @throws IOException               if there is a problem with the socket
     * @throws IncorrectMessageException if the server's message don't follow protocol
     *                                   we are testing :
     *                                   -> the beginning of the message is [REGNO] or [REGOK] and has the right length
     *                                   -> and for REGOK, if the rest of the message has the right length
     */
    public static int receiveREGOKorREGNO() throws IOException, IncorrectMessageException {
        byte[] byte_header = new byte[5];
        // read first 5 bytes to know which answer it is
        int res;
        res = Client.tcp_socket_reader.read(byte_header, 0, 5);
        if (res != 5) {
            Client.logIncorrectLengthMessage("REGNO or REGOK", 5, res);
        }

        int game_id;
        String header = new String(byte_header);
        if (header.equals("REGNO")) {
            Client.LOGGER.info("Received [REGNO] answer\n");
            // read the last "***" of the message
            res = Client.tcp_socket_reader.read(byte_header, 0, 3);
            if (res != 3) {
                Client.logIncorrectLengthMessage("REGNO", 3, res);
            }
        } else if (header.equals("REGOK")) {
            // read the rest of the message and extract game number
            res = Client.tcp_socket_reader.read(byte_header, 0, 5);
            if (res != 5) {
                Client.logIncorrectLengthMessage("REGNO or REGOK", 5, res);
            }

            game_id = Client.getShortFromByte(byte_header[1]); // first is a space, then the number
            Client.LOGGER.info(String.format("Received [REGOK] : game id is %d\n", game_id));
            return game_id;
        } else {
            Client.logIncorrectHeader("REGNO or REGOK", header);
        }
        return -1;
    }

    /**
     * Asks the server to register the player to the game
     *
     * @param id     the pseudo of the user. Exactly 8 chars
     * @param gameId the if of the game to register to
     * @return the number of the game, or -1 if the server says error
     */
    public static int registerToGame(short gameId, String id) throws IOException, IncorrectMessageException {
        // make the message
        String m = "REGIS " + id + " " + String.format("%04d", Client.playerUDPPort) + " ";
        byte[] start_of_message = m.getBytes(StandardCharsets.UTF_8);

        // copy stuff into the array
        byte[] message = new byte[25];
        System.arraycopy(start_of_message, 0, message, 0, start_of_message.length);
        message[21] = Client.getByteFromShort(gameId);
        System.arraycopy("***".getBytes(StandardCharsets.UTF_8), 0, message, 22, 3);

        // send the message
        Client.tcp_socket_writer.write(message);
        Client.tcp_socket_writer.flush();
        Client.LOGGER.info(String.format("Sent [REGIS] message for game = %d with id = %s and port = %d\n",
                gameId, id, Client.playerUDPPort));

        // receive the server's answer and return result
        return receiveREGOKorREGNO();
    }

    /**
     * Asks the server to unregister the player from a game
     *
     * @return true if successful, false otherwise
     * @throws IOException               if there is a problem with the socket
     * @throws IncorrectMessageException if the server's messages don't follow protocol
     *                                   -> we are only testing that the message starts with [UNROK] or [DUNNO]
     *                                   Other message format errors are not taken into account.
     */
    public static boolean unregisterFromGame() throws IOException, IncorrectMessageException {
        // send message
        byte[] message = "UNREG***".getBytes(StandardCharsets.UTF_8);
        Client.tcp_socket_writer.write(message);
        Client.tcp_socket_writer.flush();
        Client.LOGGER.info("Sent [UNREG] message\n");

        // read server's answer
        boolean success = false;
        byte[] byte_header = new byte[5];
        int res = Client.tcp_socket_reader.read(byte_header, 0, 5);
        if (res != 5) {
            Client.logIncorrectLengthMessage("UNROK or DUNNO", 5, res);
        }

        String header = new String(byte_header);
        if (header.equals("UNROK")) {
            success = true;
            Client.LOGGER.info("Received [UNROK] answer : successfully unregistered\n");
            // read the last part of the message (space + number + ***)
            res = Client.tcp_socket_reader.read(byte_header, 0, 5);
            if (res != 5) {
                Client.logIncorrectLengthMessage("UNROK", 5, res);
            }
        } else if (header.equals("DUNNO")) {
            // read the rest of the message (the ***)
            res = Client.tcp_socket_reader.read(byte_header, 0, 3);
            if (res != 3) {
                Client.logIncorrectLengthMessage("DUNNO", 3, res);
            }
            Client.LOGGER.info("Received [DUNNO] : unregistration unsuccessfull");
        } else {
            Client.logIncorrectHeader("UNROK or DUNNO", header);
        }

        return success;
    }

    /**
     * Sends the [START...] message to the server
     */
    public static void sendStart() throws IOException, IncorrectMessageException, InterruptedException {
        byte[] message = "START***".getBytes(StandardCharsets.UTF_8);
        Client.tcp_socket_writer.write(message);
        Client.tcp_socket_writer.flush();
        Client.LOGGER.info("Sent [START] message\n");

        // wait to receive the WELCO message
        // The game will then be handled by the GameLogic class
        GameLogic.receiveWelcomeMessage();
    }

    /**
     * Asks the server for the list of players for a game
     *
     * @param gameId the id of the game
     * @return a list of the players' ids
     * @throws IOException               if there is a problem with the socket
     * @throws IncorrectMessageException if the server's messages don't follow protocol
     */
    public static String[] getPlayersForGame(short gameId) throws IOException, IncorrectMessageException {
        // make the message
        String m = "LIST? ";
        byte[] message_to_send = new byte[10];
        System.arraycopy(m.getBytes(StandardCharsets.UTF_8), 0, message_to_send, 0, 6);
        message_to_send[6] = Client.getByteFromShort(gameId);
        System.arraycopy("***".getBytes(StandardCharsets.UTF_8), 0, message_to_send, 7, 3);

        // send the message
        Client.tcp_socket_writer.write(message_to_send);
        Client.tcp_socket_writer.flush();
        Client.LOGGER.info(String.format("Sent [LIST?] message for game = %d\n", gameId));

        // read the first [LIST!] or [DUNNO] message and check it has the right length
        Client.LOGGER.info("Reading answer to [LIST?] request\n");
        byte[] byte_header = new byte[5];
        int res = Client.tcp_socket_reader.read(byte_header, 0, 5);
        if (res != 5) {
            Client.logIncorrectLengthMessage("[DUNNO or LIST!]", 5, res);
        }

        String header = new String(byte_header);
        if (header.equals("DUNNO")) { // game doesn't exist
            res = Client.tcp_socket_reader.read(byte_header, 0, 3);
            if (res != 3) {
                Client.logIncorrectLengthMessage("SIZE!", 3, res);
            }
            Client.LOGGER.info("Received [DUNNO] answer : this game doesn't exist\n");
            return null;
        } else if (!header.equals("LIST!")) { // wrong header
            Client.logIncorrectHeader("DUNNO or LIST!", header);
        }

        // if we got to here, then the header was [LIST!]
        return readPlayersIds();
    }

    /**
     * Reads the rest of the [LIST!] message (after the header == from position 5 on)
     * and all the [PLAYR] messages that follow
     *
     * @return the list of IDs for the players in the game
     * @throws IOException               if there is a problem with the socket
     * @throws IncorrectMessageException if the server's messages don't follow protocol
     *                                   we are testing if :
     *                                   -> the [PLAYR] headers are at the beginning of the messages
     *                                   -> the [PLAYR] messages have the right length
     *                                   -> the rest of the [LIST!] message has the right length (from position 5 on)
     *                                   Other message format problems are not taken into account
     */
    public static String[] readPlayersIds() throws IOException, IncorrectMessageException {
        int res;
        // read the rest of the message
        byte[] rest_of_message = new byte[7];
        res = Client.tcp_socket_reader.read(rest_of_message, 0, 7);
        if (res != 7) {
            Client.logIncorrectLengthMessage("LIST!", 7, res);
        }

        // read the total number of players (at position 3 bc we read the header in a different byte buffer)
        short nb_players = Client.getShortFromByte(rest_of_message[3]);
        Client.LOGGER.info(String.format("Server says there are %d players in this game\n", nb_players));
        String[] player_ids = new String[nb_players];

        // read the id for each player
        Client.LOGGER.info("Reading list of ids : \n");
        for (int i = 0; i < nb_players; i++) {
            // read the message and check it has the right length
            byte[] message = new byte[17];
            res = Client.tcp_socket_reader.read(message, 0, 17);
            if (res != 17) {
                Client.logIncorrectLengthMessage("[PLAYR]", 17, res);
            }

            // check that the header is correct
            String header2 = new String(message);
            if (!header2.startsWith("PLAYR")) {
                Client.logIncorrectHeader("PLAYR", header2.substring(0, 5));
            }

            // read the id from the message (starts at position 6; length = 8)
            player_ids[i] = new String(message, 6, 8);
            Client.LOGGER.info(String.format("%d : %s\n", i, player_ids[i]));
        }
        Client.LOGGER.info("Done reading server answer to [LIST?]\n");
        return player_ids;
    }

    /**
     * Asks the server for the size of the maze for that game
     *
     * @param gameId the id of the game
     * @return [width, height] of the maze; {-1; -1} if the game doesn't exist
     */
    public static int[] getMazeSizeForGame(short gameId) throws IncorrectMessageException, IOException {
        // make the message
        String m = "SIZE? ";
        byte[] message_to_send = new byte[10];
        System.arraycopy(m.getBytes(StandardCharsets.UTF_8), 0, message_to_send, 0, 6);
        message_to_send[6] = Client.getByteFromShort(gameId);
        System.arraycopy("***".getBytes(StandardCharsets.UTF_8), 0, message_to_send, 7, 3);

        // send the message
        Client.tcp_socket_writer.write(message_to_send);
        Client.tcp_socket_writer.flush();
        Client.LOGGER.info(String.format("Sent [SIZE?] message for game = %d\n", gameId));


        // read the first [LIST!] or [DUNNO] message and check it has the right length
        Client.LOGGER.info("Reading the server's answer to [SIZE?] request\n");
        byte[] byte_header = new byte[5];
        int res = Client.tcp_socket_reader.read(byte_header, 0, 5);
        if (res != 5) {
            Client.logIncorrectLengthMessage("[DUNNO or SIZE!!]", 5, res);
        }

        String header = new String(byte_header);
        if (header.equals("DUNNO")) { // game doesn't exist
            res = Client.tcp_socket_reader.read(byte_header, 0, 3); // read the rest of the message
            if (res != 3) {
                Client.logIncorrectLengthMessage("SIZE!", 3, res);
            }
            Client.LOGGER.info("Received [DUNNO] answer : this game doesn't exist\n");
            return new int[]{-1, -1};
        } else if (!header.equals("SIZE!")) { // wrong header
            Client.logIncorrectHeader("[DUNNO] or [SIZE!]", header);
        }

        // if we get to here, then the header was [SIZE!] and we can read the answer

        // read the rest of the message and check the length
        byte[] message = new byte[11];
        res = Client.tcp_socket_reader.read(message, 0, 11);
        if (res != 11) {
            Client.logIncorrectLengthMessage("SIZE!", 11, res);
        }

        // make a ByteBuffer and put it in little endian
        ByteBuffer b = ByteBuffer.wrap(message);
        b.order(ByteOrder.LITTLE_ENDIAN);

        int[] size = new int[2];
        // get the height
        size[0] = b.getShort(3);

        // get the width
        // get the width
        size[1] = b.getShort(6);

        Client.LOGGER.info(String.format("Size of maze is h=%d, w=%d\n", size[0], size[1]));
        return size;
    }
}
