import javax.swing.*;
import java.io.IOException;


public class Ui extends JFrame {
    JPanel principal = new JPanel();

    public Ui () throws IOException, IncorrectMessageException {
        this.setTitle("Gostlab");
        this.setSize(600, 800);
        this.setResizable(false);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.add(principal);

        // add the pregame UI
        JPanel pregamePanel = new PregamePanel();
        BoxLayout boxlayout = new BoxLayout(pregamePanel, BoxLayout.Y_AXIS);
        pregamePanel.setLayout(boxlayout);
        principal.add(pregamePanel);
    }
}

