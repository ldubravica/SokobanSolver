import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.CAction;
import game.actions.compact.CMove;
import game.actions.compact.CWalk;
import game.actions.compact.CWalkPush;
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
	public static boolean[][] detect_de(BoardCompact board) {

		ArrayList<Point2D> goals = MyAgent.goalsLocation(board);
		boolean[][] deadstates = new boolean[board.width()][board.height()];
		//setting all the tiles to be dead initially
		for(boolean[] row: deadstates){
			Arrays.fill(row,true);
		}

		for (Point2D goal : goals) {
			//see if you can get to the player from each goal.
			List<Point2D> tiles = new ArrayList<>();
			BoardCompact cloneBoard = board.clone();
			int playerx  = cloneBoard.playerX;
			int playery = cloneBoard.playerY;


			//get a box, move it to the goal.
			Point2D randomBox = MyAgent.boxesLocation(cloneBoard).get(0);
			cloneBoard.moveBox( (int) randomBox.getX() ,(int) randomBox.getY() ,(int) goal.getX(),(int) goal.getY());
			CWalk walk = null;
			if(CTile.isFree(cloneBoard.tiles[(int) randomBox.getX() - 1][(int) randomBox.getY()])){
				walk =  new CWalk((int)goal.getX() -1,(int)goal.getY());
			}
			else if(CTile.isFree(cloneBoard.tiles[(int) randomBox.getX() ][(int) randomBox.getY()] - 1)){
				walk =  new CWalk((int)goal.getX(),(int)goal.getY() - 1);
			}
			else if(CTile.isFree(cloneBoard.tiles[(int) randomBox.getX() +1][(int) randomBox.getY()])){
				walk =  new CWalk((int)goal.getX() + 1,(int)goal.getY());
			}
			else if(CTile.isFree(cloneBoard.tiles[(int) randomBox.getX() ][(int) randomBox.getY()] + 1) ){
				walk =  new CWalk((int)goal.getX(),(int)goal.getY() + 1);
			}
			else {//?
			}

			if(!walk.isPossible(cloneBoard)) return null;
			walk.perform(cloneBoard);

			detect_dfs(6,tiles,cloneBoard,playerx,playery,deadstates);
		}

		return deadstates;
	}


	public static void detect_dfs(int level , List<Point2D> tiles , BoardCompact board , int playerx,int playery
			,boolean[][] deadStates){
		if (level <= 0) return; // DEPTH-LIMITED
		List<CAction> actions = new ArrayList<CAction>(4);

		// COLLECT POSSIBLE ACTIONS

		for (CMove move : CMove.getActions()) {
			if (move.isPossible(board)) {
				actions.add(move);
			}
		}
		for (CPush push : CPush.getActions()) {
			if (push.isPossible(board)) {
				actions.add(push);
			}
		}

		for(CAction action : actions){
			action.perform(board);
			//current location of our agent on the tiles. we add it to the path.
			tiles.add(new Point2D.Double(board.playerX,board.playerY));

			if(board.playerX == playerx  && board.playerY == playery){
				//we gucci, all the tiles leading up to this were alive.
				for(Point2D p : tiles){
					deadStates[(int)p.getX()][(int)p.getY()] = false;
				}
			}
			detect_dfs(level - 1, tiles , board , playerx , playery, deadStates);
			action.reverse(board);
		}
		//we aint gucci
		return;
	}


	public static boolean[][] detect(BoardCompact board){
		ArrayList<Point2D> goals = MyAgent.goalsLocation(board);
		boolean[][] deadstates = new boolean[board.width()][board.height()];
		//setting all the tiles to be dead initially
		for(boolean[] row: deadstates){
			Arrays.fill(row,true);
		}

		for (Point2D goal : goals){
			deadstates[(int)goal.getX()][(int)goal.getY()] = false;
		}


		for (Point2D goal : goals) {
			ArrayList<Point2D> visited = new ArrayList<>();
			dfs(board,(int)goal.getX() ,(int)goal.getY(), deadstates, visited,EDirection.NONE);
//            dfs(board,(int)goal.getX() -1,(int)goal.getY(),deadstates, visited,EDirection.LEFT);
//            dfs(board,(int)goal.getX(),(int)goal.getY() -1,deadstates, visited,EDirection.UP);
//            dfs(board,(int)goal.getX()+1,(int)goal.getY(),deadstates, visited,EDirection.RIGHT);
//            dfs(board,(int)goal.getX(),(int)goal.getY() + 1,deadstates, visited,EDirection.DOWN);
		}
		return deadstates;
	}


	public static void dfs(BoardCompact board , int x, int y, boolean[][] deadstates,ArrayList<Point2D> visited
			, EDirection dir){

		if(CTile.isWall(board.tile(x,y))){
			return;
		}
		Point2D cur = new Point2D.Double(x,y);
		if( visited.contains(cur)){
			return;
		}

		if (dir.index == -1 ){
			//first time.
		}
		if(dir.index == 0){//up
			if(CTile.isFree(board.tile(x,y-1)))  {
				deadstates[x][y] = false;
			}
		}
		if(dir.index == 1){//right
			if(CTile.isFree(board.tile(x+1,y))) {
				deadstates[x][y] = false;
			}
		}
		if(dir.index == 2){//down
			if(CTile.isFree(board.tile(x,y+1))) {
				deadstates[x][y] = false;
			}
		}
		if(dir.index == 3){//left
			if(CTile.isFree(board.tile(x-1,y))) {
				deadstates[x][y] = false;
			}
		}

//
//		System.out.println("dead squares: ");
//		for (int z = 0 ; z < board.height() ; ++z) {
//			for (int a = 0 ; a < board.width() ; ++a)
//				System.out.print(CTile.isWall(board.tile(a, z)) ? '#' : (deadstates[a][z] ? 'X' : '_'));
//			System.out.println();
//		}

		visited.add(cur);
		//visited and the one next to it
		if(!deadstates[x][y]) {
			dfs(board, x, y - 1, deadstates, visited, EDirection.UP);
			dfs(board, x + 1, y, deadstates, visited, EDirection.RIGHT);
			dfs(board, x, y + 1, deadstates, visited, EDirection.DOWN);
			dfs(board, x - 1, y, deadstates, visited, EDirection.LEFT);
		}
		else return;


	}

	public static boolean[][] detectCorners(BoardCompact board) {
		boolean[][] b = new boolean[board.width()][board.height()];

		ArrayList<Point2D> allBoxes = MyAgent.boxesLocation(board);
		ArrayList<Point2D> allGoals = MyAgent.goalsLocation(board);
		int[][] t = board.tiles;
		for (int row = 0; row < board.width(); row++) {
			for (int col = 0; col < board.height(); col++) {
				//If this tile is walkable and not a goal tile
				if (CTile.isWalkable(board.tiles[row][col]) && !CTile.forSomeBox(board.tiles[row][col])) {
					//corner states. Shouldnt be out of bound since we're only traversing on walkable tiles which are
					//surrounded by walls in minimum.
					//top and left
					if (CTile.isWall(t[row - 1][col]) && CTile.isWall(t[row][col - 1])) {
						b[row][col] = true; //Is a dead state, corner state
					}
					//top and right
					else if (CTile.isWall(t[row + 1][col]) && CTile.isWall(t[row][col - 1])) {
						b[row][col] = true; //Is a dead state, corner state
					}
					//bottom and left
					else if (CTile.isWall(t[row - 1][col]) && CTile.isWall(t[row][col + 1])) {
						b[row][col] = true; //Is a dead state, corner state
					}
					//bottom and right
					else if (CTile.isWall(t[row + 1][col]) && CTile.isWall(t[row][col + 1])) {
						b[row][col] = true; //Is a dead state, corner state
					}

					//if not corner (and other to-be-added)
					else b[row][col] = false; //not a dead state.
				}
			}
		}
		System.out.println(board.toString());
		System.out.println(Arrays.deepToString(b));
		return b;
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