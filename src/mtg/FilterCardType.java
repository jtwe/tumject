package mtg;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterCardType extends CardFilter {
	private boolean toRet = true;
	private List<String> types;
	
	public FilterCardType(String... types) {
		this.types = new ArrayList<String>();
		for (String type : types) this.types.add(type);
	}
	
	public FilterCardType(List<String> types) {
		this.types = types;
	}

	public FilterCardType invert() {
		toRet = !toRet;
		return this;
	}
	
	@Override
	public boolean isOk(JsonObject cardObject) {
		JsonArray cardTypes = null;
		String[] keys = {"subtypes", "supertypes", "types"};
		for (String key : keys) {
			cardTypes = cardObject.getJsonArray(key);
			if (cardTypes==null) continue;
			for (int i=0; i<cardTypes.size(); i++) {
				if (types.contains(cardTypes.getString(i))) return toRet;
			}
		}

		return !toRet;
	}

	@Override
	public String generateDescription() {
		if (types.size()==1) {
			return "type " + (toRet?"contains ":"doesn't contain ") + types.get(0);
		} else if (types.size()==2) {
			return "type contains " + (toRet?"either ":"neither ") + types.get(0) + (toRet?" or ":" nor ") + types.get(1);
		} else if (types.size()>2) {
			StringBuffer sb = new StringBuffer();
			// 0, 1, 2, 3, or 4
			for (int i=0; i<types.size(); i++) {
				if (i==types.size()-1) sb.append("or ");
				sb.append(types.get(i));
				if (i<types.size()-1) sb.append(", ");
			}
			return "type contains " + (toRet?"at least one of ":"none of ") + sb.toString();
		}
		return null;
	}

}
