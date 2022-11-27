package examples.StarterPacMan;

import pacman.controllers.Controller;
import examples.StarterPacMan.MCTree;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class MCTPacman extends Controller<MOVE> {
	
	private MCTree<Game, MOVE> mct = new TheRealMCTSPacman();
	
	private MOVE lastMove = MOVE.RIGHT;
	
	private int lateGameTimeTrigger = 2000;
	private int lateGameActivePillsNoTrigger = 20;
	
	private boolean debugVar = false;
	
	public MCTPacman() {
	
	}
	
	public MCTPacman(MCTree<Game,MOVE> mcTree) {
		this.mct = mcTree;
	}
	
	
	@Override
	public MOVE getMove(Game game, long timeDue) {
		int closestGhostNodeIndex = game.getGhostCurrentNodeIndex(getClosestGhostToPac(game));
		int pacmanIndex = game.getPacmanCurrentNodeIndex();
		boolean pacmanIsInJunction = game.isJunction(pacmanIndex);
		boolean closestGhostIsInJunction = game.isJunction(closestGhostNodeIndex);
		boolean powerPillEaten = game.wasPowerPillEaten();
		boolean ghostWasEaten = wasGhostEaten(game);
		
		if(pacmanIsInJunction || closestGhostIsInJunction || powerPillEaten || ghostWasEaten){
			try {
				lastMove = mct.runMCTSearch(game);
			} catch (InterruptedException e) {
				System.err.println("MCT was Interrupted");
			}
		} else {
			lastMove = game.getPossibleMoves(pacmanIndex, game.getPacmanLastMoveMade())[0];
		}
		return lastMove;
	}
	
	private boolean wasGhostEaten(Game game) {
		for(GHOST ghost : GHOST.values()){
			if(game.wasGhostEaten(ghost)){
				return true;
			}
		}
		return false;
	}

	private GHOST getClosestGhostToPac(Game game){
		int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
		GHOST closestGhost = null;
		int minDistance = Integer.MAX_VALUE;
		for(GHOST ghost : GHOST.values()){
			int currentGhostNodeIndex = game.getGhostCurrentNodeIndex(ghost);
			int distanceFromGhost = game.getShortestPathDistance(currentGhostNodeIndex, pacmanNodeIndex);
			if(distanceFromGhost < minDistance){
				closestGhost = ghost;
				minDistance = distanceFromGhost;
			}
		}
		return closestGhost;
	}
}
