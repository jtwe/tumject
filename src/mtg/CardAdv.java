package mtg;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CardAdv implements Card {
	private String name;
	private List<String> sets;
	
	public CardAdv(String name) {
		this.name = name;
		this.sets = new ArrayList<String>();
	}
	
	public String getName() {
		return name;
	}
	
	public String getRandomSet(Random r) {
		return sets.get(r.nextInt(sets.size()));
	}
	
	public boolean addSet(String set) {
		if (sets.contains(set)) {
			return false;
		} else {
			sets.add(set);
			return true;
		}
	}
	
	public String toString() {
		return name + " (" + sets.toString() + ")";
	}
}

