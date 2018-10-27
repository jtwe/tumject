package mtg.filter;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

public class FilterSet extends CardFilter {
	private boolean toRet = true;
	private List<String> sets;

	public FilterSet(String... sets) {
		this.sets = new ArrayList<String>();
		for (String set : sets) this.sets.add(set);
	}
	
	public FilterSet(List<String> sets) {
		this.sets = sets;
	}
	
	public FilterSet invert() {
		toRet = !toRet;
		return this;
	}
	
	@Override
	public boolean isOk(JsonObject cardObject) {
		throw new IllegalStateException("Need a setId!");
		/*
		if (cardObject.getInt("multiverseid")==45301) {
			System.out.println(); 
			System.out.println(cardObject);
		}
		return true;
		*/
	}
	
	@Override
	public boolean isOk(JsonObject cardObject, JsonObject setObject, String setId) {
//		String setId = setObject.getString("code");
		if (sets.contains(setId)) return toRet;
		return !toRet;
	}

	@Override
	protected String generateDescription() {
		StringBuffer sb = new StringBuffer();
		for (String s : sets) sb.append(", " + s);		
		return (toRet?"":"not ") + "in set " + sb.toString().substring(2);
	}

}
