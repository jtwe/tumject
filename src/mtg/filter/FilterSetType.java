package mtg.filter;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

public class FilterSetType extends CardFilter {
	private boolean toRet = true;

	private List<String> setTypes;

	public FilterSetType(String... setTypes) {
		this.setTypes = new ArrayList<String>();
		for (String setType : setTypes) this.setTypes.add(setType);
	}
	
	public FilterSetType(List<String> setTypes) {
		this.setTypes = setTypes;
	}
	
	public FilterSetType invert() {
		toRet = !toRet;
		return this;
	}
	

	@Override
	public boolean isOk(JsonObject cardObject) {
		throw new IllegalStateException("Need a setObject!");
	}
	
	@Override
	public boolean isOk(JsonObject cardObject, JsonObject setObject) {
		String setType = "";
		try {
			setType = setObject.getString("type");
		} catch (Exception e) {
		}
		if (setTypes.contains(setType)) return toRet;
		return !toRet;
	}

	@Override
	protected String generateDescription() {
		StringBuffer sb = new StringBuffer();
		for (String s : setTypes) sb.append(", " + s);		
		return (toRet?"":"not ") + "in type of set " + sb.toString().substring(2);
	}

}
