import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.LinkedList;

public class GamePanel extends JPanel {
    public JPanel grid;

    public JLabel score;
    public JLabel nb_ghosts_left;
    public JButton quit_button;

    public int width;
    public int height;
    public int current_x;
    public int current_y;


    public ChatGroup group_chat;
    public ChatPannel perso_chat;

    public class ChatPannel extends JPanel{
        ChatFrame [] chats;

        public class ChatFrame extends JPanel {
            LinkedList <JLabel> pannel;
            String pseudo;
            JButton sendButton = new JButton("SEND");
            JTextField txtField = new JTextField(200);


            public ChatFrame(String pseudo){

                this.pseudo=pseudo;
                sendButton.addActionListener(e->{
                    String message = txtField.getText();
                    GameLogic.send_personal_message(message ,pseudo);
                    afficheMessage("Me:",message );
                });
                for (JLabel pan : pannel) {
                    this.add(pan);
                }
                this.add(txtField);
                this.add(sendButton);
            }

            public void afficheMessage (String id, String message){
                pannel.add(new JLabel(id + " : " + message));
                this.updateUI();
            }

        }

        public ChatPannel(){
            this.setLayout(new BorderLayout());
            chats = new ChatFrame [GameLogic.players.length];
            for (int i=0; i < GameLogic.players.length; i++){
                String name = GameLogic.players[i].id;
                JButton privChat = new JButton(name);
                privChat.addActionListener( e -> { 
                    getFrame(name).setVisible(true);
                });
            }            
        }

        //Obtenir le Frame liée au joueur
        public ChatFrame getFrame (String id){
            for (int i=0; i < chats.length; i++){
                if (id.equals(chats[i].pseudo)){
                    return chats[i];     
                }
            }
            return null;
        }

        public void recv(String message_received, String sender_id) {
            getFrame(sender_id).afficheMessage(sender_id, message_received);
        
        }
    }

    public class ChatGroup extends JPanel{

        LinkedList <JLabel> pannel;
        JButton sendButton = new JButton("SEND");
        JTextField txtField = new JTextField(200);

        public ChatGroup(){

            sendButton.addActionListener(e->{
                String message = txtField.getText();
                GameLogic.send_general_message(message);
                afficheMessage("Me:",message );
            });
            for (JLabel pan : pannel) {
                this.add(pan);
            }
            this.add(txtField);
            this.add(sendButton);
        }

        public void afficheMessage (String id, String message){
            pannel.add(new JLabel(id + " : " + message));
            this.updateUI();
        }
    }


    //Update UI when receive a private message
    public void recvUDPUpdate(String message_received, String sender_id) {
        perso_chat.recv(message_received, sender_id);
    }

    //Update UI when receive a group message
    public void recvMulticastUpdate(String message_received, String sender_id) {
        group_chat.afficheMessage(message_received, sender_id);
    }


    public GamePanel(int width, int height, int x, int y) {
        this.width = width;
        this.height = height;
        this.current_x = x;
        this.current_y = y;

        this.setLayout(new BorderLayout());

        // add the playing grid
        make_grid(width, height);
        this.add(grid, CENTER_ALIGNMENT);
        this.group_chat = new ChatGroup();
        this.perso_chat = new ChatPannel();
        this.add(perso_chat,LEFT_ALIGNMENT );
        this.add(group_chat, RIGHT_ALIGNMENT);


        // add controls and score, nb of ghosts and quit buttons
        add_top_info();
    }

    public void make_grid(int width, int height) {
        // the grid is made of JButtons, without any listeners -> they can't be clicked
        // but this is an easy way of making a grid
        grid = new JPanel(new GridLayout(width, height));

        for (int i = 0; i<width; i++) {
            for (int j = 0; j<height; j++) {
                if (i == current_x && j == current_y) { // place current player (in blue)
                    JButton player = new JButton();
                    player.setBorder(new LineBorder(Color.BLACK));
                    player.setBackground(Color.BLUE);
                    grid.add(player, i, j);
                } else { // place grey block
                    JButton block = new JButton();
                    block.setBackground(Color.LIGHT_GRAY);
                    block.setBorder(new LineBorder(Color.DARK_GRAY));
                    grid.add(block, i, j);
                }
            }
        }
    }

    public void add_top_info() {
        JPanel controls = make_controls();

        score = new JLabel("Score : 0");
        nb_ghosts_left = new JLabel("Ghosts left : " + GameLogic.nb_ghosts_left
                + "/" + GameLogic.nb_ghosts);

        quit_button = new JButton("QUIT");
        quit_button.addActionListener(e -> {
            GameLogic.i_quit();
            show_quit_dialog();
        });

        JPanel info_panel = new JPanel();
        info_panel.add(controls);
        info_panel.add(score);
        info_panel.add(nb_ghosts_left);
        info_panel.add(quit_button);
    }

    public JPanel make_controls() {
        JPanel controls = new JPanel(new BorderLayout());

        JButton up = new JButton("UP");
        up.addActionListener(e -> move_up(1));
        controls.add(up, BorderLayout.NORTH);

        JButton down = new JButton("DOWN");
        down.addActionListener(e -> move_down(1));
        controls.add(down, BorderLayout.SOUTH);

        JButton left = new JButton("LEFT");
        left.addActionListener(e -> move_left(1));
        controls.add(left, BorderLayout.WEST);

        JButton right = new JButton("RIGHT");
        right.addActionListener(e -> move_right(1));
        controls.add(right, BorderLayout.EAST);

        return controls;
    }

