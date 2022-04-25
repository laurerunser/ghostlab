import java.io.IOException;

public class FauxClient {
    static int nbJoueur [] =new int[] {4,5,6,7,8};
    //Ressemble a client mais sans trucs trop reseau pour test ig

    public static int[] getAllGamesAndNbOfPlayers() throws IOException, IncorrectMessageException {
        return nbJoueur;
    }

    public static int createGame (){
        nbJoueur = new int [nbJoueur.length + 1];
        nbJoueur[nbJoueur.length -1] = 1; 
        return nbJoueur.length;
    }
    
    public static int registerToGame(short gameId, String id) throws IOException, IncorrectMessageException {
        if (nbJoueur.length<gameId){
            return -1;
        }else{
            nbJoueur[gameId] += 1;
            return gameId;
        }
    }

    
}
