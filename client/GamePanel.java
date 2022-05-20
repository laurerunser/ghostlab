import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class GamePanel extends JPanel {
    public JPanel grid;

    public JLabel score;
    public JLabel nb_ghosts_left;
    public JButton quit_button;

    public int width;
    public int height;
    public int current_x;
    public int current_y;

    public GamePanel(int width, int height, int x, int y) {
        this.width = width;
        this.height = height;
        this.current_x = x;
        this.current_y = y;

        this.setLayout(new BorderLayout());

        // add the playing grid
        make_grid(width, height);
        this.add(grid, CENTER_ALIGNMENT);

        // add the controls

        // add score, nb of ghosts and quit buttons
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
        score = new JLabel("Score : 0");
        nb_ghosts_left = new JLabel("Ghosts left : " + GameLogic.nb_ghosts_left
                + "/" + GameLogic.nb_ghosts);

        quit_button = new JButton("QUIT");
        quit_button.addActionListener(e -> GameLogic.i_quit());

        JPanel info_panel = new JPanel();
        info_panel.add(score);
        info_panel.add(nb_ghosts_left);
        info_panel.add(quit_button);
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
}
