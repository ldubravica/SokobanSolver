import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.CAction;
import game.actions.compact.CMove;
import game.actions.compact.CPush;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

import java.awt.geom.Point2D;
import java.util.*;

import static java.lang.System.out;


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

		frontier.add(new Node(initialBoard, (double) 0));
		cameFromBoard.put(initialBoard, null);
		cameFromAction.put(initialBoard, null);
		costSoFar.put(initialBoard, 0);

		boolean[][] deadSquares = DeadSquareDetector.detect(board);
//		int statesExplored = 1;

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

			// LOOP THROUGH POSSIBLE ACTIONS AND ADD FEASIBLE BOARDS

			for (CAction action : actions) {
				BoardCompact nextBoard = currentBoard.clone();
				action.perform(nextBoard);

				if (DeadSquareDetector.deadBoxes(nextBoard, deadSquares)) continue;
//				if (!costSoFar.containsKey(nextBoard)) statesExplored++;

				int actionCost = 1;
				int newCost = costSoFar.get(currentBoard) + actionCost;

				if (!costSoFar.containsKey(nextBoard) || newCost < costSoFar.get(nextBoard)) {
					double priority = newCost + heuristic(nextBoard);
					frontier.add(new Node(nextBoard, priority));

					costSoFar.put(nextBoard, newCost);
					cameFromBoard.put(nextBoard, currentBoard);
					cameFromAction.put(nextBoard, action);
				}
			}
		}

//		System.out.println("States explored: " + statesExplored);

		// ASSEMBLE THE LIST OF ACTIONS

		BoardCompact currentBoard = goalBoard;
		BoardCompact previousBoard = cameFromBoard.get(currentBoard);
		CAction previousAction = cameFromAction.get(currentBoard);

		while (previousAction != null && previousBoard != null) {
			result.add(previousAction.getDirection());
			currentBoard = previousBoard;
			previousBoard = cameFromBoard.get(currentBoard);
			previousAction = cameFromAction.get(currentBoard);
		}

		Collections.reverse(result);

		return false;
	}

	private double heuristic(BoardCompact boardCompact) {
		int h = 0;
		ArrayList<Point2D> boxes = boxesLocation(boardCompact);
		ArrayList<Point2D> goals = goalsLocation(boardCompact);

		for(Point2D box : boxes){
			double dist = Integer.MAX_VALUE;
			Point2D closestGoal = goals.get(0);

			for(Point2D goal : goals){
				double temp = manhattanDistance(box, goal);
				if (temp < dist) {
					dist = temp;
					closestGoal = goal;
				}
			}

			goals.remove(closestGoal);
			h += dist;
		}

		return h;
	}

	public static ArrayList<Point2D> boxesLocation(BoardCompact boardCompact) {
		ArrayList<Point2D> boxesLocation = new ArrayList<>();
		for (int x = 0; x < boardCompact.width(); x++) {
			for (int y = 0; y < boardCompact.height(); y++) {
				// stores the location if it contains a box
				if (CTile.isSomeBox(boardCompact.tile(x,y))) {
					boxesLocation.add(new Point2D.Double(x,y));
				}
			}
		}
		return boxesLocation;
	}

	public static ArrayList<Point2D> goalsLocation(BoardCompact boardCompact) {
		ArrayList<Point2D> goalsLocation = new ArrayList<>();
		for (int x = 0; x < boardCompact.width(); x++) {
			for (int y = 0; y < boardCompact.height(); y++) {
				// stores the location if it is a goal for some box
				if (CTile.forSomeBox(boardCompact.tile(x,y))) {
					goalsLocation.add(new Point2D.Double(x,y));
				}
			}
		}
		return goalsLocation;
	}

	public double manhattanDistance(Point2D box, Point2D goal) {
		return Math.abs((box.getX() - goal.getX())) + Math.abs((box.getY() - goal.getY()));
	}

	public double euclideanDistance(Point2D box, Point2D goal) {
		return Math.sqrt(Math.pow(box.getX() - goal.getX(), 2) + Math.pow(box.getY() - goal.getY(), 2));
	}

}


class Node {

	BoardCompact boardCompact;
	Double priority;

	Node(BoardCompact boardCompact, Double priority) {
		this.boardCompact = boardCompact;
		this.priority = priority;
	}

}


class NodeComparator<S> implements Comparator<Node> {

	public int compare(Node n1, Node n2) {
		if (n1.priority > n2.priority)
			return 1;
		else if (n1.priority < n2.priority)
			return -1;
		return 0;
	}

}


class DeadSquareDetector {

	// RETURNS BOOLEAN GRID WITH ALL DEAD SQUARES
	public static boolean[][] detect(BoardCompact boardCompact) {
		boolean[][] deadSquares = new boolean[boardCompact.width()][boardCompact.height()];

		for (int x = 0; x < boardCompact.width(); x++) {
			for (int y = 0; y < boardCompact.height(); y++) {
				if (CTile.isWall(boardCompact.tile(x,y))
						|| MyAgent.goalsLocation(boardCompact).contains(new Point2D.Double(x,y))) continue;

				boolean leftWall = x == 0 || CTile.isWall(boardCompact.tile(x-1,y));
				boolean rightWall = x == boardCompact.width() - 1 || CTile.isWall(boardCompact.tile(x+1,y));
				boolean topWall = y == 0 || CTile.isWall(boardCompact.tile(x,y-1));
				boolean bottomWall = y == boardCompact.height() - 1 || CTile.isWall(boardCompact.tile(x,y+1));

				if (leftWall && topWall || leftWall && bottomWall) deadSquares[x][y] = true;
				if (rightWall && topWall || rightWall && bottomWall) deadSquares[x][y] = true;
			}
		}

		return deadSquares;
	}

	// CHECKS WHETHER THERE ARE BOXES IN THE DEAD SQUARES
	public static boolean deadBoxes(BoardCompact boardCompact, boolean[][] deadSquares) {
		for (int x = 0; x < boardCompact.width(); x++) {
			for (int y = 0; y < boardCompact.height(); y++) {
				if (deadSquares[x][y] && CTile.isSomeBox(boardCompact.tile(x,y))) return true;
			}
		}
		return false;
	}

}