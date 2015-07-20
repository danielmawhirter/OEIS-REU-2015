package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.graphstream.graph.Graph;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

public class Clicks implements ViewerListener {
	public static AtomicBoolean instanciated = new AtomicBoolean(false);
	public AtomicBoolean activePump;
	private Graph graph;
	private ViewerPipe fromViewer;
	private static Clicks instance = null;

	public Clicks(Graph graph) {
		// We do as usual to display a graph. This
		// connect the graph outputs to the viewer.
		// The viewer is a sink of the graph.
		this.graph = graph;
		Viewer viewer = new Viewer(graph,
				Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
		View view = viewer.addDefaultView(false);
		
		JFrame graphFrame = new JFrame();
		JPanel framePanel = new JPanel(new BorderLayout());
		graphFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		((Component) view).setPreferredSize(new Dimension(900, 800));
		framePanel.add((Component) view, BorderLayout.CENTER);
		graphFrame.add(framePanel);

		// The default action when closing the view is to quit
		// the program.
		viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

		// We connect back the viewer to the graph,
		// the graph becomes a sink for the viewer.
		// We also install us as a viewer listener to
		// intercept the graphic events.
		fromViewer = viewer.newViewerPipe();
		fromViewer.addViewerListener(this);
		fromViewer.addSink(graph);

		// Then we need a loop to wait for events.
		// In this loop we will need to call the
		// pump() method to copy back events that have
		// already occured in the viewer thread inside
		// our thread.
		activePump = new AtomicBoolean(false);
		instance = this;
		graphFrame.pack();
		graphFrame.setVisible(true);
		viewer.enableAutoLayout();
	}
	
	public static void pump() {
		boolean started = false;
		while (true) {
			try {
				if(instanciated.get()) {
					if(instance.activePump.get()) {
						started = true;
						instance.fromViewer.pump();
					} else if(started)
						break;
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void startPump() {
		instance.activePump.set(true);
	}

	@Override
	public void viewClosed(String id) {
		activePump.set(false);
	}

	@Override
	public void buttonPushed(String id) {
		//org.graphstream.graph.Node n = graph.getNode(id);
		//System.out.println("Button pushed on node " + id);

	}

	@Override
	public void buttonReleased(final String id) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				org.graphstream.graph.Node n = graph.getNode(id);
				org.graphstream.graph.Edge e = graph.getEdge(id);
				//System.out.println("Button released on node " + id);
				if (n != null) {
					n.setAttribute("ui.label", id);
					n.setAttribute("ui.style", "text-background-mode: plain;");
				}
				if (e != null) {
					e.setAttribute("ui.label", id);
					e.setAttribute("ui.style", "text-background-mode: plain;");
				}
				
			}
			
		});
		
	}

}