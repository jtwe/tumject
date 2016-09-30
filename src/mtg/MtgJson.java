package mtg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import mtg.filter.*;
import mtg.filter.CardFilter.Direction;

import mtg.parser.*;

public class MtgJson {
// http://mtgjson.com/

	private String mtgDirectory;
	private String backgroundImageName;
	
	public MtgJson(String mtgDirectory, String backgroundImageName) {
		this.mtgDirectory = mtgDirectory;
		this.backgroundImageName = backgroundImageName;
	}
	
	public enum Spread {
		THREE_CARD (
				new double[]{-0.2d, 0.0d, 0.2d},
				new int[]{251, 502, 753},
				new int[]{258, 258, 258},
				new String[]{"The Past", "The Present", "The Future"}
			),
		CELTIC_CROSS (
				new double[]{0.0d, Math.PI/2, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d},
				new int[]{502, 502, 211, 793, 502,  502, 1045, 1045, 1045, 1045},
				new int[]{745, 745, 745, 745, 380, 1110,  218,  563,  928, 1273},
				new String[]{"The Present", "The Challenge", "The Past", "The Future", "Above", "Below", "Advice", "External Influences", "Hopes/Fears", "Outcome"}
			);
		
		private double[] thetas;
		private int[] defX;
		private int[] defY;
		private String[] captions;
		
		Spread(double[] thetas, int[] defX, int[] defY, String[] captions) {
			this.thetas = thetas;
			this.defX = defX;
			this.defY = defY;
			this.captions = captions;
		}

		public double[] getThetas() {
			return thetas;
		}

		public int[] getDefX() {
			return defX;
		}

		public int[] getDefY() {
			return defY;
		}

		public String[] getCaptions() {
			return captions;
		}
		
		public int getSize() {
			return captions.length;
		}
	};

	private boolean verbose = false;
	private boolean semiverbose = true;
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	public void setSemiverbose(boolean semiverbose) {
		this.semiverbose = semiverbose;
	}

	private String imageFilename;
	private StringBuffer caption = new StringBuffer();
	private CardFilter[] filters = null;
	
	public String getImageFilename() {
		return imageFilename;
	}

	public String getCaption() {
		return caption.toString().replaceAll("\n", "</p><p>").replaceAll(Character.toString((char)0x2013), "&ndash;").replaceAll(Character.toString((char)0x2014), "&mdash;")
				.replaceAll(Character.toString((char)0x2018), "&lsquo;").replaceAll(Character.toString((char)0x2019), "&rsquo;").replaceAll(Character.toString((char)0x2022), "&bull;")
				.replaceAll(Character.toString((char)0x2122), "&trade;").replaceAll(Character.toString((char)0x2212), "&minus;").replaceAll(Character.toString((char)0x221e), "&infin;");
	}

	public String getFilterDescriptions(boolean onOneLine) {
		StringBuffer sb = new StringBuffer();
		String sep = "";
		for (CardFilter cf : filters) {
			if (cf.getDescription()==null) continue;
			sb.append(sep + cf.getDescription());
			sep = onOneLine?" ":"\n";
		}
		return sb.toString();
	}

	private JsonObject jo = null;
	private CardFilter[] firstFilters = null;
	private boolean lockFilters = false;
	private double[] thetas = null;
	private int[] defX = null;
	private int[] defY = null;
	private String[] captions = null;

	public void setJsonObject(JsonObject jo) {
		this.jo = jo;
	}
	
	public void setFirstFilters(CardFilter[] firstFilters) {
		this.firstFilters = firstFilters;
	}
	
	public void setLockFilters(boolean lockFilters) {
		this.lockFilters = lockFilters;
	}
	
	public void setSpread(Spread spread) {
		this.thetas = spread.getThetas();
		this.defX = spread.getDefX();
		this.defY = spread.getDefY();
		this.captions = spread.getCaptions();
	}
	
	public void setThetas(double[] thetas) {
		this.thetas = thetas;
	}

	public void setDefX(int[] defX) {
		this.defX = defX;
	}

	public void setDefY(int[] defY) {
		this.defY = defY;
	}
	
	public void setCaptions(String[] captions) {
		this.captions = captions;
	}

