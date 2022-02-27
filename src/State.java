import game.actions.compact.CAction;
import game.board.compact.BoardCompact;

public class State implements Comparable<State>{
    BoardCompact b;
    double h;
    double g;
    CAction c;

    public State(BoardCompact b, double h, double g, CAction c) {
        this.b = b;
        this.h = h;
        this.g = g;
        this.c = c;
    }

    @Override
    public int compareTo(State o) {
        return 0;
    }
}