package examples.StarterPacMan.montecarlo;

import java.util.EnumMap;
import java.util.Stack;

import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class MonteCarloSimulator {

	public static final int MAX_SIMULATION_LENGTH = 1000;
	private Stack<Game> gameStack = new Stack<Game>();
	private Node rootNode;
	private Game game;
	Controller<EnumMap<GHOST,MOVE>> ghostController = new StarterGhosts();
	Controller<MOVE> pacManController = new StarterPacMan();
	long startTime, stopTime;

	int currentLives, currentLevel;
	/**
	 * Exploration coefficient
	 */
	private float C = (float) (1.0/Math.sqrt(2));

	public MonteCarloSimulator(Game game, long timeDue) {
		if (timeDue < 0) {
			timeDue = System.currentTimeMillis() + 40;
		}
		this.game = game;
		startTime = System.currentTimeMillis();
		stopTime = timeDue;
		currentLives = game.getPacmanNumberOfLivesRemaining();
		currentLevel = game.getCurrentLevel();
	}

	public MOVE MCTreeSearch() {
		System.out.println("Tree search");
		rootNode = new Node();
		rootNode.NodePos = game.getPacmanCurrentNodeIndex();
		pushToGameStack();
		try {			
			advanceGameToJunction();
			int iter = 0;
			while (!terminate()) {
				iter++;
				pushToGameStack();
				Node vl = TreePolicy(rootNode);
				float delta = DefaultPolicy(vl);
				//				System.out.println("Reward: " + delta); 
				Backup(vl, delta);
				popFromGameStack();
			}
			System.out.println("Iterations: " + iter);
		}
		finally {
			popFromGameStack();
		}
		Node best = BestChild(rootNode, C);
		if (best != null) {
			return best.move;
		}
		return MOVE.NEUTRAL;
	}

	/**
	 * Tree policy for selecting best child to simulate.
	 * Advances the game (in subroutines) to be in the state represented 
	 * by the returned child node.
	 * @param current rootNode
	 * @return the best child
	 */
	private Node TreePolicy(Node current) {
		while (!terminalState()) {
			if (!current.IsFullyExpanded(game)) {
				return Expand(current);
			}
			else {
				current = BestChild(current, C);
			}
		}
		return current;
	}


	/**
	 * Expand and advances the game state for the new node
	 * @param current
	 * @return
	 */
	private Node Expand(Node current) {
		Node child = current.Expand(game);
		playMove(child.move);
		return child;
	}

	private void playMove(MOVE move) {
		game.advanceGame(move, ghostController.getMove(game, 0));
	}

	/**
	 * Uses UCT value to find the best child, ie. the child with 
	 * maximum UCT value. Advances the game to be in the state of
	 * the best child. Assumes that the game has been advanced to
	 * the state of current
	 * @param current parent of the children
	 * @param c Exploration coefficient
	 * @return the best child
	 */
	private Node BestChild(Node current, float c) {
		//		if (current.equals(rootNode)) {
		//			for (Node ch : current.children) {
		//				System.out.println("Child move: " + ch.move);
		//			}
		//		}
		Node bestChild = null;
		float bestVal = Integer.MIN_VALUE;

		for (Node child : current.children) {
			float val = UCTvalue(child, C);
			if (val > bestVal) {
				bestChild = child;
				bestVal = val;
			}
		}
		if (bestChild != null) {
			playMove(bestChild.move);
//			advanceGameToJunction();
		}
		return bestChild;
	}

	/**
	 * Calculate UCT value for the best child choosing
	 * @param n child node of currentNode
	 * @param c Exploration coefficient
	 * @return
	 */
	private float UCTvalue(Node n, float c) {
		return (float) (n.reward / n.timesvisited + c *
				Math.sqrt((2 * Math.log(n.parent.timesvisited) / n.timesvisited)));
	}


	/**
	 * Simulation of game (playout) from node vl to the end using pacManController
	 * @param vl child node to start simulation from
	 * @return the outcome (reward) of the simulation
	 */
	private float DefaultPolicy(Node vl) { 
		// TODO Auto-generated method stub
		int level = game.getCurrentLevel();
		int i = 0;
		int length = 4000 - game.getCurrentLevelTime();
		while (i++ < length && game.getPacmanNumberOfLivesRemaining() == currentLives &&
				!game.gameOver() && level == game.getCurrentLevel()) {
			game.advanceGame(pacManController.getMove(game, 0), ghostController.getMove(game, 0));
		}
		int score = game.getScore();
		if (game.getPacmanNumberOfLivesRemaining() < currentLives) {
			// Pacman died - negative reward
//			System.out.println("Ouch");
			score -= 5000;
		}
		return score;
	}

	/**
	 * Updates the value of the simulation run on node vl
	 * with reward delta. All ancestors of vl gets their
	 * reward increased with delta and their visited count
	 * increased by 1.
	 * @param vl the node that had been simulated
	 * @param delta the value (reward) of the simulation
	 */
	private void Backup(Node vl, float delta) {
		// TODO Auto-generated method stub
		Node v = vl;
		while (v != null) {
			v.timesvisited++;
			v.reward += delta;
			v = v.parent;
		}
	}

	private MOVE getBestMove() {
		return MOVE.NEUTRAL;
	}

	private boolean terminalState() {
		return game.gameOver() || game.getPacmanNumberOfLivesRemaining() != currentLives || game.getCurrentLevel() != currentLevel;
	}

	private boolean terminate() {
		long time = System.currentTimeMillis();
		return time >= stopTime;
	}

	/**
	 * Save a state of the game on top of the stack
	 * @return the state
	 */
	private Game pushToGameStack() {
		gameStack.push(game);
		game = game.copy();
		return game;
	}

	/**
	 * Get the top most game state from the stack
	 * @return the top most state
	 */
	private Game popFromGameStack() {
		game = gameStack.pop();
		return game;
	}

	private void advanceGameToJunction() {
		MOVE move = game.getPacmanLastMoveMade();

		while (!isAtJunction(move)) {
			game.advanceGame(move, ghostController.getMove(game.copy(), -1));
		}
	}

	/**
	 * Checks if pacman is at a junction or game is ended
	 * @return
	 */
	private boolean isAtJunction(MOVE move) {
		return true;
//		return game.isJunction(game.getPacmanCurrentNodeIndex()) || isAtWall(move) || game.gameOver();
	}

	private boolean isAtWall(MOVE move) {
		MOVE[] possibles = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());

		for (MOVE m : possibles) {
			if (m == move) {
				return false;
			}
		}
		return true;
	}

}
