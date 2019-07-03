package io.antmedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamNameValidator {
	private static Pattern namePattern = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
	
	public static boolean isStreamNameValid(String name) {
		Matcher m = namePattern.matcher(name);
		return m.find();
	}
}
