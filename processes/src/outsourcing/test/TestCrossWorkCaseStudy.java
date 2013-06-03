package outsourcing.test;

import org.junit.Test;

public class TestCrossWorkCaseStudy {

	@Test
	public void TestMatchingOEMWithClusterA() throws Exception {
		Helper.testProcessWithProjection("CrossWorkCaseStudy/OEM/OEMProcessView.bpel", "CrossWorkCaseStudy/ClusterA/ClusterAProcessView.bpel");
	}

	@Test
	public void TestP1projection2() throws Exception {
		Helper.testProcessWithProjection("CrossWorkCaseStudy/OEM/OEMProcessView.bpel", "CrossWorkCaseStudy/ClusterB/ClusterBProcessView.bpel");
	}
}