    public void update_nb_ghosts() {
        nb_ghosts_left.setText("Ghosts left : " + GameLogic.nb_ghosts_left
                + "/" + GameLogic.nb_ghosts);
    }

    public void update_score(int val) {
        score.setText("Score : " + val);
    }

    /**
     * change the color of the block from light_gray to yellow
     * then wait 0.5 second
     * then change the color back to light_gray
     * @param x ghost x coordinate
     * @param y ghost y coordinate
     */
    public void show_ghost(int x, int y) {
        JButton ghost = (JButton)grid.getComponentAt(x, y);
        ghost.setBackground(Color.YELLOW);
        try {
            wait(50);
        } catch (Exception e) {
            // do nothing
        }
        ghost.setBackground(Color.LIGHT_GRAY);
    }


    /**
     * Shows a ghost dying: goes from yellow to red (then back to normal light gray)
     * @param x ghost x coordinate
     * @param y ghost y coordinate
     */
    public void ghost_dies(int x, int y) {
        JButton ghost = (JButton)grid.getComponentAt(x, y);
        ghost.setBackground(Color.YELLOW);
        try {
            wait(50);
        } catch (Exception e) {
            // do nothing
        }
        ghost.setBackground(Color.RED);
        try {
            wait(50);
        } catch (Exception e) {
            // do nothing
        }
        ghost.setBackground(Color.LIGHT_GRAY);
    }


    /**
     * Shows a dialog that tells the user the game has ended
     * Cliking the OK button terminates the program (the 2 UDP threads have
     * been terminated before)
     * @param winner_id         id of the winning player
     * @param winning_score     score of the winning player
     */
    public void show_endgame(String winner_id, int winning_score) {
        JDialog dialog = new JDialog();
        dialog.add(new JLabel("The game has ended : " + winner_id + " won with " + winning_score + " points !"));

        JButton ok = new JButton("OK (this will close the program)");
        ok.addActionListener(e -> System.exit(0));
        dialog.add(ok);

        dialog.setVisible(true);
    }

    public void move_up(int d) {
        int[] new_coordinates = GameLogic.moveUp(d);

        boolean move_ok = update_player_on_grid(new_coordinates[0], new_coordinates[1], d);

        if (!move_ok) { // make a black block just above the current player position
            if (current_y + 1 != height) { // if the player is not at the top of the game already
                JButton block = (JButton) grid.getComponentAt(current_x, current_y+1);
                block.setBackground(Color.BLACK);
                block.setBorder(new LineBorder(Color.BLACK));
            }
        }
    }

    public void move_down(int d) {
        int[] new_coordinates = GameLogic.moveDown(d);

        boolean move_ok = update_player_on_grid(new_coordinates[0], new_coordinates[1], d);

        if (!move_ok) { // make a black block just above the current player position
            if (current_y != 0) { // if the player is not at the bottom of the game already
                JButton block = (JButton) grid.getComponentAt(current_x, current_y-1);
                block.setBackground(Color.BLACK);
                block.setBorder(new LineBorder(Color.BLACK));
            }
        }
    }

    public void move_left(int d) {
        int[] new_coordinates = GameLogic.moveLeft(d);

        boolean move_ok = update_player_on_grid(new_coordinates[0], new_coordinates[1], d);

        if (!move_ok) { // make a black block just above the current player position
            if (current_x != 0) { // if the player is not at the left of the game already
                JButton block = (JButton) grid.getComponentAt(current_x-1, current_y);
                block.setBackground(Color.BLACK);
                block.setBorder(new LineBorder(Color.BLACK));
            }
        }
    }

    public void move_right(int d) {
        int[] new_coordinates = GameLogic.moveRight(d);

        boolean move_ok = update_player_on_grid(new_coordinates[0], new_coordinates[1], d);

        if (!move_ok) { // make a black block just above the current player position
            if (current_x + 1 != width) { // if the player is not at the top of the game already
                JButton block = (JButton) grid.getComponentAt(current_x+1, current_y);
                block.setBackground(Color.BLACK);
                block.setBorder(new LineBorder(Color.BLACK));
            }
        }
    }

    /**
     * Updates the blue dot of the player
     * also update the current_x and current_y variables
     * @param new_x the new x-coordinate
     * @param new_y the new y-coordinate
     * @return true if the move was done completely, false if not (means there is an obstacle)
     */
    public boolean update_player_on_grid(int new_x, int new_y, int d) {
        boolean ok;
        if (new_x == current_x && new_y == current_y) {
            return false; // no visual update of player, can't move
        } else {
            ok = current_x + d == new_x || current_x - d == new_x
                    || current_y + d == new_y || current_y - d == new_y;
            // ok will be true if the move was complete, or false if the player
            // couldn't travel all the way
        }

        // update the player on the grid
        JButton old_pos = (JButton)grid.getComponentAt(current_x, current_y);
        old_pos.setBackground(Color.LIGHT_GRAY);
        old_pos.setBackground(Color.DARK_GRAY);

        JButton new_pos = (JButton)grid.getComponentAt(new_x, new_y);
        new_pos.setBackground(Color.BLUE);
        new_pos.setBackground(Color.BLACK);

        // update current position variables
        current_x = new_x;
        current_y = new_y;
        return ok;
    }

    public void show_quit_dialog() {
        JDialog dialog = new JDialog();
        dialog.add(new JLabel("You've quit the game. Click on OK to close the program"));

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> System.exit(1));
        dialog.add(ok);

        dialog.setVisible(true);
    }

}
