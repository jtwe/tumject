package mtg;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterRomantic extends CardFilter {

	@Override
	public boolean isOk(JsonObject cardObject) {
		if (cardObject.getString("name", "").toLowerCase().contains("heart")) return true;
		if (cardObject.getString("name", "").toLowerCase().contains("love")) return true;

		String flavor = "";
		try {
			flavor = cardObject.getString("flavor").toLowerCase();
		} catch (Exception e) {
		}
		
//		if (flavor.contains("heart")) return true;
		if (flavor.contains("love")) return true;

		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String generateDescription() {
		return "romantic";
	}

}
