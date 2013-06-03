package outsourcing.processtree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.Else;
import org.eclipse.bpel.model.ElseIf;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.OnAlarm;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.emf.common.util.EList;

import outsourcing.Helper;
import outsourcing.processtree.Comparator.MetricResult;

import de.uni_stuttgart.iaas.bpel.model.utilities.Utility;

/**
 *
 * @author koppor
 *
 * Two implementation alternatives:
 *   a) Build tree as described in paper out of BPEL process
 *      Pro:
 *        * Datastructure for upcoming algorithms is as described by the paper
 *        * We can do "modifications" (pick -> XOR) during the build
 *        * View transformations of the process tree will be simpler
 *      Con:
 *        * Origin of nodes in the tree might not be clear
 *   b) Render tree as view on the BPEL process
 *      Pro:
 *        * Routines of de.uni_stuttgart.iaas.bpel_d.algorithm.analysis.Utility shows that
 *          such a view is possible
 *      Con:
 *        * "modifications" of the BPEL process are hard and the code could be
 *          unmaintainable.
 *        * Alex' view transformations will be hard to implement (and the code will be hard to maintain)
 *
 * Option a) chosen
 *
 * ProcessTree = (A,N,child,type,rank,label)
 *  * A: n.getLabel(), n \in N (with n.getType == BASIC)
 *  * N: graph - allNodes
 *  * child: graph
 *  * type: n.getType()
 *  * rank: n.getRank()
 *  * label: n.getLabel()
 *  The <l relation is constructed from flowGraph
 */
public class ProcessTree  {
	private static final Logger logger = Logger.getLogger(ProcessTree.class);

	private SimpleDirectedGraph<Node, DefaultEdge> graph;
	private Node root;

	// stores the graph spanned by BPEL's links (<link>, <source>, <target>, ...)
	private SimpleDirectedGraph<Node, DefaultEdge> flowGraph;

	// mapping from activity to node - used for building flowGraph
	public HashMap<Activity,Node> actToNode = new HashMap<Activity, Node>();

	private HashSet<Node> invokes = new HashSet<Node>();  // "In" in the paper
	private HashSet<Node> receives = new HashSet<Node>(); // "Rc" in the paper
	private HashSet<Node> replies = new HashSet<Node>();  // "Rp" in the paper
	private HashSet<Node> allBasicNonSilentActivies = new HashSet<Node>(); // In cup Rc cup Rp in the paper

	public final static int RANK_DEFAULT = 0;
	public final static int RANK_FIRSTCHILD = 1;

	private ProcessType processType = null;

	// a mapping from index to Node
	// accessed by Comparator
	public Node[] basicNodes;

	private final static String newline = System.getProperty("line.separator");

	private HashMap<ProcessTree,Comparator> comparatorCache = new HashMap<ProcessTree,Comparator>();

	private BPELResource BPELresource;

	public ProcessTree(BPELResource r) {
		graph = new SimpleDirectedGraph<Node,DefaultEdge>(DefaultEdge.class);
		this.BPELresource = r;
		root = new Node(r.getProcess(), Type.OTHER, Mult.ONE, RANK_DEFAULT, 0);
		graph.addVertex(root);

		flowGraph = new SimpleDirectedGraph<Node,DefaultEdge>(DefaultEdge.class);

		// build process tree
		handleActivity(r.getProcess().getActivity(), root, Mult.ONE, RANK_DEFAULT);
		allBasicNonSilentActivies.addAll(invokes);
		allBasicNonSilentActivies.addAll(receives);
		allBasicNonSilentActivies.addAll(replies);
	}

