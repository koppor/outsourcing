package outsourcing.processtree;

import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

public class Comparator {
	private static Logger logger = Logger.getLogger(Comparator.class);

	private ProcessTree pt1, pt2;

	private HashMap<Node,Node> equalN2forN1;
	private HashMap<Node,Node> equalN1forN2;

	public static class MetricResult {
		/*
		 * email-discussion May 2011: parallel relation could counted twice as the paper does not explicitly state that parallel(n1,n2) is the same as parallel(n2,n1)
		 */

		// metric as discussed in the emails in May 2011, interpretation of the paper that PT_L restricts the process type to the EXISTING nodes
		// i.e., if there is a relation r(n1,n2), there is a correspondent node for n1, but none for n2, the relation is NOT counted
		public double M1 = 0.0;

		// metric as used in the paper: extra relations are also counted
		public double M2 = 0.0;

		// loop insensitive matching
		public double MI1 = 0.0;

		public double MI2 = 0.0;
	}

	private static class SimilarNode {
		private final Logger logger = Logger.getLogger(SimilarNode.class);

		Node realNode;

		public SimilarNode(Node realNode) {
			this.realNode = realNode;
		}

		@Override
		public int hashCode() {
			int res = realNode.getLabel().hashCode();
			//logger.debug(String.format("Hashcode of %s is %x", realNode.getDebugString(), res));
			return res;
		}

		public boolean equals(Object o) {
			if (!(o instanceof SimilarNode))
				return false;

			SimilarNode otherNode = (SimilarNode) o;

			//logger.debug(String.format("Comparing %s and %s", realNode, otherNode.realNode));

			return (realNode.getLabel().equals(otherNode.realNode.getLabel()));
		}
	}

	public Comparator(ProcessTree pt1, ProcessTree pt2) {
		this.pt1 = pt1;
		this.pt2 = pt2;

		buildHashMaps();
	}

	private void buildHashMaps() {
		NDC.push("buildHashMaps");
		equalN2forN1 = new HashMap<Node, Node>(pt1.actToNode.size());
		equalN1forN2 = new HashMap<Node, Node>(pt2.actToNode.size());

		// hash from a node in pt2 to itself -- needed because set.remove() does not return the element itself
		HashMap<SimilarNode,Node> pt2nodes =  new HashMap<SimilarNode,Node>(pt2.getAllBasicNonSilentActivies().size());
		for (Node n2: pt2.getAllBasicNonSilentActivies()) {
			SimilarNode sn2 = new SimilarNode(n2);
			pt2nodes.put(sn2,n2);
			//logger.debug(String.format("Put %s %s", sn2, n2.getDebugString()));
		}
		for (Node n1: pt1.getAllBasicNonSilentActivies()) {
			//logger.debug(String.format("Checking for %s", n1.getDebugString()));
			Node n2 = pt2nodes.get(new SimilarNode(n1));
			if (n2==null) {
				logger.debug(String.format("No matching node found for %s.", n1.getDebugString()));
			} else {
				logger.debug(String.format("equal node for %s is %s", n1, n2));
//				logger.debug(n1.getDebugString());
//				logger.debug(n2.getDebugString());

				equalN2forN1.put(n1,n2);

				// the relation is symmetric. Thus we can add the inverse to the other hashmap
				equalN1forN2.put(n2,n1);
			}
		}
		NDC.pop();
	}

	public boolean haveEqualBasicNonSilentActivities() {
		// equalN2forN1 and equalN2forN1 already built.
		// if there is an entry for each node of pt1, pt1 and pt2 are equal

		return ((pt1.getAllBasicNonSilentActivies().size() == equalN2forN1.size()) &&
				(pt2.getAllBasicNonSilentActivies().size() == equalN1forN2.size()));
	}

	public boolean haveEqualProcessType() {

		ProcessType ptype1 = pt1.getProcessType();
		ProcessType ptype2 = pt2.getProcessType();

		if (!haveEqualBasicNonSilentActivities())
			return false;

//	for (Node na: pt1.getAllBasicNonSilentActivies()) {
//		for (Node nb: pt1.getAllBasicNonSilentActivies()) {
//			if (na != nb) {
		for (int i = 0; i < pt1.basicNodes.length; i++) {
			Node na = pt1.basicNodes[i];
			for (int j = i + 1; j < pt1.basicNodes.length; j++) {
				Node nb = pt1.basicNodes[j];
				// for all pairs of nodes in pt1
				Operation op1 = ptype1.getRelation(na, nb);
				Operation op2 = ptype2.getRelation(equalN2forN1.get(na), equalN2forN1.get(nb));
				if (!op1.equals(op2)) {
					logger.info(String.format("Relations %s/%s %s/%s do not match", na, nb, equalN2forN1.get(na), equalN2forN1.get(nb)));
					return false;
				}
			}
		}

		return true;
	}

	public boolean haveSameMultiplicities() {
		return haveSameMultiplicities(pt1.getAllBasicNonSilentActivies());
	}

