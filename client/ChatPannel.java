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