package mtg;

import javax.json.JsonObject;

public class FilterLayout extends CardFilter {

	private String layout;

	public FilterLayout(String layout) {
		this.layout = layout;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		try {
			String layoutVal = cardObject.getString("layout");
			if (layout.equals(layoutVal)) return true;
		} catch (Exception e) {
			// noop
		}
		return false;
		
	}

	@Override
	protected String generateDescription() {
		return layout + " layout";
	}

}
