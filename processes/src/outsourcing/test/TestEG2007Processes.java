package outsourcing.test;

import org.junit.Test;

public class TestEG2007Processes {

	@Test
	public void testMatchingPaperExample() throws Exception {
		Helper.testProcesses("EG2007/Example/Offer1.bpel", "EG2007/Example/Offer2.bpel");
	}

	@Test
	public void testMatchingPaperFigure2() throws Exception {
		Helper.testProcesses("EG2007/Example/p1.bpel", "EG2007/Example/p2.bpel");
	}

	@Test
	public void testMatchingPaperFigure4() throws Exception {
		Helper.testProcesses("EG2007/Example/p1.bpel", "EG2007/Example/p2.bpel");
	}

}
