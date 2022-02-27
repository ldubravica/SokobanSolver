import static java.lang.System.out;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.*;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

/**
 * The simplest Tree-DFS agent.
 * @author Jimmy
 */
public class MyAgent extends ArtificialAgent {
	protected BoardCompact board;
	protected int searchedNodes;
	
	@Override
	protected List<EDirection> think(BoardCompact board) {
		this.board = board;
		searchedNodes = 0;
		long searchStartMillis = System.currentTimeMillis();
		
		List<EDirection> result = new ArrayList<EDirection>();
		search(result);

		long searchTime = System.currentTimeMillis() - searchStartMillis;
        
        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                        ((double)searchedNodes / (double)searchTime * 1000));
        }
		
		return result.isEmpty() ? null : result;
	}

	private boolean search(List<EDirection> result) {
		BoardCompact initialBoard = board.clone();
		BoardCompact goalBoard = null;

		PriorityQueue<Node> frontier = new PriorityQueue<>(new NodeComparator());
		Map<BoardCompact, BoardCompact> cameFromBoard = new HashMap<>();
		Map<BoardCompact, CAction> cameFromAction = new HashMap<>();
		Map<BoardCompact, Integer> costSoFar = new HashMap<>();

		frontier.add(new Node(initialBoard, 0));
		cameFromBoard.put(initialBoard, null);
		cameFromAction.put(initialBoard, null);
		costSoFar.put(initialBoard, 0);

		int statesExplored = 1;

		// find the goal state
		while (!frontier.isEmpty()) {
			BoardCompact currentBoard = frontier.poll().boardCompact;

			if (currentBoard.isVictory()) {
				goalBoard = currentBoard.clone();
				break;
			}

			// COLLECT POSSIBLE ACTIONS

			List<CAction> actions = new ArrayList<CAction>(4);

			for (CMove move : CMove.getActions()) {
				if (move.isPossible(currentBoard)) {
					actions.add(move);
				}
			}
			for (CPush push : CPush.getActions()) {
				if (push.isPossible(currentBoard)) {
					actions.add(push);
				}
			}

//			// PERFORM THE ACTION
//			result.add(action.getDirection());
//			action.perform(board);
//
//			// REVERSE ACTION
//			result.remove(result.size()-1);
//			action.reverse(board);

			for (CAction action : actions) {
				int actionCost = 1;
				BoardCompact nextBoard = currentBoard.clone();
				action.perform(nextBoard);

				int newCost = costSoFar.get(currentBoard) + actionCost;
				if (!costSoFar.containsKey(nextBoard)) statesExplored++;
				if (!costSoFar.containsKey(nextBoard) || newCost < costSoFar.get(nextBoard)) {
					costSoFar.put(nextBoard, newCost);
					int priority = newCost + heuristic(nextBoard);
					frontier.add(new Node(nextBoard, priority));
					cameFromBoard.put(nextBoard, currentBoard);
					cameFromAction.put(nextBoard, action);
				}
			}
		}

		System.out.println("States explored: " + statesExplored);

		// find the solution
//		List<CAction> actions = new ArrayList<>();  // series of actions from start state to goal state

		BoardCompact currentBoard = goalBoard;
		BoardCompact previousBoard = cameFromBoard.get(currentBoard);
		CAction previousAction = cameFromAction.get(currentBoard);

		while (previousAction != null && previousBoard != null) {
//			actions.add(previousAction);
			result.add(previousAction.getDirection());
			currentBoard = previousBoard;
			previousBoard = cameFromBoard.get(currentBoard);
			previousAction = cameFromAction.get(currentBoard);
		}

		Collections.reverse(result);

		return false;
	}

	private int heuristic(BoardCompact boardCompact) {
		int h = 0;
		ArrayList<Point2D> boxes = boxes_location(boardCompact);
		ArrayList<Point2D> goals = goals_location(boardCompact);

		for(Point2D box : boxes){
			h += heuristic_for_this_box(box , goals);
		}

		return h;
	}
	public int manhattan_dist(Point2D box , Point2D goal){
		return  Math.abs((int)(box.getX() - goal.getX())) + Math.abs((int)(box.getY() - goal.getY()));
	}
	public double euclidean_dist(Point2D box, Point2D goal){
		return Math.sqrt(  ((box.getX() - goal.getX()) * (box.getX() - goal.getX()))
				+ ((box.getY() - goal.getY()) * (box.getY() - goal.getY())));
	}

	public int heuristic_for_this_box(Point2D box_co, ArrayList<Point2D> goals){

//		int boxNum = CTile.getBoxNum(board.tiles[(int)box_co.getX()][(int)box_co.getY()]);
		int dist = Integer.MAX_VALUE;
		Point2D min = goals.get(0);

		for(Point2D goal : goals){
			int temp = manhattan_dist(box_co , goal);
			if (temp < dist) {
				dist = temp;
				min = goal;
			}
		}

		goals.remove(min);
		return dist;
	}

	public ArrayList<Point2D> boxes_location (BoardCompact boardCompact){
		ArrayList<Point2D> boxes_location = new ArrayList<>();
		for( int x = 0 ; x < boardCompact.width(); x++){
			for (int y = 0 ; y < boardCompact.height() ; y++){
				int tile = boardCompact.tile(x,y);
				if(CTile.isSomeBox(tile)){
					//box location
					Point2D loc = new Point2D.Double(x,y);
					boxes_location.add(loc);
				}
			}
		}
		return boxes_location;
	}


	public ArrayList<Point2D> goals_location (BoardCompact boardCompact){
		ArrayList<Point2D> goals_location = new ArrayList<>();
		for( int x = 0 ; x < boardCompact.width(); x++){
			for (int y = 0 ; y < boardCompact.height() ; y++){
				int tile = boardCompact.tile(x,y);
				if(CTile.forSomeBox(tile)){
					//goal location
					Point2D loc = new Point2D.Double(x,y);
					goals_location.add(loc);
				}
			}
		}
		return goals_location;
	}




}




class Node {

	BoardCompact boardCompact;
	Integer priority;

	Node(BoardCompact boardCompact, Integer priority) {
		this.boardCompact = boardCompact;
		this.priority = priority;
	}

}

class NodeComparator<S> implements Comparator<Node>{

	public int compare(Node n1, Node n2) {
		if (n1.priority > n2.priority)
			return 1;
		else if (n1.priority < n2.priority)
			return -1;
		return 0;
	}
}

class DeadSquareDetector {

	public static boolean[][] detect(BoardCompact board) {
		boolean[][] deadSquares = new boolean[board.width()][board.height()];

		for (int x = 0; x < board.width(); x++) {
			for (int y = 0; y < board.height(); y++) {
				int countWalls = 0;
				// TODO - IMPLEMENT CHECKING FOR CORNERS
			}
		}

		return deadSquares;
	}

}