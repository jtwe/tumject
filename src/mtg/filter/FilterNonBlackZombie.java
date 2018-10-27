package mtg.filter;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterNonBlackZombie extends CardFilter {
	private boolean toRet = true;

	@Override
	public boolean isOk(JsonObject cardObject) {
		JsonArray cardColors = cardObject.getJsonArray("colors");
		if (cardColors!=null) {
			for (int i=0; i<cardColors.size(); i++) {
				if (cardColors.getString(i).equals("B")) return !toRet;
			}
		}

		JsonArray cardTypes = cardObject.getJsonArray("subtypes");
		if (cardTypes!=null) {
			for (int i=0; i<cardTypes.size(); i++) {
				if ("Zombie".contains(cardTypes.getString(i))) return toRet;
			}
		}

		if (cardObject.getString("text", "").toLowerCase().contains("embalm") || cardObject.getString("text", "").toLowerCase().contains("eternalize")) return toRet;
		
		return !toRet;
	}

	@Override
	protected String generateDescription() {
		return "unconventional dead";
	}

}
