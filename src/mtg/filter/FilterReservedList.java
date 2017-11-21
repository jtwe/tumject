package mtg.filter;

import javax.json.JsonObject;

public class FilterReservedList extends CardFilter {

	@Override
	public boolean isOk(JsonObject cardObject) {
		try {
			return cardObject.getBoolean("reserved");
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	protected String generateDescription() {
		// TODO Auto-generated method stub
		return "on the Reserved List";
	}

}
