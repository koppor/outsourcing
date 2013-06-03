package outsourcing.test;

import org.junit.Test;

public class TestEN2010Processes {

	@Test
	public void testEDOCPaperFigure2() throws Exception {
		Helper.testProcess("EN2010/Figure2/ProviderX.bpel");
	}

	@Test
	public void testEDOCPaperFigure5() throws Exception {
		Helper.testProcessWithProjection("EN2010/Figure5/serviceConsumer.bpel", "EN2010/Figure5/projection.txt");
	}

}
