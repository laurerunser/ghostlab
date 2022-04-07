package client;

import java.io.*; 
import java.net.*;
/**
 * Client
 */
public class Client {
    public static void main(String[] args) {
        Joueur j = new Joueur(args[0], args[1]); //Objet representant le joueur
        //Connexion au serveur
        Socket socket;
        
        DataInputStream userInput;
        PrintStream theOutputStream;

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
}