package outsourcing.processtree;

public enum Operation {
	// sequential
	S1, SA,
	// sequential reversed // n1 S1R n2 means n2 S1 n1
	S1R, SAR,

	// exclusive
	X1, XA,

	// parallel
	P1, PA;

	public boolean isParallel() {
		return ((this == Operation.P1) || (this == Operation.PA));
	}
}
