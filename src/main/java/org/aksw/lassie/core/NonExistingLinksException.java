package org.aksw.lassie.core;

public class NonExistingLinksException extends Exception{
	
	private static final String message = "There are no links to the target KB.";

	public NonExistingLinksException() {
		super(message);
	}

}
