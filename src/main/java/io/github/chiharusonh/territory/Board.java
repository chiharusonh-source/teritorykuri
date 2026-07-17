package io.github.chiharusonh.territory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Board {
    static final int VERTEX_COUNT = 50;

    static final double[][] VERTICES = {
            {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0},
            {0, 1}, {1, 1}, {2, 1}, {3, 1}, {2, 2}, {3, 2}, {4, 2}, {5, 2},
            {1, 3}, {2, 3}, {3, 3}, {4, 3}, {1, 4}, {2, 4}, {3, 4}, {4, 4},
            {0, 5}, {1, 5}, {2, 5}, {3, 5}, {2, 6}, {3, 6}, {4, 6}, {5, 6},
            {0, 7}, {1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7},
            {0, 2}, {0, 3}, {0, 4}, {0, 6}, {1, 2}, {1, 6}, {4, 1}, {4, 5},
            {5, 1}, {5, 3}, {5, 4}, {5, 5}, {2.5, 2.5}, {2.5, 4.5}
    };

    static final int[][] FACES = {
            {1, 0, 7}, {5, 4, 42}, {3, 2, 8, 9}, {6, 7, 0},
            {4, 3, 9, 8, 11, 42}, {2, 1, 7, 10, 11, 8}, {12, 13, 44, 5, 42},
            {7, 6, 14, 40}, {11, 12, 42}, {11, 10, 48},
            {14, 15, 48, 10, 7, 40}, {12, 11, 48, 16, 17}, {13, 12, 17, 21},
            {6, 36, 37, 38, 22, 14}, {15, 14, 19}, {17, 16, 21},
            {15, 19, 49, 20, 16, 48}, {18, 19, 14}, {20, 21, 16},
            {45, 13, 21, 29, 47, 46}, {22, 23, 18, 14}, {19, 18, 23, 24, 49},
            {21, 20, 49, 25, 28, 43}, {24, 25, 49}, {24, 23, 41},
            {28, 29, 21, 43}, {23, 22, 39, 30, 41}, {25, 24, 27, 33, 34, 28},
            {26, 27, 24, 41, 31, 32}, {29, 28, 35}, {27, 26, 32, 33},
            {30, 31, 41}, {34, 35, 28}
    };

    static final int[][] RAILS = {
            {2, 8}, {3, 9}, {6, 7}, {6, 14}, {8, 9}, {8, 11}, {13, 21},
            {14, 15}, {14, 22}, {15, 19}, {16, 17}, {16, 20}, {18, 19},
            {20, 21}, {21, 29}, {24, 27}, {26, 27}, {26, 32}, {27, 33},
            {28, 29}, {10, 11, 12, 13}, {22, 23, 24, 25}, {5, 42, 11, 48, 15},
            {20, 49, 24, 41, 30}, {0, 1, 2, 3, 4, 5},
            {0, 7, 10, 48, 16, 21}, {14, 19, 49, 25, 28, 35},
            {30, 31, 32, 33, 34, 35}, {0, 6, 36, 37, 38, 22, 39, 30},
            {1, 7, 40, 14, 18, 23, 41, 31}, {4, 42, 12, 17, 21, 43, 28, 34},
            {5, 44, 13, 45, 46, 47, 29, 35}
    };

    static final Set<Integer> CORNERS = Set.of(0, 5, 30, 35);
    static final int[] BLUE_HOME = {30};
    static final int[] RED_HOME = {2};
    static final int[] BLUE_STONES = {26, 32};
    static final int[] RED_STONES = {3, 9};
    static final List<List<Integer>> INCIDENT_FACES = buildIncidentFaces();
    static final List<List<Edge>> FACE_EDGES = buildFaceEdges();
    static final Map<Edge, List<Integer>> EDGE_FACES = buildEdgeFaces();
    static final Set<Edge> BOUNDARY_EDGES = buildBoundaryEdges();
    static final Set<Integer> BOUNDARY_VERTICES = buildBoundaryVertices();

    private Board() {
    }

    record Edge(int a, int b) {
        Edge {
            if (a > b) {
                int swap = a;
                a = b;
                b = swap;
            }
        }
    }

    private static List<List<Integer>> buildIncidentFaces() {
        List<List<Integer>> result = new ArrayList<>(VERTEX_COUNT);
        for (int v = 0; v < VERTEX_COUNT; v++) {
            result.add(new ArrayList<>());
        }
        for (int face = 0; face < FACES.length; face++) {
            for (int vertex : FACES[face]) {
                result.get(vertex).add(face);
            }
        }
        return result;
    }

    private static List<List<Edge>> buildFaceEdges() {
        List<List<Edge>> result = new ArrayList<>(FACES.length);
        for (int[] vertices : FACES) {
            List<Edge> edges = new ArrayList<>(vertices.length);
            for (int i = 0; i < vertices.length; i++) {
                edges.add(new Edge(vertices[i], vertices[(i + 1) % vertices.length]));
            }
            result.add(List.copyOf(edges));
        }
        return List.copyOf(result);
    }

    private static Map<Edge, List<Integer>> buildEdgeFaces() {
        Map<Edge, List<Integer>> result = new HashMap<>();
        for (int face = 0; face < FACE_EDGES.size(); face++) {
            for (Edge edge : FACE_EDGES.get(face)) {
                result.computeIfAbsent(edge, ignored -> new ArrayList<>()).add(face);
            }
        }
        return result;
    }

    private static Set<Edge> buildBoundaryEdges() {
        Set<Edge> result = new HashSet<>();
        EDGE_FACES.forEach((edge, faces) -> {
            if (faces.size() == 1) {
                result.add(edge);
            }
        });
        return Set.copyOf(result);
    }

    private static Set<Integer> buildBoundaryVertices() {
        Set<Integer> result = new HashSet<>();
        for (Edge edge : BOUNDARY_EDGES) {
            result.add(edge.a());
            result.add(edge.b());
        }
        return Set.copyOf(result);
    }

    static boolean contains(int[] values, int expected) {
        return Arrays.stream(values).anyMatch(value -> value == expected);
    }
}
