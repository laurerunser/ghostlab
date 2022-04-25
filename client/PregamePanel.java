import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.IOException;


public class PregamePanel extends JPanel {
    JPanel listOfGames = new JPanel();

    JButton startButton = new JButton("START");
    JButton unregisterButton = new JButton("UNREG");
    JButton refreshButton = new JButton("REFRESH");

    boolean isGameSelected = false; // true if a game has been selected

    public PregamePanel() throws IOException, IncorrectMessageException {
        // add the buttons + give them listeners
        startButton.addActionListener(e -> startGame());
        this.add(startButton);
        unregisterButton.addActionListener(e -> unregisterGame());
        this.add(unregisterButton);
        refreshButton.addActionListener(e -> refreshListOfGames());
        this.add(refreshButton);

        // read the first message sent by the server
        // and store the number of players for each available game
        // The number is 0 if the game is not available (started or not yet created)
        int[] nbPlayers = Client.readNbPlayersAnswer();

        // for each available game, add a button with the number of the game
        addListOfGames(nbPlayers);
        this.add(listOfGames);

    }

    public void addListOfGames(int[] nbPlayers) {
        for (int i = 0; i < nbPlayers.length; i++) {
            if (nbPlayers[i] > 0) {
                JButton bouton = new JButton("game " + i);
                int j = i; // the variable in the listener below must be effectively final
                bouton.addActionListener(e -> seeGameDetails(j));
                listOfGames.add(bouton);
            }
        }
    }

    public void refreshListOfGames() {
        // remove current content
        listOfGames.removeAll();

        // get the updated games and add them to the panel
        try {
            int[] nbPlayers = Client.getAllGamesAndNbOfPlayers();
            addListOfGames(nbPlayers);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        // this method is used inside of a listener, so there needs to be
        // a try/catch block
    }


    public void seeGameDetails(int gameId) {
        String[] playersID = null;
        try {
            // get the list of players and make a panel for them
            playersID = Client.getPlayersForGame((short) gameId);

        } catch (IOException | IncorrectMessageException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // this is used inside of a listener, so there needs to be a
        // try/catch block to handle the error

        assert playersID != null; // because the function inside the try/catch can return null
                                  // program should terminate if it is, but better safe than sorry

        // create the panel to hold the list of players
        JPanel playersIDPannel = new JPanel();
        for (int j = 0; j < playersID.length; j++) {
            JLabel joueur = new JLabel(j + " :" + playersID[j]);
            playersIDPannel.add(joueur);
        }
        // add how many spots are left
        int spotsLeft = 4 - playersID.length;
        JLabel spotsLeftLabel = new JLabel("There are " + spotsLeft + " left !");
        playersIDPannel.add(spotsLeftLabel);

        // make the popup
        JDialog popup = new JDialog();
        popup.setTitle("Game " + gameId + " details");
        popup.add(playersIDPannel);

        // add register button
        JButton registerButton = new JButton("Choose this game !");
        JTextField pseudoField = new JTextField(8);
        pseudoField.setBorder(new TitledBorder("Your name : "));
        registerButton.addActionListener(e -> selectGame(gameId, pseudoField.getText()));
        popup.add(registerButton);

        // make the popup visible
        popup.setVisible(true);

        // there is an automatic "close" button to hide the dialog
    }

    public void selectGame(int gameId, String playerId) {
        // pad the ID with spaces until it is the right length
        // the ID cannot be too big, because the Textfield used is limited to 8 chars
        // (see `seeGameDetails()` method)
        while (playerId.length() < 8) {
            playerId = playerId.concat(" ");
        }

        // try to register the player
        int res = 0;
        try {
            res = Client.registerToGame((short) gameId, playerId);
        } catch (IOException | IncorrectMessageException e) {
            e.printStackTrace();
        }

        // display a popup to say if success or failure
        if (res == -1) { // failed
            JOptionPane.showMessageDialog(this, "Sorry, we can't register you into " +
                    "that game.\nTry again later !");
            refreshListOfGames(); // refresh the list of games : maybe someone stole the spot before we could register
        } else { // registration OK
            isGameSelected = true;
            JOptionPane.showMessageDialog(this, "You are registered ! " +
                    "Click on the START button to play!");
        }
    }

    public void startGame() {
        if (isGameSelected) {
            try {
                Client.sendStart();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, "You aren't registered into a game yet !");
        }
    }


    public void unregisterGame() {
        if (isGameSelected) {
            boolean res = false;
            try {
                res = Client.unregisterFromGame();
            } catch (Exception e) { e.printStackTrace();}
            if (res) {
                JOptionPane.showMessageDialog(this, "You have been unregistered !");
            } else {
                JOptionPane.showMessageDialog(this, "Sorry, try again later.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "You aren't registered into a game yet !");
        }
    }

}

