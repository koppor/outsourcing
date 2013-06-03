package outsourcing.processtree;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.ui.internal.ide.misc.DisjointSet;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;

/**
 * Determination of least common ancestors
 *
 * Implementation of http://en.wikipedia.org/wiki/Tarjan%27s_off-line_least_common_ancestors_algorithm
 */
public class LCA<V,E> {
	//private static Logger logger = Logger.getLogger(LCA.class);

	private DisjointSet<V> s = new DisjointSet<V>();

	private V[][] lca;

	private HashMap<V,Integer> nodeToNumber;

	private HashMap<V,V> ancestor = new HashMap<V,V>();
	private HashSet<V> coloredBlack = new HashSet<V>();

	private DirectedGraph<V, E> graph;


	/**
	 * @param graph the graph to work on
	 * @param root the root node to start with
	 */
	// we have an unchecked conversion from Object[][] to V[][] as arrays of generics cannot be constructed
	// This warning is suppressed
	@SuppressWarnings("unchecked")
	public LCA(DirectedGraph<V,E> graph, V root) {
		this.graph = graph;

		lca = (V[][]) new Object[this.graph.vertexSet().size()][this.graph.vertexSet().size()];

		// assign numbers to nodes
		nodeToNumber = new HashMap<V,Integer>(this.graph.vertexSet().size());
		int num = 0;
		for (V v: this.graph.vertexSet()) {
			nodeToNumber.put(v, num);
			num++;
		}

		TarjanOLCA(root);
	}

	public V getLCA(V u, V v) {
		//logger.debug(String.format("Getting LCA from (%1s, %2s)", u, v));
		int uNum = nodeToNumber.get(u);
		int vNum = nodeToNumber.get(v);
		return (lca[uNum][vNum]);
	}

	private void TarjanOLCA(V u) {
		//logger.debug(String.format("Handling %s", u));
		s.makeSet(u);
		ancestor.put(u, u);
		for (V v: Graphs.successorListOf(graph, u)) {
			TarjanOLCA(v);
			s.union(u, v);
			V n = s.findSet(u);
			ancestor.put(n, u);
		}
		coloredBlack.add(u);
		for (V v: graph.vertexSet()) {
			if (coloredBlack.contains(v)) {
				// there is no Pair class in Java. Thus, we do it manually.
				//  we did not add any order in u,v. Therefore, we have to check for both u and v whether there is data available
				V lcaV =  ancestor.get((s.findSet(v)));
				//logger.debug(String.format("Tarjan's Least Common Ancestor of %s and %s is %s", u, v, lcaV));

				int uNum = nodeToNumber.get(u);
				int vNum = nodeToNumber.get(v);

				lca[uNum][vNum] = lcaV;
				lca[vNum][uNum] = lcaV;
			}
		}
	}
}
