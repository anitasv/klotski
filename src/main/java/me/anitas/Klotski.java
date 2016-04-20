package me.anitas;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Klotski {

    private final Set<String> visited = new HashSet<>();

    private final Set<Character> pieces = new HashSet<>();

    private final String initPosition;

    public Klotski(String initPosition) {
        this.initPosition = initPosition;
    }

    public static class MoveTuple {
        int opt;
        char ch;

        public MoveTuple(int opt, char ch) {
            this.opt = opt;
            this.ch = ch;
        }
    }

    private List<MoveTuple> generateMoves1(String position, Character rejCh) {
        List<MoveTuple> moves = new ArrayList<>();
        for (Character ch : pieces) {
            if (ch != rejCh) {
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
            for (int i = 0; i < position.length(); i++) {
                if (position.charAt(i) == ch) {
                    if (i + opt < 0 || i + opt >= position.length()) {
                        valid = false;
                        break;
                    }
                    char target = position.charAt(i + opt);
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

    public static class Move {
        char ch;
        String position;

        public Move(char ch, String position) {
            this.ch = ch;
            this.position = position;
        }
    }

    private List<Move> generateMoves(String position, Character rejCh) {
        List<MoveTuple> moves = generateMoves1(position, rejCh);
        List<Move> finalPositions = new ArrayList<>();
        for (MoveTuple moveTuple : moves) {
            String nextPosition = computeMove(position, moveTuple.opt, moveTuple.ch);
            finalPositions.add(new Move(moveTuple.ch, nextPosition));
            List<MoveTuple> secondMoves = generateMoves2(nextPosition, moveTuple.ch);
            for (MoveTuple second : secondMoves) {
                String nextNextPosition = computeMove(nextPosition, second.opt, second.ch);
                finalPositions.add(new Move(second.ch, nextNextPosition));
            }
        }
        return finalPositions;
    }

    private String computeMove(String position, int opt, char ch) {
        String out = "";
        for (int i = 0; i < position.length(); i++) {
            char current = position.charAt(i);
            int j = i - opt;
            if (j >=0 && j < position.length()) {
                char source = position.charAt(j);
                if (source == ch) {
                    out += source;
                } else if (current == ch) {
                    out += ' ';
                } else {
                    out += current;
                }
            } else {
                if (current == ch) {
                    out += ' ';
                } else {
                    out += current;
                }
            }
        }
        return out;
    }

    private int lowerBound(String position) {
        int n = position.indexOf('#');
        int nr = n / 5;
        int nc = n % 5;
        return Math.abs(nr - 3) + Math.abs(nc - 1);
    }

    private static class Choice implements Comparable<Choice> {
        int height;
        int lowerBound;
        Move move;
        Choice parent;

        public Choice(int height, int lowerBound, Move move, Choice parent) {
            this.height = height;
            this.lowerBound = lowerBound;
            this.move = move;
            this.parent = parent;
        }

        @Override
        public int compareTo(Choice o) {
            return Integer.compare(lowerBound + height, o.lowerBound + o.height);
        }
    }

    private boolean solve() {
        for (int i = 0; i < initPosition.length(); i++) {
            char ch = initPosition.charAt(i);
            if (ch != '.' && ch != ' ' && ch != '\n') {
                pieces.add(ch);
            }
        }

        Queue<Choice> topChoices = new ArrayDeque<>();
        topChoices.add(new Choice(0, lowerBound(initPosition), new Move('\0', initPosition), null));

        int bestScore = Integer.MAX_VALUE;

        int progress = 0;
        while (!topChoices.isEmpty()) {
            Choice choice = topChoices.poll();
            Move lastMove = choice.move;
            int height = choice.height;
            int lowerBound = choice.lowerBound;
            String position = lastMove.position;

            if (choice.height > progress) {
                progress = choice.height;
                System.out.println("Progress: " + progress);
            }
            if (lowerBound == 0) {
                int score = lowerBound + height;
                if (score < bestScore) {
                    bestScore = score;
                    System.out.println("------- START ---- SCORE: " + bestScore);
                    printSolution(choice);
                    System.out.println("------- END   ---- SCORE: " + bestScore);
                    return true;
                }
            }
            List<Move> moves = generateMoves(position, lastMove.ch);

            for (Move move : moves) {
                if (visited.contains(move.position)) {
                    continue;
                }
                visited.add(move.position);
                int nextHeight = height + 1;
                int nextLowerBound = lowerBound(move.position);
                topChoices.add(new Choice(nextHeight, nextLowerBound, move, choice));
            }
        }
        System.out.println(topChoices.size());
        return false;
    }

    private void printSolution(Choice choice) {
        List<String> seq = new ArrayList<>();
        while (choice != null) {
            seq.add(choice.move.position);
            choice = choice.parent;
        }
        for (int a = seq.size() - 1; a >= 0; a--) {
            System.out.println(seq.get(a));
        }
    }

    public static void main(String[] args) {
        String positionForgetMeNot =
                "1##2\n" +
                "1##2\n" +
                "3445\n" +
                "3675\n" +
                " 8 9\n";

        String position120 =
                "1##2\n" +
                "3##4\n" +
                "3554\n" +
                "6778\n" +
                " 99 \n";

        long startTime = System.nanoTime();
        System.out.println(new Klotski(position120).solve());
        long endTime = System.nanoTime();
        System.out.println(TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
    }

}
