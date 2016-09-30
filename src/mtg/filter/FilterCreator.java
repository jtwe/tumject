package mtg.filter;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.regex.*;

public class FilterCreator extends CardFilter {

	private static Pattern p = Pattern.compile("put a.*creature token.*onto the battlefield", Pattern.CASE_INSENSITIVE);
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
			if (cardText == null || cardText.length()==0) return false; // pretty sure this never happens, but hey
			Matcher m = p.matcher(cardText);
			if (m.find()) return true; 
		} catch (Exception e) {
			return false;
		}
		
		return false;
	}

	@Override
	protected String generateDescription() {
		// TODO Auto-generated method stub
		return "laboring creature";
	}

}
