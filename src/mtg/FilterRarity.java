package mtg;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

public class FilterRarity extends CardFilter {
	private boolean toRet = true;
	private List<String> rarities;

	public FilterRarity(String... rarities) {
		this.rarities = new ArrayList<String>();
		for (String rarity : rarities) this.rarities.add(rarity);
	}
	
	public FilterRarity(List<String> rarities) {
		this.rarities = rarities;
	}
	
	public FilterRarity invert() {
		toRet = !toRet;
		return this;
	}
	
	@Override
	public boolean isOk(JsonObject cardObject) {
		String rarity;
		try {
			rarity = cardObject.getString("rarity");
		} catch (Exception e) {
			rarity = null;
		}
		if (rarities.contains(rarity)) return toRet;
		return !toRet;
	}

	@Override
	public String generateDescription() {
		if (rarities.size()==1) {
			return "rarity is " + (toRet?"":"not ") + rarities.get(0);
		} else if (rarities.size()==2) {
			return "rarity is " + (toRet?"either ":"neither ") + rarities.get(0) + (toRet?" or ":" nor ") + rarities.get(1);
		} else if (rarities.size()>2) {
			StringBuffer sb = new StringBuffer();
			// 0, 1, 2, 3, or 4
			for (int i=0; i<rarities.size(); i++) {
				if (i==rarities.size()-1) sb.append("or ");
				sb.append(rarities.get(i));
				if (i<rarities.size()-1) sb.append(", ");
			}
			return "rarity is " + (toRet?"":"none of ") + sb.toString();
		}
		return null;
	}

	
}
