package mtg.parser;

import javax.json.JsonObject;

public interface CardParser {
	String[] parseCard(JsonObject cardObject);
}