	/**
	 * BPELExtensibleElement has to be used as parameter type for act as we are processing onMessage as node
	 *
	 * Updates global variables invokes, receives, and replies
	 */
	private void handleActivity(BPELExtensibleElement act, Node parent, Mult mult, int rank) {
		NDC.push("handleActivity");
		logger.trace(act.getClass().getName());

		int level = parent.getLevel()+1;

		Node n = null;
		if (act instanceof Invoke) {
			n = new Node((Invoke) act, mult, rank, level);
			graph.addVertex(n);
			graph.addEdge(parent, n);
			invokes.add(n);
			handleLinks((Activity) act, n);
		} else if (act instanceof Receive) {
			n = new Node((Receive) act, mult, rank, level);
			graph.addVertex(n);
			graph.addEdge(parent, n);
			receives.add(n);
			handleLinks((Activity) act, n);
		} else if (act instanceof Reply) {
			n = new Node((Reply) act, mult, rank, level);
			graph.addVertex(n);
			graph.addEdge(parent, n);
			replies.add(n);
			handleLinks((Activity) act, n);
		} else if (act instanceof Pick) {
			// NOTE: the current code treats a pick EITHER as IXOR or EXOR. A mixture is not possible (and also not foreseen at the EDOC paper)

			boolean isEXOR = (Helper.getStatus(act) == Status.EXOR);

			if (isEXOR) {
				n = new Node(act, Type.EXOR, mult, rank, level);
			} else {
				n = new Node(act, Type.IXOR, mult, rank, level);
			}
			graph.addVertex(n);
			graph.addEdge(parent, n);
			handleLinks((Activity) act, n);

			Pick pick = (Pick) act;

			//onMessage
			EList<OnMessage> onMessage = pick.getMessages();
			Iterator<OnMessage> itMessage = onMessage.iterator();
			while (itMessage.hasNext()) {
				// internal decision: each branch is translated to a sequence consisting of the receive and the translation of the activity nested in the onMesasge branch
				// external decision: the receiving message is dropped as this choice is dedicated to the partner

				if (isEXOR) {
					// we "just" connect the activity of the onMessage to the pick (n) and drop the onMessage itself
					OnMessage om = itMessage.next();
					handleActivity(om.getActivity(), n, mult, rank);
				} else {
					// create the sequence as described in EG2007 page 4
					Node seq = new Node(null, Type.SEQ, mult, RANK_DEFAULT, level+1);
					graph.addVertex(seq);
					graph.addEdge(n, seq);

					// the nesting in the sequence is done in the branch for onMessage in "handleActivity"
					handleActivity(itMessage.next(), seq, mult, RANK_FIRSTCHILD);
				}
			}

			//onAlarm
			EList<OnAlarm> onAlarm = pick.getAlarm();
			Iterator<OnAlarm> itAlarm = onAlarm.iterator();
			while (itAlarm.hasNext()) {
				// each branch is translated to a sequence consisting of the receive and the translation of the activity nested in the onMesasge branch

				Node seq = new Node(null, Type.SEQ, mult, RANK_DEFAULT, level+1);
				// create the sequence
				graph.addVertex(seq);
				graph.addEdge(n, seq);

				// the nesting in the sequence is done in the branch for onMessage in "handleActivity"
				handleActivity(itAlarm.next(), seq, mult, RANK_FIRSTCHILD);
			}
		} else if (act instanceof OnMessage) {
			OnMessage om = (OnMessage) act;
			n = new Node(om, mult, RANK_FIRSTCHILD, level);
			graph.addVertex(n);
			graph.addEdge(parent, n);
			receives.add(n);
			handleActivity(om.getActivity(), parent, mult, RANK_FIRSTCHILD+1);
		} else if (act instanceof OnAlarm) {
			OnAlarm oa = (OnAlarm) act;
			// onAlarm is an internal activity and can be ommitted,
			// just connect the child directly to the parent pick-XOR-node
			// obsolete: n = new Node(Type.OTHER, mult, RANK_FIRSTCHILD);
			handleActivity(oa.getActivity(), parent, mult, RANK_FIRSTCHILD);
		} else if (act instanceof Sequence) {
			n = new Node(act, Type.SEQ, mult, rank, level);
			graph.addVertex(n);
			graph.addEdge(parent, n);
			handleLinks((Activity) act, n);

			Sequence seq = (Sequence) act;
			EList<Activity> children = seq.getActivities();

			int cRank = RANK_FIRSTCHILD;
			for (Activity a: children) {
				handleActivity(a, n, mult, cRank);
				cRank++;
			}
		} else if (act instanceof Flow) {
			n = new Node(act, Type.AND, mult, rank, level);
			graph.addVertex(n);
			graph.addEdge(parent, n);
			handleLinks((Activity) act, n);

			Flow f = (Flow) act;

			int cRank = RANK_FIRSTCHILD;
			for (Activity a: f.getActivities()) {
				handleActivity(a, n, mult, cRank);
				cRank++;
			}
		} else if ((act instanceof While) || (act instanceof RepeatUntil) || (act instanceof ForEach)) {
			n = new Node(act, Type.OTHER, mult, rank, level);
			graph.addVertex(n);
			graph.addEdge(parent, n);
			handleLinks((Activity) act, n);

			Activity child;
			if (act instanceof While)
				child = ((While)act).getActivity();
			else if (act instanceof RepeatUntil)
				child = ((RepeatUntil)act).getActivity();
			else if (act instanceof ForEach)
				child = ((ForEach)act).getActivity();
			else {
				child = null;
				logger.fatal("This branch should never be reached.");
			}
			handleActivity(child, n, Mult.ARBITRARY, RANK_DEFAULT);
		} else if (act instanceof If) {
			// depending on the if being an EXOR or an IXOR, create the appropriate node
			boolean isEXOR = (Helper.getStatus(act) == Status.EXOR);
			if (isEXOR) {
				n = new Node(act, Type.EXOR, mult, rank, level);
			} else {
				n = new Node(act, Type.IXOR, mult, rank, level);
			}

			graph.addVertex(n);
			graph.addEdge(parent, n);
			handleLinks((Activity) act, n);

			If aIf = (If) act;
			handleActivity(aIf.getActivity(), n, mult, RANK_FIRSTCHILD);
			int cRank = RANK_FIRSTCHILD+1;
			EList<ElseIf> elseIf = aIf.getElseIf();
			if (elseIf!=null) {
				for (ElseIf elIf: elseIf) {
					handleActivity(elIf.getActivity(), n, mult, cRank);
					cRank++;
				}
			}
			Else el = aIf.getElse();
			if (el != null) {
				handleActivity(el.getActivity(), n, mult, cRank);
			}
		} else if (act instanceof Scope) {
			// EH, TH, FH, CH not supported
			Scope s = (Scope) act;

			//handleLinks((Activity) act, n);
			// connect child directly to parent as scope does not add any new meaning
			handleActivity(s.getActivity(), parent, mult, rank);
		} else {
			logger.error(String.format("unhandled class %1s in handleActivity", act.getClass()));
		}

		NDC.pop();
	}

