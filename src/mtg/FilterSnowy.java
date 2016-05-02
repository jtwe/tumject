package mtg;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterSnowy extends CardFilter {

	@Override
	public boolean isOk(JsonObject cardObject) {
		if (cardObject.getString("name", "").toLowerCase().contains("snow")) return true;
		
		JsonArray cardTypes = cardObject.getJsonArray("supertypes");
		if (cardTypes!=null) for (int i=0; i<cardTypes.size(); i++) if (cardTypes.getString(i).equals("Snow")) return true;

		if (cardObject.getString("manaCost", "").contains("{S}")) return true;

		if (cardObject.getString("text", "").contains("{S}")) return true;

		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String generateDescription() {
		return "snowy";
	}

}
