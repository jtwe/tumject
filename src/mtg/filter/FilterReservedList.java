package mtg.filter;

import javax.json.JsonObject;

public class FilterReservedList extends CardFilter {

	@Override
	public boolean isOk(JsonObject cardObject) {
		try {
			return cardObject.getBoolean("isReserved");
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	protected String generateDescription() {
		return "on the Reserved List";
	}

}
