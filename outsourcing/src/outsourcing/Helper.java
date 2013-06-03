package outsourcing;


import org.apache.log4j.Logger;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.PartnerLink;
import org.w3c.dom.Element;

import outsourcing.processtree.Constants;
import outsourcing.processtree.Status;

/**
 * Helper class
 * (not called "Utility" as that name is used at BPEL-D classes)
 *
 * @author koppor
 *
 */
public class Helper {
	private static Logger logger = Logger.getLogger(Helper.class);

	private final static Status DEFAULT_STATUS = Status.observable;

	/**
	 * Determines the status on the given element
	 * @param a
	 * @return Status - default is Status.observable
	 */
	public static Status getStatus(BPELExtensibleElement el) {
		Element element = el.getElement();
		String statusAttr = element.getAttributeNS(Constants.NS_ESOURCING, Constants.ATTR_STATUS);
		Status s;
		if ((statusAttr == null) || (statusAttr.equals(""))) {
			s = DEFAULT_STATUS;
		} else {
			try {
				s = Status.fromString(statusAttr);
			} catch (Exception e) {
				logger.error("Could net decode status " + statusAttr, e);
				s = DEFAULT_STATUS;
			}
		}
		return s;
	}
}
