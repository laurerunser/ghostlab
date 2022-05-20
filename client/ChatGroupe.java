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