	public void generatePost() throws IOException {
		List<Card> cards = new ArrayList<Card>();
		
		if (jo==null) {
			JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(mtgDirectory + "AllSets-x.json"), "UTF8"));
			jo = jr.readObject();
		}
		
		int maxSize = 150;

		while (cards.size()<52 || cards.size()>maxSize) {
			if (firstFilters!=null) {
				filters = firstFilters;
				if (lockFilters && maxSize>1000) firstFilters = null;
			} else {
				filters = chooseRandomFilters();
			}
			cards = this.getCards(jo, filters, false);
			if (semiverbose) {
				for (CardFilter filter : filters) if (filter.getDescription()!=null) System.out.println(filter.getDescription());
				System.out.println("Cards: " + cards.size());
				System.out.println();
			}
			maxSize+=25;
		}

		caption = new StringBuffer();
		caption.append("<p>Concentrate upon your question, and draw from a deck of " + cards.size() + " " + this.getFilterDescriptions(true) + "...");
		
		Collections.shuffle(cards);
		
		Random r = new Random();
		
		CardDisplayInfo[] cardDisplayInfos = new CardDisplayInfo[captions.length];

		for (int i=0; i<captions.length; i++) {
			Card c = cards.get(i);

			JsonObject cardObject = null;

			while (cardObject==null) {
				String set = c.getRandomSet(r);
				if (verbose) System.out.println("> " + c + ", picked " + set);
				cardObject = getCardObject(jo, c.getName(), set);
				try {
					cardObject.getInt("multiverseid");
				} catch (Exception e) {
					if (verbose) System.out.println("Wait, no.");
					cardObject = null;
				}
			}

			fetchImage(this.mtgDirectory, cardObject.getInt("multiverseid"));

			caption.append("<h2>" + captions[i] + "</h2>");
			caption.append("<p>" + cardObject.getString("name") + "</p>");
			if (verbose) System.out.println(cardObject.getString("name"));
			try {
				caption.append("<p>" + cardObject.getString("manaCost") + "</p>");
				if (verbose) System.out.println(cardObject.getString("manaCost"));
			} catch (Exception e) {
				if (verbose) System.out.println("(no mana cost)");
			}
			caption.append("<p>" + cardObject.getString("type") + "</p>");
			if (verbose) System.out.println(cardObject.getString("type"));
			/*
			try {
				System.out.println(cardObject.getInt("multiverseid"));
			} catch (Exception e1) {
				System.out.println("(no multiverseId???)");
			}
			*/
			try {
				caption.append("<p>" + cardObject.getString("text") + "</p>");
				if (verbose) System.out.println(cardObject.getString("text"));
			} catch (Exception e) {
				caption.append("<p>(no text)</p>");
				if (verbose) System.out.println("(no text)");
			}
			try {
				caption.append("<p>" + cardObject.getString("power") + " / " + cardObject.getString("toughness") + "</p>");
				if (verbose) System.out.println(cardObject.getString("power") + " / " + cardObject.getString("toughness"));
			} catch (Exception e) {
				// noop
			}
			try {
				caption.append("<p>{" + cardObject.getInt("loyalty") + "}</p>");
				if (verbose) System.out.println("{" + cardObject.getInt("loyalty") + "}");
			} catch (Exception e) {
				// noop
			}
//			if (verbose) System.out.println(cardObject);
			if (verbose) System.out.println();
			
			cardDisplayInfos[i] = new CardDisplayInfo(Integer.toString(cardObject.getInt("multiverseid")), thetas[i], defX[i], defY[i]);
		}
		
		TableDrawing td = new TableDrawing(mtgDirectory + "images/", backgroundImageName, cardDisplayInfos);
		this.imageFilename = td.generateImage();
		if (verbose) System.out.println(imageFilename);
	}

	protected static JsonObject getCardObject(JsonObject jo, String cardName, String set) {
		JsonObject setObject = jo.getJsonObject(set);
		JsonArray cardArray = setObject.getJsonArray("cards");
		for (int j=0; j<cardArray.size(); j++) {
			JsonObject cardObject = cardArray.getJsonObject(j);
			String cardObjectName = cardObject.getString("name");
			if (cardObjectName.equals(cardName)) {
				return cardObject;
			}
		}
		
		return null;
	}

	protected static void fetchImage(String mtgDirectory, int multiverseId) {
		File targetFile = new File(mtgDirectory + "images/temp/" + multiverseId + ".jpg");
		if (targetFile.exists()) return;
		
		try {
			URL url = new URL("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=" + multiverseId + "&type=card");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			FileOutputStream fos = new FileOutputStream(targetFile);
			boolean done = false;
			byte[] buffer = new byte[1024];
			while (!done) {
				int bytes = is.read(buffer);
				if (bytes>0) {
					fos.write(buffer, 0, bytes);
				} else {
					done = true;
				}
			}
			is.close();
			fos.flush();
			fos.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<Card> getCards(String filename, CardFilter[] filters) throws UnsupportedEncodingException, FileNotFoundException {
		JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		JsonObject jo = jr.readObject();
		return getCards(jo, filters);
	}

	private List<Card> getCards(JsonObject jo, CardFilter[] filters) throws UnsupportedEncodingException, FileNotFoundException {
		return getCards(jo, filters, true);
	}

	private List<Card> getCards(JsonObject jo, CardFilter[] filters, boolean verbose) throws UnsupportedEncodingException, FileNotFoundException {
		return getCards(jo, filters, verbose, false);
	}

	private List<Card> getCards(JsonObject jo, CardFilter[] filters, boolean verbose, boolean superverbose) throws UnsupportedEncodingException, FileNotFoundException {
		Map<String, CardAdv> nameToCardMap = new HashMap<String, CardAdv>();
		
		for (String setId : jo.keySet()) {
			JsonObject setObject = jo.getJsonObject(setId);
			
			JsonArray cardArray = setObject.getJsonArray("cards");
			if (verbose) System.out.print(setId + ", " + cardArray.size() + "; ");
			for (int j=0; j<cardArray.size(); j++) {
				JsonObject cardObject = (JsonObject)cardArray.get(j);
				
				if (isOk(cardObject, setId, setObject, filters)) {
					if (superverbose) {
						System.out.println(cardObject.toString());
					}
					String cardName = cardObject.getString("name");
					CardAdv card = nameToCardMap.get(cardName);
					if (card==null) {
						card = new CardAdv(cardName);
						nameToCardMap.put(cardName, card);
					}
					card.addSet(setId);
				}
			}
		}
		if (verbose) System.out.println();
		
		return new ArrayList<Card>(nameToCardMap.values());
	}

	private static boolean isOk(JsonObject cardObject, String setId, JsonObject setObject, CardFilter[] filters) {
		if (filters!=null) {
			for (CardFilter filter : filters) {
				if (!filter.isOk(cardObject, setObject)) return false;
			}
		}
		return true;
	}

	private CardFilter[] chooseRandomFilters() {
		return chooseRandomFilters(null, true);
	}
	
	private CardFilter[] chooseRandomFilters(Integer[] filterVals) {
		return chooseRandomFilters(filterVals, true);
	}
	
	private CardFilter[] chooseRandomFilters(Integer[] filterVals, boolean verbose) {
//		List<String> filterNames = Arrays.asList(new String[]{"cmc", "rarity", "color", "numColors", "legality", "type"});
//		Collections.shuffle(filterNames);

		Random r = new Random();
		
		List<CardFilter> toRet = new ArrayList<CardFilter>();

		/*
		int[] filterVals = new int[6];
		for (int i=0; i<4 || r.nextBoolean(); i++) {
			filterVals[r.nextInt(filterVals.length)]++;
		}
		*/
//		Integer[][] baseFilterVals = {{2, 1, 1, 0, 0, 0}, {2, 1, 0, 0, 0, 0}, {1, 1, 1, 0, 0, 0}, {1, 1, 0, 0, 0, 0}};
//		Integer[][] baseFilterVals = {{2, 0, 0, 0, 0, 0}, {2, 1, 0, 0, 0, 0}, {1, 1, 1, 0, 0, 0}, {1, 1, 0, 0, 0, 0}, {1, 1, 0, 0, 0, 0}, {1, 1, 0, 0, 0, 0}, {1, 0, 0, 0, 0, 0}};
//		Integer[][] baseFilterVals = {{2, 0, 0, 0, 0, 0}, {2, 1, 0, 0, 0, 0}, {2, 1, 1, 0, 0, 0}};
		Integer[][] baseFilterVals = {{2, 1, 0, 0, 0, 0}, {2, 1, 1, 0, 0, 0}, {2, 1, 1, 0, 0, 0}};

		while (filterVals==null) {
			filterVals = baseFilterVals[r.nextInt(baseFilterVals.length)];
			List<Integer> filterValList = new ArrayList<Integer>();
			for (Integer i : filterVals) filterValList.add(i);
			Collections.shuffle(filterValList);
			filterVals = filterValList.toArray(filterVals);
			
			if (filterVals[2]==2) filterVals[3] = 0;
			
			if (filterVals[0]==1 && r.nextBoolean()) filterVals = null;
		}

		if (verbose) {
			System.out.print("Filters: ");
			for (Integer i : filterVals) System.out.print(i + " ");
			System.out.println();
		}

		// 1: rarity
		if (filterVals[1]>1) {
			toRet.add(new FilterRarity("Mythic Rare").setDescription("mythic rare"));
			toRet.add(new FilterSetType("core", "expansion").setDescription(""));
		} else if (filterVals[1]==1) {
			if (r.nextBoolean()) {
				if (r.nextBoolean()) {
					toRet.add(new FilterRarity("Common", "Uncommon").setDescription("common or uncommon"));
				} else {
					toRet.add(new FilterRarity("Uncommon", "Rare", "Mythic Rare").setDescription("uncommon, rare, or mythic rare"));
				}
			} else {
				int x = r.nextInt(3);
				switch (x) {
				case 0:
					toRet.add(new FilterRarity("Rare").setDescription("rare"));
					break;
				case 1:
					toRet.add(new FilterRarity("Uncommon").setDescription("uncommon"));
					break;
				case 2:
					toRet.add(new FilterRarity("Common").setDescription("common"));
					break;
				}
			}
		}

		// 3: num colors
		if (filterVals[3]>1) {
			if (r.nextBoolean()) {
				toRet.add(new FilterColorCount(CardFilter.Direction.EQUAL_TO, 2).setDescription("two colored"));
			} else {
				toRet.add(new FilterColorCount(CardFilter.Direction.GREATER_THAN_OR_EQUAL_TO, 3).setDescription("three or more colored"));
			}
		} else if (filterVals[3]==1) {
			if (r.nextBoolean()) {
				if (r.nextBoolean()) {
					toRet.add(new FilterColorCount(CardFilter.Direction.GREATER_THAN_OR_EQUAL_TO, 2).setDescription("multicolored"));
				} else {
					toRet.add(new FilterColorCount(CardFilter.Direction.LESS_THAN_OR_EQUAL_TO, 1).setDescription("non-multicolored"));
				}
			} else {
				if (r.nextBoolean()) {
					toRet.add(new FilterColorCount(CardFilter.Direction.EQUAL_TO, 1).setDescription("monocolored"));
				} else {
					toRet.add(new FilterColorCount(CardFilter.Direction.EQUAL_TO, 0).setDescription("colorless"));
				}
			}
		}

		// 2: color
		List<String> colors = Arrays.asList("White", "Blue", "Black", "Red", "Green");
		Collections.shuffle(colors);
		if (filterVals[2]>1) {
			toRet.add(new FilterColor(colors.get(0)).setDescription(colors.get(0).toLowerCase() + " and " + colors.get(1).toLowerCase()));
			toRet.add(new FilterColor(colors.get(1)).setDescription(""));
//			toRet.add(new FilterColor(colors.get(2), colors.get(3), colors.get(4)).invert().setDescription(""));
		} else if (filterVals[2]==1) {
			if (r.nextBoolean()) {
				toRet.add(new FilterColor(colors.get(0)).setDescription(colors.get(0).toLowerCase()));
				toRet.add(new FilterColor(colors.get(1), colors.get(2), colors.get(3), colors.get(4)).invert().setDescription(""));
			} else {
				toRet.add(new FilterColor(colors.get(0), colors.get(1)).setDescription(colors.get(0).toLowerCase() + " or " + colors.get(1).toLowerCase()));
				toRet.add(new FilterColor(colors.get(2), colors.get(3), colors.get(4)).invert().setDescription(""));
			}
		}

		// 4: Legality
		if (filterVals[4]>1) {
			String[][] blockSets = {
					{"early set", "LEA", "LEB", "2ED", "ARN", "ATQ", "3ED", "LEG", "DRK", "FEM", "4ED", "HML"},
					{"Ice Age block", "ICE", "ALL", "CSP"},
					{"Mirage block", "MIR", "VIS", "WEA"},
					{"Tempest block", "TMP", "STH", "EXO"},
					{"Urza block", "USG", "ULG", "UDS"},
					{"Masques block", "MMQ", "NMS", "PCY"},
					{"Invasion block", "INV", "PLS", "APC"},
					{"Odyssey block", "ODY", "TOR", "JUD"},
					{"Onslaught block", "ONS", "LGN", "SCG"},
					{"Mirrodin block", "MRD", "DST", "5DN"},
					{"Kamigawa block", "CHK", "BOK", "SOK"},
					{"Ravnica block", "RAV", "GPT", "DIS"},
					{"Time Spiral block", "TSP", "TSB", "PLC", "FUT"},
					{"Lorwyn-Shadowmoor block", "LRW", "MOR", "SHM", "EVE"},
					{"Shards of Alara block", "ALA", "CON", "ARB"},
					{"Zendikar block", "ZEN", "WWK", "ROE"},
					{"Scars of Mirrodin block", "SOM", "MBS", "NPH"},
					{"Innistrad block", "ISD", "DKA", "AVR"},
					{"Return to Ravnica block", "RTR", "GTC", "DGM"},
					{"Theros block", "THS", "BNG", "JOU"},
					{"Tarkir block", "KTK", "FRF", "DTK"},
					{"Battle for Zendikar block", "BFZ", "OGW", "EXP"},
			};
			String[] block = blockSets[r.nextInt(blockSets.length)];
			List<String> sets = new ArrayList<String>();
			for (int i=1; i<block.length; i++) sets.add(block[i]);
			toRet.add(new FilterSet(sets).setDescription("" + block[0]));
//			toRet.add(new FilterLegality(blocks[r.nextInt(blocks.length)]));
		} else if (filterVals[4]==1) {
			if (r.nextBoolean()) {
				toRet.add(new FilterLegality("Modern").setDescription("Modern-legal"));
			} else {
				if (r.nextBoolean()) {
					toRet.add(new FilterLegality("Standard").setDescription("Standard-legal"));
				} else {
					toRet.add(new FilterLegality("Modern").invert().setDescription("Vintage, but not Modern, legal"));
					toRet.add(new FilterLegality("Vintage").setDescription(""));
				}
			}
		}

		// 5: Type
		if (filterVals[5]>1) {
			String[] types = {"Enchantment", "Instant", "Human", "Sorcery", "Artifact", "Aura", "Legendary", "Land"};
			if (r.nextInt(3)==0) types = new String[]{"Elemental", "Spirit", "Zombie", "Goblin", "Elf", "Beast", "Equipment", "Bird", "Merfolk", "Dragon", "Insect", "Cat", "Giant", "Angel", "Vampire", "Horror", "Planeswalker"};
			String type = types[r.nextInt(types.length)];
/*
			String a = "a";
			if (type.startsWith("A") || type.startsWith("E") || type.startsWith("I") || type.startsWith("O") || type.startsWith("U")) a = "an";
*/
			toRet.add(new FilterCardType(type).setDescription(type));
		} else if (filterVals[5]==1) {
			if (r.nextBoolean()) {
				if (r.nextBoolean()) {
					toRet.add(new FilterCardType("Creature").setDescription("Creature"));
				} else {
					toRet.add(new FilterCardType("Creature").invert().setDescription("non-Creature"));
				}
			} else {
				if (r.nextBoolean()) {
					toRet.add(new FilterCardType("Instant", "Sorcery").setDescription("Instant or Sorcery"));
				} else {
					toRet.add(new FilterCardType("Enchantment", "Artifact").setDescription("Enchantment or Artifact"));
				}
			}
		}

		toRet.add(new FilterIsRealCard().setDescription("cards"));

		// 0: CMC
		if (filterVals[0]>1) {
			int cmc = r.nextInt(5)+r.nextInt(5);
			toRet.add(new FilterCmc(CardFilter.Direction.EQUAL_TO, cmc).setDescription("with CMC equal to " + cmc));
		} else if (filterVals[0]==1) {
			int x = r.nextInt(3);
			switch (x) {
			case 0: 
				toRet.add(new FilterCmc(CardFilter.Direction.LESS_THAN_OR_EQUAL_TO, 2).setDescription("with CMC less than 3"));
				break;
			case 1:
				toRet.add(new FilterCmc(CardFilter.Direction.GREATER_THAN_OR_EQUAL_TO, 3).setDescription("with CMC between 3 and 5"));
				toRet.add(new FilterCmc(CardFilter.Direction.LESS_THAN_OR_EQUAL_TO, 5).setDescription(""));
				break;
			case 2:
				toRet.add(new FilterCmc(CardFilter.Direction.GREATER_THAN_OR_EQUAL_TO, 6).setDescription("with CMC greater than 5"));
				break;
			}
		}
		
		return toRet.toArray(new CardFilter[]{});
	}
	
	public static CardFilter[] choosePresetFilters(Properties prop) {
//		return new CardFilter[]{new FilterIsRealCard(), new FilterSupplemental(null, prop.getProperty("mtg.directory"))};
//		return new CardFilter[]{new FilterVanilla(), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterSet("BFZ", "OGW", "EXP"), new FilterCardType("Enchantment", "Artifact", "Land")};
//		return new CardFilter[]{new FilterSet("ISD", "DKA", "AVR").setDescription("Innistrad block"), new FilterLayout("double-faced").setDescription("double-faced"), new FilterIsRealCard()}; 
//		return new CardFilter[]{new FilterSnowy(), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterCardType("Planeswalker").setDescription("Planeswalker"), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterCardType("Sliver").setDescription("Sliver"), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterRomantic(), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterCardType("Land").setDescription("Land"), new FilterIsRealCard(), new FilterArtist("John Avon")}; 
//		return new CardFilter[]{new FilterIsRealCard(), new FilterArtist("Richard Kane Ferguson")}; // or Christopher Rush, Wayne Reynolds, Drew Tucker, Kaja Foglio, Richard Kane Ferguson
//		return new CardFilter[]{new FilterSet("UGL", "UNH").setDescription("foolish cards")};
//		return new CardFilter[]{new FilterColorCount(Direction.GREATER_THAN_OR_EQUAL_TO, 2).setDescription("multicolored"), new FilterSet("KTK", "FRF").setDescription("Khans of Tarkir / Fate Reforged"), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterSet("CHR").setDescription("Chronicles"), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterCardTitle("time", "temporal", "hour", "day", "year").setDescription("time-related"), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterCardText("Madness {", "Delirium —").setDescription("madness or delirium"), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterLegality("Modern").invert().setDescription("non-Modern legal"), new FilterLegality("Vintage").setDescription(""), new FilterSet("EMA").setDescription("Eternal Masters"), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterSetType("core", "expansion", "starter").setDescription(""), new FilterLegality("Legacy").invert().setDescription("Legacy illegal"), new FilterIsRealCard().setVintageLegalityRequired(false)};
//		String watermark = "Azorius";
//		return new CardFilter[]{new FilterWatermark(watermark).setDescription(watermark), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterCardText("doesn't untap").setDescription("that don't untap")};
//		return new CardFilter[]{new FilterColorCount(Direction.EQUAL_TO, 0), new FilterCardType("Eldrazi"), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterCreator(), new FilterIsRealCard()};
//		return new CardFilter[]{new FilterSet("EXP", "MPS").setDescription("Masterpieces")};
		return new CardFilter[]{new FilterIsRealCard(), new FilterUniqueTokens(null, prop.getProperty("mtg.directory"))};
	}
	
	private static CardFilter[] chooseSetFilters(Properties prop) {
//		return new CardFilter[]{new FilterIsRealCard()};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterSet("BFZ", "OGW", "EXP"), new FilterCardType("Instant", "Sorcery")};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterSet("BFZ", "OGW", "EXP"), new FilterCardType("Creature", "Planeswalker")};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterSet("BFZ", "OGW", "EXP"), new FilterCardType("Enchantment", "Artifact", "Land")};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterSet("USG", "ULG", "UDS"), new FilterColorCount(Direction.GREATER_THAN_OR_EQUAL_TO, 2)};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterCmc(Direction.LESS_THAN_OR_EQUAL_TO, 2), new FilterColor("Blue").invert(), new FilterColorCount(Direction.LESS_THAN_OR_EQUAL_TO, 1), new FilterCardType("Land")}
//		return new CardFilter[]{new FilterIsRealCard(), new FilterCardType("Legendary"), new FilterCardType("Creature").invert()};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterCardType("Legendary", "World"), new FilterCardType("Land", "Enchantment")};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterCmc(FilterCmc.Direction.GREATER_THAN, 3), new FilterCmc(FilterCmc.Direction.LESS_THAN, 5), new FilterTwoColorRed()};

//		return new CardFilter[]{new FilterIsRealCard(), new FilterRarity("Mythic Rare").setDescription("mythic rare"), new FilterSetType("reprint")}; //new FilterSetType("core", "expansion")};
//		return new CardFilter[]{new FilterIsRealCard(), new FilterSetType("box")}; 
//		return new CardFilter[]{new FilterRomantic(), new FilterIsRealCard()};
		return choosePresetFilters(prop);
	}
	
	////////////////////////////////

	private static Properties getPropertiesAsProperties(String propertyFileName) throws IOException {
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));
		return prop;
	}

	private static String[] getProperties(String propertyFileName) throws IOException {
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));
		return new String[]{prop.getProperty("mtg.directory"), prop.getProperty("background.image")};
	}

	public static void maine1(String[] args) throws IOException {
		String[] props = getProperties(args[0]);
		MtgJson mj = new MtgJson(props[0], props[1]);
		
		for (int i=0; i<3; i++) {
			mj.setSpread(Spread.THREE_CARD);
			mj.generatePost();
			System.out.println(mj.getCaption());
		}
	}

	
	public static void maine2(String[] args) throws IOException {
		String[] props = getProperties(args[0]);
		MtgJson mj = new MtgJson(props[0], props[1]);

		JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(props[0] + "AllSets-x.json"), "UTF8"));
		JsonObject jo = jr.readObject();
		
		for (int i=0; i<3; i++) {
			mj.setSpread(Spread.THREE_CARD);
			mj.setJsonObject(jo);
			mj.setFirstFilters(new CardFilter[]{new FilterIsRealCard(), new FilterSet("BFZ") });
			mj.generatePost();
			System.out.println(mj.getCaption());
		}
	}
	
	public static void main(String[] args) throws IOException {
		// The main main method, for testing the filters.
		Properties prop = getPropertiesAsProperties(args[0]);
		String[] props = getProperties(args[0]);
		MtgJson mj = new MtgJson(props[0], props[1]);

		JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(props[0] + "AllSets-x.json"), "UTF8"));
		JsonObject jo = jr.readObject();

		mj.setSpread(Spread.THREE_CARD);
		List<Card> cards = mj.getCards(jo, chooseSetFilters(prop), false, false);
		int i=0; 
		Collections.sort(cards, new Comparator<Card>(){
			@Override
			public int compare(Card arg0, Card arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}});
		for (Card card : cards) {
			i++;
			System.out.println(i + ": " + card);
		}
		if (i==0) System.out.println("No results.");
	}

	public static void mainListSets(String[] args) throws IOException {
		// List all the sets. 
		String[] props = getProperties(args[0]);
		MtgJson mj = new MtgJson(props[0], props[1]);

		JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(props[0] + "AllSets-x.json"), "UTF8"));
		JsonObject jo = jr.readObject();
		
		for (String setId : jo.keySet()) {
			JsonObject setObject = jo.getJsonObject(setId);
			System.out.println(setId + ", " + setObject.getString("name") + ", " + setObject.getJsonArray("cards").size() + ", " + setObject.getString("type"));
		}
	}

	static class CountedString implements Comparable {
		private String val;
		private Integer count;

		public CountedString(String val) {
			super();
			this.val = val;
			this.count = 0;
		}
		
		public void increment() {
			this.count++;
		}

		@Override
		public int compareTo(Object b) {
			CountedString that = (CountedString)b;
			if (this.count!=that.count) return that.count.compareTo(this.count);
			return this.val.compareTo(that.val);
		}

		@Override
		public String toString() {
			return val + "\t" + count;
		}
	}
	
	public static void mainCountStuff(String[] args) throws IOException {
		// For a given filter, makes a hashmap based on a ceratain key, so, like, to count the number of X colored cards in Alara block, or the number of cards with X watermark, or whatever
/*
		CardFilter[] cardFilter = new CardFilter[]{new FilterIsRealCard(), new FilterLegality("Shards of Alara Block")};
		CardParser parser = new ParserColorCount();
*/

		CardFilter[] cardFilter = new CardFilter[]{new FilterIsRealCard()};
		CardParser parser = new ParserWatermark();
		
		boolean showSets = false;
		boolean cardUnique = true;

		countArbitraryValue(args, cardFilter, parser, showSets, cardUnique);
	}

	public static void countArbitraryValue(String[] args, CardFilter[] cardFilter, CardParser parser, boolean showSets, boolean cardUnique) throws IOException {
		String[] props = getProperties(args[0]);

		String filename = props[0] + "AllSets-x.json";
		JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		JsonObject jo = jr.readObject();

		Set<String> cardsSeen = new HashSet<String>();
		Map<String, CountedString> countMap = new HashMap<String, CountedString>();
		
		for (String setId : jo.keySet()) {
			JsonObject setObject = jo.getJsonObject(setId);

			JsonArray cardArray = setObject.getJsonArray("cards");
			if (showSets) System.out.println(setId + ", " + cardArray.size());
			for (int j=0; j<cardArray.size(); j++) {
				JsonObject cardObject = (JsonObject)cardArray.get(j);

				if (!isOk(cardObject, setId, setObject, cardFilter)) continue;

				String cardName = cardObject.getString("name");
				String[] cardKeys = parser.parseCard(cardObject);

				if (cardUnique && cardsSeen.contains(cardName)) {
					// nope
				} else {
					if (cardKeys!=null) {
						for (String cardKey : cardKeys) {
							cardsSeen.add(cardName);
							CountedString count = countMap.get(cardKey);
							if (count==null) {
								count = new CountedString(cardKey);
							}
							count.increment();
							countMap.put(cardKey, count);
						}
					}
				}
			}
		}

		List<CountedString> l = new ArrayList<CountedString>();
		l.addAll(countMap.values());
		Collections.sort(l);

		System.out.println();
		for (CountedString cs : l) {
			System.out.println(cs);
		}
		
	}

	public static void mainMonteCarlo(String[] args) throws IOException {
		// Do 100 tests for each primary/secondary random filter combination and count how many end up with a right number of cards.
		String[] props = getProperties(args[0]);
		MtgJson mj = new MtgJson(props[0], props[1]);

		JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(props[0] + "AllSets-x.json"), "UTF8"));
		JsonObject jo = jr.readObject();

		int trials = 100;

		for (int i=0; i<6; i++) {
			for (int j=0; j<6; j++) {
				if (i==j) continue;
				Integer[] filterVals = new Integer[6];
				for (int a=0; a<6; a++) filterVals[a] = 0;
				filterVals[i] = 2;
				filterVals[j] = 1;

				int passed = 0;
				for (int a=0; a<trials; a++) {
					CardFilter[] filters = mj.chooseRandomFilters(filterVals, a==0);
					List<Card> l = mj.getCards(jo, filters, false);
					if (l.size()>52) {
						if (l.size()<201) {
							passed++;
							System.out.print("+");
						} else {
							System.out.print("*");
						}
					} else {
						System.out.print("-");
					}
				}
				System.out.println();
				System.out.println("Passed: " + passed);
			}

			System.out.println();

		}
	}

	public static void mainMonteCarloStats(String[] args) throws IOException {
		// Do 100 tests for each primary/secondary random filter combination and count how many end up with a right number of cards, plus statistics
		String[] props = getProperties(args[0]);
		MtgJson mj = new MtgJson(props[0], props[1]);

		JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(props[0] + "AllSets-x.json"), "UTF8"));
		JsonObject jo = jr.readObject();

		int trials = 100;

		for (int i=0; i<6; i++) {
			for (int j=0; j<6; j++) {
				if (i==j) continue;
				ArrayList<Integer> sizeList = new ArrayList<Integer>();
				int minSize = Integer.MAX_VALUE, totalSize = 0, maxSize = 0;
				
				Integer[] filterVals = new Integer[6];
				for (int a=0; a<6; a++) filterVals[a] = 0;
				filterVals[i] = 2;
				filterVals[j] = 1;

				int passed = 0;
				for (int a=0; a<trials; a++) {
					CardFilter[] filters = mj.chooseRandomFilters(filterVals, a==0);
					List<Card> l = mj.getCards(jo, filters, false);
					if (l.size()>52) {
						if (l.size()<201) {
							passed++;
							System.out.print("+");
						} else {
							System.out.print("*");
						}
					} else {
						System.out.print("-");
					}
					sizeList.add(l.size());
					totalSize+=l.size();
					if (l.size()<minSize) minSize = l.size();
					if (l.size()>maxSize) maxSize = l.size();
				}
				Collections.sort(sizeList);
				
				System.out.println();
				System.out.println("Passed: " + passed);
				System.out.println("Min: " + minSize + ", max: " + maxSize + ", median: " + sizeList.get(49) + ", mean: " + (totalSize/100) + ", std. dev.: " + stdDev(sizeList, (double)(totalSize/100)));
			}

			System.out.println();

		}
	}

	private static double stdDev(ArrayList<Integer> intList, double mean) {
		double totalDev = 0;
		for (Integer i : intList) totalDev += ((double)i-mean)*((double)i-mean);
		double variance = totalDev / (double)intList.size();
		return Math.sqrt(variance);
	}
	
	public static void main4(String[] args) throws IOException {
		String[] props = getProperties(args[0]);

		Spread spread = Spread.THREE_CARD;
		CardDisplayInfo[] cardDisplayInfos = new CardDisplayInfo[spread.getSize()];
		for (int i=0; i<spread.getSize(); i++) {
			cardDisplayInfos[i] = new CardDisplayInfo("0", spread.getThetas()[i], spread.getDefX()[i], spread.getDefY()[i]);
		}
		TableDrawing td = new TableDrawing(props[0] + "images/", props[1], cardDisplayInfos);
		System.out.println(td.generateImage());
	}
	

	public static void mainArtist(String[] args) throws IOException {
		// Counts artists
		String[] props = getProperties(args[0]);

		String filename = props[0] + "AllSets-x.json";
		JsonReader jr = Json.createReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		JsonObject jo = jr.readObject();

		CardFilter[] cardFilter = new CardFilter[]{new FilterIsRealCard()};
		
		Set<String> seenImages = new HashSet<String>();
		Map<String, Set<String>> artistCounts = new HashMap<String, Set<String>>();

		for (String setId : jo.keySet()) {
			JsonObject setObject = jo.getJsonObject(setId);

			JsonArray cardArray = setObject.getJsonArray("cards");

			for (int j=0; j<cardArray.size(); j++) {
				JsonObject cardObject = (JsonObject)cardArray.get(j);

				if (!isOk(cardObject, setId, setObject, cardFilter)) continue;

				String imageName = cardObject.getString("imageName");
				if (seenImages.contains(imageName)) continue;
				seenImages.add(imageName);
				
				String cardName = cardObject.getString("name");
				String artistName = cardObject.getString("artist");
				Set<String> artistCount = artistCounts.get(artistName);
				if (artistCount==null) {
					artistCount = new HashSet<String>();
					artistCounts.put(artistName, artistCount);
				}
				artistCount.add(cardName);
			}
		}

		List<Object[]> artistCountList = new ArrayList<Object[]>();
		for (String key : artistCounts.keySet()) artistCountList.add(new Object[]{key, artistCounts.get(key).size()});
		artistCountList.sort(new Comparator<Object[]>(){
			@Override
			public int compare(Object[] o1, Object[] o2) {
				Integer i1 = (Integer)o1[1], i2 = (Integer)o2[1];
				if (!i1.equals(i2)) return i2.compareTo(i1);
				String s1 = (String)o1[0], s2 = (String)o2[0];
				return s1.compareTo(s2);
			}});
		
		for (Object[] oa : artistCountList) {
			System.out.println(oa[0] + ", " + oa[1]);
		}
	}


}
