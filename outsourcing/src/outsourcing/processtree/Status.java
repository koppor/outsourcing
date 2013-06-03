package outsourcing.processtree;

public enum Status {
	 invokable,

	 observable,

	 // used at choices (if activities)
	 EXOR,
	 IXOR;

	 /**
	  * @throws IllegalStateException if s does not match the given string (TODO: add a self-made exception)
	  */
	 public static Status fromString(String s) {
		 if (s.equalsIgnoreCase(Status.invokable.toString()))
			 return Status.invokable;
		 else if (s.equalsIgnoreCase(Status.observable.toString()))
			 return Status.observable;
		 else if (s.equalsIgnoreCase(Status.EXOR.toString()))
			 return Status.EXOR;
		 else if (s.equalsIgnoreCase(Status.IXOR.toString()))
			 return Status.IXOR;
		 else
			 throw new IllegalStateException("Could not decode " + s);
	 }
}
