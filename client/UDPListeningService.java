import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPListeningService implements Runnable{
    DatagramSocket dso;
    boolean game_ended; // change to false when game ends

    GamePanel gamePanel;

    public UDPListeningService(String ip, int port, GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        game_ended = false;
        try {
            dso = new DatagramSocket(port, InetAddress.getByName(ip));
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
        String message_received = message.substring(10, message.length()-4);
        // test this -> I'm not sure we are getting rid of only the +++ at the end of the message

        Client.LOGGER.info(String.format("Player %s sent personal message %s", sender_id, message_received));

        PlayerInfo sender = null;
        for (int i = 0; i<4; i++) {
            if (GameLogic.players[i].id.equals(sender_id)) {
                sender = GameLogic.players[i];
            }
        }

        // TODO update the UI
        // (even if the sender is the current user !)
        // the ui is not updated anywhere else
    }
}
