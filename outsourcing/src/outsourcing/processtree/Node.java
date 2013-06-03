package outsourcing.processtree;


import org.apache.log4j.Logger;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.PartnerActivity;

import outsourcing.Helper;
import outsourcing.processtree.labels.CommunicationConstructLabel;
import outsourcing.processtree.labels.EmptyLabel;
import outsourcing.processtree.labels.Label;

public class Node {
	private static Logger logger = Logger.getLogger(Node.class);

	private final static String newline = System.getProperty("line.separator");

	// the element the node has been generated from
	// null if it is an artificial node
	// we need the back reference for the aggregation function. There, the partnerlink and variable has to be determined for the inserted activity
	// the resolution is done using the BPEL process itself by internal routines of BPELreader.java
	private BPELExtensibleElement el;

	private Status status;

	private Label label;

	private Type type;

	private Mult mult;

	// position within the sequence
	private int rank;

	// used to determine proper SEQuence ancestor
	// count starts from 0
	private int level;

	// used for hashing / indexing in class ProcessType
	// Alternative implementation (inspired by JGraphT): vertices.indexOf(a) -- vertices instanceof List<Node>
	private int index = -1;

	// for debugging purposes
	private int DEBUGnumber;

	private static int DEBUGhighestNumber = 0;

	private Node() {
	}

	private Node(BPELExtensibleElement el, Label label, Type type, Mult mult, int rank, int level) {
		this.el = el;
		this.status = null;
		this.label = label;
		this.type = type;
		this.mult = mult;
		this.rank = rank;
		this.level = level;

		this.DEBUGnumber = DEBUGhighestNumber;
		DEBUGhighestNumber++;
	}

	/**
	 *
	 * @param el - may be null
	 * @param type
	 * @param mult
	 * @param rank
	 * @param level
	 */
	public Node(BPELExtensibleElement el, Type type, Mult mult, int rank, int level) {
		this(el, new EmptyLabel(), type, mult, rank, level);
	}

	public Node(PartnerActivity act, Mult mult, int rank, int level) {
		this(act, new CommunicationConstructLabel(act), Type.BASIC, mult, rank, level);
	}

	public Node(OnMessage om, Mult mult, int rank, int level) {
		this(om, new CommunicationConstructLabel(om), Type.BASIC, mult, rank, level);
	}

	public Type getType() {
		return this.type;
	}

	public Mult getMult() {
		return this.mult;
	}

	public Label getLabel() {
		return this.label;
	}

	/**
	 * @return Position in the sequence (in case the parent is a SEQ), 1 otherwise
	 */
	public int getRank() {
		return this.rank;
	}

	private String appendOutput(String res, String label, String content) {
		res = res.concat(label);
		res = res.concat(content);
		res = res.concat(newline);
		return res;
	}

	public String getDebugString() {
		return(String.format("N%d -- Label: %s | Type: %s | Mult: %s | Rank: %d", DEBUGnumber, this.label.toString(), this.type, this.mult, this.rank));
	}

	public String toString() {
		return(String.format("N%d", DEBUGnumber));
	}

	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 *
	 * @return the index of the node - only defined for basic activities (as ProcessTree.java only sets it for that type)
	 */
	public int getIndex() {
		return index;
	}

	public int getLevel() {
		return level;
	}

	public BPELExtensibleElement getBPELel() {
		return this.el;
	}

	public Status getStatus() {
		// status does not change, therefore we can cache the status
		// however, it is unlikely, that this function will be called more than once...
		if (this.status == null) {
			this.status = Helper.getStatus(this.getBPELel());
		}
		return this.status;
	}

	public int getDEBUGnumber() {
		return DEBUGnumber;
	}

}
