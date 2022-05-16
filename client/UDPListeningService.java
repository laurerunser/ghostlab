import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPListeningService implements Runnable {
    public String ip;
    public int port;

    DatagramSocket dso;

    public UDPListeningService(String multicast_ip, int multicast_port) {
        this.ip = multicast_ip;
        this.port = multicast_port;

        try {
            dso = new DatagramSocket(port);
        } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
        }
    }

    public void run() {
        String header = receiveHeader();
        if (header.startsWith("SCORE")) {
            updateScore();
        } else if (header.equals("GHOST")) {
            updateGhostPosition();
        } else if (header.equals("ENDGA")) {
            signalEndgame();
        } else if (header.equals("MESSA")) {
            receiveGeneralMessage();
        } else if (header.equals("MESSP")) {
            receivePersonalMessage();
        } else { // incorrect header
            try {
                Client.logIncorrectHeader("a UDP header", header);
            } catch (IncorrectMessageException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public String receiveHeader() {
        try {
            byte[] data = new byte[5];
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            dso.receive(paquet);
            if (paquet.getLength() != 5) {
                Client.logIncorrectLengthMessage("a UDP header", 5, paquet.getLength());
            }
            String header = new String(paquet.getData(), 0, paquet.getLength());
            Client.LOGGER.info(String.format("Receive UDP header %s\n", header));
            return header;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null; // if the reception fails, the program terminates anyway
    }

    public void updateScore() {
        // receive the rest of the message
        byte[] data = new byte[27];
        String message = "";
        try {
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            dso.receive(paquet);
            if (paquet.getLength() != 27) {
                Client.logIncorrectLengthMessage("SCORE", 27, paquet.getLength());
            }
            message = new String(paquet.getData(), 0, paquet.getLength());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // extract the information
        String playerID = message.substring(1, 9);
        int new_score = Integer.parseInt(message.substring(10, 14));
        int ghost_x = Integer.parseInt(message.substring(15, 19));
        int ghost_y = Integer.parseInt(message.substring(20, 24));

        Client.LOGGER.info(String.format("Player %s, new score %d, caught ghost at pos x=%d, y=%d\n",
                playerID, new_score, ghost_x, ghost_y));

        // update the info in GameLogic
        if (GameLogic.this_player.id.equals(playerID)) {
            GameLogic.this_player.score = new_score;
        } else {
            for (GameLogic.PlayerInfo p : GameLogic.players) {
                if (p.id.equals(playerID)) {
                    p.score = new_score;
                }
            }
        }
        // TODO : update the UI with the new score
        // TODO : launch an animation that shows the ghost dying in its coordinates
    }

    public void updateGhostPosition() {

    }

    public void signalEndgame() {

    }

    public void receiveGeneralMessage() {

    }

    public void receivePersonalMessage() {

    }
}
