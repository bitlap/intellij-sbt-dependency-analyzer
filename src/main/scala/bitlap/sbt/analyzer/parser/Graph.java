package bitlap.sbt.analyzer.parser;

import java.util.*;

public class Graph {
    private final int V;

    private final LinkedList<Integer>[] adj;

    @SuppressWarnings("unchecked")
    Graph(int v) {
        V = v;
        adj = new LinkedList[v];
        for (int i = 0; i < v; ++i)
            adj[i] = new LinkedList<>();
    }

    void addEdge(int v, int w) {
        adj[v].add(w);
    }

    void DFSUtil(int v, boolean[] visited, List<Integer> res) {
        visited[v] = true;

        res.add(v);

        for (int n : adj[v]) {
            if (!visited[n])
                DFSUtil(n, visited, res);
        }
    }

    public List<Integer> DFS(int v) {
        boolean[] visited = new boolean[V+1];
        List<Integer> res = new ArrayList<>();
        DFSUtil(v, visited, res);
        return res;
    }
}