package mtg;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterCardTitle extends CardFilter {
	private boolean inverted = true;
	private boolean andFlavor = false;
	private List<String> titleStrings;
	
	public FilterCardTitle(String... types) {
		this.titleStrings = new ArrayList<String>();
		for (String type : types) this.titleStrings.add(type);
	}
	
	public FilterCardTitle(List<String> types) {
		this.titleStrings = types;
	}

	public FilterCardTitle invert() {
		inverted = !inverted;
		return this;
	}
	
	public FilterCardTitle includeFlavor(boolean included) {
		andFlavor = included;
		return this;
	}
	
	@Override
	public boolean isOk(JsonObject cardObject) {
		for (String s : titleStrings) {
			if (cardObject.getString("name", "").toLowerCase().contains(s.toLowerCase())) return inverted;

			if (andFlavor) {
				String flavor = "";
				try {
					flavor = cardObject.getString("flavor").toLowerCase();
				} catch (Exception e) {
				}

				if (flavor.contains(s.toLowerCase())) return inverted;
			}
		}

		return !inverted;
	}

	@Override
	protected String generateDescription() {
		String toRet = "whose title " + (andFlavor?"or flavor text ":"") + (inverted?"does not contain ":"contains ");
		if (titleStrings.size()>1) toRet = toRet + "any of ";
		if (titleStrings.size()<1) {
			toRet = toRet + "anything(?)";
		} else if (titleStrings.size()==1) {
			toRet = toRet + "'" + titleStrings.get(0) + "'";
		} else if (titleStrings.size()==2) {
			toRet = toRet + "'" + titleStrings.get(0) + "' or '" + titleStrings.get(1) + "'";
		} else {
			for (int i=0; i<titleStrings.size()-1; i++) toRet = toRet + "'" + titleStrings.get(i) + "', ";
			toRet = toRet + "or '" + titleStrings.get(titleStrings.size()-1) + "'";
		}
		
		return toRet;
	}


}
