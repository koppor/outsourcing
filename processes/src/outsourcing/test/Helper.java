package outsourcing.test;

import static org.junit.Assert.*;

import java.io.File;

import org.eclipse.emf.common.util.URI;

import outsourcing.Main;

public class Helper {

	static void testProcess(String fn) throws Exception {
		testProcesses(fn, fn);
	}

	static void testProcesses(String fn1, String fn2) throws Exception {
		testProcessesWithProjection(fn1, fn2, null, null);
	}

	static void testProcessWithProjection(String procFn, String projFn) throws Exception {
		testProcessesWithProjection(procFn, procFn, projFn, projFn);
	}

	static void testProcessesWithProjection(String procFn1, String procFn2, String projFn1, String projFn2) throws Exception {
		URI uri1 = URI.createFileURI(new File(procFn1).getAbsolutePath().toString());
		URI uri2 = URI.createFileURI(new File(procFn2).getAbsolutePath().toString());
		File f1;
		if (projFn1 == null) {
			f1 = null;
		} else {
			f1 = new File(projFn1).getAbsoluteFile();
		}
		File f2;
		if (projFn2 == null) {
			f2 = null;
		} else {
			f2 = new File(projFn2).getAbsoluteFile();
		}
		Main m = new Main();
		m.analyzeProcesses(uri1, uri2, f1, f2);
	}

}
