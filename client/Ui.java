import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.IOException;


public class Ui extends JFrame {
    JPanel principal = new JPanel();
    JPanel menu = new Menu();

    public static void main(String[] args){
        javax.swing.SwingUtilities.invokeLater(
            () -> {
                Ui v = new Ui();
                    v.setVisible(true);
            }
        );
    
    }

    public class Menu extends JPanel {
        

        JButton startButton = new JButton("START");
        JButton unregisterButton = new JButton("UNREG");
        boolean selected = false;  //Indique si une partie a été séctionnée

        public class ListeJoueur extends JPanel{
            public ListeJoueur(String [] liste){
                super();
                for (int i =0; i<liste.length; i++){
                    JLabel joueur = new JLabel(i +" :" +liste[i]);
                    this.add(joueur);
                }
            }
        }
        public void addButton(int i){
            JButton bouton = new JButton("partie" + i);
            bouton.addActionListener(e -> seePartie(i));
            this.add(bouton);
        }

        public void seePartie (int i){
            short a= (short) i;
            //TODO afficher un panneau avec les infos de la partie
            try {
                String [] players = Client.getPlayersForGame(a);
                ListeJoueur pannelJoueurs = new ListeJoueur (players);
                this.add(pannelJoueurs);
                JButton registerButton = new JButton("START");
                JTextField pseudoField = new JTextField(8);
                pseudoField.setBorder(new TitledBorder("Nom du Joueur"));
                registerButton.addActionListener(e -> selectpartie(i, pseudoField.getText()));

            } catch (IOException | IncorrectMessageException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void selectpartie (int i, String s){
            while (s.length()<8){
                s.concat(" ");
            }
            try {
                int res = Client.registerToGame ((short)i, s);
                if (res == -1){
                    JOptionPane.showMessageDialog(this, "Enregistrement impossible");
                }else{
                    this.selected=true;
                }
            } catch (IOException | IncorrectMessageException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }        
        }
        public void startPartie(){
            if (this.selected){
                try {
                    Client.sendStart();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else{
                JOptionPane.showMessageDialog(this, "Vous n'avez pas selectionner de partie");
            }
        }

        public void unregisterPartie(){
            if (this.selected){
                try {
                    Client.unregisterFromGame();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IncorrectMessageException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else{
                JOptionPane.showMessageDialog(this, "Vous n'avez pas selectionner de partie");
            }
        }
        
        public Menu (){
            
            try {
                int [] games = Client.getAllGamesAndNbOfPlayers();
                for (int i = 0; i< games.length; i++){
                    if (games [i] >= 0){
                        addButton(i);
                        }
                }
                startButton.addActionListener(e -> startPartie());
                this.add(startButton);
                unregisterButton.addActionListener(e -> unregisterPartie());
                this.add(unregisterButton);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IncorrectMessageException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    

    
    public Ui (){
        this.setTitle("Gostlab");
        this.setSize(600, 800);
        this.setResizable(false);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);


        this.add(principal);


        BoxLayout boxlayout = new BoxLayout(menu, BoxLayout.Y_AXIS);
        menu.setLayout(boxlayout);
        principal.add(menu);


    }


    
}