	private boolean haveSameMultiplicities(Set<Node> activitiesOfP1) {
		for (Node n1: activitiesOfP1) {
			Node n2 = equalN2forN1.get(n1);
			if (n1.getMult() != n2.getMult())
				return false;
		}

		return true;
	}

	public boolean operationsOfP2areSubsetOfOperationsOfP1() {
		for (Node n: pt2.getReceiveActivies()) {
			if (equalN1forN2.get(n)==null)
				// if there is no equal node in pt1 found, return false
				return false;
		}
		for (Node n: pt2.getReplyActivies()) {
			if (equalN1forN2.get(n)==null)
				// if there is no equal node in pt1 found, return false
				return false;
		}

		// in case of an empty "operation" set of P2, true is also valid
		return true;
	}

	public boolean invokationsOfP1areSubsetOfInvokationsOfP2() {
		for (Node n: pt1.getInvokeActivities()) {
			if (equalN2forN1.get(n)==null)
				return false;
		}
		return true;
	}

	public boolean commonActivitiesAgreeOnProcessTypesAndMultiplicity() {
		for (Node p1n1: equalN2forN1.keySet()) {
			// all keys of the hashmap are operations which are provided by both processes. Otherwise they would not be equal!
			Node p2n1 = equalN2forN1.get(p1n1);

			// agreeing on the multiplicity
			if (p1n1.getMult() != p2n1.getMult())
				return false;

			// check processType
			for (Node p1n2: equalN2forN1.keySet()) {
				if (p1n1.getDEBUGnumber() < p1n2.getDEBUGnumber()) {
					// compare two nodes N1 and N2 of same process only once
					// i.e. either (p1n1=N1 and p1n2=N2) or (p1n1=N2 and p1n2=N2) -- not both
					// N1 and N2 may not be equal -- relation would be null then
					Node p2n2 = equalN2forN1.get(p1n2);
					Operation op1 = pt1.getProcessType().getRelation(p1n1, p1n2);
					Operation op2 = pt2.getProcessType().getRelation(p2n1, p2n2);
					if (op1 != op2)
						return false;
				}
			}

		}
		return true;
	}

	private boolean isIgnoredOperation(Operation op) {
		return ((op == Operation.S1R) || (op == Operation.SAR)); // || (op == Operation.P1R) || (op == Operation.PAR));
	}

	/**
	 * Determines the number of parallel relations starting from n
	 *
	 * @param n The node where the parallel relation starts
	 * @param pt the process tree, where the node n belongs to
	 * @return the number of parallel relations starting from n. 0 if none are starting.
	 */
	private static int getNumberOfParallelRelations(Node n, ProcessTree pt) {
		int parallelRelations = 0;
		for (int j = 0; j < pt.basicNodes.length; j++) {
			Node n2 = pt.basicNodes[j];
			if (n2 != n) {
				Operation op = pt.getProcessType().getRelation(n, n2);
				if (op.isParallel()) {
					parallelRelations++;
				}
			}
		}
		return parallelRelations;
	}

