package mtg.parser;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class ParserColorCount implements CardParser {

	/*

				String[] cardKeys = null;
				try {
//					cardKeys = new String[]{Integer.toString(cardObject.getInt("cmc"))};
//					cardKeys = new String[]{cardObject.getString("rarity")};
/*
					JsonArray ja = cardObject.getJsonArray("colors");
					cardKeys = new String[ja.size()];
					for (int i=0; i<ja.size(); i++) {
						cardKeys[i] = ja.getString(i);
					}
* /

					JsonArray ja = cardObject.getJsonArray("colors");
					cardKeys = new String[]{Integer.toString(ja.size())};

/*
					List<String> cardKeysList = new ArrayList<String>();
					JsonObject joo = cardObject.getJsonObject("legalities");
					for (String key : joo.keySet()) {
						String s = key + " " + joo.getString(key);
						cardKeysList.add(s);
					}
					cardKeys = cardKeysList.toArray(new String[]{});
* /
				} catch (Exception e) {
//					cardKeys = null;
//					if (cardsSeen.isEmpty()) e.printStackTrace();
					cardKeys = new String[]{"0"};
				}


	 */
	
	@Override
	public String[] parseCard(JsonObject cardObject) {
		String[] cardKeys = null;
		try {
			JsonArray ja = cardObject.getJsonArray("colors");
			cardKeys = new String[]{Integer.toString(ja.size())};
		} catch (Exception e) {
			cardKeys = new String[]{"0"};
		}
		return cardKeys;
	}

}
