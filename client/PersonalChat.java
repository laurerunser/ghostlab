import javax.swing.*;
import java.util.Objects;

public class PersonalChat extends JPanel {
    ChatFrame[] chats;

    public PersonalChat() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        chats = new ChatFrame[4];

        for (int i = 0; i < 4; i++) {
            String name = GameLogic.players[i].id;
            if (!name.equals(GameLogic.this_player.id)) { // can't chat with yourself
                chats[i] = new ChatFrame(name);
                this.add(chats[i]);
            }
        }
    }

    public void add_message_to_panel(String message_received, String sender_id) {
        for (int i = 0; i < 4; i++) {
            if (chats[i] != null && chats[i].recipient_id.equals(sender_id)) {
                chats[i].addMessage(sender_id, message_received);
            }
        }
    }

    public class ChatFrame extends JPanel {
        String recipient_id;

        public ChatFrame(String recipient_id) {
            this.recipient_id = recipient_id;

            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JTextField txtField = new JTextField(200);

            JButton sendButton = new JButton("SEND");
            sendButton.addActionListener(e -> {
                String message = txtField.getText();
                GameLogic.send_personal_message(message, recipient_id);
                this.add(new JLabel(String.format("me : %s", message)));
            });

            this.add(new JLabel(String.format("chat with %s", recipient_id)));
            this.add(txtField);
            this.add(sendButton);
        }

        public void addMessage(String sender_id, String message) {
            this.add(new JLabel(String.format("%s : %s", sender_id, message)));
            this.updateUI();
        }
    }
}