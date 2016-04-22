package me.anitas;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class Bfs<T, E> implements Function<T, Bfs.Node> {

    private final Function<T, Boolean> visitor;
    private final Function<Node, List<? extends Edge<T, E>>> getChildren;
    private final int maxCapacity;
    private final Executor executor;

    public Bfs(Function<T, Boolean> visitor,
               Function<Node, List<? extends Edge<T, E>>> getChildren,
               int maxCapacity,
               Executor executor) {
        this.visitor = visitor;
        this.getChildren = getChildren;
        this.maxCapacity = maxCapacity;
        this.executor = executor;
    }

    @Getter
    public static class Node <T, E> {
        private final T value;
        private final List<Parent<T, E>> parents = new CopyOnWriteArrayList<>();

        public Node(T value) {
            this.value = value;
        }

        public void addParent(E edge, Node<T, E> parent) {
            if (parents.isEmpty()) {
                parents.add(new Parent<T, E>(edge, parent));
            }
        }
    }

    public static class Parent<T, E> {
        private final E edge;
        private final Node<T, E> node;

        public Parent(E edge, Node<T, E> node) {
            this.edge = edge;
            this.node = node;
        }

        public E getEdge() {
            return edge;
        }

        public Node<T, E> getNode() {
            return node;
        }
    }

    public interface Edge<T, E> {

        E getLabel();

        T getVal();
    }

    @Override
    public Node<T, E> apply(T root) {
        Map<T, Node<T, E>> visited = new ConcurrentHashMap<>();
        BlockingQueue<Node<T, E>> nodeQueue = new ArrayBlockingQueue<>(maxCapacity);
        Node<T, E> SENTINEL = new Node<T, E>(null);

        nodeQueue.add(new Node<T, E>(root));
        nodeQueue.add(SENTINEL);

        DynCountDownLatch cdLatch = new DynCountDownLatch();

        int progress = 0;
        try {
            while (true) {
                Node<T, E> current = nodeQueue.take();
                if (current == SENTINEL) {
                    cdLatch.await();
                    ++progress;
                    System.out.println("Progress: " + progress);
                    if (nodeQueue.isEmpty()) {
                        return null;
                    }
                    nodeQueue.add(SENTINEL);
                    continue;
                }

                if (visitor.apply(current.value)) {
                    return current;
                }
                cdLatch.register();
                executor.execute(() -> {
                    for (Edge<T, E> edge : getChildren.apply(current)) {
                        Node<T, E> existing = visited.computeIfAbsent(edge.getVal(), (x) -> {
                            Node<T, E> node = new Node<T, E>(edge.getVal());
                            nodeQueue.add(node);
                            return node;
                        });
                        existing.addParent(edge.getLabel(), current);
                    }
                    cdLatch.countDown();
                });
            }
        } catch (InterruptedException  e) {
            throw new RuntimeException(e);
        }
    }
}