	/**
	 *
	 * @param act
	 * @param n Node to treat - has a reference to the activity, where we get the links from
	 */
	private void handleLinks(Activity act, Node n) {
		// it cannot be assured that the sources of the incoming links are already converted to nodes
		// therefore, we have to check both incoming and outgoing links
		EList<Source> sourceLinks = Utility.getSourceLinks(act);
		EList<Target> targetLinks = Utility.getTargetLinks(act);
		if (sourceLinks.isEmpty() && targetLinks.isEmpty()) {
			return;
		}

		actToNode.put(act, n);
		flowGraph.addVertex(n);

		for (Target t: targetLinks) {
			Activity source = t.getLink().getSources().get(0).getActivity();
			Node sourceNode = actToNode.get(source);
			if (sourceNode != null) {
				// if sourceNode is in the hashMap, it has already been added to the flowGraph
				// Thus, we just connect it to n
				flowGraph.addEdge(sourceNode, n);
			}
		}
		for (Source s: sourceLinks) {
			Activity target = s.getLink().getTargets().get(0).getActivity();
			Node targetNode = actToNode.get(target);
			if (targetNode != null) {
				flowGraph.addEdge(n, targetNode);
			}
		}
	}

	/**
	 * NOT POSSIBLE:
	 *   Call to this method required when nodes are removed or added
	 *   --> the index points to a NEW node. Thus, the calculated index (@determineProcessType) gets wrong!!
	 *
	 * This is a sort of hack... The other possibility is to call "determineProcessType()".
	 * But the type itself does not change as only communication activities are added or removed
	 */
	private void rebuildNodeIndex() {
		int size = allBasicNonSilentActivies.size();
		basicNodes = new Node[size];

		// set index for each node for processType matrix
		//   only basic activities are needed
		//Set<Node> allV = this.graph.vertexSet();
		int i = 0;
		for (Node v: allBasicNonSilentActivies) {
			v.setIndex(i);
			basicNodes[i] = v;
			i++;
		}
	}

	private Set<Node> getDescendants(Node n) {
		HashSet<Node> res = new HashSet<Node>();
		for (DefaultEdge e : graph.outgoingEdgesOf(n)) {
			Node t = graph.getEdgeTarget(e);
			res.add(t);
			Set<Node> r = getDescendants(t);
			res.addAll(r);
		}
		return res;
	}

