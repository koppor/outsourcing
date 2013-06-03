package outsourcing.processtree.projections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.PartnerActivity;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.resource.LineCapturingDOMParser;
import org.eclipse.emf.ecore.EObject;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import outsourcing.MyBPELReader;
import outsourcing.processtree.Node;
import outsourcing.processtree.ProcessTree;

public class Projections {
	private static Logger logger = Logger.getLogger(Projections.class);

	private ProcessTree pt;
	private ActionList act;

	private MyBPELReader r;

	private DOMParser db;

	/**
	 * The implementation of the DFS uses recursive method calling
	 * For large PSTs this could break. Let's see, when this is the case
	 *
	 * @param pt - The processTree to perform the projections on
	 * @param act - The projection actions to perform
	 */
	public Projections(ProcessTree pt, ActionList act) {
		this.pt = pt;
		this.act = act;

		/*
		 * we have to convert the XML to a BPEL activity
		 * possibilities:
		 *  * by hand
		 *  * Using BPELUtils
		 *  * Using BPELReader
		 * we opt for the latter as this is a complete parser
		 * BPELReader is not meant to do that, but can nevertheless be used for it...
		 * pass2() method has to be made public
		 */
		r = new MyBPELReader();
		// pseudo initialization to enable working of pass2()
		r.read(pt.getBPELresource(), (Document) null);
}

	/**
	 * @note Implementation follows Alex' description
	 */
	public void perform() {
		// traverse tree
		// for each node: check whether there is a projection

		// I cannot use the DFS search provided by JGraphT
		// the issue is that the iterator does NOT support the remove() operation
		// the current implementation of JGraphT uses a kind of BFS with marking of the
		// nodes (WHITE,GRAY,BLACK) and deciding on top of that the DFS order
		// Although JGraphT's implementation supports corss-component iteration, we don't need it.

		Node root = pt.getRoot();
		visit(root, true);
	}

	/**
	 *
	 * @param n
	 * @param isRoot - quick hack to increase performance -- indicates whether n is the root note - only for NON-root nodes, the node may be removed if it has only one outgoing edge
	 */
	private void visit(Node n, boolean isRoot) {
		//logger.debug(String.format("Visiting %s", n));

		// we cannot modify the tree during traversal of it
		//   if we copied the outgoing edges and used that list as traversal, it would be possible
		//   we opted against that and use a list of operations to do
        if (act.hide(n)) {
        	hide(n);
        } else if (act.omit(n)) {
        	omit(n);
        } else if (act.aggregate(n)) {
        	aggregate(n);
        } else {
        	// continue DFS
        	Set<DefaultEdge> edges = pt.getTree().outgoingEdgesOf(n);

        	// we have to collect the nodes out of edges as a modification of the graph leads to an exception when we have still a reference to the edge set
        	ArrayList<Node> nodesToVisit = new ArrayList<Node>();
        	for (DefaultEdge e: edges) {
        		Node t = pt.getTree().getEdgeTarget(e);
        		nodesToVisit.add(t);
        	}
        	edges = null;

        	for (Node t: nodesToVisit) {
        		visit(t, false);
        	}
        	// now, the subtree has been altered

        	if (!isRoot) {
        		// number of outgoing edges has to be recalculated as nodes could have been removed
        		// if there is only one outgoing edges, then this node can be removed... (As we handle a tree, that node surely has only one incoming edge)
        		if (pt.getTree().outgoingEdgesOf(n).size()==1 && (!( // loops MUST NOT be removed to enable a proper construction of the process type
        				(n.getBPELel() instanceof org.eclipse.bpel.model.While) ||
        				(n.getBPELel() instanceof org.eclipse.bpel.model.RepeatUntil) ||
        				(n.getBPELel() instanceof org.eclipse.bpel.model.ForEach))))
        			remove(n);
        	}
        }
	}

	private void addAllChildrenToSet(Node n, Set<Node> s) {
    	Set<DefaultEdge> edges = pt.getTree().outgoingEdgesOf(n);
    	for (DefaultEdge e: edges) {
    		Node t = pt.getTree().getEdgeTarget(e);
    		s.add(t);
    		// recurse into children
    		addAllChildrenToSet(t,s);
    	}
	}

	private void removeNodeAndAllItsChildren(Node n) {
		HashSet<Node> nodesToRemove = new HashSet<Node>();
		addAllChildrenToSet(n, nodesToRemove);
		nodesToRemove.add(n);
		pt.removeAllNodes(nodesToRemove);
	}

	private void hide(Node n) {
		logger.debug(String.format("Hiding %s", n));
		removeNodeAndAllItsChildren(n);

		// type = OTHER is NOT supported in ProcessType/ProcessTree
		// Node nodeWithHiddenLabel = new Node(Type.OTHER, n.getMult(), n.getRank(), n.getLevel());
		// ProcessTree's allBasicNonSilentActivies does NOT contain nodes of this type
		// NOT adding a new node contradicts the paper, but follows the
		// implementation guideline of Alex
	}

