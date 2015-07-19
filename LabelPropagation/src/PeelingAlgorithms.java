
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class PeelingAlgorithms {
	
	// Internal peeling algorithm
	public static Graph peel (Graph graph, int k) {
		
		final long startTime = System.currentTimeMillis();
		
		System.out.println("Peeling " + k + " layers..." );
		int count = 0;
		
		HashMap<String,Integer> counter = new HashMap<String,Integer>();
		// initialize counter
		for (GraphNode n : graph.getNodeSet()) {
			if (n.toString().contains("-0"))
				System.out.println(n.id + ": " + n.getDegree());
			counter.put(n.toString(), n.getDegree());
		}
		
		Queue<String> Q = new LinkedList<String>();
		ArrayList<String> peeled_nodes = new ArrayList<String>();
		
		TreeNode<String> disconnected_components = new TreeNode<String>("disc_components");
		LabelPropogation.tree.put("disc_components", disconnected_components);
		
		for (GraphNode v : graph.getNodeSet()) {
			if (!peeled_nodes.contains(v.toString()) && counter.get(v.toString()) < k) {
				peeled_nodes.add(v.toString());
				//if (v.toString().contains("103"))
					//System.out.println("just peeled " + v.toString());
				count++;
				if (count % 1000 == 0)
					System.out.println("peeled: " + count);
				
				if (counter.get(v.toString()) == 0) {
					TreeNode<String> v_tn = null;
					if (!LabelPropogation.tree.containsKey(v.toString())) {
						v_tn = new TreeNode<String>(v.toString());
						disconnected_components.addChild(v_tn);
					}
					else {
						v_tn = LabelPropogation.tree.get(v.toString());
						disconnected_components.addChild(v_tn);
						LabelPropogation.tree.remove(v.toString());
					}
				}
				else {
					// find adjacent UNPEELED node to set as parent
					GraphNode adj_v = null;
					for (GraphNode adj: v.neighbors) {
						if(!peeled_nodes.contains(adj.toString())) {
							adj_v = adj;
							break;
						}
					}
					// System.out.println("peeling degree 1 vertex: " + v.toString() + " with parent " + adj_v.toString());
					if (!LabelPropogation.tree.containsKey(adj_v.toString())) {
						LabelPropogation.tree.put(adj_v.toString(), new TreeNode<String>(adj_v.toString()));
					}
					TreeNode<String> v_tree = null;
					if (!LabelPropogation.tree.containsKey(v.toString())) {
						v_tree = new TreeNode<String>(v.toString());
						LabelPropogation.tree.get(adj_v.toString()).addChild(v_tree);
					}
					else {
						
						v_tree = LabelPropogation.tree.get(v.id);
						LabelPropogation.tree.get(adj_v.id).addChild(v_tree);
						
						for (TreeNode<String> child : v_tree.getChildren()) {
							LabelPropogation.tree.get(adj_v.toString()).addChild(child);
						}
	
						v_tree.clearChildren();
  
						LabelPropogation.tree.remove(v.toString());
					}
					LabelPropogation.tree.remove(v.toString());
						
				}
				
				Q.add(v.toString());
				while (!Q.isEmpty()) {
					String w = Q.remove();
					for (GraphNode u : graph.getNode(w).neighbors) {

						int old_degree = counter.get(u.toString());
						counter.put(u.toString(), old_degree - 1);
						if (!peeled_nodes.contains(u.toString()) && (counter.get(u.toString()) < k)) {
							peeled_nodes.add(u.toString());
							//if (u.toString().contains("103"))
							//	System.out.println("just peeled " + u.toString());
							count++;
							if (count % 1000 == 0)
								System.out.println("peeled: " + count);
							
							if (counter.get(u.toString()) == 0) {
								TreeNode<String> u_tn = null;
								if (!LabelPropogation.tree.containsKey(u.toString())) {
									u_tn = new TreeNode<String>(u.toString());
									disconnected_components.addChild(u_tn);
								}
								else {
									u_tn = LabelPropogation.tree.get(u.toString());
									disconnected_components.addChild(u_tn);
									LabelPropogation.tree.remove(u.toString());
								}
								
							}
							else {
								// find adjacent UNPEELED node to set as parent
								GraphNode adj_u = null;
								for (GraphNode adj: u.neighbors) {
									if(!peeled_nodes.contains(adj.toString())) {
										adj_u = adj;
										break;
									}
								}
								if (adj_u == null) {
									System.out.println(u.toString() + " " + counter.get(u.id));
									
								}
								// System.out.println("peeling degree 1 vertex: " + u.toString() + " with parent " + adj_u.toString());
								if (!LabelPropogation.tree.containsKey(adj_u.toString())) {
									LabelPropogation.tree.put(adj_u.toString(), new TreeNode<String>(adj_u.toString()));
								}
								TreeNode<String> u_tree = null;
								if (!LabelPropogation.tree.containsKey(u.toString())) {
									u_tree = new TreeNode<String>(u.toString());
									LabelPropogation.tree.get(adj_u.toString()).addChild(u_tree);
								}
								else {		
									u_tree = LabelPropogation.tree.get(u.toString());
									LabelPropogation.tree.get(adj_u.toString()).addChild(u_tree);
									
									for (TreeNode<String> u_child : u_tree.getChildren()) {
										LabelPropogation.tree.get(adj_u.toString()).addChild(u_child);
									}
									
									u_tree.clearChildren();
									LabelPropogation.tree.remove(u.toString());
								}
								LabelPropogation.tree.remove(u.toString());
									
							}
							
							Q.add(u.toString());
						}
					}
				}
			}
		}
		
		
		HashMap<String, TreeNode<String>> to_replace = new HashMap<String, TreeNode<String>>();
		
		// replacement
		for (String s : LabelPropogation.tree.keySet()) {
			TreeNode<String> obj = LabelPropogation.tree.get(s);
			to_replace.put(s + "_tree", obj);
		}
		
		LabelPropogation.tree.clear();
		
		for (String s : to_replace.keySet()) {
			TreeNode<String> obj = to_replace.get(s);
			String old_Object = obj.toString();
			obj.setObject(old_Object + "_tree");
			LabelPropogation.tree.put(s, obj);
		}
		// end replacement
		
		
		for (String tree_node : LabelPropogation.tree.keySet()) {
			
			if (tree_node.contains("disc"))
				continue;
			
			String non_tree_node =  tree_node.split("_")[0];
			
			GraphNode n1 = graph.addNode(tree_node);
			GraphNode n2 = graph.getNode(non_tree_node);
				
			n1.weight = LabelPropogation.tree.get(tree_node).getChildCount(); // node weight is size of tree
			n2.weight = 1;
			LabelPropogation.total_node_weight += LabelPropogation.tree.get(tree_node).getChildCount();
			
			int weight = 2; 
			graph.addEdge(n1, n2).weight = weight;
			LabelPropogation.total_edge_weight += weight;
			
			// Edge weight:
			// = 1000 for jaccard graph
			// = 1    for crossrf graph
			
		}
		
		System.out.println("Now removing.");
		
		for (String to_remove : peeled_nodes) {
			GraphNode to_remove_node = graph.getNode(to_remove);
			graph.removeNode(to_remove_node);
		}
		
		System.out.println("Peeled " + k + " layers: removed " + peeled_nodes.size() + " vertices.");
		
		final long endTime = System.currentTimeMillis();
		System.out.println("done in: "
				+ ((double) (endTime - startTime) / 1000) + " seconds.");
		
		return graph;
	}
	
}