	/**
	 * This method calculates both loop-sensitive and loop-insensitive matching
	 */
	public MetricResult getDegreeOfInexactMatching() {
		if (equalN2forN1.size()==0) {
			// P1 and P2 have no activities in common
			// return 0 as result
			return new MetricResult();
		}

		// not required as this method also calculates loop-insensitive matching
//		if (!haveSameMultiplicities(equalN2forN1.keySet()))
//			return new MetricResult();

		// go through all activities in pt1 which have a matching partner,
		// get the relation between them, get the relation for the other tree
		// if these two are equal: increase matchingRelationCount

		int exactMatchingRelations = 0;

		// not exact, but loop insensitive matches
		int loopInsenstiveMatchingRelations = 0;

		int totalRelations = 0;

		// extra: not included in the set of matching activities
		int extraRelations = 0;

		for (int i = 0; i < pt1.basicNodes.length; i++) {
			Node n11 = pt1.basicNodes[i];
			logger.debug(String.format("n1: %s", n11));
			Node n21 = equalN2forN1.get(n11);
			if (n21 == null) {
				// all relations starting from n11 are extra relations as there is no equal node in pt2
				// as a node has a relation to all other nodes, but not to itself, we use the total number of nodes and subtract 1
				// This is right for the sequence relation as either the sequence or the reverse relation is there
				// But the parallel relation is also there from the opposite node.
				// Therefore we have to count them, too
				int parallelRelations = getNumberOfParallelRelations(n11, pt1);
				logger.debug(String.format("No matching node for %s found. Adding %d extra relations plus %d extra parallel relations.", n11, pt1.basicNodes.length-1, parallelRelations));
				extraRelations += (pt1.basicNodes.length-1);
				extraRelations += parallelRelations;
			} else {
				// analyze all nodes n12 after n11 as partner
				// the node before has been treated in an iteration before
				for (int j = i + 1; j < pt1.basicNodes.length; j++) {
					Node n12 = pt1.basicNodes[j];
					logger.debug(String.format("n2: %s", n12));
					Node n22 = equalN2forN1.get(n12);
					if (n22 == null) {
						// no matching node for n12 found
						// this will be treated when hitting n22 in the outer loop
						// then, the branch (n21 == null) will be hit.
						logger.debug(String.format("No matching node for %s found. Will be treated later.", n12));
					} else {
						logger.debug(String.format("Handling %s==%s and %s==%s", n11, n21, n12, n22));
						Operation op1 = pt1.getProcessType().getRelation(n11, n12);
						Operation op2 = pt2.getProcessType().getRelation(n21, n22);

						// here, all pairs of nodes are visited. "All" especially includes the ones of pt2

						// if (n1,n2) is visited, (n2,n1) is NOT visited!
						//   this is because j loops from i+1 on

						totalRelations++;
						if (op1.isParallel()) {
							// parallel relations are symmetric, therefore, we have to count them twice
							logger.debug("Counting parallel relation twice.");
							totalRelations++;
						}

						if (op1.equals(op2)) {
							logger.debug(String.format(
									"Checking (%s/%s/%s) with (%s/%s/%s): match",
									n11, n12, op1, n21, n22, op2));
							// S1R and SAR also included
							// if two nodes (n11, n12) in pt1 are in reverse-sequence relation and
							// the equal two nodes (n21, n22) in pt2 are also in reverse-sequence relation,
							// then the inverse tuples are in the sequence relation.
							// The metric counts the sequence only and not the reverse sequence
							// Since we either hit the sequence or the reverse sequence, it is OK to regard only one of them
							exactMatchingRelations++;
							if (op1.isParallel()) {
								// parallel relations are symmetric, therefore, we have to count them twice
								logger.debug("Counting parallel relation twice.");
								exactMatchingRelations++;
							}
						} else {
							boolean loopInsenstiveMatch = false;
							// check for nearly match... equal operations only have to be checked above. Now, if there is only a difference in the multiplicity, then everything is allright
							switch (op1) {
							case P1:
								loopInsenstiveMatch = (op2==Operation.PA);
								break;
							case PA:
								loopInsenstiveMatch = (op2==Operation.P1);
								break;
							case S1:
								loopInsenstiveMatch = (op2==Operation.SA);
								break;
							case SA:
								loopInsenstiveMatch = (op2==Operation.S1);
								break;
							case S1R:
								loopInsenstiveMatch = (op2==Operation.SAR);
								break;
							case SAR:
								loopInsenstiveMatch = (op2==Operation.S1R);
								break;
							case X1:
								loopInsenstiveMatch = (op2==Operation.XA);
								break;
							case XA:
								loopInsenstiveMatch = (op2==Operation.X1);
								break;
							}
							if (loopInsenstiveMatch) {
								logger.debug(String.format(
										"Checking (%s/%s/%s) with (%s/%s/%s): loop insenstive match",
										n11, n12, op1, n21, n22, op2));
								loopInsenstiveMatchingRelations++;
								if (op1.isParallel()) {
									loopInsenstiveMatchingRelations++;
								}
							} else {
								// op2 is different from op1
								// we therefore have to count it into total relations
								totalRelations++;
								if (op2.isParallel()) {
									totalRelations++;
								}
								logger.debug(String
										.format("Checking (%s/%s/%s) with (%s/%s/%s): no match. Adding extra relation(s)",
												n11, n12, op1, n21, n22, op2));
							}
						}
					}
				}
			}
		}

		// process tree 2 may contain activities not contained in process tree 1. These have to be counted, too
		logger.debug("Checking extra nodes of pt2");
		for (int i = 0; i < pt2.basicNodes.length; i++) {
			Node n2 = pt2.basicNodes[i];
			logger.debug(String.format("n2: %s",n2));
			Node n1 = equalN1forN2.get(n2);
			if (n1 == null) {
				// see comments above for n21==null
				int parallelRelations = getNumberOfParallelRelations(n2, pt2);
				logger.debug(String.format("No matching node for %s found. Adding %d extra relations plus %d extra parallel relations.", n2, pt2.basicNodes.length-1, parallelRelations));
				extraRelations += (pt2.basicNodes.length-1);
				extraRelations += parallelRelations;
			}
		}

		MetricResult res = new MetricResult();
		res.M1 = ((double)exactMatchingRelations)/((double)totalRelations);
		res.M2 = ((double)exactMatchingRelations)/((double)totalRelations+extraRelations);
		res.MI1 = ((double)exactMatchingRelations+loopInsenstiveMatchingRelations)/((double)totalRelations);
		res.MI2 = ((double)exactMatchingRelations+loopInsenstiveMatchingRelations)/((double)totalRelations+extraRelations);
		logger.debug(String.format("Result M1: %d/%d = %f", exactMatchingRelations, totalRelations, res.M1));
		logger.info(String.format("Result M2: %d/%d = %f", exactMatchingRelations, totalRelations+extraRelations, res.M2));
		logger.debug(String.format("Result MI1: %d/%d = %f", exactMatchingRelations+loopInsenstiveMatchingRelations, totalRelations, res.MI1));
		logger.info(String.format("Result MI2: %d/%d = %f", exactMatchingRelations+loopInsenstiveMatchingRelations, totalRelations+extraRelations, res.MI2));
		return res;
	}

}