	private void omit(Node n) {
		logger.debug(String.format("Omiting %s", n));
		removeNodeAndAllItsChildren(n);
	}

	/**
	 * @param n the node to aggregate
	 * @throws IllegalStateException() if XML given in the projection file is invalid
	 */
	private void aggregate(Node n) {
		logger.debug(String.format("Aggregating %s", n));

		// get predecessor of n
		// has to be done before removal
		Node pre = null;
    	Set<DefaultEdge> edges = pt.getTree().incomingEdgesOf(n);
    	for (DefaultEdge e: edges) {
    		pre = pt.getTree().getEdgeSource(e);
    	}

		removeNodeAndAllItsChildren(n);

		String XML = act.getAggregateForNode(n);

		Document doc;
		org.xml.sax.InputSource inStream = new org.xml.sax.InputSource();
		inStream.setCharacterStream(new java.io.StringReader(XML));
		// we have to use the same DOM parser as BPELReader does. In that way the parsed XML node can be adopted to the DOM node tree of the BPEL process
		db = new LineCapturingDOMParser();
		try {
			db.parse(inStream);
			doc = db.getDocument();
		} catch (Exception e) {
			logger.error("Could not parse XML String " + XML, e);
			throw new IllegalStateException(e);
		}

		// DocumentElement is NOT from the DOM of BPEL
		// this causes problems when adding it to the BPEL model
		// we have to adopt the node to the tree
		Element newActivityElement = doc.getDocumentElement();
		Document ownerDocument = this.pt.getBPELresource().getProcess().getElement().getOwnerDocument();
		org.w3c.dom.Node node = ownerDocument.adoptNode(newActivityElement);
		newActivityElement = (Element) node;

		// onMessage does NOT work - it seems not to be intended
		// if it should work, we have two options
		//  * offer xml2extensible element being a superset for xml2activity
		//  * find out whether the current element is onMessage|onAlarm|onEvent and pass it directly to xml2onMessage, xml2onAlarm, ...
		//    --> in that case, these method have to changed from protected to public, too.
		// furthermore, we have to adopt all the adding things....
	    PartnerActivity a = (PartnerActivity) r.xml2Activity(newActivityElement);
	    if (a == null) {
	    	logger.error("Error during BPEL parsing of new XML");
	    	logger.error(XML);
	    	return;
	    }

	    // We do a really, really ugly hack here.
	    // We do NOT change the BPEL process in the way the projection demands it, but just the way that a resolution of partnerlink and variable is possible

	    Sequence seq = BPELFactory.eINSTANCE.createSequence();

	    EObject el = n.getBPELel();
	    // go up the hierarchy until we hit a scope or a process. Here pls and vars are declared
	    while (!(el instanceof Scope) && !(el instanceof Process)) {
	    	el = el.eContainer();
	    }
	    // replace child activity of scope/process by sequence of that child activity and the newly generated one
	    Activity oldChildActivity;
	    if (el instanceof Scope) {
	    	Scope sc = (Scope) el;
	    	oldChildActivity = sc.getActivity();
	    	sc.setActivity(seq);
	    } else {
	    	Process pr = (Process) el;
	    	oldChildActivity = pr.getActivity();
	    	pr.setActivity(seq);
	    }
	    seq.getActivities().add(oldChildActivity);
	    seq.getActivities().add(a);


	    // resolve partnerLinks and variables
	    r.pass2();

	    // restore old state of process not needed as we do not work further w/ the BPEL process


	    /** add node to process Tree **/

	    Node newN = new Node(a, n.getMult(), n.getRank(), n.getLevel());

	    // add to appropriate fields in tree
	    if (a instanceof Invoke) {
	    	pt.addAsInvoke(newN);
	    } else if (a instanceof Receive) {
	    	pt.addAsReceive(newN);
	    } else {
	    	// assert: Reply
	    	pt.addAsReply(newN);
	    }

	    // link node to parent of oldNode
	    pt.getTree().addEdge(pre, newN);
	}

	/**
	 * Removes given node out of the tree. Connects parent with child of of node
	 * @param n
	 */
	private void remove(Node n) {
		logger.debug(String.format("Removing %s", n));

		SimpleDirectedGraph<Node, DefaultEdge> tree = this.pt.getTree();

		// determine parent and child
		Set<DefaultEdge> in = this.pt.getTree().incomingEdgesOf(n);
		assert(in.size() == 1);
		Node par = tree.getEdgeSource(in.iterator().next());
		Set<DefaultEdge> out = this.pt.getTree().outgoingEdgesOf(n);
		assert(out.size() == 1);
		Node child = tree.getEdgeTarget(out.iterator().next());

		this.pt.removeNode(n); // ensures that the data structures are updated accordingly
		tree.addEdge(par, child); // here we dive deeply into the data structures of pt, but we know what we're doing ;)
	}

}
