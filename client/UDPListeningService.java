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
            receiveMessage(true);
        } else if (header.equals("MESSP")) {
            receiveMessage(false);
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
            for (PlayerInfo p : GameLogic.players) {
                if (p.id.equals(playerID)) {
                    p.score = new_score;
                }
            }
        }
        // TODO : update the UI with the new score
        // TODO : launch an animation that shows the ghost dying in its coordinates
    }

    public void updateGhostPosition() {
        // receive the rest of the message
        byte[] data = new byte[11];
        String message = "";
        try {
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            dso.receive(paquet);
            if (paquet.getLength() != 11) {
                Client.logIncorrectLengthMessage("GHOST", 11, paquet.getLength());
            }
            message = new String(paquet.getData(), 0, paquet.getLength());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // extract the information
        int ghost_x = Integer.parseInt(message.substring(1, 4));
        int ghost_y = Integer.parseInt(message.substring(5, 8));
        Client.LOGGER.info(String.format("Ghost moved to x=%d, y=%d", ghost_x, ghost_y));

        // TODO : update the UI (make the ghost appear briefly or smth)

    }

    public void signalEndgame() {
        // receive the rest of the message
        byte[] data = new byte[17];
        String message = "";
        try {
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            dso.receive(paquet);
            if (paquet.getLength() != 17) {
                Client.logIncorrectLengthMessage("ENDGA", 17, paquet.getLength());
            }
            message = new String(paquet.getData(), 0, paquet.getLength());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        String player_id = message.substring(1, 9);
        int winning_score = Integer.parseInt(message.substring(10, 14));

        Client.LOGGER.info(String.format("Player %s won with score %d", player_id, winning_score));

        // TODO : kill the game (maybe update a boolean in GameLogic ?)
        // TODO : update UI with a pop-up
    }

    /**
     * Receive a message
     * @param general true if the message is a general one (MESSA), false if it is personal (MESSP)
     */
    public void receiveMessage(boolean general) {
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

        String context = general?"general":"personal";
        Client.LOGGER.info(String.format("Player %s sent %s message %s", sender_id, context, message_received));

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
