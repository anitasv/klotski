package me.anitas;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class Klotski {

    private final char[] pieces;

    private final String initPosition;

    public Klotski(char[] pieces, String initPosition) {
        this.pieces = pieces;
        this.initPosition = initPosition;
    }

    public static Klotski create(String initPosition) {
        Set<Character> temp = new HashSet<>();
        for (int i = 0; i < initPosition.length(); i++) {
            char ch = initPosition.charAt(i);
            if (ch != '.' && ch != ' ' && ch != '\n') {
                temp.add(ch);
            }
        }
        char[] pieces = new char[temp.size()];

        int i = 0;
        for (char ch : temp) {
            pieces[i++] = ch;
        }
        return new Klotski(pieces, initPosition);
    }

    @Data
    public static class MoveTuple {

        private final int opt;

        private final char ch;
    }

    @Data
    public class Edge implements Bfs.Edge<String, MoveTuple> {

        private final String val;

        private final MoveTuple label;
    }

    private List<MoveTuple> generateMoves1(String position, Set<Character> rejCh) {
        List<MoveTuple> moves = new ArrayList<>();
        for (char ch : pieces) {
            if (!rejCh.contains(ch)) {
                moves.addAll(generateMoves2(position, ch));
            }
        }
        return moves;
    }

    private List<MoveTuple> generateMoves2(String position, char ch) {
        List<MoveTuple> moves = new ArrayList<>();
        int[] options = { -5, -1, +1, +5};
        for (int opt : options) {
            boolean valid = true;
            for (int i = 0, j = opt; i < position.length(); i++, j++) {
                if (position.charAt(i) == ch) {
                    if (j < 0 || j >= position.length()) {
                        valid = false;
                        break;
                    }
                    char target = position.charAt(j);
                    if (target != ch && target != ' ') {
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                moves.add(new MoveTuple(opt, ch));
            }
        }
        return moves;
    }

    private List<Edge> generateMoves(Bfs.Node<String, MoveTuple> node) {
        Set<Character> blacklist = new HashSet<>();
        for (Bfs.Parent<String, MoveTuple> parent : node.getParents()) {
            blacklist.add(parent.getEdge().getCh());
        }
        List<MoveTuple> moves = generateMoves1(node.getValue(), blacklist);
        List<Edge> finalPositions = new ArrayList<>();
        for (MoveTuple moveTuple : moves) {
            String nextPosition = computeMove(node.getValue(), moveTuple.opt, moveTuple.ch);
            finalPositions.add(new Edge(nextPosition, moveTuple));
            List<MoveTuple> secondMoves = generateMoves2(nextPosition, moveTuple.ch);
            for (MoveTuple second : secondMoves) {
                String nextNextPosition = computeMove(nextPosition, second.opt, second.ch);
                if (!nextNextPosition.equals(node.getValue())) {
                    finalPositions.add(new Edge(nextNextPosition, second));
                }
            }
        }
        return finalPositions;
    }

    private String computeMove(String position, int opt, char ch) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < position.length(); i++) {
            char current = position.charAt(i);
            int j = i - opt;
            if (j >= 0 && j < position.length()) {
                char source = position.charAt(j);
                if (source == ch) {
                    out.append(source);
                } else if (current == ch) {
                    out.append(' ');
                } else {
                    out.append(current);
                }
            } else {
                if (current == ch) {
                    out.append(' ');
                } else {
                    out.append(current);
                }
            }
        }
        return out.toString();
    }

    private static boolean accepted(String position) {
        int n = position.indexOf('#');
        return n == 16;
    }

    private boolean solve() throws InterruptedException {
        ForkJoinPool fjp = ForkJoinPool.commonPool();

        Bfs<String, MoveTuple> bfs = new Bfs<>(
                Klotski::accepted,
                this::generateMoves,
                400000,
                fjp);

        Bfs.Node<String, MoveTuple> solution = bfs.apply(initPosition);
        if (solution == null) {
            return false;
        } else {
            printSolution(solution);
            return true;
        }
    }

    private void printSolution(Bfs.Node<String, MoveTuple> node) {
        System.out.println("Solution:");
        List<String> seq = new ArrayList<>();
        while (node != null) {
            seq.add(node.getValue());
            if (node.getParents().isEmpty()) {
                break;
            } else {
                node = node.getParents().get(0).getNode();
            }
        }
        for (int a = seq.size() - 1; a >= 0; a--) {
            System.out.println(seq.get(a));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String positionForgetMeNot =
                "1##2\n" +
                "1##2\n" +
                "3445\n" +
                "3675\n" +
                "8  9\n";

        String position120 =
                "1##2\n" +
                "3##4\n" +
                "3554\n" +
                "6778\n" +
                " 99 \n";

        String pennant =
                "##11\n" +
                "##22\n" +
                "34  \n" +
                "5677\n" +
                "5688\n";

        long startTime = System.nanoTime();
        System.out.println(Klotski.create(positionForgetMeNot).solve());
        long endTime = System.nanoTime();
        System.out.println(TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
    }

}
