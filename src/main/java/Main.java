
import examples.StarterGhostComm.Blinky;
import examples.StarterGhostComm.Inky;
import examples.StarterGhostComm.Pinky;
import examples.StarterGhostComm.Sue;
import examples.StarterPacMan.*;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Constants.*;
import pacman.game.internal.POType;
import examples.StarterPacMan.montecarlo.*;

import java.util.EnumMap;


/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {

    public static void main(String[] args) {

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setPacmanPO(false)
                .setTickLimit(10000)
                .setScaleFactor(2) // Increase game visual size
                .setPOType(POType.RADIUS) // pacman sense objects around it in a radius wide fashion instead of straight line sights
                .setSightLimit(5000) // The sight radius limit, set to maximum 
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());

        MASController ghosts = new POCommGhosts(50);
        // delay=10; smaller delay for faster gameplay tree search
        // executor.runGame(new TreeSearchPacMan(), ghosts, 10); 

        // monte carlo version
        // executor.runGame(new MonteCarloPacMan(), ghosts, 10);

        // A Star
        // executor.runGame(new AStar(), ghosts, 10);

        //DFS
        executor.runGame(new DFS(), ghosts, 10);
        // executor.runGame(new CustomTreeSearchPacMan(), ghosts, 10);
    }
}
