import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import main.Clicks;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class HierarchyView {

	// public static final String path =
	// "C:\\Users\\reu\\workspace2\\HierarchyView\\src\\test_graph\\";
	public static final String path = "Z:\\REU OEIS Group\\Heirarchies\\_HierarchyView\\";
	public static final String to_pump = "pump";
	public static int type = 0;
	// public static final String edgeFile = "tree_edges.txt";
	// public static final String hierarchyFile = "tree.txt";
	public static String edgeFile = "hards,weight.txt-1-Edge_Set.txt";
	public static String hierarchyFile = "hardsbutnotreally.txt";
	public static boolean label = true;
	public static final String sel = "selection";
	public static final int MIN_SIZE = 3;
	public static int sizeScale = 5;
	// public static PrintStream errStream = null;
	public static Graph graph;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new OptionSelect();
			}
		});
		synchronized(to_pump) {
			try {
				to_pump.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		switch(type) {
		case 0:
			UI.pump();
			break;
		case 1:
			Clicks.pump();
			break;
		}
		
	}

	public static void begin() {
		graph = new SingleGraph("Expandable Graph");
		graph.addAttribute("ui.stylesheet", "url('style.css')");
		// graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
		UI.create(graph);
	}

	public static DefaultMutableTreeNode buildTree(String fname) {
		DefaultMutableTreeNode root = null;
		try (BufferedReader br = new BufferedReader(
				new FileReader(path + fname))) {
			root = buildTreeRecursive(br);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return root;
	}

	private static DefaultMutableTreeNode buildTreeRecursive(BufferedReader br)
			throws IOException {
		String[] line = br.readLine().split(" ");
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(line[0]);
		for (int i = 0; i < Integer.parseInt(line[1]); i++) {
			node.add(buildTreeRecursive(br));
		}
		return node;
	}

	public static void expandNode(DefaultMutableTreeNode node) {
		Map<String, String> leafToCluster = new HashMap<>();
		String id = (String) node.getUserObject();
		double color = 0.0;
		if (graph.getNode(id) != null) {
			// keep track of existing objects with edges incident on expanding
			// node
			for (Edge e : graph.getNode(id).getEachEdge()) {
				Node dest = e.getNode0();
				if (dest.getId().equals(id))
					dest = e.getNode1();
				String destId = dest.getId();
				ArrayList<String> destLeaves = dest.getAttribute("leaves");
				for (String leaf : destLeaves) {
					leafToCluster.put(leaf, destId);
				}
			}
			ArrayList<Edge> es = new ArrayList<>();
			for (Edge e : graph.getNode(id).getEachEdge()) {
				es.add(e);
			}
			for (Edge e : es) {
				graph.removeEdge(e);
			}
			color = graph.getNode(id).getAttribute("ui.color");
			graph.removeNode(id);
		}
		// System.out.println(new Throwable().getStackTrace()[0]);
		// Create each new vertex
		int clusterSize = UI.getChildCount(node);
		for (int i = 0; i < clusterSize; i++) {
			// System.out.println(new Throwable().getStackTrace()[0]);
			DefaultMutableTreeNode clusterNode = UI.getChild(node, i);
			String newId = (String) clusterNode.getUserObject();
			Node newNode = graph.addNode(newId);
			if (label)
				newNode.addAttribute("ui.label", newId.split(":")[0]);
			newNode.addAttribute("ui.color", color);
			ArrayList<String> leaves = new ArrayList<>();

			// BFS for leaves, per newly created node
			LinkedList<DefaultMutableTreeNode> queue = new LinkedList<>();
			queue.push(clusterNode);
			while (queue.size() > 0) {
				DefaultMutableTreeNode current = queue.pop();
				int children = UI.getChildCount(current);
				if (children == 0) {
					leafToCluster.put(newId, newId);
					leaves.add(newId);
				}
				// System.out.print(current.getUserObject());
				// System.out.print("  ");
				// System.out.println(children);
				for (int j = 0; j < children; j++) {
					DefaultMutableTreeNode child = UI.getChild(current, j);
					if (UI.getChildCount(child) > 0) {
						queue.push(child);
					} else {
						String leaf = (String) child.getUserObject();
						leafToCluster.put(leaf, newId);
						leaves.add(leaf);
					}
				}
			}
			// newNode.setAttribute("ui.size",
			// sizeCalc(UI.getChildCount(clusterNode)));
			newNode.setAttribute("ui.size", sizeCalc(leaves.size()));
			newNode.addAttribute("leaves", leaves);
		}
		// System.out.println(new Throwable().getStackTrace()[0]);
		// edge between leaves in file implies edge of weight current + 1
		// between their containing clusters
		// for (String key : leafToCluster.keySet()) {
		// System.out.println(key + "->" + leafToCluster.get(key));
		// }
		try (BufferedReader br = new BufferedReader(new FileReader(path
				+ edgeFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(" ");
				if (leafToCluster.containsKey(split[0])) {
					String id1 = leafToCluster.get(split[0]);
					if (id1 == null)
						System.err.println(split[0] + " is not in the map");
					for (int i = 0; i < Integer.parseInt(split[1]); i++) {
						String id2 = leafToCluster.get(split[2 + i]);
						if (id2 == null)
							continue; // edge is already accounted for in graph

						Node n1 = graph.getNode(id1);
						if (n1 == null) {
							System.err.println("Could not find node " + id1);
							continue;
						}
						Edge e = n1.getEdgeBetween(id2);
						if (e == null) {
							if (!id1.equals(id2)) {
								e = graph.addEdge(id1 + "--" + id2, id1, id2);
								e.addAttribute("weight", 1);
							}
						} else {
							e.setAttribute("weight",
									(Integer) e.getAttribute("weight") + 1);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		UI.resetView();
	}

	@SuppressWarnings("unchecked")
	public static void collapseCluster(DefaultMutableTreeNode node) {
		String id = (String) node.getUserObject();
		double color = 0.0;
		Node newNode = graph.addNode(id);
		if (label)
			newNode.addAttribute("ui.label", id.split(":")[0]);
		ArrayList<String> leaves = new ArrayList<>();
		int children = UI.getChildCount(node);
		// newNode.addAttribute("ui.size", sizeCalc(children));
		for (int i = 0; i < children; i++) {
			DefaultMutableTreeNode child = UI.getChild(node, i);
			String childId = (String) child.getUserObject();
			Node toRemove = graph.getNode(childId);
			if (toRemove == null)
				collapseCluster(child);
		}
		for (int i = 0; i < children; i++) {
			DefaultMutableTreeNode child = UI.getChild(node, i);
			String childId = (String) child.getUserObject();
			Node toRemove = graph.getNode(childId);
			// if(toRemove == null) collapseCluster(child);
			for (Edge e : toRemove.getEachEdge()) {
				Node dest = e.getNode0();
				if (childId.equals(dest.getId()))
					dest = e.getNode1();
				String id2 = dest.getId();
				Edge existing;
				if ((existing = dest.getEdgeBetween(newNode)) == null) {
					existing = graph.addEdge(id + "--" + id2, newNode, dest);
					existing.addAttribute("weight", 1);
				} else {
					existing.setAttribute("weight",
							(Integer) existing.getAttribute("weight") + 1);
				}
			}
			leaves.addAll((ArrayList<String>) toRemove.getAttribute("leaves"));
			ArrayList<Edge> es = new ArrayList<>();
			for (Edge e : toRemove.getEachEdge()) {
				es.add(e);
			}
			for (Edge e : es) {
				graph.removeEdge(e);
			}
			color = toRemove.getAttribute("ui.color");
			graph.removeNode(toRemove);
		}
		newNode.addAttribute("leaves", leaves);
		newNode.addAttribute("ui.size", sizeCalc(leaves.size()));
		newNode.addAttribute("ui.color", color);
		UI.resetView();
	}

	public static void assignColors() {
		double nodes = graph.getNodeCount(), i = 0;
		for (Node n : graph) {
			n.setAttribute("ui.color", i / nodes);
			i++;
		}
	}

	public static int sizeCalc(int param) {
		return (int) (sizeScale * Math.log(param + 1) + MIN_SIZE);
	}
}
