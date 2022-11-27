package examples.StarterPacMan;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public abstract class MCTree<S, A> {
	
	public class MCNode {
		public S state;
		public List<MCNode> children = new ArrayList<MCNode>();
		public MCNode parent = null;
		public A parentAction = null;
		public double reward = 0;
		public int timesVisited = 0;
		
		
		public MCNode(S state){
			this.state = state;
		}
	}
	
	
	private Random random = new Random();
	// Exploration coefficient
	protected double C = (float) (1.0 / Math.sqrt(2));

	protected abstract S getCopy(S state);
	protected void preRunHook(S state){ ; }
	
	public A runMCTSearch(S state) throws InterruptedException {
		preRunHook(state);
		MCNode rootNode = new MCNode(getCopy(state));
		MCNode currentNode = null;
		int iterations = 0;
		while (!terminateSearch() && iterations < getMaxIterations(state)) {
			iterations++;
			//System.out.println("Iteration #"+iterations);
			currentNode = treePolicy(rootNode);
			//System.out.println("Tree policy done");
			double reward = defaultPolicy(currentNode.state);
			//System.out.println("Default policy done");
			backpropagate(currentNode, reward);
			//System.out.println("BackPropagate done");
		}
		//System.out.println("MCTree terminated");

		/* Get the action that directs to the best node
		 * rootNode is the one we are working with
		 * and we apply the exploitation of it to find the child with the highest average reward */
		currentNode = bestChild(rootNode, C);
		A bestAction = currentNode.parentAction;

		return bestAction;
	}
	
	protected abstract int getMaxIterations(S state);
	protected abstract double defaultPolicy(S state);

	/**
	 * Expand the nonterminal nodes with one available child. Chose a node to
	 * expand with BestChild method
	 */
	private MCNode treePolicy(MCNode rootNode) {
		MCNode currentNode = rootNode;

		while (!isTerminalState(currentNode.state)) {
			if(!isFullyExpanded(currentNode)){
				return expand(currentNode);
			} else {
				currentNode = bestChild(currentNode, C);
			}
		}
		return currentNode;
	}

	protected abstract boolean isTerminalState(S state);

	
	private boolean isFullyExpanded(MCNode node){
		List<A> validActions = getValidAcitons(node.state);
		return (node.children.size() == validActions.size());
	}

	protected abstract List<A> getValidAcitons(S state);

	/**
	 * Expand the current node by adding new child to the currentNode
	 */
	private MCNode expand(MCNode node) {
		// Choose untried action
		A action = getUntriedAction(node);
		
		// Create a child, populate it and add fit to the node
		MCNode child = new MCNode(computeStateAfterAction(node.state, action));
		
		node.children.add(child);
		child.parentAction = action;
		child.parent = node;
		return child;
	}

	/**
	 * Assign the received reward to every parent of the parent up to the
	 * rootNode. Increase the visited count of every node included in
	 * backpropagation
	 * 
	 * @param reward
	 */
	private void backpropagate(MCNode node, double reward) {
		while (node.parent != null) {
			node.timesVisited++;
			node.reward += reward;
			node = node.parent;
		}
		//root node
		node.timesVisited++;
		node.reward += reward;
	
	}

	/**
	 * Choose the best child according to the UCT value Assign it as a
	 * currentNode
	 * 
	 * @param c
	 */
	private MCNode bestChild(MCNode parentNode, double c) {
		MCNode bestChild = null;

		List<MCNode> children = parentNode.children;
		double bestChildScore = Integer.MIN_VALUE;
		for (MCNode node : children) {
			double childNodeScore = getUCTvalue(node, c);
			if (childNodeScore > bestChildScore) {
				bestChild = node;
				bestChildScore = childNodeScore;
			}
		}
		return bestChild;
	}

	/**
	 * Calculate UCT value for the best child choosing
	 * 
	 * @param n
	 * @param c
	 * @return
	 */
	private double getUCTvalue(MCNode node, double c) {
		double childNodeScore = (node.reward / node.timesVisited)+ c *(Math.sqrt((2.0 * Math.log(node.parent.timesVisited))
				/ node.timesVisited));
		
		if (Double.isNaN(childNodeScore)) {
			childNodeScore = 0.0;
		}
		return childNodeScore;
	}

	protected abstract S computeStateAfterAction(S stateInfo, A action);

	/**
	 * Returns the first untried action of the node
	 * 
	 * @param n
	 * @return
	 */
	private A getUntriedAction(MCNode node) {
		List<A> untriedActions = new LinkedList<A>();
		List<A> validActions = getValidAcitons(node.state);
		for(A action : validActions){
			boolean untried = true;
			for(MCNode child : node.children){
				if(action == child.parentAction){
					untried = false;
					break;
				}
			}
			if(untried){
				untriedActions.add(action);
			}
		}
		int randomIndex = random.nextInt(untriedActions.size());
		A randomUntriedAction = untriedActions.get(randomIndex);
		return randomUntriedAction;
	}

	/**
	 * Check if the algorithm is to be terminated. Does not include max interation check, 
	 * which is done thanks to the getMaxIterations method
	 * 
	 * @param i
	 * @return
	 */
	protected boolean terminateSearch(){
		return false;
	}
}