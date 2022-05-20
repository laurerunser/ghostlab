import javax.swing.*;
import java.io.IOException;


public class Ui extends JFrame {
    JPanel principal = new JPanel();

    public Ui () throws IOException, IncorrectMessageException {
        this.setTitle("Gostlab");
        this.setSize(600, 600);
        this.setResizable(false);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.add(principal);

        // add the pregame UI
        JPanel pregamePanel = new PregamePanel();
        BoxLayout boxlayout = new BoxLayout(pregamePanel, BoxLayout.Y_AXIS);
        pregamePanel.setLayout(boxlayout);
        principal.add(pregamePanel);
    }

    public void set_game_panel(GamePanel gamePanel) {
        principal = gamePanel;
        principal.updateUI();
    }

    public static void timeout() {
        JFrame error = new JFrame();
        error.setTitle("Error");
        JOptionPane.showMessageDialog(error, "Server take too long to respond");
        error.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        error.setVisible(true);
        System.exit(2);
    }
}

