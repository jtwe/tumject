package mtg;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterLegality extends CardFilter {
	private boolean toRet = true;
	private List<String> formats;
	
	public FilterLegality(String... formats) {
		this.formats = new ArrayList<String>();
		for (String type : formats) this.formats.add(type);
	}
	
	public FilterLegality(List<String> formats) {
		this.formats = formats;
	}

	public FilterLegality invert() {
		toRet = !toRet;
		return this;
	}
	
	@Override
	public boolean isOk(JsonObject cardObject) {
		JsonArray legalities = cardObject.getJsonArray("legalities");
		
		if (legalities==null) return false;
		
		for (int i=0; i<legalities.size(); i++) {
			JsonObject jo = legalities.getJsonObject(i);
			String format = jo.getString("format");
			if (formats.contains(format) && (jo.getString("legality").equals("Legal") || jo.getString("legality").equals("Restricted") ) ) return toRet;
		}
/*		
		for (String format : formats) {
			if (legalities.containsKey(format) && (legalities.getString(format).equals("Legal") || legalities.getString(format).equals("Restricted")) ) return toRet;
		}
*/
		return !toRet;
	}

	@Override
	public String generateDescription() {
		if (formats.size()==1) {
			return "is " + (toRet?"":"not ") + "playable in " + formats.get(0);
		} else if (formats.size()==2) {
			return "is playable in " + (toRet?"either ":"neither ") + formats.get(0) + (toRet?" or ":" nor ") + formats.get(1);
		} else if (formats.size()>2) {
			StringBuffer sb = new StringBuffer();
			// 0, 1, 2, 3, or 4
			for (int i=0; i<formats.size(); i++) {
				if (i==formats.size()-1) sb.append("or ");
				sb.append(formats.get(i));
				if (i<formats.size()-1) sb.append(", ");
			}
			return "is playable in " + (toRet?"at least one of ":"none of ") + sb.toString();
		}
		return null;
	}

}
