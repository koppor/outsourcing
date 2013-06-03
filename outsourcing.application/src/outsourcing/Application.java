package outsourcing;

import java.io.File;

import org.eclipse.emf.common.util.URI;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import outsourcing.Main;


/**
 * The main application
 */
public class Application implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[])context.getArguments().get("application.args");
		String fn1, fn2;
		String pr1 = null, pr2 = null; // the projections

		switch (args.length) {
		case 4:
			pr1 = args[2];
			pr2 = args[3];
			System.out.println(String.format("Using projection files %s and %s", pr1, pr2));
		case 2:
			fn1 = args[0];
			fn2 = args[1];
			System.out.println(String.format("Trying to use files %s and %s", fn1, fn2));
			break;

		default:
			fn1 = "p1.bpel";
			fn2 = "p2.bpel";
			System.out.println("Not two or four parameters given. Using default processes p1.bpel and p2.bpel. No projection used");
			break;
		}

		File f1 = new File(fn1);
		File f2 = new File(fn2);
		URI uri1 = URI.createFileURI(f1.getAbsolutePath());
		URI uri2 = URI.createFileURI(f2.getAbsolutePath());

		File p1 = null;
		File p2 = null;
		if (pr1 != null) {
			p1 = new File(pr1);
			p2 = new File(pr2);
		}

		Main m = new Main();
		m.analyzeProcesses(uri1, uri2, p1, p2);

		return null;
	}

	@Override
	public void stop() {
	}

}
