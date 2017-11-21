package mtg.filter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class FilterLongNames extends CardFilter {
	private static Set<String> longNamedCards = null;
	private boolean toRet = true;

	public FilterLongNames(JsonObject jo, String directory, int targetQuantity) {
		this(jo, directory, targetQuantity, true);
	}
	
	public FilterLongNames(JsonObject jo, String directory, int targetQuantity, boolean verbose) {
		if (longNamedCards==null) {
			if (jo==null) {
				try {
					JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(directory + "AllSets-x.json"), "UTF8"));
					jo = jr.readObject();
				} catch (UnsupportedEncodingException | FileNotFoundException e) {
					jo = null;
				}
			}

			if (jo!=null) {
				longNamedCards = new HashSet<String>();

				Map<Integer, Set<String>> lengthToCardNames = new HashMap<Integer, Set<String>>();
				int maxLength = 0;
				boolean firstPost = true;

				for (String key : jo.keySet()) {
					JsonObject setObject = jo.getJsonObject(key);
					JsonArray cardsArray = setObject.getJsonArray("cards");

					if (verbose) System.out.println("Set: " + setObject.getString("name") + ", size = " + cardsArray.size() + ", hash size = " + lengthToCardNames.size() + ", max length = " + maxLength);
					if (verbose && !firstPost) System.out.println(" ... hash size of max length " + lengthToCardNames.get(maxLength).size());
					firstPost = false;

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

						Set<String> cardNamesForLength = lengthToCardNames.get(cardName.length());
						if (cardNamesForLength==null) {
							cardNamesForLength = new HashSet<String>();
							lengthToCardNames.put(cardName.length(), cardNamesForLength);
						}
						cardNamesForLength.add(cardName);
						if (cardName.length()>maxLength) {
							maxLength = cardName.length();
						}
					}
				}

				if (verbose) System.out.println("Post: hash size = " + lengthToCardNames.size() + ", max length = " + maxLength + ", hash size of max length " + lengthToCardNames.get(maxLength).size());

				for (int len=maxLength; len>0; len--) {
					Set<String> cardNamesForLength = lengthToCardNames.get(len);
//					if (verbose) System.out.println("Trying length = " + len + ", set = " + cardNamesForLength);
					if (cardNamesForLength==null) continue;
					if (verbose) System.out.println("Len = " + len + ", size = " + cardNamesForLength.size());
					longNamedCards.addAll(cardNamesForLength);
					if (longNamedCards.size()>targetQuantity) break;
				}

				System.out.println("Long named cards: " + longNamedCards.size());
			}
		}
	}

	public void invert() {
		this.toRet = !this.toRet;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		if (longNamedCards==null) return false;
		if (longNamedCards.contains(cardObject.getString("name"))) return toRet;
		return !toRet;
	}

	@Override
	protected String generateDescription() {
		return "with long names";
	}

}
