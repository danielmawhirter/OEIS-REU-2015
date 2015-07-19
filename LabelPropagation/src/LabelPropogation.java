// Clustering - 7/7/15
// Using GraphNode class


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;

public class LabelPropogation {

	// Don't edit these variables.
	public static int total_node_weight = 0;
	public static int total_edge_weight = 0;
	public static double size_constraint = 0;
	public static double epsilon = 0.0;
	public static int k = 1;
	static String path;
	public static HashMap<String, TreeNode<String>> tree = new HashMap<String, TreeNode<String>>();
	public static HashMap<String, Integer> degree_map = new HashMap<String, Integer>();

	// Can edit these variables
	public static int capacity = 260000; // Limits the number of lines read by graph initialize functions
	public static final int internal_node_limit = 200000; // when the graph has less nodes then this, internal LPA is used
	public static int top_level_node_limit = 1200; // when the graph has less nodes then this, the coarsening phase ends
	//public static final String graph_file_name = "AuthorsHards_graph.txt";	
	public static final String graph_file_name = "g3_undir_std_wweights_peel1.txt"; 
	//public static String graph_file_name = "hards, weight.txt"; 
	//public static String graph_file_name = "bfs_tree.txt";
	public static String tree_file_name = "tree-" + graph_file_name;
	public static String input_tree = "AuthorsHards_graph.txt"; 
	
	public static boolean authors_flag = false;
	
	public LabelPropogation() {}

	public static void main(String[] args) throws IOException {
		
		k = 18; // parts
		epsilon = 0.03; // imbalanace parameter
		
		// Cluster_SequenceGraph(boolean semi-ext) : true  ---> use semi-external clustering
		// 											 false ---> use internal memory clustering 
		// --- internal implementation addedfeatures: vertex splitting + peeling + cluster breaking
		Cluster_SequenceGraph(true);
		//Cluster_AuthorsGraph(graph_file_name, tree_file_name);
	}
	
	//@SuppressWarnings("unused")
	public static void Cluster_SequenceGraph(boolean semi_flag) throws IOException {
		
		//graph_file_name = "g3_undir_std_wweights_peel1.txt"; 
		tree_file_name = "tree-" + graph_file_name;
		
		int max_deg = 18; // before clustering split vertices so that each has degree < max_deg
		
		// Creates directory for tree file (I am no longer storing the edge file since there is no need to have a duplicate)
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		path = graph_file_name + '_' + timeStamp;
		File dir = new File(path);
		dir.mkdir();
		
		// Initialize Graph: 
		// Arguments: file name, boolean semi_ext (only initialize node set)
		
		// (1) This version assumes "node1,weight1 node2,weight2 ... " adjacency format
		Graph graph = Graph_InitializeandStore.initialize_graph(graph_file_name, semi_flag);
		// (2) This version assumes "node1 node2 ..." adjacency format
		// Graph graph = Graph_InitializeandStore.initialize_graph_unweighted(graph_file_name, semi_flag);
		
		if (!semi_flag) {
			
			if (true) {
				vertex_split(graph, max_deg);
				Graph_InitializeandStore.storeGraph_w_degree(graph, graph_file_name, path);
		
				// Set peel layers (assumes internal)
				graph = PeelingAlgorithms.peel(graph, 2);
		
				Graph_InitializeandStore.storeGraph(graph, graph_file_name, path);
			}
			// This fills in the anti_chain after peeling occurs (internal)
			fill_anti_chain(graph);
		} else {
			capacity = 260000;
			fill_anti_chain(graph);
		}
		
		// Build Hierarchy:
		// Arguments: graph, # LP iterations, name for graph set, visualize?, apply ui stuff?, peel layers (at
		// each level before lp), semi-external?
		Clustering.buildHeirarchy(graph, 10, graph_file_name, false, false, 0, semi_flag);
	}
	
