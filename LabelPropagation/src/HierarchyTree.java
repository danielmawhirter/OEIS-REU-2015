import java.util.TreeMap;
import java.util.Set;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HierarchyTree {
  private TreeNode<String> root = null;
  private TreeMap<String, TreeNode<String>> lookup = null;
  
  public HierarchyTree(String file_path) {
    lookup = new TreeMap<>();
    root = buildTree(new File(file_path));
  }
  
  public HierarchyTree(File file) {
    lookup = new TreeMap<>();
    root = buildTree(file);
  }
  
  public Set<String> getLeaves(String id) {
    TreeNode<String> node = lookup.get(id);
    if(null == node) return null;
    else return node.getLeafObjects();
  }
  
  private TreeNode<String> buildTree(File file) {
    TreeNode<String> root = null;
    try (BufferedReader br = new BufferedReader(
      new FileReader(file))) {
      root = buildTreeRecursive(br);
    } catch (IOException e) {
      e.printStackTrace(System.out);
    }
    return root;
  }
  
  private TreeNode<String> buildTreeRecursive(BufferedReader br)
    throws IOException {
    String[] line = br.readLine().split(" ");
    TreeNode<String> node = new TreeNode<>(line[0]);
    this.lookup.put(line[0], node);
    for (int i = 0; i < Integer.parseInt(line[1]); i++) {
      node.addChild(buildTreeRecursive(br));
    }
    return node;
  }
  
  public void outputToFile(String file_path) {
    TreeNode.outputTree(this.root, file_path);
  }
  
}
