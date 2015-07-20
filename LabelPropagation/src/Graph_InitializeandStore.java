

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class Graph_InitializeandStore {
	
	
	public static void storeGraph(Graph g, String name, String path)
			throws IOException {
	

		System.out.println("Storing graph " + name + '-' + g.getId() + "...");

		String edge_file_name = name + '-' + g.getId() + "-Edge_Set.txt";

		// empty files
		PrintWriter pf = new PrintWriter("./" + path + '/' + edge_file_name);
		pf.close();

		PrintWriter edge_file = new PrintWriter("./" + path + '/'
				+ edge_file_name);

		ArrayList<GraphNode> nodes = new ArrayList<>();
		for (GraphNode n : g.getNodeSet()) {
			nodes.add(n);
		}
		if (!LabelPropagation.authors_flag)
			Collections.sort(nodes, idComparator);

		for (GraphNode n : nodes) {
			// long p = edge_file.getFilePointer();
			// p_file.write(n.getId() + ':' + Long.toString(p) + ' ');
			if (g.getId() == "1")
				edge_file.write(n.id + " ");
			else
				edge_file.write("c" + n.id + " ");
			for (GraphNode m : n.neighbors) {
				edge_file.write(m.id + " ");
			}
			edge_file.println();
		}
		edge_file.close();
	}
	
	public static void storeGraph_w_degree(Graph g, String name, String path)
			throws IOException {
	

		System.out.println("Storing graph " + name + '-' + g.getId() + "...");

		String edge_file_name = name + '-' + g.getId() + "-EDGE_DISPLAY.txt";

		// empty files
		PrintWriter pf = new PrintWriter("./" + path + '/' + edge_file_name);
		pf.close();

		PrintWriter edge_file = new PrintWriter("./" + path + '/'
				+ edge_file_name);

		ArrayList<GraphNode> nodes = new ArrayList<>();
		for (GraphNode n : g.getNodeSet()) {
			nodes.add(n);
		}
		
		if (!LabelPropagation.authors_flag)
			Collections.sort(nodes, idComparator);

		for (GraphNode n : nodes) {
			// long p = edge_file.getFilePointer();
			// p_file.write(n.getId() + ':' + Long.toString(p) + ' ');
			if (g.getId() == "1")
				edge_file.write(n.id + " " + n.getDegree() + " ");
			else
				edge_file.write("c" + n.id + " ");
			for (GraphNode m : n.neighbors) {
				edge_file.write(m.id + " ");
			}
			edge_file.println();
		}
		edge_file.close();
	}


	public static Graph initialize_graph_crossref(String filename, boolean semi_ext) 
			throws IOException {
		
		java.nio.file.Path FROM = Paths.get(LabelPropagation.graph_file_name);
		java.nio.file.Path TO = Paths.get(LabelPropagation.path + "/" 
				+ LabelPropagation.graph_file_name.substring(0, 
						LabelPropagation.graph_file_name.length() - 4) 
				+ '-' + "1" + "-Edge_Set.txt");
		
		Files.copy(FROM, TO);
		
		Graph graph = new Graph(false, "1");
		FileReader file_r = null;
		try {
			file_r = new FileReader(filename);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			System.out.println("Reading...");
			BufferedReader reader = new BufferedReader(file_r);
			String line;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(" ");
				int id = Integer.parseInt(split[0]);
				GraphNode current = graph.getNode(split[0]);
				if (current == null)
					current = graph.addNode(split[0]);
		
				current.weight = 1;
				HashSet<String> toAdd = new HashSet<>();
				
				for (int i = 0; i < split.length - 2; i++) {
					int to = Integer.parseInt(split[2 + i]);
					if (to > 258545)
						continue;
					toAdd.add(split[2 + i]);
				}

				// Build edge set 
				for (String s : toAdd) {
					GraphNode dest = graph.getNode(s);
					if (dest == null) {
						dest = graph.addNode(s);
						graph.getNode(s).weight = 1;
					}

					if (!semi_ext) {
						if (graph.getEdge(split[0],s) == null)
							graph.addEdge(split[0], s);
					}

				}

				// Specify number of nodes to add
				if (id > LabelPropagation.capacity) {
					break;
				}

			}

			for (Edge e : graph.getEdgeSet()) {
				e.weight = 1;
			}

			reader.close();
			file_r.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Graph initialized. ");

		return graph;
	}
	
	// (1) This version assumes "node1,weight1 node2,weight2 ... " adjacency format
	public static Graph initialize_graph(String filename, boolean semi_ext) 
			throws IOException {
		
		java.nio.file.Path FROM = Paths.get(LabelPropagation.graph_file_name);
		java.nio.file.Path TO = Paths.get(LabelPropagation.path + "/" 
				+ LabelPropagation.graph_file_name.substring(0, 
						LabelPropagation.graph_file_name.length() - 4) 
				+ '-' + "1" + "-Edge_Set.txt");
		
		Files.copy(FROM, TO);
		
		Graph graph = new Graph(false, "1");
		FileReader file_r = null;
		try {
			file_r = new FileReader(filename);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			System.out.println("Reading graph...");
			BufferedReader reader = new BufferedReader(file_r);
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(" ");
				
				String current_name = split[0];
				GraphNode currentnode = graph.getNode(current_name);
				
				if (currentnode == null)
					currentnode = graph.addNode(current_name);	
				
				currentnode.weight = 1; // Assumes node weight 1
				
				// if current has no adjacency list, continue
				if (split.length == 1) {
					continue;
				}
				
						
				// scan adjacency list
				for (int i = 1; i < split.length; i++) {
					
					String[] node_w_weight = split[i].split(",");
					
					String dest_name = node_w_weight[0];
					int edge_weight = Integer.parseInt(node_w_weight[1]);
					
					GraphNode destnode = graph.getNode(dest_name);
					
					if (!LabelPropagation.degree_map.containsKey(current_name)) 
						LabelPropagation.degree_map.put(current_name, 1);
					else {
						int old_deg = LabelPropagation.degree_map.get(current_name);
						LabelPropagation.degree_map.put(current_name, old_deg + 1);
					}
					
					// add destnode to graph if not yet added
					if (destnode == null) {
						graph.addNode(dest_name);
					}
					
					destnode = graph.getNode(dest_name);
					
					if (destnode == null)
						System.out.println(dest_name + "dest is null");
					
					if (currentnode == null)
						System.out.println(current_name + "curr is null");
					
					destnode.weight = 1; // Assumes node weight 1 
					
					if (!semi_ext) {
						if (!graph.hasEdgeBetween(currentnode, destnode) && 
								!graph.hasEdgeBetween(destnode, currentnode)) {
							Edge ei = graph.addEdge(currentnode, destnode);
							ei.weight = edge_weight;
							LabelPropagation.total_edge_weight += edge_weight;
						}
					}
							
				}
				
				count++;
				// Specify number of nodes to add
				if (count > LabelPropagation.capacity && !semi_ext) {
					break;
				}
				
				
			}
			
			LabelPropagation.total_node_weight = graph.getNodeCount();

			reader.close();
			file_r.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return graph;
	}
	
	// (2) This version assumes "node1 node2 ..." adjacency format
	public static Graph initialize_graph_unweighted(String filename, boolean semi_ext) 
			throws IOException {
		
		java.nio.file.Path FROM = Paths.get(LabelPropagation.graph_file_name);
		java.nio.file.Path TO = Paths.get(LabelPropagation.path + "/" 
				+ LabelPropagation.graph_file_name.substring(0, 
						LabelPropagation.graph_file_name.length() - 4) 
				+ '-' + "1" + "-Edge_Set.txt");
		
		Files.copy(FROM, TO);
		
		Graph graph = new Graph(false, "1");
		FileReader file_r = null;
		try {
			file_r = new FileReader(filename);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			System.out.println("Reading graph...");
			BufferedReader reader = new BufferedReader(file_r);
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(" ");
				
				String current_name = split[0];
				GraphNode currentnode = graph.getNode(current_name);
				
				if (currentnode == null)
					currentnode = graph.addNode(current_name);	
				
				currentnode.weight = 1; // Assumes node weight 1
				
				// if current has no adjacency list, continue
				if (split.length == 1) {
					continue;
				}
				
						
				// scan adjacency list
				for (int i = 1; i < split.length; i++) {
					
					String dest_name = split[i];
					
					GraphNode destnode = graph.getNode(dest_name);
					
					if (!LabelPropagation.degree_map.containsKey(current_name)) 
						LabelPropagation.degree_map.put(current_name, 1);
					else {
						int old_deg = LabelPropagation.degree_map.get(current_name);
						LabelPropagation.degree_map.put(current_name, old_deg + 1);
					}
					
					// add destnode to graph if not yet added
					if (destnode == null) {
						graph.addNode(dest_name);
					}
					
					destnode = graph.getNode(dest_name);
					
					if (destnode == null)
						System.out.println(dest_name + "dest is null");
					
					if (currentnode == null)
						System.out.println(current_name + "curr is null");
					
					destnode.weight = 1; // Assumes node weight 1 
					
					if (!semi_ext) {
						if (!graph.hasEdgeBetween(currentnode, destnode) && 
								!graph.hasEdgeBetween(destnode, currentnode)) {
							Edge ei = graph.addEdge(currentnode, destnode);
							ei.weight = 1;
							LabelPropagation.total_edge_weight += 1;
						}
					}
							
				}
				
				count++;
				// Specify number of nodes to add
				if (count > LabelPropagation.capacity && !semi_ext) {
					break;
				}
				
				
			}
			
			LabelPropagation.total_node_weight = graph.getNodeCount();

			reader.close();
			file_r.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return graph;
	}
	
	
	// write cluster file (c_i : child_1, child_2 ...)
	public static void storeClustering(Graph g, String name, String path)
			throws IOException {
		String cluster_file_name = name + '-' + g.getId() + "-Cluster_Set.txt";

		PrintWriter pw = new PrintWriter("./" + path + '/' + cluster_file_name);

		HashMap<String, HashSet<GraphNode>> cluster_to_kids = new HashMap<String, HashSet<GraphNode>>();

		for (GraphNode n : g.getNodeSet()) {
			String clust = n.clusterId;


			if (!cluster_to_kids.containsKey(clust)) {
				HashSet<GraphNode> kids = new HashSet<GraphNode>();
				kids.add(n);
				cluster_to_kids.put(clust, kids);
			} else {
				HashSet<GraphNode> kids = cluster_to_kids.get(clust);
				kids.add(n);
			}
		}

		for (String s : cluster_to_kids.keySet()) {
			HashSet<GraphNode> kids = cluster_to_kids.get(s);
			pw.print(s + " " + kids.size());
			pw.println();
			for (GraphNode k : kids) {
				String kstr = k.toString();
				if (!kstr.contains("c")) {
					pw.print(k.id + " 0");
					pw.println();
				} else {
					pw.print(k.id + " " + k.weight);
					pw.println();
				}
			}
		}
		pw.close();
	}
	
	static Comparator<GraphNode> idComparator = new Comparator<GraphNode>() {
		@Override
		public int compare(GraphNode n1, GraphNode n2) {
			int n1Id = 0, n2Id = 0;
			
			if (n1Id == 0 && n1.toString().contains("p"))
				n1Id = Integer.parseInt(n1.toString().split("p")[0]);
			
			if (n2Id == 0 && n2.toString().contains("p"))
				n2Id = Integer.parseInt(n2.toString().split("p")[0]);
			
			if (n1Id == 0 && n1.toString().contains("tree")) {
				n1Id = Integer.parseInt(n1.toString()
						.substring(0, n1.toString().length() - 5));
			}
			if (n2Id == 0 && n2.toString().contains("tree")) {
				n2Id = Integer.parseInt(n2.toString()
						.substring(0, n2.toString().length() - 5));
			}
			
			if (!n1.toString().contains("c") || !n2.toString().contains("c")) {
				if (n1Id == 0)
					n1Id = Integer.parseInt(n1.toString());
				if (n2Id == 0)
					n2Id = Integer.parseInt(n2.toString());
			} else {
				if (n1Id == 0)
					n1Id = Integer.parseInt(n1.toString().substring(2));
				if (n2Id == 0)
					n2Id = Integer.parseInt(n2.toString().substring(2));
			}
			return n1Id - n2Id;
		}
	};
	
	
}
