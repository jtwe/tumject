package mtg.filter;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterVanilla extends CardFilter {

	private static boolean firstPost = false;
	
	@Override
	public boolean isOk(JsonObject cardObject) {
		if (firstPost) {
			System.out.println(cardObject);
			for (String key : cardObject.keySet()) {
				System.out.println(key + " : " + cardObject.get(key));
			}
			
			firstPost = false;
		}
		
		JsonArray cardTypes = cardObject.getJsonArray("types");
		if (cardTypes==null) return false;
		boolean isCreature = false;
		for (int i=0; i<cardTypes.size(); i++) if (cardTypes.getString(i).equals("Creature")) isCreature = true;
		
		if (!isCreature) return false;

		try {
			String cardText = cardObject.getString("text");
			if (cardText == null || cardText.length()==0) return true; // pretty sure this never happens, but hey
			if (cardText.startsWith("(") && cardText.endsWith(")") && cardText.indexOf(")")==cardText.length()-1 ) return true; // the "Dryad Arbor Rule"
		} catch (Exception e) {
			return true;
		}
		
		return false;
	}

	@Override
	protected String generateDescription() {
		// TODO Auto-generated method stub
		return "vanilla creature";
	}

}
