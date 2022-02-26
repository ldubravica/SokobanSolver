import static java.lang.System.out;

import java.util.*;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.*;
import game.board.compact.BoardCompact;

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
		return 0;
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