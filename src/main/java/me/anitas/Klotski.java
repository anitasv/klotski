package me.anitas;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Klotski {

    private final Set<String> visited = new ConcurrentSkipListSet<>();

    private final char[] pieces;

    private final String initPosition;

    private final BlockingQueue<Node> nodeQueue = new ArrayBlockingQueue<>(400000);

    public Klotski(String initPosition) {
        this.initPosition = initPosition;
        Set<Character> temp = new HashSet<>();
        for (int i = 0; i < initPosition.length(); i++) {
            char ch = initPosition.charAt(i);
            if (ch != '.' && ch != ' ' && ch != '\n') {
                temp.add(ch);
            }
        }
        pieces = new char[temp.size()];

        int i = 0;
        for (char ch : temp) {
            pieces[i++] = ch;
        }
    }

    public static class MoveTuple {
        int opt;
        char ch;

        public MoveTuple(int opt, char ch) {
            this.opt = opt;
            this.ch = ch;
        }
    }

    private List<MoveTuple> generateMoves1(String position, char rejCh) {
        List<MoveTuple> moves = new ArrayList<>();
        for (char ch : pieces) {
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

    public static class Move {
        char ch;
        String position;

        public Move(char ch, String position) {
            this.ch = ch;
            this.position = position;
        }
    }

    private List<Move> generateMoves(String position, char rejCh) {
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
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < position.length(); i++) {
            char current = position.charAt(i);
            int j = i - opt;
            if (j >=0 && j < position.length()) {
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

    private boolean accepted(String position) {
        int n = position.indexOf('#');
        return n == 16;
    }

    private static class Node {
        Move move;
        Node parent;

        public Node(Move move, Node parent) {
            this.move = move;
            this.parent = parent;
        }
    }

    private boolean solve() throws InterruptedException {

        Node SENTINEL = new Node(null, null);

        nodeQueue.add(new Node(new Move('\0', initPosition), null));
        nodeQueue.add(SENTINEL);

        int progress = 0;
        ForkJoinPool fjp = ForkJoinPool.commonPool();

        DynCountDownLatch cdLatch = new DynCountDownLatch();
        while (true) {
            Node node = nodeQueue.take();
            if (node == SENTINEL) {
                cdLatch.await();
                progress++;
                System.out.println("Progress: " + progress);
                nodeQueue.add(SENTINEL);
                continue;
            }

            Move lastMove = node.move;
            String position = lastMove.position;

            if (accepted(position)) {
                printSolution(node);
                return true;
            }

            cdLatch.register();
            fjp.execute(() -> {
                List<Move> moves = generateMoves(position, lastMove.ch);

                for (Move move : moves) {
                    if (visited.add(move.position)) {
                        Node nextNode = new Node(move, node);
                        nodeQueue.add(nextNode);
                    }
                }
                cdLatch.countDown();
            });
        }
    }

    private void printSolution(Node node) {
        List<String> seq = new ArrayList<>();
        while (node != null) {
            seq.add(node.move.position);
            node = node.parent;
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

        long startTime = System.nanoTime();
        System.out.println(new Klotski(positionForgetMeNot).solve());
        long endTime = System.nanoTime();
        System.out.println(TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
    }

}
