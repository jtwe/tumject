package mtg.filter;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterColorIdentity extends CardFilter {
	private boolean toRet = true;
	private List<String> colors;
	
	public FilterColorIdentity(String... colors) {
		this.colors = new ArrayList<String>();
		for (String color : colors) this.colors.add(color);
	}
	
	public FilterColorIdentity(List<String> colors) {
		this.colors = colors;
	}

	public FilterColorIdentity invert() {
		toRet = !toRet;
		return this;
	}
	
	@Override
	public boolean isOk(JsonObject cardObject) {
		JsonArray cardColors = null;

		cardColors = cardObject.getJsonArray("colorIdentity");
		if (cardColors==null) return !toRet;
		for (int i=0; i<cardColors.size(); i++) {
			if (colors.contains(cardColors.getString(i))) return toRet;
		}

		return !toRet;
	}

	@Override
	public String generateDescription() {
		if (colors.size()==1) {
			return "color idenity is " + (toRet?"":"not ") + colors.get(0);
		} else if (colors.size()==2) {
			return "color idenity is " + (toRet?"":"neither ") + colors.get(0) + (toRet?" or ":" nor ") + colors.get(1);
		} else if (colors.size()>2) {
			StringBuffer sb = new StringBuffer();
			// 0, 1, 2, 3, or 4
			for (int i=0; i<colors.size(); i++) {
				if (i==colors.size()-1) sb.append("or ");
				sb.append(colors.get(i));
				if (i<colors.size()-1) sb.append(", ");
			}
			return "is " + (toRet?"":"none of ") + sb.toString();
		}
		return null;
	}


}
