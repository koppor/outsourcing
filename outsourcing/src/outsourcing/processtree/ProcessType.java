package outsourcing.processtree;

import org.apache.log4j.Logger;

/**
 * This class does NOT implement toString() as it stores an index to each node only and not the node itself
 * toString() is emulated by ProcessTree.toString()
 */
public class ProcessType {
	private static Logger logger = Logger.getLogger(ProcessType.class);

	// a mapping from index x index to relation
	// alternative solution: build a graph as shown in Figures 8,9,10 in the paper
	private Operation[][] relation;

	public ProcessType(Node[] nodes) {
		relation = new Operation[nodes.length][nodes.length];

		// we have to null the values as we test for "null" to check whether a value has already been written to
		for (int i=0; i<nodes.length; i++) {
			for (int j=0; j<nodes.length; j++) {
				relation[i][j] = null;
			}
		}
	}

	/**
	 * Set relations between n1 and n2
	 * n1 op1 n2  -  n2 op2 n1
	 * @param n1
	 * @param n2
	 * @param op1
	 * @param op2
	 */
	public void setRelation(Node n1, Node n2, Operation op1, Operation op2) {
		//logger.debug(String.format("setRelation of %s(%d) and %s(%d): %s and %s", n1, n1.getIndex(), n2, n2.getIndex(), op1, op2));
		relation[n1.getIndex()][n2.getIndex()] = op1;
		relation[n2.getIndex()][n1.getIndex()] = op2;
	}

	/**
	 * Sets the (symmetric) relation op between n1 and n2
	 * n1 op n2  -  n2 op n1
	 *
	 * @param n1
	 * @param n2
	 * @param op
	 */
	public void setRelation(Node n1, Node n2, Operation op) {
		setRelation(n1, n2, op, op);
	}

	/**
	 * Gets the relation between n1 and n2
	 *
	 * Currently, the relation between basic nodes is set
	 *
	 * @param n1
	 * @param n2
	 * @return
	 */
	public Operation getRelation(Node n1, Node n2) {
		//logger.debug(String.format("getRelation() called with nodes %s and %s",n1,n2));
		Operation res = relation[n1.getIndex()][n2.getIndex()];
		//logger.debug(String.format("getRelation of %s(%d) and %s(%d): %s", n1, n1.getIndex(), n2, n2.getIndex(), res));
		return res;
	}

}
