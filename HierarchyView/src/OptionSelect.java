import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import main.Clicks;
import main.Main;

public class OptionSelect implements ActionListener {
	private JFrame frame;
	private JComboBox<Object> box;
	private JCheckBox labeling;
	private JTextField one, two;
	private Map<String, Integer> nameToType;
	private Map<String, String> nameToArg2;
	private Map<String, String> nameToArg3;

	public OptionSelect() {
		nameToArg2 = new HashMap<>();
		nameToArg3 = new HashMap<>();
		nameToType = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(
				HierarchyView.path + "_options.txt"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(" ");
				nameToType.put(split[0], Integer.parseInt(split[1]));
				nameToArg2.put(split[0], split[2]);
				nameToArg3.put(split[0], split[3]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		frame = new JFrame();
		JPanel panel = new JPanel(new GridLayout(5, 1));
		box = new JComboBox<>(nameToArg3.keySet().toArray());
		JButton button = new JButton("Select");
		button.addActionListener(this);
		labeling = new JCheckBox("Label?");
		one = new JTextField(10);
		two = new JTextField(10);
		panel.add(box);
		panel.add(button);
		panel.add(labeling);
		panel.add(one);
		panel.add(two);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String item = (String) box.getSelectedItem();
		switch(HierarchyView.type = this.nameToType.get(item)) {
		case 0:
			HierarchyView.edgeFile = this.nameToArg2.get(item);
			HierarchyView.hierarchyFile = this.nameToArg3.get(item);
			HierarchyView.label = labeling.isSelected();
			frame.setVisible(false);
			HierarchyView.begin();
			UI.startPump();
			break;
		case 1:
			if(one.getText().length() > 0) Main.from = one.getText();
			else Main.from = this.nameToArg2.get(item);
			if(two.getText().length() > 0) Main.to = two.getText();
			else Main.to = this.nameToArg3.get(item);
			frame.setVisible(false);
			Main.notmain();
			break;
		}
		synchronized(HierarchyView.to_pump){
			HierarchyView.to_pump.notify();
		}
		
	}
}
