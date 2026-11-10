package tests.fixtures;

public record PrintRun(int[] copies, boolean[] approved) {
	public PrintRun {
		copies = copies.clone();
		approved = approved.clone();
	}

	@Override
	public int[] copies() {
		return copies.clone();
	}

	@Override
	public boolean[] approved() {
		return approved.clone();
	}
}
