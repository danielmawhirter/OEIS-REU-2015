package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

import static org.graphstream.algorithm.Toolkit.*;

import org.graphstream.algorithm.Algorithm;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.algorithm.PageRank;
import org.graphstream.algorithm.flow.EdmondsKarpAlgorithm;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

// 06.22.2015 2:16 PM

public class Main {
	
	public static String from = "40", to = "108";

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				notmain();
			}
		});
		Clicks.pump();
	}
	
	public static void notmain() {
		Graph graph = new SingleGraph("OEIS");

		int count = 0;

		// viewer.disableAutoLayout();

		graph.addAttribute("ui.stylesheet", "url('style1.css')");
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		HashSet<String> nodes = new HashSet<>();

		String edgeFile = "graph3.txt";
		String keywordFile = "data_keywords.txt";
		String commentsFile = "data_comment_words.txt";

		try {

			count = 0;
			FileReader fileReader2 = new FileReader(keywordFile);
			BufferedReader bufferedReader2 = new BufferedReader(fileReader2);

			String line = null;

			while ((line = bufferedReader2.readLine()) != null) {
				String[] inputLine = line.split(" ");
				String nstr = inputLine[0];
				if (graph.getNode(nstr) == null) {
					for (int i = 1; i < inputLine.length; i++) {
						String wordi = inputLine[i];
						if (wordi.equals("hard") || wordi.equals("easy") || wordi.equals("nonn")) {

							Node n = graph.addNode(nstr);
							nodes.add(nstr);
							n.addAttribute("ui.class", wordi);
							n.addAttribute("keyword", wordi);
							break;
						}
					}
				}
				//Thread.sleep(1);
				count++;
				if (count > 100000) {
					//break;
				}
			}

		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + keywordFile + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + keywordFile + "'");
		}

		Node source = graph.getNode(from);
		Node dest = graph.getNode(to);

		if (source == null || dest == null) {
			System.out.println("dest or source is null");
			return;
		}

		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(edgeFile);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line = null;
			count = 0;
			while ((line = bufferedReader.readLine()) != null) {

				String[] inputLine = line.split(" ");

				String nstr = inputLine[0];
				String deg = inputLine[1];

				if (graph.getNode(nstr) == null) {
					continue;
					// graph.addNode(nstr);
					// nodes.add(nstr);
				}

				Node n = graph.getNode(nstr);

				for (int i = 2; i < inputLine.length; i++) {
					String n2str = inputLine[i];

					if (graph.getNode(n2str) == null) {
						continue;
						// graph.addNode(n2str);
						// nodes.add(n2str);
					}

					Node n2 = graph.getNode(n2str);

					String estr = "(" + nstr + "," + n2str + ")";

					if (graph.getEdge(estr) == null) {
						Edge e = graph.addEdge(estr, nstr, n2str, true);
						e.addAttribute("length", 1);
						//System.out.println("edge added: " + e.toString());
						/*
						 * try { Thread.sleep(30); } catch (InterruptedException e1) { // TODO Auto-generated
						 * catch block e1.printStackTrace(); }
						 */
					}
				}
			}

		}

		catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + edgeFile + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + edgeFile + "'");
		}
		
		//int diameter = (int) diameter(graph);

		multiDijkstra(graph, source, dest);

		viewNeighbors(graph);

		try {
			FileReader fileReader = new FileReader(commentsFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				String[] inputLine = line.split(" ");
				if (inputLine.length < 3) {
					continue;
				}

				String nstr = inputLine[0];
				Node n = graph.getNode(nstr);
				if (n == null) {
					continue;
				}

				HashMap<String, Integer> comments = new HashMap<String, Integer>();
				for (int i = 2; i < inputLine.length; i = i + 2) {
					String word = inputLine[i];
					int freq = Integer.parseInt(inputLine[i + 1]);
					comments.put(word, freq);
				}

				n.setAttribute("comments", comments);

			}

		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + edgeFile + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + edgeFile + "'");
		}

		HashMap<String, Integer> displayedWords = new HashMap<String, Integer>();

		count = 0;

		eachEdge: for (Edge e : graph.getEdgeSet()) {
			Node u = e.getNode0();
			Node v = e.getNode1();

			double jaccard = -1;
			int num = 0;
			int denom = 0;

			HashMap<String, Integer> ucomments = u.getAttribute("comments");
			HashMap<String, Integer> vcomments = v.getAttribute("comments");

			HashMap<String, Integer> commonWords = new HashMap<String, Integer>();

			if (ucomments == null || vcomments == null) {
				continue;
			}

			if (ucomments.keySet() == null || vcomments.keySet() == null) {
				continue;
			}

			for (String x : ucomments.keySet()) {
				int xfreq = ucomments.get(x);
				for (String y : vcomments.keySet()) {
					int yfreq = vcomments.get(y);
					if (x.equals(y)) {
						int minFreq = Math.min(xfreq, yfreq);
						//System.out.println(x + ": " + Math.min(xfreq, yfreq));
						num += minFreq;
						denom += minFreq;

						commonWords.put(x, minFreq);
					} else {
						denom += xfreq;
						denom += yfreq;
					}
				}
			}

			jaccard = (double) num / denom;
			double jaccardScaled = Math.cbrt(jaccard);

			e.setAttribute("ui.color", jaccardScaled);

			String topWord = "";
			int max = 0;

			for (String s : commonWords.keySet()) {
				int freq = commonWords.get(s);
				if (freq >= max) {
					max = freq;
					topWord = s;
				}
			}

			e.setAttribute("ui.label", topWord);
			count++;
			e.setAttribute("ui.style", "text-background-mode: rounded-box;");
			e.setAttribute("ui.style", "text-size: " + 20 + ";");

			if (!displayedWords.containsKey(topWord)) {
				displayedWords.put(topWord, 1);
				System.out.println("just added: " + topWord);
				//count++;
			} else {
				displayedWords.put(topWord, displayedWords.get(topWord) + 1);
			}

			//System.out.println(Math.cbrt(jaccard));

		}

		/*for (Edge e : graph.getEdgeSet()) {
			Node u = e.getNode0();
			Node v = e.getNode1();
			String label = e.getAttribute("ui.label");
			if (label == null) {
				continue;
			}
			int freq = displayedWords.get(label);
			if (freq <= 1) {
				continue;
			} else {
				e.removeAttribute("ui.label");
				displayedWords.put(label, freq - 1);
			}
			
		}*/

		//AStar astar = new AStar(graph);

		//astar.compute(source.toString(), dest.toString());

		// FordFulkersonAlgorithm ffa = new FordFulkersonAlgorithm();

		if (source == null || dest == null) {
			System.out.println("source or dest is null");
			return;
		}

		if (source.hasEdgeToward(dest)) {
			System.out.println(source.toString() + " cross references " + dest.toString());
			//return;
		}

		//multiDijkstra(graph, source, dest);

		//System.out.println("maxFlow: " + maxFlow);
		//System.out.println(astar.getShortestPath());

		//viewNeighbors(graph);

		// applyCores(graph);

		String clusterFile = "data_comment_words.txt";

		ArrayList<String> allSequences = allSequences(clusterFile);

		peel(graph, 3);

		for (Edge e : graph.getEdgeSet()) {
			e.setAttribute("represented", false);
		}

		for (Node n : graph) {
			n.setAttribute("represented", false);
		}

		HashSet<Node> nodeReps = nodeReps(graph);

		for (Node n : graph) {
			if (nodeReps.contains(n)) {
				n.setAttribute("ui.label", n.getAttribute("ui.label"));
				int strength = n.getAttribute("strength");
				int size = (int) (10 * Math.log(strength + 1));
				n.setAttribute("ui.style", "text-size: " + size + ";");
			} else {
				n.removeAttribute("ui.label");
			}
		}

		//HashSet<Edge> edgeReps = getEdgeReps(graph);

		//HashMap<Edge, String> repToLabel = new HashMap<Edge, String>();

		/*
		for (Edge e : graph.getEdgeSet()) {
			if (edgeReps.contains(e)) {
				e.setAttribute("ui.label", e.getAttribute("ui.label"));
				int strength = e.getAttribute("strength");
				int size = (int) (10 * Math.log(strength + 1));
				e.setAttribute("ui.style", "text-size: " + size + ";");
				//e.setAttribute("ui.style", "text-visibility-mode: under-zoom;");
				//e.setAttribute("ui.style", "text-visibility: 0.9;");
			} else {
				e.removeAttribute("ui.label");
				//e.setAttribute("ui.style", "text-visibility-mode: under-zoom;");
				//e.setAttribute("ui.style", "text-visibility: 0.9;");
			}
			//e.setAttribute("ui.style", "text-visibility-mode: under-zoom;");
			//e.setAttribute("ui.style", "text-visibility: 0.9;");
		}
		
		*/

		System.out.println("count: " + count);

		System.out.println("graph.getNodeSet().size(): " + graph.getNodeSet().size());
		System.out.println("graph.getEdgeSet().size(): " + graph.getEdgeSet().size());
		//System.out.println("displayedWords.keySet().size(): " + displayedWords.keySet().size());
		System.out.println("nodeReps.size(): " + nodeReps.size());
		
		//System.out.println("diameter: " + diameter);

		Clicks clicks = new Clicks(graph);
		Clicks.instanciated.set(true);
		Clicks.startPump();
	}

	public static HashSet<Node> nodeReps(Graph graph) {
		HashSet<Node> reps = new HashSet<Node>();

		for (Node n : graph) {
			if (n.getAttribute("represented")) {
				continue;
			}
			int same = 0;
			int total = 0;
			double portion = 0;

			String nLabel = getTopLabel(graph, n);
			Iterator<Node> neighbors = n.getNeighborNodeIterator();
			while (neighbors.hasNext()) {
				Node ni = neighbors.next();
				String niLabel = getTopLabel(graph, ni);
				if (niLabel.equals(nLabel)) {
					same++;
					total++;
				} else {
					total++;
				}
			}
			portion = (double) same / total;
			if (portion > 0.9 && same > 1000) {
				reps.add(n);
				n.addAttribute("strength", same);
			}
		}

		return reps;
	}

	public static String getTopLabel(Graph graph, Node n) {

		String topWord = "";
		int max = 0;
		HashMap<String, Integer> labels = labelsOfIncidentEdges(graph, n);
		for (String s : labels.keySet()) {
			int freq = labels.get(s);
			if (freq > max) {
				max = freq;
				topWord = s;
			}
		}

		return topWord;

	}

	public static HashMap<String, Integer> labelsOfIncidentEdges(Graph graph, Node n) {
		HashMap<String, Integer> labels = new HashMap<String, Integer>();
		Collection<Edge> incidentEdges = n.getEdgeSet();
		for (Edge e : incidentEdges) {
			String elabel = e.getAttribute("ui.label");
			if (elabel == null || elabel.length() < 2) {
				continue;
			}
			if (labels.containsKey(e)) {
				labels.put(elabel, labels.get(e) + 1);
			} else {
				labels.put(elabel, 1);
			}
		}

		return labels;
	}

	public static Edge getEdgeRep(Graph graph, String label) {
		Edge rep = null;
		int max = 0;

		HashSet<Edge> hasTheLabel = new HashSet<Edge>();
		for (Edge e : graph.getEdgeSet()) {
			String elabel = e.getAttribute("ui.label");
			boolean represented = e.getAttribute("represented");
			if (elabel == null || represented) {
				continue;
			}

			if (elabel.equals(label)) {
				hasTheLabel.add(e);
			}

		}

		if (hasTheLabel.isEmpty()) {
			System.out.println("empty...");
		}

		for (Edge e : hasTheLabel) {
			int same = 0;
			int total = 1;
			//double portion = (double) same / total;

			Node u = e.getNode0();
			Node v = e.getNode1();

			Iterator<Edge> uIt = u.getEachEdge().iterator();
			Iterator<Edge> vIt = v.getEachEdge().iterator();

			while (uIt.hasNext()) {
				total++;
				Edge eu_i = uIt.next();
				if (hasTheLabel.contains(eu_i)) {
					same++;
				}
			}

			while (vIt.hasNext()) {
				total++;
				Edge ev_i = vIt.next();
				if (hasTheLabel.contains(ev_i)) {
					same++;
				}
			}

			//portion = (double) same / total;

			//System.out.println("portion: " + portion);
			if (same >= max) {
				max = same;
				rep = e;
			}
		}

		if (rep == null) {
			return rep;
		}

		rep.setAttribute("strength", max);
		System.out.println("strength: " + max);

		return rep;
	}

	public static HashSet<Edge> edgeReps(Graph graph) {
		HashSet<Edge> reps = new HashSet<Edge>();

		for (Edge e : graph.getEdgeSet()) {
			if (e.getAttribute("represented")) {
				continue;
			}

			String label = e.getAttribute("ui.label");

			int same = 0;
			int total = 0;
			double portion = 0;

			Node u = e.getNode0();
			Node v = e.getNode1();
			Iterator<Edge> uIt = u.getEachEdge().iterator();
			Iterator<Edge> vIt = v.getEachEdge().iterator();

			while (uIt.hasNext()) {
				Edge eu_i = uIt.next();
				String eu_iLabel = eu_i.getAttribute("ui.label");
				if (eu_iLabel != null && eu_iLabel.equals(label)) {
					same++;
					total++;
				} else {
					total++;
				}
			}

			while (vIt.hasNext()) {
				Edge ev_i = vIt.next();
				String ev_iLabel = ev_i.getAttribute("ui.label");
				if (ev_iLabel != null && ev_iLabel.equals(label)) {
					same++;
					total++;
				} else {
					total++;
				}
			}

			portion = (double) same / total;

			if (portion > 0.8) {
				reps.add(e);
				e.addAttribute("strength", same);
				markTheRepresented(graph, e);
			}
		}

		return reps;
	}

	public static void markTheRepresented(Graph graph, Edge rep) {
		Node u = rep.getNode0();
		Node v = rep.getNode1();

		rep.setAttribute("represented", true);

		Iterator<Edge> uIt = u.getEachEdge().iterator();
		Iterator<Edge> vIt = v.getEachEdge().iterator();

		while (uIt.hasNext()) {
			Edge eu_i = uIt.next();
			eu_i.setAttribute("represented", true);
		}

		while (vIt.hasNext()) {
			Edge ev_i = vIt.next();
			ev_i.setAttribute("represented", true);
		}

	}

	/* returns an ArrayList of sequences;
	 * the i-th entry is the string of comments
	 * from the i-th sequence */
	public static ArrayList<String> allSequences(String fileName) {

		ArrayList<String> allSequences = new ArrayList<String>();

		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				allSequences.add(line);
			}

			bufferedReader.close();

		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + fileName + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + fileName + "'");
		}

		return allSequences;

	}

	public static String clusterName(Node n, ArrayList<String> allSequences) {

		int nodeId = Integer.parseInt(n.toString());
		String line = null;

		if (nodeId > (allSequences.size() - 1)) {
			return "none";
		}

		String[] currComments = allSequences.get(nodeId).split(" ");

		for (int i = 2; i < currComments.length; i = i + 2) {

			String word = currComments[i];

			if (word.equals("primes") || word.equals("prime")) {
				return "primes";
			}

			if (word.equals("catalan")) {
				return "catalan";
			}

			if (word.equals("bell")) {
				return "bell";
			}

		}

		return "none";

	}

	public static void writeToPrimesCatalansBells() throws IOException {
		File primes = new File("C:/Users/abello/Documents/OEIS/primes.txt");
		File catalans = new File("C:/Users/abello/Documents/OEIS/catalans.txt");
		File bells = new File("C:/Users/abello/Documents/OEIS/bells.txt");
		File primes_catalans_bells = new File("C:/Users/abello/Documents/OEIS/primes_catalans_bells.txt");

		PrintWriter printWriter = new PrintWriter(primes_catalans_bells);

		FileReader f1 = new FileReader(primes);
		BufferedReader b1 = new BufferedReader(f1);
		FileReader f2 = new FileReader(catalans);
		BufferedReader b2 = new BufferedReader(f2);
		FileReader f3 = new FileReader(bells);
		BufferedReader b3 = new BufferedReader(f3);
		String line = null;
		while ((line = b1.readLine()) != null) {
			printWriter.println(line);
		}
		line = null;
		while ((line = b2.readLine()) != null) {
			printWriter.println(line);
		}
		line = null;
		while ((line = b3.readLine()) != null) {
			printWriter.println(line);
		}

		printWriter.close();

	}

	public static void singleDijkstra(Graph graph, Node source, Node dest) {
		Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, "result", "length");
		dijkstra.init(graph);

		source.setAttribute("ui.size", 1);
		source.setAttribute("ui.label", source.toString());

		dijkstra.setSource(source);

		dijkstra.compute();

		dest.setAttribute("ui.size", 1);
		dest.setAttribute("ui.label", dest.toString());

		for (Node node : dijkstra.getPathNodes(dest)) {
			node.addAttribute("ui.style", "fill-color: green;");
			node.setAttribute("ui.label", node.toString());
			node.setAttribute("ui.style", "text-background-mode: rounded-box;");

			node.addAttribute("protected", "1");
			System.out.println("on the path: " + node.toString());
		}

		for (Edge e : dijkstra.getPathEdges(dest)) {
			System.out.println("looking at edge: " + e.toString());
			e.addAttribute("ui.style", "fill-color: rgba(0,255,0,255);");
		}
	}

	public static void multiDijkstra(Graph graph, Node source, Node dest) {
		Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, "result", "length");
		dijkstra.init(graph);

		source.setAttribute("ui.size", 1);
		source.setAttribute("ui.label", source.toString());

		dijkstra.setSource(source);

		dijkstra.compute();

		dest.setAttribute("ui.size", 1);
		dest.setAttribute("ui.label", dest.toString());

		for (Path p : dijkstra.getAllPaths(dest)) {
			for (Node node : p.getEachNode()) {
				node.addAttribute("ui.style", "fill-color: green;");
				node.setAttribute("ui.label", node.toString());
				node.setAttribute("ui.style", "text-background-mode: plain;");

				node.addAttribute("protected", "1");
				System.out.println("on the path: " + node.toString());
			}

			for (Edge e : p.getEachEdge()) {
				System.out.println("looking at edge: " + e.toString());
				e.addAttribute("ui.class", "isOnPath");
			}
		}

	}

	public static boolean isPeeled(Graph g, int round) {

		for (Node n : g.getNodeSet()) {
			String prot = n.getAttribute("protected");
			String spec = n.getAttribute("special");
			if (prot != null && prot.equals("1")) {
				continue;
			}
			if (spec != null && spec.equals("1")) {
				continue;
			}
			if (n.getDegree() <= round) {
				return false;
			}
		}

		return true;
	}

	public static int getEdgeConnectivity(Graph g) {

		int k = Integer.MAX_VALUE;
		EdmondsKarpAlgorithm flow = new EdmondsKarpAlgorithm();

		System.out.println("k: " + k);
		if (g.getNodeCount() < 2) {
			return 0;
		}
		for (Node u : g) {
			System.out.println("u: " + u.toString());
			for (Node v : g) {
				System.out.println("v: " + v.toString());
				if (!u.equals(v) && u != null && v != null) {
					flow.init(g, u.toString(), v.toString());
					flow.setAllCapacities(1.0);
					flow.compute();
					k = Math.min(k, (int) flow.getMaximumFlow());
					System.out.println("k: " + k);
				}

			}
		}
		return k;
	}

	public static void randomlyRemoveEasies(Graph g, Double prob) {
		ArrayList<Node> toRemove = new ArrayList<Node>();
		for (Node n : g) {
			if (n.getAttribute("protected") != null) {
				if (n.getAttribute("protected").equals("1")) {
					continue;
				}
			}
			String keyword = n.getAttribute("keyword");
			if (keyword != null) {
				if (keyword.equals("easy")) {
					if (Math.random() <= prob) {
						toRemove.add(n);
					}
				}
			}

		}

		for (Node n : toRemove) {
			g.removeNode(n);
		}

	}

	public static void applyPageRank(Graph graph) {
		PageRank pageRank = new PageRank();
		pageRank.init(graph);

		double rmin = 1.0;
		double rmax = 0.0;
		for (Node node : graph) {
			double r = pageRank.getRank(node);
			if (r < rmin)
				rmin = r;
			if (r > rmax)
				rmax = r;
		}

		for (Node node : graph) {
			double rank = pageRank.getRank(node);

			/*
			 * node.addAttribute("ui.size", 5 + Math.sqrt(graph.getNodeCount() * rank * 20));
			 */
			/*
			 * if (rank > 0.005) { node.addAttribute("ui.label",node.getId()); //String.format("%.2f%%", rank
			 * * 100)); //node.setAttribute("ui.size", 50); node.addAttribute("ui.class", "important"); }
			 * //else { //node.setAttribute("ui.size", 2); //}
			 */

			double r_scaled = ((rank - rmin) / (rmax - rmin));
			// node.setAttribute("ui.color", r_scaled);

			// node.addAttribute("ui.size", 3 + r_scaled * 20);
			if (r_scaled > .5) {
				// node.addAttribute("ui.label", node.getId());
			}
			// System.out.println("just modified: " + node.toString());

		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void peel(Graph graph, int layers) {

		for (int i = 1; i <= layers; i++) {
			while (!isPeeled(graph, i)) {
				ArrayList<Node> toRemove = new ArrayList<>();
				for (Node n : graph) {
					String prot = n.getAttribute("protected");
					String spec = n.getAttribute("special");
					if (prot != null && prot.equals("1")) {
						continue;
					}
					if (spec != null && spec.equals("1")) {
						continue;
					}

					if (n.getDegree() <= i) {
						toRemove.add(n);

						//System.out.println("just peeled node " + n.toString());
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				for (Node n : toRemove) {
					graph.removeNode(n);
				}
			}
		}
	}

	public static void viewNeighbors(Graph graph) {

		for (Node n : graph) {
			String prot = n.getAttribute("protected");

			if (prot == null || !prot.equals("1")) {
				continue;
			}

			n.addAttribute("safe", "1");
			Iterator<Node> it = n.getNeighborNodeIterator();
			while (it.hasNext()) {
				Node u = it.next();
				u.addAttribute("safe", "1");
				u.setAttribute("ui.color", 1);
				// System.out.println("is safe: " + u.toString());
			}
		}

		ArrayList<Node> toRemove = new ArrayList<Node>();

		for (Node n : graph) {
			String safe = n.getAttribute("safe");
			if (safe == null || !safe.equals("1")) {
				toRemove.add(n);
			}
		}

		for (Node n : toRemove) {
			graph.removeNode(n);
			// System.out.println("removed: " + n.toString());
		}

	}

	public static void graphPeel(Graph graph, int k) {

		Queue<Node> q = new LinkedList<Node>();

		for (Node n : graph) {
			n.addAttribute("active", "true");

			n.addAttribute("counter", Integer.toString(n.getDegree()));
		}

		for (Node v : graph) {

			String active = v.getAttribute("active");
			int counter = Integer.parseInt(v.getAttribute("counter").toString());

			if (active.equals("true") && counter < k) {
				v.setAttribute("active", false);
				q.add(v);
				while (!q.isEmpty()) {
					Node w = q.remove();
					Iterator<Node> it = w.getBreadthFirstIterator();
					while (it.hasNext()) {
						Node u = it.next();
						int ucounter = Integer.parseInt(u.getAttribute("counter").toString());
						String uactive = u.getAttribute("active").toString();

						u.setAttribute("counter", ucounter - 1);
						if (uactive.equals("true") && ucounter < k) {
							u.setAttribute("active", false);
							System.out.println("no longer active: " + u.toString());
							q.add(u);
						}
					}
				}
			}
		}

		Graph g = new SingleGraph("asdf");
		for (Node n : graph) {
			if (n.getAttribute("active").equals(true)) {
				graph.removeNode(n);
				// System.out.println("just removed: " + n.toString());
			}
		}

	}

	static Comparator<Node> degreeComparator = new Comparator<Node>() {
		@Override
		public int compare(Node n1, Node n2) {
			int deg1 = Integer.parseInt(n1.getAttribute("deg").toString());
			int deg2 = Integer.parseInt(n2.getAttribute("deg").toString());
			return deg1 - deg2;
		}
	};

	public static void applyCores(Graph graph) {

		SortedMap<Node, Integer> degrees = new TreeMap<Node, Integer>(degreeComparator);

		for (Node n : graph) {
			n.addAttribute("deg", n.getDegree());
			int deg = Integer.parseInt(n.getAttribute("deg").toString());
			degrees.put(n, deg);
		}

		for (Node v : graph) {
			int degv = Integer.parseInt(v.getAttribute("deg").toString());

			v.setAttribute("core", degv);
			Iterator<Node> it = v.getBreadthFirstIterator();
			while (it.hasNext()) {
				Node u = it.next();
				int degu = Integer.parseInt(u.getAttribute("deg").toString());
				if (degu > degv) {
					u.setAttribute("deg", degu - 1);
				}
				System.out.println("degu: " + degu);
			}
		}

	}

}
