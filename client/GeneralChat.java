import javax.swing.*;
import java.awt.*;

public class GeneralChat extends JPanel {

    public GeneralChat() {
        JTextField txtField = new JTextField(10);

        JButton sendButton = new JButton("SEND");
        sendButton.addActionListener(e -> {
            String message = txtField.getText();
            GameLogic.send_general_message(message);
        });

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(new Label("General chat"));

        this.add(txtField);
        this.add(sendButton);
    }

    public void addMessage(String id, String message) {
        this.add(new JLabel(id + " : " + message));
        this.updateUI();
    }
    }