	/**
	 * Determines the sequential successors of the given node
	 * the given lca stops the recursion.
	 */
	private Set<Node> determineSequentialSuccessors(Node n, Node lca) {
		HashSet<Node> res = new HashSet<Node>();

		// all descendants are successors
		res.addAll(getDescendants(n));

		Node curN = n;
		Node p = getParent(n);
		while (p != lca) {
			if (p.getType().equals(Type.SEQ)) {
				int rankCurN = curN.getRank();
				Set<DefaultEdge> edges = graph.outgoingEdgesOf(p);
				for (DefaultEdge e: edges) {
					Node seqChild = graph.getEdgeTarget(e);
					if (seqChild.getRank() > rankCurN) {
						// child is a child executed AFTER the child where the "recursion" came from
						res.addAll(getDescendants(seqChild));
						res.add(seqChild);
					}
				}
			} else {
				// just climb up
				// also at if -- the other branches of the if are irrelevant
			}

			curN = p;
			p = getParent(p);
		}

		return res;
	}

	/**
	 * Determines the sequential predecessors of the given node
	 * the given lca stops the recursion.
	 */
	private Set<Node> determineSequentialPredecessors(Node n, Node lca) {
		HashSet<Node> res = new HashSet<Node>();

		Node curN = n;
		Node p = getParent(n);
		while (p != lca) {
			if (p.getType().equals(Type.SEQ)) {
				int rankCurN = curN.getRank();
				Set<DefaultEdge> edges = graph.outgoingEdgesOf(p);
				for (DefaultEdge e: edges) {
					Node seqChild = graph.getEdgeTarget(e);
					if (seqChild.getRank() < rankCurN) {
						// child is a child executed BEFORE the child where the "recursion" came from
						res.addAll(getDescendants(seqChild));
						res.add(seqChild);
					}
				}
			} else {
				// just climb up
				// also at if -- the other branches of the if are irrelevant
			}

			curN = p;
			p = getParent(p);
		}

		return res;
	}



	/**
	 * Determine the processType of this processTree
	 * This method also builds data structures to enable the Comparator to compare two trees
	 *
	 * We have two possibilities to call this method
	 *   a) at the constructor
	 *      The projection is then done AFTER the determination of the processtype
	 *      Here the indexes do not match and some hacks have to be made
	 *   b) after a projection (chosen alternative)
	 *      The projection MUST NOT remove a WHILE loop (and other nodes required to determine the process type)
	 *      Normally, the WHILE loop would be removed as it has one parent and one child. (Alex Algol #3)
	 */
	public void determineProcesType() {
		//Dominators<Node,DefaultEdge> dom = new Dominators<Node, DefaultEdge>(graph, root);
		//we don't need the dominators as we work on a *tree* and not on a flowgraph.
		//  the control links are handled differently

		LCA<Node,DefaultEdge> lcaData = new LCA<Node,DefaultEdge>(this.graph, this.root);

		rebuildNodeIndex();

		int maxI = basicNodes.length-1;

		processType = new ProcessType(basicNodes);

		// in case there are links between activities,
		// the links transitively form a sequential relation
		if (flowGraph.edgeSet().size() > 0) {
			Set<DefaultEdge> edgeSet = flowGraph.edgeSet();
			for (DefaultEdge edge : edgeSet) {
				Node source = flowGraph.getEdgeSource(edge);
				Node target = flowGraph.getEdgeTarget(edge);
				if (processType.getRelation(source, target) == null) {
					// the processType has not been set by another run of this loop yet

					Node lca = lcaData.getLCA(source, target);

					Set<Node> predsOfSource = determineSequentialPredecessors(source, lca);
					Set<Node> succsOfTarget = determineSequentialSuccessors(target, lca);

					// source and target have to get a relation assigned, too
					predsOfSource.add(source);
					succsOfTarget.add(target);

					for (Node n1: predsOfSource) {
						for (Node n2: succsOfTarget) {
							Node lcaOfN1N2 = lcaData.getLCA(n1, n2);
							switch (lcaOfN1N2.getMult()) {
							case ONE:
								processType.setRelation(n1, n2, Operation.S1, Operation.S1R);
								break;
							case ARBITRARY:
								processType.setRelation(n1, n2, Operation.SA, Operation.SAR);
								break;
							}

						}
					}
				}
			}
		}

		// the paper defines the process type on labels(n)
		// labels(n) is defined on basic activities only
		for (int i=0; i<=maxI; i++) {
			Node n1 = basicNodes[i];
			for (int j=i+1; j<=maxI; j++) {
				// visit each pair of basic nodes
				// for each pair of n1 and n2 we have to calculate the
				// operation

				Node n2 = basicNodes[j];

				logger.debug(String.format("Handling %s and %s", n1.toString(), n2.toString()));

				Node lca = lcaData.getLCA(n1, n2);
				Type type = lca.getType();
				if (type.equals(Type.EXOR) || type.equals(Type.IXOR)) {
					// even if a control link induced a sequential relation,
					// this relation is changed to an exclusive relation
					if (processType.getRelation(n1, n2) != null) {
						// this should happen only at illformed processes
						logger.debug(String.format("%s/%s: overwriting existing relation by exclusive relation", n1.toString(), n2.toString()));
					}

					switch (lca.getMult()) {
					case ONE:
						processType.setRelation(n1, n2, Operation.X1);
						break;
					case ARBITRARY:
						processType.setRelation(n1, n2, Operation.XA);
						break;
					}
				} else if (processType.getRelation(n1, n2) == null) {
					if (type.equals(Type.AND)) {
						// no path from n1 to n2 or vice versa
						switch (lca.getMult()) {
						case ONE:
							processType.setRelation(n1, n2, Operation.P1);
							break;
						case ARBITRARY:
							processType.setRelation(n1, n2, Operation.PA);
							break;
						}
					} else if (type.equals(Type.SEQ)) {
						// go up the hierarchy until the children of the sequence
						int targetLevel = lca.getLevel() + 1;
						Node seqN1 = n1;
						Node seqN2 = n2;
						while (seqN1.getLevel() > targetLevel)
							seqN1 = getParent(seqN1);
						while (seqN2.getLevel() > targetLevel)
							seqN2 = getParent(seqN2);
						boolean n1beforen2 = (seqN1.getRank() < seqN2.getRank());

						switch (lca.getMult()) {
						case ONE:
							if (n1beforen2) {
								processType.setRelation(n1, n2, Operation.S1, Operation.S1R);
							} else {
								processType.setRelation(n1, n2, Operation.S1R, Operation.S1);
							}
							break;
						case ARBITRARY:
							if (n1beforen2) {
								processType.setRelation(n1, n2, Operation.SA, Operation.SAR);
							} else {
								processType.setRelation(n1, n2, Operation.SAR, Operation.SA);
							}
							break;
						}
					} else {
						logger.error("unexpected case BASIC/OTHER");
					}
				} else {
					logger.debug(String.format("Relation for nodes %s/%s already set", n1.toString(), n2.toString()));
				}
			}
		}
	}