	/*
	public static void Cluster_AuthorsGraph(String g_filename, String t_filename) throws IOException {
		authors_flag = true;
		graph_file_name = "AuthorsHards_graph.txt";
		tree_file_name = "tree-" + graph_file_name;
		input_tree = "AuthorsHards_Htree.txt";
		
		int max_deg = 18; // before clustering split vertices so that each has degree < max_deg
		boolean semi_flag = false;
		
		// Creates directory for tree file (I am no longer storing the edge file since there is no need to have a duplicate)
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		path = graph_file_name + '_' + timeStamp;
		File dir = new File(path);
		dir.mkdir();
		
		Graph graph1 = Graph_InitializeandStore.initialize_graph_crossref(graph_file_name, false);
		System.out.println("nodes: " + graph1.getNodeCount() + ", edges: " + graph1.getEdgeCount());
		
		TreeNode<String> root = TreeNode.buildTree(input_tree);
		
		Graph graph2 = Build_Author_Quotient(graph1, root);
		
		// vertex_split(graph2, max_deg);
		// Graph_InitializeandStore.storeGraph_w_degree(graph2, graph_file_name, path);
		
		for (TreeNode<String> author : root.getChildren()) {
			if (graph2.getNode(author.toString()) != null || graph2.getNode(author.toString() + "p0") != null)
				tree.put(author.toString(), author);
		}
		
		System.out.println(tree.size());
		System.out.println(tree.toString());
		
		Clustering.buildHeirarchy(graph2, 10, graph_file_name, false, false, 0, semi_flag);
		
	}
	
	public static Graph Build_Author_Quotient(Graph s_graph, TreeNode<String> root) {
		Graph a_graph = new Graph(false, "1");
		
		// Create map: Sequence ---> Author 
		HashMap<String,String> s_to_a = new HashMap<String,String>();
		
		for (TreeNode<String> author : root.getChildren()) {
			for (TreeNode<String> sequence : author.getChildren()) {
				s_to_a.put(sequence.toString(), author.toString());
			}
		}
		
		for (Edge e : s_graph.getEdgeSet()) {
			String p_src_name = s_to_a.get(e.src.toString());
			String p_dst_name = s_to_a.get(e.dest.toString());
			
			if (a_graph.getNode(p_src_name) == null)
				a_graph.addNode(p_src_name);
			if (a_graph.getNode(p_dst_name) == null)
				a_graph.addNode(p_dst_name);
			
			if (!a_graph.hasEdgeBetween(p_src_name, p_dst_name)) {
				a_graph.addEdge(p_src_name, p_dst_name).weight = 1;
			} else {
				a_graph.getEdge(p_src_name, p_dst_name).weight += 1;
			}
		}
		
		System.out.println("Authors graph: n = " + a_graph.getNodeCount() + ", m = " + a_graph.getEdgeCount());

		return a_graph;
	}
	*/
	
	public static void vertex_split(Graph graph, int max_deg) {
	
		
		double avg_deg = 0;
		
		ArrayList<GraphNode> to_split = new ArrayList<GraphNode>();
		for (GraphNode n : graph.getNodeSet()) {
			
			avg_deg += (double) n.getDegree();
			if (n.getDegree() > max_deg) {
				to_split.add(n);
			}
		}
		
		avg_deg = avg_deg / graph.getNodeCount();
		System.out.println("average degree: " + avg_deg);

		for (GraphNode split : to_split) {
			LinkedList<GraphNode> to_give = new LinkedList<GraphNode>();
			while(split.getDegree() > 0) {
				GraphNode rem = split.neighbors.first();
				graph.removeEdge(split, rem);
				if (rem.toString() == split.toString()) continue;
				to_give.add(rem);
			}
			graph.removeNode(split);
			
			GraphNode center = graph.addNode(split.id + "p0");
			total_node_weight += 1;
			int x = 0;
			while (!to_give.isEmpty()) {
				graph.addNode(split.id + "p" + ++x).weight = 1;
				if (x != 1) {
					graph.addEdge(split.id + "p" + x, split.id + "p" + (x-1)).weight = 1;
				}
				total_node_weight += 1;
				GraphNode new_node = graph.getNode(split.id + "p" + x);
				graph.addEdge(center, new_node).weight = 1;
				int count = 0;
				while (count <= max_deg && !to_give.isEmpty()) {
					graph.addEdge(new_node, to_give.pop()).weight = 1;
					count++;
				}
			}
			graph.addEdge(split.id + "p" + x, split.id + "p" + 1).weight = 1;
		
		}
		
		
	}
	
	public static void fill_anti_chain (Graph graph) {
		
		for (GraphNode n : graph.getNodeSet()) {
			TreeNode<String> node = new TreeNode<String>(n.id);
			if (!tree.containsKey(n.id)) {
				tree.put(n.id, node);
			}
		}
	}

	public static double logOfBase(double base, double num) {
		return Math.log(num) / Math.log(base);
	}

}
