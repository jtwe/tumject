package mtg.filter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class FilterUniqueTokens extends CardFilter {
	private static Set<String> uniqueTokenCards = null;
	private boolean toRet = true;

	public FilterUniqueTokens(JsonObject jo, String directory) {
		this(jo, directory, true, false);
	}

	public FilterUniqueTokens(JsonObject jo, String directory, boolean vintageLegalOnly, boolean modernLegalOnly) {
		if (uniqueTokenCards==null) {
			if (jo==null) {
				try {
					JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(directory + "AllSets-x.json"), "UTF8"));
					jo = jr.readObject();
				} catch (UnsupportedEncodingException | FileNotFoundException e) {
					jo = null;
				}
			}

			if (jo!=null) {
				uniqueTokenCards = new HashSet<String>();
				boolean verbose = false, superverbose = false;

				Pattern p = Pattern.compile("([^ ]*?) counters?");
				Set<String> ignored = new HashSet<String>();
				ignored.addAll(Arrays.asList(new String[]
						{"a", "all", "and", "another", "be", "control", "each", "five", "had", "have", "is", "may", "more", "no", "spell", "that", "the", "those", "of", "was", "with", "would", "X"}
				));

				Set<String> cardNames = new HashSet<String>();
				Map<String, Integer> counterCounts = new HashMap<String, Integer>();
				Map<String, String> counterExamples = new HashMap<String, String>();
				for (String key : jo.keySet()) {
					JsonObject setObject = jo.getJsonObject(key);

					String setName = setObject.getString("name");
					String setType = setObject.getString("type");
					String setReleaseDate = setObject.getString("releaseDate");
					if ("promo".equals(setType)) continue;
					if (verbose) System.out.println(setName + ", " + setType);

					JsonArray cardsArray = setObject.getJsonArray("cards");
					for (int j=0; j<cardsArray.size(); j++) {
						JsonObject cardObject = cardsArray.getJsonObject(j);
						String cardName = cardObject.getString("name");

						String vintageLegality = null, modernLegality = null;
						try {
							JsonArray legalities = cardObject.getJsonArray("legalities");
							for (int i=0; i<legalities.size(); i++) {
								String format = legalities.getJsonObject(i).getString("format");
								if ("Vintage".equals(format)) vintageLegality = legalities.getJsonObject(i).getString("legality");
								if ("Modern".equals(format)) modernLegality = legalities.getJsonObject(i).getString("legality");
							}
							if (superverbose) System.out.println("Found??? " + cardName + ", " + vintageLegality);
						} catch (Exception e) {
							if (superverbose) System.out.println("Found??? " + cardName + " but exception in legality: " + e);
						}
/*
						if ("Kaladesh".equals(setName)) {
							vintageLegality = "Legal";
							modernLegality = "Legal";
						}
*/
						if (vintageLegalOnly && !("Legal".equals(vintageLegality) || "Restricted".equals(vintageLegality))) continue;
						if (modernLegalOnly && !("Legal".equals(modernLegality))) continue;

						if (cardNames.contains(cardName)) continue;
						cardNames.add(cardName);

						String cardText = "";
						try {
							cardText = cardObject.getString("text");
						} catch (Exception e) {
							//noop
						}
						
						if (superverbose) System.out.println("Found? " + cardName + ", " + cardText + ", " + cardText.contains("counter"));

						Matcher m = p.matcher(cardText);
						Set<String> thisCardCounters = null;
						while (m.find()) {
							String counterName = m.group(1);
							if (counterName.startsWith("(")) counterName = counterName.substring(1);
							if (counterName.endsWith(",")) continue;
							if (ignored.contains(counterName)) continue;
							if (thisCardCounters==null) thisCardCounters = new HashSet<String>();
							if (superverbose) System.out.println(" Found: " + cardName /*+ ", " + cardText + ", " + m.group() */+ ", " + counterName );
							thisCardCounters.add(counterName);
						}

						JsonArray types = cardObject.getJsonArray("types");
						for (int i=0; i<types.size(); i++) {
							if ("Planeswalker".equals(types.getString(i))) {
								if (thisCardCounters==null) thisCardCounters = new HashSet<String>();
								thisCardCounters.add("loyalty");
							}
						}
						
						if ("Frankenstein's Monster".equals(cardName)) {
							thisCardCounters.add("+2/+0");
							thisCardCounters.add("+1/+1");
						}
						
						if (thisCardCounters!=null) {
							if (verbose) System.out.print(" Found: " + cardName /*+ ", " + cardText + ", " + m.group() + ", " +m.group(1)*/ );
							for (String s : thisCardCounters) {
								if (verbose) System.out.print("; [" + s + "]");
								Integer counterCount = counterCounts.get(s);
								if (counterCount==null) counterCount = 0;
								counterCounts.put(s, 1+counterCount);
								if (!counterExamples.containsKey(s)) counterExamples.put(s, cardName/* + " (" + setName + ")"*/);
							}
							if (verbose) System.out.println();
						}

					}
				}

				if (verbose) System.out.println();
				for (String counter : counterCounts.keySet()) {
					String prefix = "";
//					if (counter.startsWith("+") || counter.startsWith("-")) prefix = "'";
					if (verbose) System.out.println(prefix + counter + "\t" + counterCounts.get(counter) + "\t" + counterExamples.get(counter));
					if (counterCounts.get(counter)==1) uniqueTokenCards.add(counterExamples.get(counter));
				}
				
				System.out.println("All cards: " + cardNames.size() + ", unique token cards: " + uniqueTokenCards.size());
			}
		}
	}

	public void invert() {
		this.toRet = !this.toRet;
	}

	@Override
	public boolean isOk(JsonObject cardObject) {
		if (uniqueTokenCards==null) return false;
		if (uniqueTokenCards.contains(cardObject.getString("name"))) return toRet;
		return !toRet;
	}

	@Override
	protected String generateDescription() {
		return "which create unique tokens";
	}

}
