package client;



public class Joueur {
    
    private String joueurID; //ID de taille 8
    private int port; //Permet de creer la socket avec DatagramSocket (int port)

    public Joueur(String s, String s2) throws IllegalArgumentException{
        if ((s.length()==8) &&  (Integer.parseInt(s2)>8000)){
            this.joueurID=s;
            this.port=Integer.parseInt(s2);
        }else{
            throw new IllegalArgumentException();
        }

    }

    public String getJoueurID() {
        return joueurID;
    }
    public int getPort() {
        return port;
    }


}
