import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

public class Clustering {

	public static boolean first_contraction_flag = true;
	public static boolean second_contraction_flag = false;
	public static boolean refinement_flag = false;
	public static boolean first_refinement_iteration = true;
	public static boolean has_false_leaves = true;
	public static String clstr_mark = "c";
	public static int num_vertices;
	
	public static void buildHeirarchy(Graph g, int levels, String name,
			boolean vis, boolean ui, int peel_layers, boolean semi_ext)
			throws IOException {
		
		if (LabelPropogation.authors_flag) 
			clstr_mark = "CLUSTER";
		
		TreeMap<Integer, Graph> Graph_Set = new TreeMap<Integer, Graph>();
		
		// Initialize graph hierarchy
		Graph_Set.put(1, g);
		
		System.out.println("Graph " + g.getId() + ": "
				+ g.getNodeSet().size() + " vertices and " + g.getEdgeCount() + " edges.");
		
		// Coarsening phase
		int i = 1;
		while (true) {
			//System.out.println("LP it.: " + 1 + ", graph: " + i);
			// Run an LPA depending on size of graph
			if ((semi_ext && (Graph_Set.get(i).getNodeCount() > LabelPropogation.internal_node_limit || i == 1
					|| Graph_Set.get(i).getEdgeCount() == 0))) {
				Graph_Set.put(i + 1, semi_ext_label_propogation(Graph_Set.get(i)));
			}
			else
				Graph_Set.put(i + 1, label_propogation(Graph_Set.get(i)));
				
			System.out.println("Graph " + Graph_Set.get(i + 1).getId() + " has "
					 + Graph_Set.get(i + 1).getNodeSet().size() + " vertices.");
			
			i++;
			if (Graph_Set.get(i).getNodeCount() < LabelPropogation.top_level_node_limit) {
				System.out.println("Coarsening phase 1 is done.");
				break;
			}
		}
		
		first_contraction_flag = false;
		refinement_flag = true;
		
		
		// Take last generated graph and run KaHip
		
		
		
		// Refinement phase
		System.out.println("Refinement phase...");
		num_vertices = Graph_Set.get(1).getNodeCount();
		TreeMap<Integer, Graph> Graph_Setr = new TreeMap<Integer, Graph>();
		ArrayList<GraphNode> to_remove = new ArrayList<GraphNode>();
		Graph_Setr = Graph_Set;
		boolean kahip = false;
		while (i > 1) {
			
			int q = i + 1;
			Graph q_refined = new Graph(false, Integer.toString(q));
			if (!kahip) {
				Graph x = callKaHIP(Graph_Set.get(i));
				Graph_Setr.put(i, x);
				kahip = true;
				
				for (GraphNode n : Graph_Setr.get(i).getNodeSet()) {
					q_refined.addNode(n.clusterId);
				}
				
				for (Edge e : Graph_Setr.get(i).getEdgeSet()) {
					String c1_name = e.src.clusterId;
					String c2_name = e.dest.clusterId;
					
					
					if (q_refined.getNode(c1_name) == null) {
						q_refined.addNode(c1_name);
					}
					if (q_refined.getNode(c2_name) == null) {
						q_refined.addNode(c2_name);
					}
					q_refined.addEdge(q_refined.getNode(c1_name), q_refined.getNode(c2_name));
					
				}
				Graph_Setr.put(i + 1, q_refined);
				for (GraphNode n : x.getNodeSet()) {
					System.out.println(n.toString() + " in cluster: " + n.clusterId);
				}
			}
			else {
				if (Graph_Setr.get(i).getEdgeCount() == 0) {
					q_refined = semi_ext_label_propogation(Graph_Setr.get(i));
				}
				else {
					q_refined = label_propogation(Graph_Setr.get(i));
				}
			
				Graph_Setr.put(i + 1, q_refined);
			}

			System.out.println("Graph " + Graph_Set.get(i+1).getId() + ": "
					+ Graph_Setr.get(i+1).getNodeSet().size() + " vertices and " 
					+ Graph_Setr.get(i+1).getEdgeCount() + " edges.");
			
			for (GraphNode n : Graph_Setr.get(i-1).getNodeSet()) {
				String old_cluster = n.clusterId;
				String new_cluster;
				// if node associated with old_cluster was removed due to disconnectedness:
				// 		remove node from graph
				if (Graph_Setr.get(i).getNode(old_cluster) != null) {
					new_cluster = Graph_Setr.get(i).getNode(old_cluster).clusterId;
					String clust_num = new_cluster.substring(0, 1);
					new_cluster = Integer.toString((Integer.parseInt(clust_num) - 1)) + 
							new_cluster.substring(1, new_cluster.length());		
				}
				else {
					to_remove.add(n);
					continue;
				}
				n.clusterId = new_cluster;
			}
			
			for (GraphNode s : to_remove) {
				if (Graph_Setr.get(i-1).getNode(s.id) != null)
					Graph_Setr.get(i-1).removeNode(s);	
			}
				
			first_refinement_iteration = false;
			i--;
		}

		System.out.println("Refinement phase is done.");
		
		
		// Second contraction phase
		refinement_flag = false;
		second_contraction_flag = true;
		
		System.out.println("Contraction phase 2...");
		
		TreeMap<Integer, Graph> Graph_Set2 = new TreeMap<Integer, Graph>();
		Graph_Set2 = Graph_Setr;
		
		LabelPropogation.tree.remove("disc_components_tree");
		//LabelPropogation.fill_anti_chain(Graph_Set2.get(1));
		
		/*
		for (String anchor : LabelPropogation.tree.keySet()) {
			System.out.println("anchor: " + anchor + " " + LabelPropogation.tree.get(anchor).getChildCount());
		}
		*/
		
		// Remove nodes from tree which are not part of giant component
		ArrayList<String> nodes_to_remove = new ArrayList<String>();
		
		for (String key : LabelPropogation.tree.keySet()) {
			if (!Graph_Set2.get(1).idToNode.containsKey(key))
				nodes_to_remove.add(key);
		}
		
		for (String rem : nodes_to_remove) {
			LabelPropogation.tree.remove(rem);
		}
		
		i = 1;
		LabelPropogation.top_level_node_limit = 200;
		while (true) {
			
			// Test initial cluster IDs
			//if (i==1) {
			//	for (GraphNode n : Graph_Set2.get(i).getNodeSet()) {
			//		System.out.println(n.toString() + ": " + n.clusterId);
			//	}
			//}
			
			//System.out.println("LP it.: " + 2 + ", graph: " + i);
			// Run an LPA depending on size of graph
			if ((semi_ext && (Graph_Set2.get(i).getNodeCount() > LabelPropogation.internal_node_limit || i == 1
					|| Graph_Set2.get(i).getEdgeCount() == 0))) {
				Graph_Set2.put(i + 1, semi_ext_label_propogation(Graph_Set2.get(i)));
			}
			else
				Graph_Set2.put(i + 1, label_propogation(Graph_Set2.get(i)));
				
			System.out.println("Graph " + Graph_Set2.get(i+1).getId() + ": "
					+ Graph_Set2.get(i+1).getNodeSet().size() 
					+ " vertices and " + Graph_Set2.get(i+1).getEdgeCount() + " edges.");
			
			i++;
			if (Graph_Set2.get(i).getNodeCount() < LabelPropogation.top_level_node_limit) {
				System.out.println("Coarsening phase 2 is done.");
				break;
			}
		}
		
		// Create root TreeNode
		TreeNode<String> root = new TreeNode<String>("root");
		for (TreeNode<String> c : LabelPropogation.tree.values()) {
			root.addChild(c);
		}
		
		// Can only run tree_split if graphs are internal
		if (!semi_ext) {
			// Number of tree_splits
			int x = 2;
			for (int y = 1; y <= x; y++) {
				System.out.println(y);
				tree_split(root, Graph_Set2);
			}
		}	
		
		tree_trim(root);
		while (has_false_leaves) {
			remove_false_leaves(root);
		}
		tree_trim(root);
		
		
		System.out.println("Top clusters: " + root.getChildren());
		
		// Output file
		TreeNode.outputTree(root, "./" + LabelPropogation.path + "/" + LabelPropogation.tree_file_name);
		
		System.out.println("Done.");
	}
	
