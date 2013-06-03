package outsourcing.processtree;

/**
 * "Configuration" constants
 *
 * @author koppor
 *
 */
public class Constants {

	/*** XML stuff ***/

	public static final String NS_ESOURCING = "http://www.cs.helsinki.fi/u/anorta/research/eSourcing/";

	public static final String ATTR_STATUS = "status"; // may be "invokable", "observable", "IXOR", or "EXOR" - stored in enum Status (Status.java)

	public static final String ATTR_OUTSOURCE = "outsource";

	public static final String ATTR_DESTINATION_URI = "destination_uri";
}
