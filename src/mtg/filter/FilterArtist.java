package mtg.filter;

import javax.json.JsonObject;

public class FilterArtist extends CardFilter {

	private String name;

	public FilterArtist(String name) {
		this.name = name;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		try {
			String artistName = cardObject.getString("artist");
			if (name.equals(artistName)) return true;
		} catch (Exception e) {
			// noop
		}
		return false;
		
	}

	@Override
	protected String generateDescription() {
		return "illustrated by " + name;
	}

}