	public static Graph callKaHIP(Graph g) {
		int n = g.getNodeCount();
		int[] vwgt = new int[n];
		int[] xadj = new int[n + 1];
		int[] adjcwgt = new int[2 * g.getEdgeCount()];
		int[] adjncy = new int[2 * g.getEdgeCount()];
		int nparts              = 18;
		double imbalance        = 0.3;
		boolean suppress_output = false;
		int seed                = 123456;
		int mode                = KaHIPWrapper.KAHIP_FAST;
		
		
		ArrayList<GraphNode> nodes = new ArrayList<GraphNode>(n);
		HashMap<String, Integer> node_to_index = new HashMap<String, Integer>(n);
		
		int i = -1;
		for(GraphNode node : g.getNodeSet()) {
			nodes.add(node);
			vwgt[++i] = (int) Math.sqrt(Math.sqrt(node.weight)); 
			System.out.println(node.toString() + ": " + vwgt[i]);
			
			node_to_index.put(node.toString(), i);
		}
		
		xadj[n] = 2 * g.getEdgeCount();
		//i = -1;
		int j = 0; // adjcy index 
		for (GraphNode u : nodes) {
			xadj[node_to_index.get(u.toString())] = j;
			if(u.neighbors.contains(u)) System.err.println("Bad thing happened at " + new Throwable().getStackTrace()[0].toString());
			for (GraphNode v : u.neighbors) {
				adjncy[j] = node_to_index.get(v.toString());
				adjcwgt[j] = g.getEdge(u, v).weight;
				j++;
			}
		}
		
		
		KaHIPWrapperResult result = KaHIPWrapper.kaffpa(n, vwgt, xadj, adjcwgt, adjncy, 
				nparts, imbalance, suppress_output, seed, mode);
		
		int[] partition = result.getPart();
		
		System.err.println(result.getEdgecut());
		
		for (GraphNode node : g.getNodeSet()) {
			String old_clust_num = node.toString().substring(0, 1);
			node.clusterId = old_clust_num + "c" + Integer.toString(partition[node_to_index.get(node.toString())]);
		}
		
		return g;

		
	}
	
