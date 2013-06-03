package outsourcing.test;

import org.junit.Test;

public class TestGSMProcesses {

	@Test
	public void TestMatchingConsumerWithProviderX() throws Exception {
		Helper.testProcessWithProjection("GSM/Figure2/consumerProcessView.bpel", "GSM/Figure3/providerProcessX.bpel");
	}

	@Test
	public void TestMatchingConsumerWithProviderY() throws Exception {
		Helper.testProcessWithProjection("GSM/Figure2/consumerProcessView.bpel", "GSM/Figure4/providerProcessY.bpel");
	}

}
