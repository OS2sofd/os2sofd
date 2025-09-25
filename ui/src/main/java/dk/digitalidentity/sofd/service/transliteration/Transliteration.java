package dk.digitalidentity.sofd.service.transliteration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Code copied from https://github.com/micshome/transliteration/blob/master/src/main/java/me/saitov/libs/transliteration/Transliteration.java
public class Transliteration {

	public static String transliterate(String input, Character separator) {
		Character ascii;
		separator = separator == null ? '?' : separator;
		input = ucs2decode(input);
		String strNew = "";

		for (Character ord : input.toCharArray()) {
			if (ord > 0xffff) {
				strNew += separator;
				continue;
			}
			
			Character bank = (char) (ord >> 8);

			if (ReplacementData.codemap.get(bank) == null) {
				ReplacementData.codemap.put(bank, new Character[256]);
				Arrays.fill(ReplacementData.codemap.get(bank), separator);
			}
			
			ord = (char) (0xff & ord);
			ascii = ReplacementData.codemap.get(bank)[ord];

			if (ascii == null) {
				ascii = separator;
			}
			
			strNew += ascii;
		}

		return strNew;
	}
	
	private static String ucs2decode(String input) {
		List<Character> output = new ArrayList<Character>();
		int counter = 0;
		int length = input.length();
		char value;
		char extra;
		
		while (counter < length) {
			value = input.charAt(counter++);
			if (value >= 0xD800 && value <= 0xDBFF && counter < length) {
				// high surrogate, and there is a next character
				extra = input.charAt(counter++);
				if ((extra & 0xFC00) == 0xDC00) { // low surrogate
					output.add(Character.valueOf((char) (((value & 0x3FF) << 10) + (extra & 0x3FF) + 0x10000)));
				}
				else {
					// unmatched surrogate; only append this code unit, in case the next
					// code unit is the high surrogate of a surrogate pair
					output.add(Character.valueOf(value));
					counter--;
				}
			}
			else {
				output.add(Character.valueOf(value));
			}
		}
		
		StringBuilder sb = new StringBuilder(output.size());

		for (Character c : output) {
			sb.append(c);
		}
		
		return sb.toString();
	}
}
