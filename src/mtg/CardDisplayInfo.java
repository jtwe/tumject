package mtg;

public class CardDisplayInfo {
	private String fileName;
	private double theta;
	private int defX;
	private int defY;

	public String getFileName() {
		return fileName;
	}
	public double getTheta() {
		return theta;
	}
	public int getDefX() {
		return defX;
	}
	public int getDefY() {
		return defY;
	}
	public CardDisplayInfo(String fileName, double theta, int defX, int defY) {
		super();
		this.fileName = fileName;
		this.theta = theta;
		this.defX = defX;
		this.defY = defY;
	}
}

