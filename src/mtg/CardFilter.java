package mtg;

import javax.json.JsonObject;

public abstract class CardFilter {
	public enum Direction {
		GREATER_THAN ("greater than"), 
		GREATER_THAN_OR_EQUAL_TO ("greater than or equal to"), 
		EQUAL_TO ("equal to"), 
		LESS_THAN_OR_EQUAL_TO ("less than or equal to"), 
		LESS_THAN ("less than");
		
		private String description;
		
		Direction(String description) {
			this.description = description;
		}
		
		public String getDescription() {
			return description;
		}
	};
	
	public abstract boolean isOk(JsonObject cardObject);
	public boolean isOk(JsonObject cardObject, JsonObject setObject) {
		return isOk(cardObject);
	}
	
	protected String description;
	
	protected abstract String generateDescription();
	public String getDescription() {
		if ("".equals(description)) return null;
		if (description!=null) return this.description;
		return this.generateDescription();
	}
	
	public CardFilter setDescription(String description) {
		this.description = description;
		return this;
	}
	
}
