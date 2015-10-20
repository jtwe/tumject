package mtg;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class FilterSupplemental extends CardFilter {
	private static Set<String> supplementalCards = null;
	private boolean toRet = true;

	public FilterSupplemental(JsonObject jo, String directory) {
		if (supplementalCards==null) {
			if (jo==null) {
				try {
					JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(directory + "AllSets-x.json"), "UTF8"));
					jo = jr.readObject();
				} catch (UnsupportedEncodingException | FileNotFoundException e) {
					jo = null;
				}
			}

			if (jo!=null) {
				supplementalCards = new HashSet<String>();

				Set<String> allCards = new HashSet<String>();
				Set<String> regularCards = new HashSet<String>();

				for (String key : jo.keySet()) {
					JsonObject setObject = jo.getJsonObject(key);

					String setType = setObject.getString("type");

					boolean isRegularSet = false;
					if (setType.equals("core") || setType.equals("starter") || setType.equals("expansion")) isRegularSet = true;

					JsonArray cardsArray = setObject.getJsonArray("cards");
					for (int j=0; j<cardsArray.size(); j++) {
						JsonObject cardObject = cardsArray.getJsonObject(j);
						String cardName = cardObject.getString("name");

						try {
							JsonArray legalities = cardObject.getJsonArray("legalities");
							String vintageLegality = "";
							for (int i=0; i<legalities.size(); i++) {
								JsonObject legality = legalities.getJsonObject(i);
								if (legality.getString("format").equals("Vintage")) {
									vintageLegality = legality.getString("legality");
								}
							}

							if (!"Legal".equals(vintageLegality) && !"Restricted".equals(vintageLegality)) continue;
						} catch (NullPointerException e) {
							continue;
						}

						allCards.add(cardName);
						if (isRegularSet) regularCards.add(cardName);
					}
				}

				for (String cardName : allCards) {
					if (regularCards.contains(cardName)) continue;

					supplementalCards.add(cardName);
				}

				System.out.println("All cards: " + allCards.size() + ", regular cards: " + regularCards.size() + ", supplemental cards: " + supplementalCards.size());
			}
		}
	}

	public void invert() {
		this.toRet = !this.toRet;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		if (supplementalCards==null) return false;
		if (supplementalCards.contains(cardObject.getString("name"))) return toRet;
		return !toRet;
	}

	@Override
	protected String generateDescription() {
		return "from supplemental sets only";
	}

}
