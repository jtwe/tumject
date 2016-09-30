package mtg.parser;

import javax.json.JsonObject;

public class ParserWatermark implements CardParser {

	@Override
	public String[] parseCard(JsonObject cardObject) {
		String watermark = cardObject.getString("watermark", "none");

		return new String[]{watermark};
	}

}
