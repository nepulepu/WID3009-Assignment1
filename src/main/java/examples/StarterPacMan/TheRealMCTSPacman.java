package examples.StarterPacMan;


import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import pacman.controllers.Controller;
import pacman.controllers.examples.Legacy2TheReckoning;
import pacman.controllers.examples.NearestPillPacMan;
import pacman.controllers.examples.RandomNonRevPacMan;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterPacMan;

import pacman.controllers.Controller;
import pacman.controllers.examples.StarterPacMan;
import examples.StarterGhostComm.POCommGhost;
import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import examples.StarterPacMan.MCTree;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.internal.Ghost;
import pacman.game.Game;

public class TheRealMCTSPacman extends MCTree<Game, MOVE>{

	private Random random = new Random();
	
	// private Legacy2TheReckoning ghosts = new Legacy2TheReckoning();
	private MASController ghosts = new POCommGhosts(50);
	private Controller<MOVE> defaultPacmanController = new RandomNonRevPacMan();
	
	private int maxIterations = 360;
	private int maxDPolicyIters = 20;

	//Original stats
	private double levelCompleteReward = 3000;
	private double pillEatenReward = 61.212785333139465;
	private double eatenGhostReward = 30.401937677892262;
	private double pacmanWasEatenReward = -91.28320090796609;
	private double ateMultiplePowerPillsReward = -30;
	private double noGhostsEatenAfterPowerPillReward = -57.16515188322474;
	private double ghostDistanceAfterPPThreshold = 56.405912751239896;
	private double ghostDistanceAfterPPRewardMult = 0.4222901868213781;


    // private double levelCompleteReward = 3000;
	// private double pillEatenReward = 5;
	// private double eatenGhostReward = 1000;
	// private double pacmanWasEatenReward = -10000;
	// private double ateMultiplePowerPillsReward = -500;
	// private double noGhostsEatenAfterPowerPillReward = -100;
	// private double ghostDistanceAfterPPThreshold = 1;
	// private double ghostDistanceAfterPPRewardMult = 50;

	public TheRealMCTSPacman() {
		C = 20; //Empirically tested to be the best value
	}
	
	public TheRealMCTSPacman(double[] params) {
		if(params.length == 1){
			C = params[0];
			return;
		}
		
		this.maxIterations = (int) params[0];
		this.maxDPolicyIters = (int) params[1];
		this.levelCompleteReward = 3000;
		this.pillEatenReward = params[2];
		this.eatenGhostReward = params[3];
		this.pacmanWasEatenReward = params[4];
		this.noGhostsEatenAfterPowerPillReward = params[5];
		this.ghostDistanceAfterPPThreshold = params[8];
		this.ghostDistanceAfterPPRewardMult = params[9];
	}
	
	public TheRealMCTSPacman(int maxIterations, int maxDPolicyIters,
			double levelCompleteReward, double pillEatenReward,
			double pillsRewardMult, double eatenGhostReward,
			double pacmanWasEatenReward,
			double noGhostsEatenAfterPowerPillReward,
			double distaRectionRewardMult, double distaRectionTrigger) {
		this.maxIterations = maxIterations;
		this.maxDPolicyIters = maxDPolicyIters;
		this.levelCompleteReward = levelCompleteReward;
		this.pillEatenReward = pillEatenReward;
		this.eatenGhostReward = eatenGhostReward;
		this.pacmanWasEatenReward = pacmanWasEatenReward;
		this.noGhostsEatenAfterPowerPillReward = noGhostsEatenAfterPowerPillReward;
	}

	@Override
	protected Game getCopy(Game state) {
		return state.copy();
	}

	@Override
	protected int getMaxIterations(Game state) {
		return maxIterations;
	}
	
	private int getDefaultPolicyIterations(Game state){
		return maxDPolicyIters;
	}
	
	@Override
	protected double defaultPolicy(Game initialState) {
		Game currentState = getCopy(initialState);
		Controller<MOVE> pacman = defaultPacmanController;
		
		double reward = 0.0;
		boolean powerPillEaten = false;
		int ghostsEaten = 0;		
		
		int i=0;
		while(isTerminalState(currentState) || i < getDefaultPolicyIterations(currentState)){
			if(areGhostsEdible(currentState)){
				pacman = new StarterPacMan();
			}
			
			if(currentState.wasPowerPillEaten()){
				powerPillEaten = true;
				if(areGhostsEdible(initialState)){
					//Ate multiple power pills
					reward += ateMultiplePowerPillsReward;
				} else {
					reward += rewardPowerPillGrab(currentState);
				}
				pacman = new StarterPacMan();
			}
			
			// Reward for eating ghosts
			double multiplier = 1.0;
			for(GHOST ghost : GHOST.values()){
				if(currentState.wasGhostEaten(ghost)){
					reward = reward + (eatenGhostReward  * multiplier);
					multiplier += 0.5;
					ghostsEaten++;
				}
			}
		
			if(ghostsEaten>=2){
				break;
			}
			
			if(currentState.wasPacManEaten()){
				return pacmanWasEatenReward;
			}
			if(currentState.getNumberOfActivePills() == 0){
				return levelCompleteReward;
			}
			
			MOVE pacmanMove = pacman.getMove(currentState, -1);
			EnumMap<GHOST, MOVE> ghostMoves = ghosts.getMove(currentState, -1);
			currentState.advanceGame(pacmanMove, ghostMoves);
			i++;
		}
		
		boolean ghostsInLair = areGhostsInLair(initialState);  
		
		if(areGhostsEdible(initialState) && (ghostsEaten == 0 || !ghostsInLair) ){
			reward += noGhostsEatenAfterPowerPillReward ;
		}
		if(areGhostsEdible(initialState) && powerPillEaten){
			reward += noGhostsEatenAfterPowerPillReward ;
		}
		
		reward += getReward(initialState, currentState);
		
		return reward;
	}