	public static void remove_false_leaves(TreeNode<String> n) {
		has_false_leaves = false;
		LinkedList<TreeNode<String>> toRemove = new LinkedList<>();
		addFalseLeaves(n, toRemove);
		if (!toRemove.isEmpty())
			has_false_leaves = true;
		for (TreeNode<String> rem : toRemove) {
			rem.getParent().removeChild(rem);
		}
	}
	
	// helper function for remove_false_leaves
	private static void addFalseLeaves(TreeNode<String> n, LinkedList<TreeNode<String>> toRemove) {
		
		if (n.getChildCount() == 0 && (n.toString().contains(clstr_mark) || n.toString().contains("tree") || n.toString().contains("a"))) {
			toRemove.add(n);
		}
		for (TreeNode<String> c : n.getChildren()) {		
			addFalseLeaves(c, toRemove);
		}
	}

	public static void tree_trim (TreeNode<String> n) {
		LinkedList<TreeNode<String>> toRemove = new LinkedList<>();
		addToRemove(n, toRemove);
		while(toRemove.size() > 0) {
			TreeNode<String> current = toRemove.pop();
			for (TreeNode<String> child: current.getChildren()) {
				current.getParent().addChild(child);
			}
			current.getParent().removeChild(current);
			current.clearChildren();
		}
	}
	
	// helper function for tree_trim
	private static void addToRemove(TreeNode<String> n, LinkedList<TreeNode<String>> toRemove) {
		if (n.getChildCount() == 1 && !n.toString().contains("root"))
			toRemove.add(n);
		for (TreeNode<String> c : n.getChildren()) {
			addToRemove(c, toRemove);
		}
	}
	
	public static void tree_split (TreeNode<String> n, TreeMap<Integer, Graph> Graph_Set) {
		
		// Clusters larger then this are clustered again
		int upper_bound = 50;
		
		LinkedList<TreeNode<String>> toSplit = new LinkedList<>();
		addToSplit(n, toSplit);
		for (TreeNode<String> curr : toSplit) {
						
			// clear curr's children and store for later use
			HashMap<String, TreeNode<String>> children = new HashMap<String, TreeNode<String>>();
			for (TreeNode<String> c : curr.getChildren()) {
				children.put(c.toString(), c);
			}
			
			Graph clust_graph = build_clust_graph(curr, Graph_Set);
			
			curr.clearChildren();
			
			System.out.println("Breaking cluster " + curr.toString() + " with " + 
					clust_graph.getNodeCount() + " nodes and " + clust_graph.getEdgeCount() + " edges.");
			
			for (GraphNode m : clust_graph.getNodeSet()) {
				m.clusterId = m.id + "a";
			}
			
			TreeMap<String, Integer> clusters = new TreeMap<>();

			// Label Propagation
			for (GraphNode m : clust_graph.getNodeSet()) {

				TreeMap<String, Integer> map = new TreeMap<>();

				for (GraphNode two : m.neighbors) {
					String cluster = two.clusterId;
					Edge e = clust_graph.getEdge(m,two);
					int w = e.weight;
					if (!map.containsKey(cluster))
						map.put(cluster, w);
					else
						map.replace(cluster, map.get(cluster) + w);
				}

				
				int max = 0;
				String bestCluster = m.clusterId;
				for (String s : map.keySet()) {
					int current = map.get(s);
					if (current < upper_bound) {
						if (current > max) {
							max = current;
							bestCluster = s;
						}
					}
				}
				m.clusterId = bestCluster;
				int w = m.weight;
				if (!clusters.containsKey(bestCluster))
					clusters.put(bestCluster, w);
				else
					clusters.replace(bestCluster, clusters.get(bestCluster) + w);

			}
		
			// Build collection of TreeNodes (new clusters) and set them as children of curr
			HashMap<String, TreeNode<String>> tn_clusts = new HashMap<String, TreeNode<String>>();
			for (String s : clusters.keySet()) {
				TreeNode<String> clust = new TreeNode<String>(s);
				tn_clusts.put(s, clust);
				curr.addChild(clust);
			}
			
			// assign children 
			for (GraphNode m : clust_graph.getNodeSet()) {
				tn_clusts.get(m.clusterId).addChild(children.get(m.toString()));
			}
			
		}
		
	
	}
	
