package outsourcing.processtree.projections;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import org.eclipse.bpel.model.resource.BPELResource;

public class BPELnamespaceContext implements NamespaceContext {

	private String BPELNS;

	private BPELnamespaceContext() {}

	public BPELnamespaceContext(BPELResource r) {
		BPELNS = r.getProcess().getElement().getNamespaceURI();
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (prefix.equalsIgnoreCase("bpel"))
			return BPELNS;
		else
			return null;
	}

	@Override
	public String getPrefix(String namespaceURI) {
		throw new IllegalStateException();
	}

	@Override
	public Iterator getPrefixes(String namespaceURI) {
		throw new IllegalStateException();
	}

}
