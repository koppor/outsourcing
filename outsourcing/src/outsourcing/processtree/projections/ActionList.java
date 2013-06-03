package outsourcing.processtree.projections;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.bpel.model.resource.BPELResource;
import org.w3c.dom.Element;

import outsourcing.processtree.Node;


public class ActionList {

	private final static String Action_OMIT = "omit";
	private final static String Action_HIDE = "hide";
	private final static String Action_AGGREGATE = "aggregate";
	private final static String BR = System.getProperty("line.separator");

	private HashSet<Element> toOmit = new HashSet<Element>();
	private HashSet<Element> toHide = new HashSet<Element>();
	private Hashtable<Element, String> toAggregate_nameToAcivitiyXML = new Hashtable<Element, String>();

	private final BPELResource r;
	private final XPathFactory factory;
	private final XPath xpath;

	private Element getElement(String XPath) {
		org.w3c.dom.Node res;
		try {
			res = (org.w3c.dom.Node) xpath.evaluate(XPath, r.getProcess().getElement(), XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
		return (Element) res;
	}

	/**
	 * Reads the given file and interprets the actions there
	 *
	 * @param actionFile the file containing the actions
	 * @param r the BPEL resource where the actions will be applied to
	 * @throws IOException
	 */
	public ActionList(File actionFile, BPELResource r) throws IOException {
		this.r = r;
		this.factory = XPathFactory.newInstance();
		this.xpath = factory.newXPath();
		this.xpath.setNamespaceContext(new BPELnamespaceContext(r));

		BufferedReader in = new BufferedReader(new FileReader(actionFile.getAbsolutePath()));
		String line = in.readLine();
		while (line!=null) {
			if (line.startsWith(Action_OMIT)) {
				Element el = getElement(line.substring(Action_OMIT.length()+1).trim());
				toOmit.add(el);
			} else if (line.startsWith(Action_HIDE)) {
				Element el = getElement(line.substring(Action_HIDE.length()+1).trim());
				toHide.add(el);
			} else if (line.startsWith(Action_AGGREGATE)) {
				Element el = getElement(line.substring(Action_AGGREGATE.length()+1).trim());
				String XMLstring = "";
				line = in.readLine();
				while ((line!=null) && (!line.isEmpty())) {
					XMLstring = XMLstring.concat(line).concat(BR);
					line = in.readLine();
				}
				toAggregate_nameToAcivitiyXML.put(el, XMLstring);
			}
			line = in.readLine();
		};
		in.close();
	}


	public Boolean hide(Node node) {
		Element el = node.getBPELel().getElement();
		return toHide.contains(el);
	}

	public boolean omit(Node node) {
		Element el = node.getBPELel().getElement();
		return toOmit.contains(el);
	}

	public boolean aggregate(Node node) {
		Element el = node.getBPELel().getElement();
		return toAggregate_nameToAcivitiyXML.containsKey(el);
	}

	public String getAggregateForNode(Node node) {
		Element el = node.getBPELel().getElement();
		return toAggregate_nameToAcivitiyXML.get(el);
	}

}
