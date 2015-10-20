package mtg;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterIsRealCard extends CardFilter {

	@Override
	public boolean isOk(JsonObject cardObject) {
		try {
			cardObject.getInt("multiverseid");
		} catch (Exception e) {
			return false;
		}

		String vintageLegality = "";
		try {
			JsonArray legalities = cardObject.getJsonArray("legalities");
			for (int i=0; i<legalities.size(); i++) {
				JsonObject jo = legalities.getJsonObject(i);
				if (jo.getString("format").equals("Vintage")) vintageLegality = jo.getString("legality");
			}
//			vintageLegality = cardObject.getJsonObject("legalities").getString("Vintage");
		} catch (NullPointerException e) {
			return false;
		}

		if (!"Legal".equals(vintageLegality) && !"Restricted".equals(vintageLegality)) {
			return false;
		}

		return true;
	}

	@Override
	public String generateDescription() {
		return "cards";
	}

}
