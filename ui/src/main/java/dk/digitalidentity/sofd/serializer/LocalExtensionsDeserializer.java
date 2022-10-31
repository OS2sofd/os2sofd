package dk.digitalidentity.sofd.serializer;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BaseJsonNode;

import net.minidev.json.JSONObject;

public class LocalExtensionsDeserializer extends JsonDeserializer<String> {

	@SuppressWarnings("unchecked")
	@Override
	public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {		
		BaseJsonNode node = jp.readValueAsTree();

		if (!node.isObject()) {
			throw new JsonParseException(jp, "Only JSON objects are allowed as localExtensions!");
		}
		else {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> map = mapper.convertValue(node, Map.class);

			return new JSONObject(map).toString();
		}
	}
}