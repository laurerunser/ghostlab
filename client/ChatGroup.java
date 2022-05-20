import java.util.LinkedList;

import javax.swing.*;

public class ChatGroup extends JPanel{
        JButton sendButton = new JButton("SEND");
        JTextField txtField = new JTextField(200);

        public ChatGroup(){

            sendButton.addActionListener(e->{
                String message = txtField.getText();
                GameLogic.send_general_message(message);
                afficheMessage("Me:",message );
            });

            this.add(txtField);
            this.add(sendButton);
        }

        public void afficheMessage (String id, String message){
            this.add(new JLabel(id + " : " + message));
            this.updateUI();
        }
    }