	// build graph induced on nodes of a cluster
	private static Graph build_clust_graph(TreeNode<String> curr, TreeMap<Integer, Graph> Graph_Set) {
		int id;
		if(curr.toString().contains("a") || curr.toString().contains("tree")) 
			id = 1;
		else 
			id = Integer.parseInt(curr.toString().substring(0, 1));
		Graph graph = Graph_Set.get(id);
		Graph clust_graph = new Graph(false, "cluster");
		for (TreeNode<String> child : curr.getChildren()) {
			clust_graph.addNode(child.toString());
			clust_graph.getNode(child.toString()).weight = 1; //////// 
		}
		for (Edge e : graph.getEdgeSet()) {
			GraphNode n0 = e.src;
			GraphNode n1 = e.dest;
			if (clust_graph.getNode(n0.toString()) != null && clust_graph.getNode(n1.toString()) != null) {
				GraphNode n0_c = clust_graph.getNode(n0.toString());
				GraphNode n1_c = clust_graph.getNode(n1.toString());
				clust_graph.addEdge(n0_c, n1_c);
				clust_graph.getEdge(n0,n1).weight = e.weight;
			}
			
		}
		
		return clust_graph;
	
	}
	
	// helper function for tree_split
	private static void addToSplit(TreeNode<String> n, LinkedList<TreeNode<String>> toSplit) {
		if (n.getChildCount() > 50 && n.toString() != "root" && !n.toString().contains("tree")) {
			if (!n.getChildren().get(0).toString().contains("a"))
				toSplit.add(n);
		}
		for (TreeNode<String> c : n.getChildren()) {
			addToSplit(c, toSplit);
		}
	}
	
	// Internal Label Propagation Algorithm
	// Assumes "graph" contains the vertex set and edge set 
	public static Graph label_propogation(Graph graph) {
		
		final long startTime = System.currentTimeMillis();
		
		//System.out.println("edges: " + graph.getEdgeCount());
		//System.out.println(((double) LabelPropogation.total_node_weight) + " " + (double) graph.getNodeCount());
		
		// Set size constraint here:
		// LabelPropogation.size_constraint = (((double) LabelPropogation.total_node_weight) / ((double) graph.getNodeCount()));
		LabelPropogation.size_constraint = (1.0 + LabelPropogation.epsilon) * (num_vertices / LabelPropogation.k);
		
		if (LabelPropogation.authors_flag)
			LabelPropogation.size_constraint = 3000;
		
		if (refinement_flag || second_contraction_flag) {
			System.out.println("Running internal label propogation on graph "
					+ graph.getId() + " with size-constraint=" + LabelPropogation.size_constraint + "...");
		}
		else {
			System.out.println("Running internal label propogation on graph "
					+ graph.getId());
		}
		
		// if not performing local search, then assign new cluster id
		if (first_contraction_flag || first_refinement_iteration || second_contraction_flag) {
			for (GraphNode n : graph.getNodeSet()) {
				String s = n.id;
				if (s.contains(clstr_mark)) {
					s = s.split(clstr_mark)[1];
				}
				
				n.clusterId = graph.getId() + clstr_mark + s;
			//	System.out.println("set: " + n.toString() + "--->" + n.clusterId);
			}
		}
		
		TreeMap<String, Integer> clusters = new TreeMap<>();

		for (GraphNode n : graph.getNodeSet()) {
			boolean moved = false;

			TreeMap<String, Integer> map = new TreeMap<>();

			for (GraphNode two : n.neighbors) {
				Edge e = graph.getEdge(n,two);
				String cluster = two.clusterId;
				if (cluster == null) {
					System.out.println(two.neighbors);
					System.out.println(n.toString() + "---> " + two.toString() + " has no clusterid");
				}
				int w = e.weight;
				if (!map.containsKey(cluster))
					map.put(cluster, w);
				else
					map.replace(cluster, map.get(cluster) + w);
			}

			
			int max = 0;
			String bestCluster = n.clusterId;
			for (String s : map.keySet()) {
				int current = map.get(s);
				if (current > max) {
					// apply size constraint
					if (refinement_flag || second_contraction_flag) {
						if (current > LabelPropogation.size_constraint) {
							//System.out.println("Rejected move: " + n.id + "--->" + s);
						}
						if(current <= LabelPropogation.size_constraint) {
							max = current;
							bestCluster = s;
							moved = true;
						}	
					}
					else {
						max = current;
						bestCluster = s;
					}
				}
			}
			
			int w;
			if (!first_contraction_flag && !moved) {
				if (!n.toString().contains("c")) {
					bestCluster = graph.getId() + "c" + n.toString();
				} else {
					bestCluster = graph.getId() + n.toString().substring(1);
				}
				
				//System.out.println(n.toString() + " moved from " + n.clusterId + " to " + bestCluster);
			}
				
			n.clusterId = bestCluster;
			w = n.weight;
			
			if (!clusters.containsKey(bestCluster))
				clusters.put(bestCluster, w);
			else
				clusters.replace(bestCluster, clusters.get(bestCluster) + w);

		}

		final long endTime = System.currentTimeMillis();
		System.out.println("done in: "
				+ ((double) (endTime - startTime) / 1000) + " seconds.");

		Graph quotient = build_quotient(graph, clusters);
		return quotient;
	}
	
