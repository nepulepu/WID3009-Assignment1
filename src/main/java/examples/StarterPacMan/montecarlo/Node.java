package examples.StarterPacMan.montecarlo;

import java.util.ArrayList;
import java.util.List;

import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Class to store node information, e.g.
 * state, children, parent, accumulative reward, visited times
 * @author dariusv
 * @modified A. Hartzen
 *
 */
public class Node{

	public int NodePos;
	public List<Node> children = new ArrayList<Node>();
	public Node parent = null;
	public int parentAction=-1;
	public float reward =0;
	public int timesvisited = 0;
	public MOVE move;

	public boolean IsFullyExpanded(Game game) {
		int[] n = game.getNeighbouringNodes(NodePos);
		return n.length == children.size();
	}

	public Node Expand(Game game) {
		int[] n = game.getNeighbouringNodes(NodePos);
		Node next = null;

		for (int x : n) {
			boolean ok = true;
			for (Node c : children) {			
				if (c.NodePos == x) {
					ok = false;
				}
			}
			if (ok) {
				next = new Node();
				next.NodePos = x;
				next.parent = this;
				next.move = game.getMoveToMakeToReachDirectNeighbour(this.NodePos, next.NodePos);
				//			System.out.println("Parent node: " + this.NodePos + ", child node: " + next.NodePos + ", Adding move: " + next.move);
				this.children.add(next);
				return next;
			}
		}
		return next;
	}
}
