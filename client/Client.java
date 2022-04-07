package client;

import java.io.*; 
import java.net.*;

public class Client {
    public static int serverTCPPort = 3490;
    public static String serverName = "lulu";
    public static BufferedReader tcp_socket_reader;
    public static PrintStream tcp_socket_writer;
    public static Socket tcpSocket;

    public static void main(String[] args) {
        // get connected to the server
        try {
            InetAddress serveur = InetAddress.getByName(serverName);
            tcpSocket = new Socket(serveur, serverTCPPort);

            tcp_socket_reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcp_socket_writer = new PrintStream(tcpSocket.getOutputStream());

        } catch (Exception e) {
            e.printStackTrace();
        }

        // start UI
        // TODO
    }

    /** Sends the [GAME?...] message and reads the answer
     * @return an array filled with the number of players for each available game
     *          value will be -1 if the server doesn't mention the game
     *          (e.g. bc it doesn't exist or bc it has already been created)
     */
    public static int[] getAllGamesAndNbOfPlayers() {
        return null;
    }

    /**
     * Reads the [GAME...] and [OGAME...] messages from the server
     * @return an array filled with the number of players for each available game
     *      *          value will be -1 if the server doesn't mention the game
     *      *          (e.g. bc it doesn't exist or bc it has already been created)
     */
    public static int[] readNbPlayersAnswer() {

    }

    /** Asks the server to create a new game
     *
     * @param id    the pseudo of the user. Exactly 8 chars
     * @return the number of the game create, or -1 if the server says error
     */
    public static int createGame(String id) {
        return -1;
    }

    /** Asks the server to register the player to the game
     *
     * @param id    the pseudo of the user. Exactly 8 chars
     * @param gameId the if of the game to register to
     * @return the number of the game, or -1 if the server says error
     */
    public static int registerToGame(int gameId, String id) {
        return -1;
    }

    /**
     * Asks the server to unregister the player from the game
     * @param gameId    the id of the game to unregister from
     * @return  true if ok, false if error
     */
    public static boolean unregisterFromGame(int gameId) {
        return false;
    }

    /**
     * Sends the [START...] message to the server
     */
    public static void sendStart() {

    }

    /**
     * Gets the list of players pseudos for the game
     * @param gameId    the id of the game
     * @return  an array of the pseudos
     */
    public static String[] getPlayersForGame(int gameId) {
        return null;
    }

    /**
     * Asks the server for the size of the maze for that game
     * @param gameId    the id of the game
     * @return  [width, height] of the maze
     */
    public static int[] getMazeSizeForGame(int gameId) {
        return null;
    }
}
