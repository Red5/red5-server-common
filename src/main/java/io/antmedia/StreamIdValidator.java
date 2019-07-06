package io.antmedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamIdValidator {
	private static Pattern namePattern = Pattern.compile("[^a-z0-9-_]", Pattern.CASE_INSENSITIVE);
	
	private StreamIdValidator() {
		
	}
	public static boolean isStreamIdValid(String name) {
		if (name != null) {
			Matcher m = namePattern.matcher(name);
			return !m.find();
		}
		return false;
	}
}
