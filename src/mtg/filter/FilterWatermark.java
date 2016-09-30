package mtg.filter;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

public class FilterWatermark extends CardFilter {
	private boolean inverted = true;
	private List<String> textStrings;

	public FilterWatermark(String... texts) {
		this.textStrings = new ArrayList<String>();
		for (String text : texts) this.textStrings.add(text);
	}
	
	public FilterWatermark(List<String> texts) {
		this.textStrings = texts;
	}

	public FilterWatermark invert() {
		inverted = !inverted;
		return this;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		for (String s : textStrings) {
			if (cardObject.getString("watermark", "").toLowerCase().contains(s.toLowerCase())) return inverted;
		}

		return !inverted;
	}

	@Override
	protected String generateDescription() {
		String toRet = "whose watermark " + (inverted?"is not ":"is ");
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
