package outsourcing.processtree.labels;

/**
 * Singleton as the empty label is NOT modifiable
 *
 * @author koppor
 *
 */
public class EmptyLabel extends Label {
	private static EmptyLabel INSTANCE = null;

	@Override
	public String toString() {
		return "(empty)";
	}

	public static Object getInstance() {
		if (INSTANCE == null)
			INSTANCE = new EmptyLabel();
		return INSTANCE;
	}
}
