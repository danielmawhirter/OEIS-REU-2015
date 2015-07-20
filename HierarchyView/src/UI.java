import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

public class UI implements ViewerListener, TreeSelectionListener,
		TreeExpansionListener, TreeWillExpandListener {
	private AtomicBoolean activePump;
	private static AtomicBoolean instanciated = new AtomicBoolean(false);
	private Graph graph;
	private Viewer viewer;
	private View view;
	private ViewerPipe fromViewer;
	private JTree tree;
	public DefaultTreeModel dm;
	private DefaultMutableTreeNode root, selected;
	private JFrame graphFrame, treeFrame;
	private long press = 0;
	private static UI instance = null;

	public static void create(Graph graph) {
		if (instance == null) {
			instance = new UI(graph);
			instanciated.set(true);
			HierarchyView.expandNode(instance.root);
			HierarchyView.assignColors();
		}
	}

	private UI(Graph graph) {
		super();
		graphFrame = new JFrame();
		JPanel framePanel = new JPanel(new BorderLayout());
		this.graph = graph;

		// prepare graph viewer
		viewer = new Viewer(graph,
				Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
		viewer.enableAutoLayout();
		view = viewer.addDefaultView(false);
		fromViewer = viewer.newViewerPipe();
		fromViewer.addViewerListener(this);
		fromViewer.addSink(graph);

		// prepare JFrame for graph viewer
		graphFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		((Component) view).setPreferredSize(new Dimension(900, 800));
		framePanel.add((Component) view, BorderLayout.CENTER);

		// prepare tree viewer and its JFrame
		// treeFrame = new JFrame();
		// JPanel treePanel = new JPanel(new BorderLayout());
		root = HierarchyView.buildTree(HierarchyView.hierarchyFile);
		dm = new DefaultTreeModel(root);
		tree = new JTree(dm);
		tree.setRootVisible(false);
		// currentParent = root;
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.addTreeExpansionListener(this);
		tree.addTreeWillExpandListener(this);
		JScrollPane treeView = new JScrollPane(tree);
		treeView.setPreferredSize(new Dimension(250, 800));
		treeFrame = new JFrame();

		treeFrame.add(treeView/*, BorderLayout.WEST*/);
		treeFrame.pack();
		treeFrame.setVisible(true);

		graphFrame.add(framePanel);
		graphFrame.pack();
		graphFrame.setTitle("Hierarchical Graph");
		//graphFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		graphFrame.setVisible(true);
		activePump = new AtomicBoolean(false);
		// treePanel.add(treeView, BorderLayout.NORTH);
		// treeFrame.add(treePanel);
		// treeFrame.pack();
		// treeFrame.setVisible(true);
	}

	public static void pump() {
		boolean started = false;
		while (true) {
			if(instanciated.get()) {
				if(instance.activePump.get()) {
					started = true;
					instance.fromViewer.pump();
				} else if(started)
					break;
			}
		}
	}
	
	public static void startPump() {
		instance.activePump.set(true);
	}

	@Override
	public void buttonPushed(String id) {
		press = Calendar.getInstance().getTimeInMillis();
	}

	@Override
	public void buttonReleased(final String id) { // Main
		if(Calendar.getInstance().getTimeInMillis() - press > 150) return;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.out.println("Clicked on " + id);
				DefaultMutableTreeNode clickedNode = null;
				for (Enumeration<?> e = root.breadthFirstEnumeration(); e
						.hasMoreElements() && clickedNode == null;) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) e
							.nextElement();
					if (id.equals(node.getUserObject())) {
						clickedNode = node;
					}
				}
				if (clickedNode == null)
					System.err.println("Cannot find clicked node in tree");
				else {
					// currentParent = clickedNode;
					// HierarchyView.expandNode(clickedNode);
					System.out.println("Graph expanded on " + id);
					tree.expandPath(new TreePath(clickedNode.getPath()));
				}
			}
		});

	}

	@Override
	public void viewClosed(String arg0) {
		activePump.set(false);
	}

	// Selected Node in Tree View
	@Override
	public void valueChanged(final TreeSelectionEvent event) { // EDT
		if (selected != null) {
			String prev = (String) selected.getUserObject();
			Node p = graph.getNode(prev);
			if (p != null)
				p.setAttribute("ui.class", (String) null);
		}
		selected = (DefaultMutableTreeNode) event.getPath()
				.getLastPathComponent();
		String id = (String) selected.getUserObject();
		Node n = graph.getNode(id);
		if (n != null)
			n.setAttribute("ui.class", "selected");
	}

	/*
	 * public static void expandTree() { instance.tree.expandPath(new
	 * TreePath(instance.dm .getPathToRoot(instance.currentParent))); }
	 */

	@Override
	public void treeCollapsed(final TreeExpansionEvent evt) { //EDT
		// viewer.disableAutoLayout();
		TreePath path = evt.getPath();
		DefaultMutableTreeNode collapsed = (DefaultMutableTreeNode) path
				.getLastPathComponent();
		String id = (String) collapsed.getUserObject();
		HierarchyView.collapseCluster(collapsed);
		// TreePath path = evt.getPath();
		System.out.println("Tree Collapsed on " + id);
		// viewer.enableAutoLayout();
	}

	@Override
	public void treeExpanded(final TreeExpansionEvent evt) { // EDT
		// viewer.disableAutoLayout();
		TreePath path = evt.getPath();
		DefaultMutableTreeNode expanded = (DefaultMutableTreeNode) path
				.getLastPathComponent();
		String id = (String) expanded.getUserObject();
		HierarchyView.expandNode(expanded);
		System.out.println("Tree Expanded on " + id);
		// viewer.enableAutoLayout();

	}

	public static DefaultMutableTreeNode getChild(DefaultMutableTreeNode n,
			int i) {
		return (DefaultMutableTreeNode) instance.dm.getChild(n, i);
	}

	public static int getChildCount(DefaultMutableTreeNode n) {
		return instance.dm.getChildCount(n);
	}

	@Override
	public void treeWillCollapse(final TreeExpansionEvent evt)
			throws ExpandVetoException { // EDT
		TreePath path = evt.getPath();
		DefaultMutableTreeNode collapsing = (DefaultMutableTreeNode) path
				.getLastPathComponent();
		int childCount = dm.getChildCount(collapsing);
		for (int i = 0; i < childCount; i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) dm
					.getChild(collapsing, i);
			if (dm.getChildCount(child) > 0
					&& tree.isVisible(path.pathByAddingChild(child)
							.pathByAddingChild(dm.getChild(child, 0)))) {
				tree.collapsePath(path.pathByAddingChild(child));
			}
		}

	}

	@Override
	public void treeWillExpand(TreeExpansionEvent arg0)
			throws ExpandVetoException {
	}

	public static void resetView() {
		instance.view.getCamera().setAutoFitView(true);
	}

	public static void zoomTo(double minx, double miny, double maxx, double maxy) {
		instance.view.getCamera().setGraphViewport(minx, miny, maxx, maxy);
	}

}
