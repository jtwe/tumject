package mtg;

import javax.json.JsonObject;

public class FilterCmc extends CardFilter {

	private Direction dir;
	private int num;
	
	public FilterCmc(Direction dir, int num) {
		this.dir = dir;
		this.num = num;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		int cmc = 0;
		try {
			cmc = cardObject.getInt("cmc");
		} catch (Exception e) {
		}
		if (dir.equals(Direction.GREATER_THAN) && cmc>num) return true;
		if (dir.equals(Direction.GREATER_THAN_OR_EQUAL_TO) && cmc>=num) return true;
		if (dir.equals(Direction.EQUAL_TO) && cmc==num) return true;
		if (dir.equals(Direction.LESS_THAN_OR_EQUAL_TO) && cmc<=num) return true;
		if (dir.equals(Direction.LESS_THAN) && cmc<num) return true;
		return false;
	}

	@Override
	public String generateDescription() {
		return "converted mana cost " + dir.getDescription() + " " + num;
	}

}
