package mtg;

import javax.json.JsonObject;

public class FilterColorCount extends CardFilter {

	private Direction dir;
	private int num;
	
	public FilterColorCount(Direction dir, int num) {
		this.dir = dir;
		this.num = num;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		int numCol = 0;
		try {
			numCol = cardObject.getJsonArray("colors").size();
		} catch (Exception e) {
		}
		if (dir.equals(Direction.GREATER_THAN) && numCol>num) return true;
		if (dir.equals(Direction.GREATER_THAN_OR_EQUAL_TO) && numCol>=num) return true;
		if (dir.equals(Direction.EQUAL_TO) && numCol==num) return true;
		if (dir.equals(Direction.LESS_THAN_OR_EQUAL_TO) && numCol<=num) return true;
		if (dir.equals(Direction.LESS_THAN) && numCol<num) return true;
		return false;
	}

	
	@Override
	public String generateDescription() {
		return "number of colors " + dir.getDescription() + " " + num;
	}

	
}
