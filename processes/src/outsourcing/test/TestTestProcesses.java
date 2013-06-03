package outsourcing.test;

import org.junit.Test;

public class TestTestProcesses {

	@Test
	public void TestP1projection1() throws Exception {
		Helper.testProcessWithProjection("tests/p1.bpel", "tests/p1-projection1.txt");
	}

	@Test
	public void TestP1projection2() throws Exception {
		Helper.testProcessWithProjection("tests/p1.bpel", "tests/p1-projection2.txt");
	}
}
