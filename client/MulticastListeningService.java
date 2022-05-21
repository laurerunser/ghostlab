import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastListeningService implements Runnable {
    public String ip;
    public int port;
    MulticastSocket mso;
    UDPListeningService udpService;
    boolean game_ended = false;

    GamePanel gamePanel;

    public MulticastListeningService(String multicast_ip, int multicast_port, UDPListeningService udpService,
                                     GamePanel gamePanel) {
        this.ip = multicast_ip;
        this.port = multicast_port;
        this.udpService = udpService;
        this.gamePanel = gamePanel;

        try {
            // make the socket
            mso = new MulticastSocket();

            // join the group
            mso.joinGroup(InetAddress.getByName(multicast_ip));
        } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
        }
    }

    public void run() {
        while(!game_ended) {
            System.out.println("waiting for header");
            String header = receiveHeader();
            if (header.startsWith("SCORE")) {
                updateScore();
            } else if (header.equals("GHOST")) {
                updateGhostPosition();
            } else if (header.equals("ENDGA")) {
                signalEndgame();
                game_ended = true; // kill this thread
            } else if (header.equals("MESSA")) {
                receiveMessage();
            } else { // incorrect header
                try {
                    Client.logIncorrectHeader("a UDP header", header);
                } catch (IncorrectMessageException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    public String receiveHeader() {
        try {
            byte[] data = new byte[5];
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            mso.receive(paquet);
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
            mso.receive(paquet);
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
        GameLogic.nb_ghosts_left--;

        // update the ui
        gamePanel.update_score(new_score);
        gamePanel.update_nb_ghosts();
        gamePanel.ghost_dies(ghost_x, ghost_y);
    }

    public void updateGhostPosition() {
        // receive the rest of the message
        byte[] data = new byte[11];
        String message = "";
        try {
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            mso.receive(paquet);
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

        // show the ghost on the UI
        gamePanel.show_ghost(ghost_x, ghost_y);
    }

    public void signalEndgame() {
        // receive the rest of the message
        byte[] data = new byte[17];
        String message = "";
        try {
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            mso.receive(paquet);
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

        // kill the udp thread just in case
        udpService.game_ended = true;

        // no need to kill the GameLogic thread : it will die when the user
        // closes the UI pop-up

        // make a pop-up to signal end of game
        gamePanel.show_endgame(player_id, winning_score);
    }

    /**
     * Receive a general message
     */
    public void receiveMessage() {
        // receive the rest of the message
        byte[] data = new byte[213];
        String message = "";
        try {
            DatagramPacket paquet = new DatagramPacket(data, data.length);
            mso.receive(paquet);
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

        Client.LOGGER.info(String.format("Player %s sent general message %s", sender_id, message_received));

        gamePanel.group_chat.addMessage(sender_id, message_received);
    }

}
