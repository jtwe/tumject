package mtg.filter;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

public class FilterCardText extends CardFilter {
	private boolean inverted = false;
	private List<String> textStrings;

	public FilterCardText(String... texts) {
		this.textStrings = new ArrayList<String>();
		for (String text : texts) this.textStrings.add(text);
	}
	
	public FilterCardText(List<String> texts) {
		this.textStrings = texts;
	}

	public FilterCardText invert() {
		inverted = !inverted;
		return this;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		for (String s : textStrings) {
			if (cardObject.getString("text", "").toLowerCase().contains(s.toLowerCase())) return !inverted;
		}

		return inverted;
	}

	@Override
	protected String generateDescription() {
		String toRet = "whose text " + (inverted?"does not contain ":"contains ");
		if (textStrings.size()>1) toRet = toRet + "any of ";
		if (textStrings.size()<1) {
			toRet = toRet + "anything(?)";
		} else if (textStrings.size()==1) {
			toRet = toRet + "'" + textStrings.get(0) + "'";
		} else if (textStrings.size()==2) {
			toRet = toRet + "'" + textStrings.get(0) + "' or '" + textStrings.get(1) + "'";
		} else {
			for (int i=0; i<textStrings.size()-1; i++) toRet = toRet + "'" + textStrings.get(i) + "', ";
			toRet = toRet + "or '" + textStrings.get(textStrings.size()-1) + "'";
		}
		
		return toRet;
	}

}