	// Build Quotient Graph resulting from internal LP
	public static Graph build_quotient(Graph graph, TreeMap<String, Integer> clusters) {
		
		String graph_num;
		
		// set graph id
		if (refinement_flag) {
			int num = Integer.parseInt(graph.getId()) + 1;
			graph_num = Integer.toString(num);
		}
		else {
			int num = Integer.parseInt(graph.getId()) + 1;
			graph_num = Integer.toString(num);
		}
		
		Graph quotient = new Graph(false, graph_num);

		System.out.println("Building quotient graph " + graph_num + "...");
		
		// Build node set
		
		HashMap<String, TreeNode<String>> clustId_to_treenode = new HashMap<String, TreeNode<String>>();
		
		for (String s : clusters.keySet()) {
			
			clustId_to_treenode.put(s, new TreeNode<String>(s));
			
			quotient.addNode(s);
			
			GraphNode qnode = quotient.getNode(s);
			qnode.weight = clusters.get(s);
			qnode.int_edge_wsum = 0;
		}

		// Build edge set
		for (Edge e : graph.getEdgeSet()) {
			GraphNode n1 = e.src;
			GraphNode n2 = e.dest;
			int edge_weight = e.weight;

			String c1_name = n1.clusterId;
			String c2_name = n2.clusterId;
			GraphNode c1 = quotient.getNode(c1_name);
			GraphNode c2 = quotient.getNode(c2_name);

			if (c1 == null)
				continue;
			
			if (c2 == null)
				continue;
			
			if (c1_name == c2_name) {
				GraphNode qnode = quotient.getNode(c1_name);
				int ie = qnode.int_edge_wsum;
				qnode.int_edge_wsum = ie + edge_weight;
				continue;
			}
			
			if (quotient.getEdge(c1, c2) == null) {
				quotient.addEdge(c1, c2).weight = edge_weight;
			} else {
				if (quotient.hasEdgeBetween(c1, c2)) {
					quotient.getEdge(c1, c2).weight += edge_weight;
				}
			}
		}
		
		// Change this later
		if (!refinement_flag || first_refinement_iteration) {
			update_anti_chain_internal(graph, quotient, clusters, clustId_to_treenode);
		}
		return quotient;
	}
	
