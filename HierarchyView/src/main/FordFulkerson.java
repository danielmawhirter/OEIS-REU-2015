package main;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class FordFulkerson {

	private boolean[] marked; // marked[v] = true iff s->v path in residual graph
	private Edge[] edgeTo; // edgeTo[v] = last edge on shortest residual s->v path
	private double value; // current value of max flow

	public FordFulkerson() {

	}

	public int getMaxFlow(Graph graph, Node s, Node t) {
		
		int u;
		
		int max = getMaxIndex(graph);
		System.out.println(max);

		int parent[] = new int[max + 1];
		System.out.println("max: " + max);

		Arrays.fill(parent, -1);

		int maxFlow = 0;
		
		
		int[][] rGraph = new int[max + 1][max + 1];
		
		for (int[] i : rGraph) {
			for (int j : i) {
				j = 0;
			}
		}
		
		for (Edge e : graph.getEdgeSet()) {
			Node n1 = e.getSourceNode();
			Node n2 = e.getTargetNode();

			int n1int = Integer.parseInt(n1.toString());
			int n2int = Integer.parseInt(n2.toString());

			rGraph[n1int][n2int] = 1;

		}

		while (thereExistsPath(graph, s, t, parent) == true) {
			
			int sint = Integer.parseInt(s.toString());
			int tint = Integer.parseInt(t.toString());
			
			System.out.println("there still exists a path");
			int min = Integer.MAX_VALUE;
			int val = tint;
			
			while (val != sint) {
				u = parent[val];
				min = Math.min(min, rGraph[u][val]);
				val = parent[val];
			}

			val = tint;
			maxFlow += min;
			
			System.out.println("maxFlow: " + maxFlow);
			
			while (val != sint) {
				u = parent[val];
				rGraph[u][val] -= min;
				rGraph[val][u] += min;
				val = parent[val];
			}
			
		}
		
		

		return maxFlow;
	}

	public int getMaxIndex(Graph graph) {
		int max = -1;
		for (Node n : graph) {
			int nint = Integer.parseInt(n.toString());
			if (nint >= max) {
				max = nint;
			}
		}
		return max;
	}

	public boolean thereExistsPath(Graph graph, Node s, Node t, int[] parent) {

		int sint = Integer.parseInt(s.toString());
		int tint = Integer.parseInt(t.toString());

		int max = getMaxIndex(graph);

		int order = graph.getNodeSet().size();

		int[][] rGraph = new int[max + 1][max + 1];
		
		for (int[] i : rGraph) {
			for (int j : i) {
				j = 0;
			}
		}
		
		for (Edge e : graph.getEdgeSet()) {
			Node n1 = e.getSourceNode();
			Node n2 = e.getTargetNode();

			int n1int = Integer.parseInt(n1.toString());
			int n2int = Integer.parseInt(n2.toString());
			
			
			rGraph[n1int][n2int] = 1;

		}

		boolean[] visited = new boolean[max + 1];
		Arrays.fill(visited, false);

		visited[sint] = true;

		Queue<Node> queue = new LinkedList<Node>();
		queue.add(s);

		while (!queue.isEmpty()) {
			//System.out.println("queue is not empty");
			int val = Integer.parseInt(queue.remove().toString());
			for (int i = 0; i < max; i++) {
				if (rGraph[val][i] != 0 && visited[i] == false) {
					parent[i] = val;
					visited[i] = true;
					queue.add(graph.getNode(Integer.toString(i)));
				}
			}
		}

		if (visited[tint] == true) {
			return true;
		} else {
			return false;
		}

	}
}
