package me.anitas;

import java.util.*;

public class Klotski {

    private final Set<String> visited = new HashSet<>();

    private final Set<Character> pieces = new HashSet<>();
    private final List<String> bfsQueue = new ArrayList<>();
    private final List<MoveTuple> bfsMoveTuple = new ArrayList<>();
    private final List<Integer> bfsParent = new ArrayList<>();

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

    private List<MoveTuple> generateMoves1(String position) {
        // TODO(anita): Add local introspection.
        List<MoveTuple> moves = new ArrayList<>();
        for (Character ch : pieces) {
            moves.addAll(generateMoves2(position, ch));
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

    private List<String> generateMoves(String position) {
        List<MoveTuple> moves = generateMoves1(position);
        List<String> finalPositions = new ArrayList<>();
        for (MoveTuple moveTuple : moves) {
            String nextPosition = computeMove(position, moveTuple.opt, moveTuple.ch);
            finalPositions.add(nextPosition);
//            List<MoveTuple> secondMoves = generateMoves2(nextPosition, moveTuple.ch);
//            for (MoveTuple second : secondMoves) {
//                finalPositions.add(computeMove(nextPosition, second.opt, second.ch));
//            }
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
        int bfsId;
        int height;
        int lowerBound;
        MoveTuple moveTuple;

        public Choice(int bfsId, int height, int lowerBound, MoveTuple moveTuple) {
            this.bfsId = bfsId;
            this.height = height;
            this.lowerBound = lowerBound;
            this.moveTuple = moveTuple;
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

        bfsQueue.add(initPosition);
        bfsParent.add(-1);

        PriorityQueue<Choice> topChoices = new PriorityQueue<>();
        topChoices.add(new Choice(0, 0, lowerBound(initPosition), null));

        int bestScore = Integer.MAX_VALUE;

        while (!topChoices.isEmpty()) {
            Choice choice = topChoices.poll();
            MoveTuple lastTuple = choice.moveTuple;
            int bfsIt = choice.bfsId;
            int height = choice.height;
            int lowerBound = choice.lowerBound;
            String position = bfsQueue.get(bfsIt);
            if (visited.contains(position)) {
                continue;
            } else {
                visited.add(position);
            }

            if (lowerBound == 0) {
                int score = lowerBound + height;
                if (score < bestScore) {
                    bestScore = score;
                    System.out.printf("------- START ---- SCORE: " + bestScore);
                    printSolution(bfsIt);
                    System.out.printf("------- END   ---- SCORE: " + bestScore);
                }
            }

            if (height > 50) {
                continue;
            }

            List<MoveTuple> moves = generateMoves1(position);

            for (MoveTuple moveTuple : moves) {
                String move = computeMove(position, moveTuple.opt, moveTuple.ch);
                bfsQueue.add(move);
                bfsParent.add(bfsIt);
                if (!visited.contains(move)) {
                    int newHeight;
                    if (lastTuple != null && lastTuple.ch == moveTuple.ch) {
                        newHeight = height;
                    } else {
                        newHeight = height + 1;
                    }

                    topChoices.add(new Choice(bfsQueue.size() - 1, newHeight, lowerBound(move), moveTuple));
                }
            }
        }
        System.out.println(topChoices.size());
        return false;
    }

    private void printSolution(int bfsIt) {
        List<String> seq = new ArrayList<>();
        while (bfsIt != -1) {
            seq.add(bfsQueue.get(bfsIt));
            bfsIt = bfsParent.get(bfsIt);
        }
        for (int a = seq.size() - 1; a >= 0; a--) {
            System.out.println(seq.get(a));
        }
    }

    public static void main(String[] args) {
        String position =
                "1##2\n" +
                "1##2\n" +
                "3445\n" +
                "3675\n" +
                " 8 9\n";

        System.out.println(new Klotski(position).solve());
    }

}