	// Semi-external Label Propagation Algorithm
	public static Graph semi_ext_label_propogation(Graph graph) {
		
		final long startTime = System.currentTimeMillis();
		
		// Set size constraint here:
		// LabelPropogation.size_constraint = ((100000.00) / ((double) graph.getNodeCount()));
		// LabelPropogation.size_constraint = (((double) LabelPropogation.total_node_weight) / ((double) graph.getNodeCount()));
		LabelPropogation.size_constraint = (1.0 + LabelPropogation.epsilon) * (num_vertices / LabelPropogation.k);
		System.out.println("Running semi-external label propogation on graph "
				+ graph.getId() + "...");
		if (!refinement_flag || graph.getId() != "1") {
			for (GraphNode n : graph.getNodeSet()) {
				String s = n.id;
				if (s.contains(clstr_mark))
					s = s.substring(2);
				
				n.clusterId = graph.getId() + clstr_mark + s;
			}
		}

		TreeMap<String, Integer> clusters = new TreeMap<>();

		System.out.println("Reading edge set...");

		try {
			FileReader file;
			if (graph.getId() == "1") {
				//file = new FileReader("Z:\\REU OEIS Group\\" + LabelPropogation.graph_file_name);
				file = new FileReader(LabelPropogation.path + "/" 
				+ LabelPropogation.graph_file_name.substring(0, 
						LabelPropogation.graph_file_name.length() - 4) 
				+ '-' + "1" + "-Edge_Set.txt");
			}
			else 
				file = new FileReader(LabelPropogation.path + "/"
						+ LabelPropogation.graph_file_name.substring(0, LabelPropogation.graph_file_name.length() - 4) 
						+ '-' + graph.getId() + "-Edge_Set.txt");
			BufferedReader reader = new BufferedReader(file);
			String line;

			// for each node
			while ((line = reader.readLine()) != null) {
				
				boolean moved = false;
				String[] split = line.split(" ");
				GraphNode n = graph.getNode(split[0]);

				if (n == null)
					continue;

				// cluster c: edge weight sum from n ---> c
				TreeMap<String, Integer> map = new TreeMap<>();
				
				String adj;
				
				String cluster;
				if (split.length > 1) {
			
					for (int i = 1; i < split.length; i++) {
						adj = split[i];
						String[] node_w_weight = adj.split(",");
						String m_name = node_w_weight[0];
						int weight = Integer.parseInt(node_w_weight[1]);
						
						GraphNode m = graph.getNode(m_name);

						if (m == null)
							continue;
						
						cluster = m.clusterId;
						
						if (!map.containsKey(cluster))
							map.put(cluster, weight);                   
						else
							map.replace(cluster, map.get(cluster) + weight);
					}
				}	

				int max = 0;
				String bestCluster = n.clusterId;
				for (String s : map.keySet()) {
					int current = map.get(s);
					if (current > max) {
						// apply size constraint if in refinement phase
						if (refinement_flag || second_contraction_flag) {
							if (current > LabelPropogation.size_constraint) {
								//System.out.println("Rejected move: " + n.id + "--->" + s);
							}
							else {
								max = current;
								bestCluster = s;
								moved = true;
							}	
						}
						else {
							max = current;
							bestCluster = s;
						}
					}
				}
				
				int w;
				if (!first_contraction_flag && !moved) {
					if (!n.toString().contains("c")) {
						bestCluster = graph.getId() + "c" + n.toString();
					} else {
						bestCluster = graph.getId() + n.toString().substring(1);
					}
				}
					
				n.clusterId = bestCluster;
				w = n.weight;

				if (!clusters.containsKey(bestCluster))
					clusters.put(bestCluster, w);
				else
					clusters.replace(bestCluster, clusters.get(bestCluster) + w);
				
			}

			reader.close();
			file.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final long endTime = System.currentTimeMillis();
		System.out.println("done in: "
				+ ((double) (endTime - startTime) / 1000) + " seconds.");
		
		
		Graph quotient = semi_ext_build_quotient(graph, clusters);
		return quotient;
	}

	// Build Quotient Graph resulting from semi-external LP
	public static Graph semi_ext_build_quotient(Graph graph,
			TreeMap<String, Integer> clusters) {
		
		for (GraphNode n : graph.getNodeSet()) {
			String c_id = n.clusterId;
			if (!clusters.containsKey(c_id)) {
				System.out.println("added key: " + c_id);
				clusters.put(c_id, 1); // assuming node weight 1
			}
		}
		
		String graph_num;
		if (refinement_flag) {
			int num = Integer.parseInt(graph.getId()) + 1;
			graph_num = Integer.toString(num);
		}
		else {
			int num = Integer.parseInt(graph.getId()) + 1;
			graph_num = Integer.toString(num);
		}
		
		Graph quotient = new Graph(false, graph_num);
		
		HashMap<String, TreeNode<String>> clustId_to_treenode = new HashMap<String, TreeNode<String>>();
		
		// Build node set
		for (String s : clusters.keySet()) {
			
			clustId_to_treenode.put(s, new TreeNode<String>(s));
			
			GraphNode qnode = quotient.addNode(s);
			qnode.weight = clusters.get(s);
			qnode.int_edge_wsum = 1; // wrong, fix later maybe
		}
		
		if (quotient.getNodeCount() < LabelPropogation.internal_node_limit) {
			System.out.println("Building quotient graph " + graph_num + " internally...");
			build_QuotientEdgeSet_internal (graph, quotient, clusters);
			if (!refinement_flag || first_refinement_iteration)
				update_anti_chain_internal(graph, quotient, clusters, clustId_to_treenode);
		}
		else {
			System.out.println("Building quotient graph " + graph_num + " semi-externally...");
			build_QuotientEdgeSet_external (graph, quotient, clusters);
			if (!refinement_flag || first_refinement_iteration)
				update_anti_chain_external(graph, quotient, clusters, clustId_to_treenode);
		}

		return quotient;

	}
	
	// Build edge set externally 
	public static void build_QuotientEdgeSet_external (Graph graph, Graph quotient,
			TreeMap<String, Integer> clusters) {

			TreeMap<String, TreeMap<String, Integer>> EdgeSet = new TreeMap<String, TreeMap<String, Integer>>();
		
			try {

				BufferedReader reader = new BufferedReader(new FileReader(
						LabelPropogation.path + "/" 
						+ LabelPropogation.graph_file_name.substring(0, LabelPropogation.graph_file_name.length() - 4) 
						+ '-' + (Integer.parseInt(quotient.getId()) - 1) + "-Edge_Set.txt"));
				String line;

				// for each node
				while ((line = reader.readLine()) != null) {
					
					String[] split = line.split(" ");
					GraphNode n1 = graph.getNode(split[0]);
					
					// if current has no adjacency list, continue
					if (split.length == 1) {
						continue;
					}
					
					String n2_name;
					int edge_weight;
					
					for (int i = 1; i < split.length; i++) {
						String[] node_w_weight = split[i].split(",");
						n2_name = node_w_weight[0];
						edge_weight = Integer.parseInt(node_w_weight[1]); 
						// accounting for previously removed vertices
						if (graph.getNode(n2_name) == null) {
							continue;
						}
						GraphNode n2 = graph.getNode(n2_name);

						String c1_name = n1.clusterId;
						String c2_name = n2.clusterId;

						if (c1_name == c2_name) {
							GraphNode qnode = quotient.getNode(c1_name);
							int ie = qnode.int_edge_wsum;
							qnode.int_edge_wsum = ie + edge_weight;
							continue;
						}
						
						TreeMap<String, Integer> c1_edges = null;
						if (!EdgeSet.containsKey(c1_name)) 
							c1_edges = new TreeMap<String, Integer>();
						else 
							c1_edges = EdgeSet.get(c1_name);
						
						if (c1_edges.containsKey(c2_name)) {
							int old_weight = c1_edges.get(c2_name);
							c1_edges.put(c2_name, old_weight + edge_weight);
							LabelPropogation.degree_map.put(c1_name, old_weight + edge_weight);
						}
						else {
							c1_edges.put(c2_name, edge_weight);
							LabelPropogation.degree_map.put(c1_name, edge_weight);
						}
							
						
						TreeMap<String, Integer> c2_edges = null;
						if (!EdgeSet.containsKey(c2_name)) 
							c2_edges = new TreeMap<String, Integer>();
						else 
							c2_edges = EdgeSet.get(c2_name);
						
						if (c2_edges.containsKey(c1_name)) {
							int old_weight = c2_edges.get(c1_name);
							c2_edges.put(c1_name, old_weight + edge_weight);	
							LabelPropogation.degree_map.put(c2_name, old_weight + edge_weight);
						}
						else {
							c2_edges.put(c1_name, edge_weight);
							LabelPropogation.degree_map.put(c2_name, edge_weight);
						}
						
						EdgeSet.put(c1_name, c1_edges);
						EdgeSet.put(c2_name, c2_edges);
						
					}

				}

				reader.close();
				
				store_quotient_edge_set (quotient, EdgeSet);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
	
	public static void store_quotient_edge_set (Graph quotient, TreeMap<String, 
			TreeMap<String, Integer>> EdgeSet) throws FileNotFoundException {
		
		System.out.println("Storing graph " + quotient.getId() + "...");

		String edge_file_name = LabelPropogation.graph_file_name.substring(0, LabelPropogation.graph_file_name.length() - 4) 
				+ '-' + quotient.getId() + "-Edge_Set.txt";

		PrintWriter pf = new PrintWriter("./" + LabelPropogation.path + '/' + edge_file_name);
		pf.close();

		PrintWriter edge_file = new PrintWriter("./" + LabelPropogation.path + '/'
				+ edge_file_name);

		for (String s : EdgeSet.keySet()) {
			
			edge_file.print(s + " ");
			for (String a : EdgeSet.get(s).keySet()) {
				edge_file.print(a + "," + EdgeSet.get(s).get(a) + " ");
			}
			
			edge_file.println();
			
		}
		
		edge_file.close();
	}

	// Build edge set internally
	public static void build_QuotientEdgeSet_internal (Graph graph, Graph quotient,
			TreeMap<String, Integer> clusters) {

			try {

				BufferedReader reader = new BufferedReader(new FileReader(
						LabelPropogation.path + "/" 
						+ LabelPropogation.graph_file_name.substring(0, LabelPropogation.graph_file_name.length() - 4) 
						+ '-' + (Integer.parseInt(quotient.getId()) - 1) + "-Edge_Set.txt"));
				String line;

				// for each node
				while ((line = reader.readLine()) != null) {
					
					String[] split = line.split(" ");
					GraphNode n1 = graph.getNode(split[0]);
					//////////////////////////////
					if (n1 == null)
						continue;
					
					// if current has no adjacency list, continue
					if (split.length == 1) {
						continue;
					}
					
					String n2_name;
					int edge_weight;
					
					for (int i = 1; i < split.length; i++) {
						String[] node_w_weight = split[i].split(",");
						n2_name = node_w_weight[0];
						edge_weight = Integer.parseInt(node_w_weight[1]); 
						GraphNode n2 = graph.getNode(n2_name);

						if (n2 == null)
							continue;

						String c1_name = n1.clusterId;
						String c2_name = n2.clusterId;

						GraphNode c1 = quotient.getNode(c1_name);
						GraphNode c2 = quotient.getNode(c2_name);

						if (c1_name == c2_name) {
							GraphNode qnode = quotient.getNode(c1_name);
							int ie = qnode.int_edge_wsum;
							qnode.int_edge_wsum = ie + edge_weight;

							continue;
						}
						
						if (quotient.getEdge(c1, c2) == null) {
							quotient.addEdge(c1, c2).weight = edge_weight;
						} else {
							if (quotient.hasEdgeBetween(c1, c2)) {
								quotient.getEdge(c1, c2).weight += edge_weight;
							}
						}
						
					}

				}

				reader.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
	
	// update_anti_chain (after an internal quotient build):
	// This function gets called at the end of build_quotient
	// --- assigns old nodes of anti_chain as children of nodes resulting from clustering 
	// --- removes old nodes from anti_chain and adds new nodes resulting from clustering to anti_chain
	public static void update_anti_chain_internal(Graph graph, Graph quotient, TreeMap<String, Integer> clusters,
			HashMap<String, TreeNode<String>> clustId_to_treenode) {

		ArrayList<String> to_remove = new ArrayList<String>();
		ArrayList<String> to_add    = new ArrayList<String>();
		
		if (second_contraction_flag) {
			for (String tree_node_label : LabelPropogation.tree.keySet()) {
				
				if (tree_node_label.contains("disc_components"))
					continue;
	
				GraphNode node = graph.getNode(tree_node_label);
	
				if (node == null)
					continue;
				
				String clust = node.clusterId;
	
				if (clustId_to_treenode.get(clust) == null)
					System.out.println(clust + " is null");
				
				// set n as child of cluster
				clustId_to_treenode.get(clust).addChild(LabelPropogation.tree.get(tree_node_label));
	
				// remove n from anti-chain
				to_remove.add(tree_node_label);
			}
	
			for (String m : to_remove) {
				LabelPropogation.tree.remove(m);
			}
	
			to_remove.clear();
		}
		
		
		TreeNode<String> disconnected_components = 
				new TreeNode<String>(graph.getId() + clstr_mark + "_disc_components");
		// add clusters to anti-chain and collect zero degree clusters
		for (String s : clusters.keySet()) {
			

			if (quotient.getNode(s).getDegree() == 0) {
				if (second_contraction_flag) {
					TreeNode<String> clust_to_remove = new TreeNode<String>(s);
					disconnected_components.addChild(clust_to_remove);
				}
				GraphNode s_node = quotient.getNode(s);
				quotient.removeNode(s_node);
				continue;
			}
			
			LabelPropogation.tree.put(s, clustId_to_treenode.get(s));
		}
		
		////////////////////////
		if (disconnected_components.getChildCount() != 0) {
			//LabelPropogation.tree.put(graph.getId() + "c_disc_components", disconnected_components);
			// System.out.println("added " + graph.getId() + "c_disc_components");
		}

		to_add.clear();
	}
	
	// update_anti_chain (after an internal quotient build):
	// This function gets called at the end of build_quotient
	// --- assigns old nodes of anti_chain as children of nodes resulting from clustering 
	// --- removes old nodes from anti_chain and adds new nodes resulting from clustering to anti_chain
	public static void update_anti_chain_external(Graph graph, Graph quotient, TreeMap<String, Integer> clusters,
			HashMap<String, TreeNode<String>> clustId_to_treenode) {

		ArrayList<String> to_remove = new ArrayList<String>();
		ArrayList<String> to_add    = new ArrayList<String>();
		
		if (second_contraction_flag) {

			for (String tree_node_label : LabelPropogation.tree.keySet()) {
				
				if (tree_node_label.contains("disc_components"))
					continue;
	
				GraphNode node = graph.getNode(tree_node_label);
				
				if (node == null) 
					continue;
	
				String clust = node.clusterId;
	
				// set n as child of cluster
				clustId_to_treenode.get(clust).addChild(LabelPropogation.tree.get(tree_node_label));
	
				// remove n from anti-chain
				to_remove.add(tree_node_label);
			}
	
			for (String m : to_remove) {
				LabelPropogation.tree.remove(m);
			}
	
			to_remove.clear();
		}
		
		TreeNode<String> disconnected_components = 
				new TreeNode<String>(graph.getId() + clstr_mark + "_disc_components");
		// add clusters to anti-chain and collect zero degree clusters
		for (String s : clusters.keySet()) {
			
			if (!LabelPropogation.degree_map.containsKey(s)) {
				if (second_contraction_flag) {
					TreeNode<String> clust_to_remove = new TreeNode<String>(s);
					disconnected_components.addChild(clust_to_remove);
				}
				GraphNode s_node = quotient.getNode(s);
				quotient.removeNode(s_node);
				continue;
			}
			
			LabelPropogation.tree.put(s, clustId_to_treenode.get(s));
		}
		
		/////////////////////// DISC components stuff
		//if (disconnected_components.getChildCount() != 0)
			//LabelPropogation.tree.put(graph.getId() + "c_disc_components", disconnected_components);
		

		to_add.clear();
	}
	
	public static void compute_jaccard_density(Graph top_level_graph) {
		double avg_jaccard_density = 0.0;
		double largest_jaccard_density = 0.0;
		double smallest_jaccard_density = 1000.0;
		GraphNode most = null;
		GraphNode least = null;
		double count = 0.0;
		for (GraphNode c: top_level_graph.getNodeSet()) {
			count += 1.0;
			
			int size_of_c = c.weight; 
			double denom = (double) (((size_of_c) * (size_of_c - 1)) / 2 + 1);
			int n = c.int_edge_wsum;
			double numer = (double) n;
			double j_density_of_c = numer/denom;
			
			if (j_density_of_c > largest_jaccard_density) {
				most = c;
				largest_jaccard_density = j_density_of_c;
			}
			
			if (j_density_of_c < smallest_jaccard_density) {
				least = c;
				smallest_jaccard_density = j_density_of_c;
			}
			
			avg_jaccard_density += j_density_of_c;
			System.out.println("Cluster " + c.id + " j-density: " + (Double.toString(j_density_of_c)));
		}
		
		avg_jaccard_density = (avg_jaccard_density / count);
		System.out.println("Average j-density: " + Double.toString(avg_jaccard_density));
		System.out.println("Largest j-density is cluster " + most.id + ": " + Double.toString(largest_jaccard_density));
		System.out.println("Smallest j-density is cluster " + least.id + ": " + Double.toString(smallest_jaccard_density));
	}
		
}
