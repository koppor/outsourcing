package outsourcing.processtree.labels;

import org.apache.log4j.Logger;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.PartnerActivity;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.wst.wsdl.Operation;

public class CommunicationConstructLabel extends Label {
	private static Logger logger = Logger.getLogger(CommunicationConstructLabel.class);

	//private Activity activity = null;

	private String role = null;
	private String partnerLinkType = null;
	private String operation = null;
	private String name = null;

	private CommunicationConstructLabel(PartnerLink pl, Operation op, boolean useMyRole, String name) {
		this.name = name;

		if (pl == null)
			logger.fatal("partnerLink is null");
		if (useMyRole) {
			this.role = pl.getMyRole().getName();
		} else {
			this.role = pl.getPartnerRole().getName();
		}

		PartnerLinkType plt = pl.getPartnerLinkType();
		if (plt == null)
			logger.fatal("partnerLinkType is null");
		// PartnerLinkType does NOT state namespace, we'll just rely on the name
		this.partnerLinkType = plt.getName();

		if (op == null)
			logger.fatal("operation is null");
		// Operation does NOT state namespace, we'll just rely on the name
		this.operation = op.getName();
	}

	public CommunicationConstructLabel(OnMessage om) {
		// TODO: onMessage cannot have a name. BPEL4Chor uses wsu:id - here this should be implemented
		this(om.getPartnerLink(), om.getOperation(), true, null);
	}

	public CommunicationConstructLabel(PartnerActivity activity) {
		this(activity.getPartnerLink(), activity.getOperation(), (activity instanceof Receive)?true:false, activity.getName());
	}

	public boolean equals(Object o) {
		if (o instanceof CommunicationConstructLabel) {
			CommunicationConstructLabel l2 = (CommunicationConstructLabel) o;
			return (this.role.equals(l2.role) &&
					this.partnerLinkType.equals(partnerLinkType) &&
					this.operation.equals(operation));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		if (role == null) {
			return 0;
		} else {
			int roleHash = role.hashCode();
			int pltHash = partnerLinkType.hashCode();
			int opHash = operation.hashCode();
			//logger.debug(String.format("%x/%x/%x", roleHash, pltHash, opHash));
			return (roleHash + pltHash + opHash);
		}
	}

	public String toString() {
		return String.format("%1s/%1s/%1s", partnerLinkType, role, operation);
	}

}
