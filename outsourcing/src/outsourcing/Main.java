package outsourcing;

import java.io.File;


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import outsourcing.processtree.ProcessTree;
import outsourcing.processtree.Comparator.MetricResult;
import outsourcing.processtree.projections.ActionList;
import outsourcing.processtree.projections.Projections;

public class Main {

	private static Logger logger = Logger.getLogger(Main.class);

	/**
	 * Starts the analysis and prints out the results on the console
	 *
	 * @param process1 First process to load
	 * @param process2 Second process to load
	 * @param projection1 the file containing the projections for the first process. May be null.
	 * @param projection2 the file containing the projections for the second process. May be null.
	 * @throws Exception
	 */
	public void analyzeProcesses(URI process1, URI process2, File projection1, File projection2) throws Exception {
	    // do default log4j initialization
		// see http://logging.apache.org/log4j/1.2/manual.html
		BasicConfigurator.configure();

		// some overriding - too lazy to use log4j.properties
		Logger.getRootLogger().setLevel(Level.INFO);
		//Logger.getLogger(Comparator.class).setLevel(Level.INFO);

		// assumption: in the current directory, there are p1.bpel and p2.bpel

		/** load processes and generate process trees **/
		BPELResource p1, p2;
		ProcessTree pt1, pt2;
		try {
			p1 = loadProcess(process1);
			pt1 = new ProcessTree(p1);
		} catch (Exception e) {
			//logger.error("Could not open first process", e);
			logger.error(String.format("Could not open first process. Reason: %s", e.getLocalizedMessage()));
			return;
		}

		try {
			p2 = loadProcess(process2);
			pt2 = new ProcessTree(p2);
		} catch (Exception e) {
			logger.error(String.format("Could not open second process. Reason: %s", e.getLocalizedMessage()));
			return;
		}

		if (projection1 != null) {
			System.out.println("Before projections:");
			System.out.println("pt1:");
			System.out.println(pt1.toString());
			System.out.println("pt2:");
			System.out.println(pt2.toString());
		}

		/**
		 * load projections
		 *
		 * projections are loaded into an ActionList. The "real" projections are done in class Projections
		 *
		 **/
		ActionList al1 = null, al2 = null;
		if (projection1 != null) {
			// could throw IOException
			al1 = new ActionList(projection1, p1);
			al2 = new ActionList(projection2,  p2);


			Projections projections1 = new Projections(pt1, al1);
			projections1.perform();

			Projections projections2 = new Projections(pt2, al2);
			projections2.perform();
		}

		pt1.determineProcesType();
		pt2.determineProcesType();

		if (projection1 != null) {
			System.out.println("After projections");
		}
		System.out.println("pt1:");
		System.out.println(pt1.toString());
		System.out.println("pt2:");
		System.out.println(pt2.toString());

		/** output matchings **/

		System.out.println(String.format("Match exactly: %s", Boolean.toString(pt1.matchesExactly(pt2))));
		// " + '\u22B3' + "
		System.out.println(String.format("Plugin Matching P1|>P2: %s", Boolean.toString(pt1.isPluginForProcessTree(pt2))));
		System.out.println(String.format("Plugin Matching P2|>P1: %s", Boolean.toString(pt2.isPluginForProcessTree(pt1))));

		MetricResult degreeLoopMatching = pt1.getDegreeOfInexactMatching(pt2);

		// we only print out M2 as this metrics is described in the paper
		// M1 has been used in a previous paper
		System.out.println(String.format("Degree of loop sensitive matching (P1" + '\u2293' + "lP2): %f", degreeLoopMatching.M2));
		System.out.println(String.format("Degree of loop insensitive matching (P1" + '\u2293' + "lP2): %f", degreeLoopMatching.MI2));
		// U+2293 (8851) or U+220F (8719)

		return;
	}

	/**
	 * Load a BPEL process from a fileName and return the loaded process
	 * @param fileName
	 * @return
	 */
	private static BPELResource loadProcess(URI uri) {
		ResourceSet resourceSet = new ResourceSetImpl();
		return (BPELResource) resourceSet.getResource(uri, true);
	}

}
