package dk.digitalidentity.sofd.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class LocalExtensionsSerializer extends JsonSerializer<String> {

	@Override
	public void serialize(String value, JsonGenerator jgen, SerializerProvider serializers) throws IOException, JsonProcessingException {		
		try {
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			Object obj = parser.parse(value);
			jgen.writeObject(obj);
		}
		catch (ParseException ex) {
			throw new IOException(ex);
		}
	}
}
