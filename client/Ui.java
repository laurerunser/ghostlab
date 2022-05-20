import javax.swing.*;
import java.awt.*;
import java.io.IOException;


public class Ui extends JFrame {
    JPanel rootPanel;

    public Ui () throws IOException, IncorrectMessageException {
        this.setTitle("Ghostlab");
        this.setSize(600, 600);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        // add the pregame UI

        rootPanel = new JPanel(new CardLayout());
        JPanel pregamePanel = new PregamePanel();
        BoxLayout boxlayout = new BoxLayout(pregamePanel, BoxLayout.Y_AXIS);
        pregamePanel.setLayout(boxlayout);

        rootPanel.add(pregamePanel, "pregame");

        CardLayout cards = (CardLayout) rootPanel.getLayout();
        cards.show(rootPanel, "pregame");


        this.add(rootPanel);
    }

    public void set_game_panel(GamePanel gamePanel) {
        rootPanel.add(gamePanel, "game");

        CardLayout cards = (CardLayout) rootPanel.getLayout();
        cards.show(rootPanel, "game");
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

