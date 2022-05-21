import java.net.*;

public class UDPListeningService implements Runnable{
    DatagramSocket dso;
    boolean game_ended; // change to false when game ends

    GamePanel gamePanel;

    public UDPListeningService(GamePanel gamePanel) {
        this.gamePanel = gamePanel;

        game_ended = false;
        try {
            dso = new DatagramSocket(Client.playerUDPPort);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void run() {
        while(!game_ended) {
            receiveMessage();
        }
    }


    public void receiveMessage() {
        // receive the rest of the message
        byte[] data = new byte[213];
        String message = "";
        try {
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            dso.receive(paquet);
            // not checking how many bytes were received because
            // we don't know how big the message really is (only that it is max 200 chars)
            message = new String(paquet.getData(), 0, paquet.getLength());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        String sender_id = message.substring(1, 9);
        String message_received = message.substring(10, message.length() - 4);
        // test this -> I'm not sure we are getting rid of only the +++ at the end of the message

        Client.LOGGER.info(String.format("Player %s sent personal message %s", sender_id, message_received));

        gamePanel.perso_chat.add_message_to_panel(message_received, sender_id);
    }
}