	private Node getParent(Node n) {
		Set<DefaultEdge> inEdges = graph.incomingEdgesOf(n);
		switch (inEdges.size()) {
		case 0:
			return null;
		default:
			logger.error(String.format("Node %1s has more than one incoming edge", n.toString()));
		case 1:
			return graph.getEdgeSource(inEdges.iterator().next());
		}
	}


	public String toString() {
		String res = "";
        DepthFirstIterator<Node,DefaultEdge> iter = new DepthFirstIterator<Node, DefaultEdge>(graph);
        while (iter.hasNext()) {
            Node node = iter.next();
            res = res.concat("== Node ");
            res = res.concat(node.toString());
            res = res.concat("==");
            res = res.concat(newline);
            res = res.concat(node.getDebugString());
            res = res.concat(newline);
            res = res.concat(graph.edgesOf(node).toString());
            res = res.concat(newline);
            res = res.concat(newline);
        }

        if (this.processType == null) {
        	res = res.concat("ProcessType is null");
        	res = res.concat(newline);
		} else {
			res = res.concat("ProcessType");
			res = res.concat(newline);
			// processType.toString() cannot be called as processType only has indexes of the ns
			//   and generating N + index in processType feels dirty
			// res = res.concat(processType.toString());
			SortedSet<Node> allBasicNonSilentActivitiesSorted = new TreeSet<Node>(new java.util.Comparator<Node>() {
				@Override
				public int compare(Node o1, Node o2) {
					return o1.getDEBUGnumber() - o2.getDEBUGnumber();
				}
			});
			allBasicNonSilentActivitiesSorted.addAll(this.allBasicNonSilentActivies);

			for (Node n1 : allBasicNonSilentActivitiesSorted) {
				for (Node n2 : allBasicNonSilentActivitiesSorted) {
					if (n1 != n2) {
						res = res.concat(n1.toString());
						res = res.concat(" | ");
						res = res.concat(n2.toString());
						res = res.concat(" | ");
						Operation relation = processType.getRelation(n1, n2);
						if (relation == null) {
							res = res.concat("null");
						} else {
							res = res.concat(relation.toString());
						}
						res = res.concat(newline);
					}
				}
			}
        }
		res = res.concat(newline);

        return res;
	}

