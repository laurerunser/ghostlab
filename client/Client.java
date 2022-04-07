package client;

import java.io.*; 
import java.net.*;

public class Client {

    public static void main(String[] args) {

        Socket socket;
        try {
            InetAddress serveur = InetAddress.getByName(args[0]);
            socket = new Socket(serveur, j.getPort());

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintStream out = new PrintStream(socket.getOutputStream());

            out.println(args[1]);
            System.out.println(in.readLine());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    int[] getAllGamesAndNbOfPlayers() {
        return null;
    }

    int createGame(String id) {
        return -1;
    }

    int registerToGame(int gameId, String id) {
        return -1;
    }

    boolean unregisterFromGame(int gameId) {
        return false;
    }

    void sendStart() {

    }

    String[] getPlayersForGame(int gameId) {
        return null;
    }

    // returns (width, height)
    int[] getMazeSizeForGame(int gameId) {
        return null;
    }
}