	private boolean areGhostsInLair(Game initialState) {
		for(GHOST ghost : GHOST.values()){
			if(initialState.getGhostLairTime(ghost) > 0 ){
				return true;
			}
		}
		return false;
	}

	private double rewardPowerPillGrab(Game state){
		double reward = 0.0;
		int pacmanNodeIndex = state.getPacmanCurrentNodeIndex();
		for(GHOST ghost : GHOST.values()){
			int ghostNodeIndex = state.getGhostCurrentNodeIndex(ghost);
			double distanceFromPacman = state.getShortestPathDistance(pacmanNodeIndex, ghostNodeIndex);
			if(distanceFromPacman < ghostDistanceAfterPPThreshold){
				double notNormalizedReward = Math.abs(distanceFromPacman - ghostDistanceAfterPPThreshold);
				double normalizedReward = (notNormalizedReward - 0) / (ghostDistanceAfterPPThreshold - 0);
				reward += normalizedReward * ghostDistanceAfterPPRewardMult;
			}
		}
		return reward;
	}
	private double getReward(Game prevState, Game finalState){
		double reward = 0.0; 
		// Level complete reward
		// After eating the last pill, a new level is loaded. Next state has 240 pills. This number becomes negative, and we can say the level was completed
		int pillsEaten = prevState.getNumberOfActivePills() - finalState.getNumberOfActivePills();
		if(finalState.getNumberOfActivePills() == 0 || finalState.gameOver() || pillsEaten < 0){
			return levelCompleteReward;
		}
		reward += (pillsEaten * pillEatenReward) ;
		return reward;
	}
	
	private boolean areGhostsEdible(Game state) {
		for(GHOST ghost : GHOST.values()){
			if(state.isGhostEdible(ghost)){
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean isTerminalState(Game state) {
		return state.wasPacManEaten() || state.getNumberOfActivePills() == 0;
	}
	
	protected List<MOVE> getValidAcitons(Game state) {
		List<MOVE> result = new LinkedList<MOVE>();
		boolean ghostWasEaten = wasGhostEaten(state);
		boolean powerPillEaten = state.wasPowerPillEaten();
		MOVE[] validMoves = null;
		
		if(ghostWasEaten || powerPillEaten){
			validMoves = state.getPossibleMoves(state.getPacmanCurrentNodeIndex());
		} else {
			validMoves = state.getPossibleMoves(state.getPacmanCurrentNodeIndex(), state.getPacmanLastMoveMade());
		}
		
		for(MOVE move : validMoves ){
			result.add(move);
		}
		return result;
	}
	
	private boolean wasGhostEaten(Game game) {
		for(GHOST ghost : GHOST.values()){
			if(game.wasGhostEaten(ghost)){
				return true;
			}
		}
		return false;
	}

	@Override
	protected Game computeStateAfterAction(Game stateInfo, MOVE action) {
		Game stateClone = getCopy(stateInfo);
		
		EnumMap<GHOST,MOVE> ghostMoves = new EnumMap<GHOST, MOVE>(GHOST.class);
		ghostMoves.put(GHOST.BLINKY, getLegacy2GhostMove(stateClone, GHOST.BLINKY));
		ghostMoves.put(GHOST.INKY, getLegacy2GhostMove(stateClone, GHOST.INKY));
		ghostMoves.put(GHOST.PINKY, getLegacy2GhostMove(stateClone, GHOST.PINKY));
		ghostMoves.put(GHOST.SUE, getLegacy2GhostMove(stateClone, GHOST.SUE));
		
		stateClone.advanceGame(action, ghostMoves);
		
		return stateClone;
	}
	
	private MOVE getLegacy2GhostMove(Game state, GHOST ghost){
		EnumMap<GHOST, MOVE> ghostMoves = ghosts.getMove(state, -1);
		return ghostMoves.get(ghost);
	}
}