	/**
	 * @return A comparator comparing this processTree to the processTree pt2
	 */
	private Comparator getComparator(ProcessTree pt2) {
		Comparator comp = comparatorCache.get(pt2);
		if (comp == null) {
			comp = new Comparator(this, pt2);
			comparatorCache.put(pt2, comp);
		}
		return comp;
	}

	public boolean matchesExactly(ProcessTree pt2) {
		NDC.push("matchesExactly");
		Comparator comp = getComparator(pt2);

		if (!comp.haveEqualBasicNonSilentActivities()) {
			logger.debug("Different basic non-silent activities.");
			NDC.pop();
			return false;
		} else {
			logger.debug("Equal basic non-silent activities.");
		}

		if (!comp.haveEqualProcessType()) {
			logger.debug("Different process type.");
			NDC.pop();
			return false;
		} else {
			logger.debug("Same process type.");
		}

		if (!comp.haveSameMultiplicities()) {
			logger.debug("Different multiplicities.");
			NDC.pop();
			return false;
		} else {
			logger.debug("Same multiplicities.");
		}

		logger.debug("match");
		NDC.pop();
		return true;
	}

	public boolean isPluginForProcessTree(ProcessTree pt2) {
		NDC.push("isPluginForProcessTree");

		Comparator comp = getComparator(pt2);

		if (!comp.operationsOfP2areSubsetOfOperationsOfP1()) {
			logger.debug("Provided operations of P2 are not a subset of the provided operations of P1.");
			NDC.pop();
			return false;
		} else {
			logger.debug("Operations of P2 are a subset of the operations of P1.");
		}

		if (!comp.invokationsOfP1areSubsetOfInvokationsOfP2()) {
			logger.debug("Invoked operations of P1 are not a subset of the invoked operations of P2.");
			NDC.pop();
			return false;
		} else {
			logger.debug("P1 does a subset of invokations.");
		}

		if (!comp.commonActivitiesAgreeOnProcessTypesAndMultiplicity()) {
			logger.debug("P1 and P2 do not agree on types and multiplicities.");
			NDC.pop();
			return false;
		} else {
			logger.debug("P1 and P2 agree on types and multiplicities.");
		}

		NDC.pop();
		return true;
	}

	public MetricResult getDegreeOfInexactMatching(ProcessTree pt2) {
		NDC.push("getDegreeOfInexactMatching");
		Comparator comp = getComparator(pt2);
		MetricResult res = comp.getDegreeOfInexactMatching();
		NDC.pop();
		return res;
	}

	public Set<Node> getAllBasicNonSilentActivies() {
		return this.allBasicNonSilentActivies;
	}

	public ProcessType getProcessType() {
		return this.processType;
	}

	public Set<Node> getReceiveActivies() {
		return receives;
	}

	public Set<Node> getReplyActivies() {
		return replies;
	}

	public Set<Node> getInvokeActivities() {
		return invokes;
	}


	/** Methods for accessing the tree itself
	 *  Used for the aggregation stuff to MODIFY the tree
	 **/

	public SimpleDirectedGraph<Node, DefaultEdge> getTree() {
		return graph;
	}

	public Node getRoot() {
		return root;
	}


	public BPELResource getBPELresource() {
		return this.BPELresource;
	}

	public void removeNode(Node n) {
		// remove nodes from the tree
		// also removes edges
		this.getTree().removeVertex(n);

		// remove it from graph representing the control links
		this.flowGraph.removeVertex(n);

		// remove node from internal data structures
		if (n.getType() == Type.BASIC) {
			allBasicNonSilentActivies.remove(n);
			invokes.remove(n);
			receives.remove(n);
			replies.remove(n);
		}
	}

	/**
	 * Removes all given nodes from the tree and updates the data structures accordingly
	 * (tree, flowGraph, sets -- ProcessType is NOT updated)
	 *
	 * control links of the respective BPEL activity are NOT removed here
	 * BPEL activity itself is also NOT removed
	 */
	public void removeAllNodes(Set<Node> nodesToRemove) {
		for (Node n: nodesToRemove) {
			removeNode(n);
		}
	}

	public void addAsInvoke(Node n) {
		this.graph.addVertex(n);
		this.invokes.add(n);
		this.allBasicNonSilentActivies.add(n);
	}

	public void addAsReceive(Node n) {
		this.graph.addVertex(n);
		this.receives.add(n);
		this.allBasicNonSilentActivies.add(n);
	}

	public void addAsReply(Node n) {
		this.graph.addVertex(n);
		this.replies.add(n);
		this.allBasicNonSilentActivies.add(n);
	}

}
