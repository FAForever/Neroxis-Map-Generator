package map;

import java.util.Arrays;
import java.util.Random;

import util.Vector2f;

public strictfp class BinaryMask {
	private boolean[][] mask;
	private Random random;
	private Symmetry symmetry = Symmetry.POINT;

	public BinaryMask(int size, long seed) {
		mask = new boolean[size][size];
		random = new Random(seed);
	}

	public BinaryMask(BinaryMask mask, long seed) {
		this.mask = new boolean[mask.getSize()][mask.getSize()];
		for (int y = 0; y < mask.getSize(); y++) {
			for (int x = 0; x < mask.getSize(); x++) {
				this.mask[x][y] = mask.get(x, y);
			}
		}
		random = new Random(seed);
	}
	
	public BinaryMask(boolean[][] mask, long seed) {
		this.mask = mask;
		random = new Random(seed);
	}

	public int getSize() {
		return mask[0].length;
	}

	public boolean get(int x, int y) {
		return mask[x][y];
	}

	private void applySymmetry() {
		switch (symmetry) {
		case POINT:
			for (int y = 0; y < getSize() / 2; y++) {
				for (int x = 0; x < getSize(); x++) {
					mask[getSize() - x - 1][getSize() - y - 1] = mask[x][y];
				}
			}
			break;
		default:
			break;
		}
	}

	public BinaryMask randomize(float density) {
		for (int y = 0; y < getSize(); y++) {
			for (int x = 0; x < getSize(); x++) {
				mask[x][y] = random.nextFloat() < density;
			}
		}
		applySymmetry();
		return this;
	}

	public BinaryMask invert() {
		for (int y = 0; y < getSize(); y++) {
			for (int x = 0; x < getSize(); x++) {
				mask[x][y] = !mask[x][y];
			}
		}
		return this;
	}

	public BinaryMask enlarge(int size) {
		boolean[][] largeMask = new boolean[size][size];
		int smallX;
		int smallY;
		for (int y = 0; y < size; y++) {
			smallY = StrictMath.min(y / (size / getSize()), getSize() - 1);
			for (int x = 0; x < size; x++) {
				smallX = StrictMath.min(x / (size / getSize()), getSize() - 1);
				largeMask[x][y] = mask[smallX][smallY];
			}
		}
		mask = largeMask;
		return this;
	}

	public BinaryMask shrink(int size) {
		boolean[][] smallMask = new boolean[size][size];
		int largeX;
		int largeY;
		for (int y = 0; y < size; y++) {
			largeY = (y * getSize()) / size + (getSize() / size / 2);
			if (largeY >= getSize())
				largeY = getSize() - 1;
			for (int x = 0; x < size; x++) {
				largeX = (x * getSize()) / size + (getSize() / size / 2);
				if (largeX >= getSize())
					largeX = getSize() - 1;
				smallMask[x][y] = mask[largeX][largeY];
			}
		}
		mask = smallMask;
		return this;
	}

	public BinaryMask inflate(float radius) {
		return deflate(-radius);
	}

	public BinaryMask deflate(float radius) {
		boolean[][] maskCopy = new boolean[getSize()][getSize()];

		boolean inverted = radius <= 0;

		Thread[] threads = new Thread[4];
		threads[0] = new Thread(() -> deflateRegion(inverted, StrictMath.abs(radius), maskCopy, 0, (getSize() / 4)));
		threads[1] = new Thread(() -> deflateRegion(inverted, StrictMath.abs(radius), maskCopy, (getSize() / 4), (getSize() / 2)));
		threads[2] = new Thread(() -> deflateRegion(inverted, StrictMath.abs(radius), maskCopy, (getSize() / 2), (getSize() / 4) * 3));
		threads[3] = new Thread(() -> deflateRegion(inverted, StrictMath.abs(radius), maskCopy, (getSize() / 4) * 3, getSize()));
		Arrays.stream(threads).forEach(Thread::start);

		for (Thread f : threads) {
			try {
				f.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mask = maskCopy;
		return this;
	}

	private void deflateRegion(boolean inverted, float radius, boolean[][] maskCopy, int startY, int endY) {
		float radius2 = (radius + 0.5f) * (radius + 0.5f);
		for (int y = startY; y < endY; y++) {
			for (int x = 0; x < getSize(); x++) {
				maskCopy[x][y] = !inverted;
				l: for (int y2 = (int) (y - radius); y2 < y + radius + 1; y2++) {
					for (int x2 = (int) (x - radius); x2 < x + radius + 1; x2++) {
						if (x2 >= 0 && y2 >= 0 && x2 < getSize() && y2 < getSize() && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2 && inverted ^ !mask[x2][y2]) {
							maskCopy[x][y] = inverted;
							break l;
						}
					}
				}
			}
		}
	}

	public BinaryMask cutCorners() {
		int size = mask[0].length;
		boolean[][] maskCopy = new boolean[size][size];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int count = 0;
				if (x > 0 && !mask[x - 1][y])
					count++;
				if (y > 0 && !mask[x][y - 1])
					count++;
				if (x < size - 1 && !mask[x + 1][y])
					count++;
				if (y < size - 1 && !mask[x][y + 1])
					count++;
				if (count > 1)
					maskCopy[x][y] = false;
				else
					maskCopy[x][y] = mask[x][y];
			}
		}
		mask = maskCopy;
		return this;
	}

	public BinaryMask acid(float strength) {
		boolean[][] maskCopy = new boolean[getSize()][getSize()];

		for (int y = 0; y < getSize(); y++) {
			for (int x = 0; x < getSize(); x++) {
				if (((x > 0 && !mask[x - 1][y]) || (y > 0 && !mask[x][y - 1]) || (x < getSize() - 1 && !mask[x + 1][y]) || (y < getSize() - 1 && !mask[x][y + 1])) && random.nextFloat() < strength)
					maskCopy[x][y] = false;
				else
					maskCopy[x][y] = mask[x][y];
			}
		}
		mask = maskCopy;
		applySymmetry();
		return this;
	}

	public BinaryMask outline() {
		boolean[][] maskCopy = new boolean[getSize()][getSize()];

		for (int y = 0; y < getSize(); y++) {
			for (int x = 0; x < getSize(); x++) {
				if (((x > 0 && !mask[x - 1][y]) || (y > 0 && !mask[x][y - 1]) || (x < getSize() - 1 && !mask[x + 1][y]) || (y < getSize() - 1 && !mask[x][y + 1]))
						&& ((x > 0 && mask[x - 1][y]) || (y > 0 && mask[x][y - 1]) || (x < getSize() - 1 && mask[x + 1][y]) || (y < getSize() - 1 && mask[x][y + 1])))
					maskCopy[x][y] = true;
				else
					maskCopy[x][y] = false;
			}
		}
		mask = maskCopy;
		applySymmetry();
		return this;
	}

	public BinaryMask smooth(float radius) {
		boolean[][] maskCopy = new boolean[getSize()][getSize()];

		Thread[] threads = new Thread[4];
		threads[0] = new Thread(() -> smoothRegion(radius, maskCopy, 0, (getSize() / 4)));
		threads[1] = new Thread(() -> smoothRegion(radius, maskCopy, (getSize() / 4), (getSize() / 2)));
		threads[2] = new Thread(() -> smoothRegion(radius, maskCopy, (getSize() / 2), (getSize() / 4) * 3));
		threads[3] = new Thread(() -> smoothRegion(radius, maskCopy, (getSize() / 4) * 3, getSize()));

		Arrays.stream(threads).forEach(Thread::start);
		for (Thread f : threads) {
			try {
				f.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mask = maskCopy;
		return this;
	}

	private void smoothRegion(float radius, boolean[][] maskCopy, int startY, int endY) {
		float radius2 = (radius + 0.5f) * (radius + 0.5f);
		for (int y = startY; y < endY; y++) {
			for (int x = 0; x < getSize(); x++) {
				int count = 0;
				int count2 = 0;
				for (int y2 = (int) (y - radius); y2 <= y + radius; y2++) {
					for (int x2 = (int) (x - radius); x2 <= x + radius; x2++) {
						if (x2 > 0 && y2 > 0 && x2 < getSize() && y2 < getSize() && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
							count++;
							if (mask[x2][y2])
								count2++;
						}
					}
				}
				if (count2 > count / 2) {
					maskCopy[x][y] = true;
				}
			}
		}
	}

	public BinaryMask combine(BinaryMask other) {
		int size = Math.max(getSize(), other.getSize());
		if (getSize() != size)
			enlarge(size);
		if (other.getSize() != size)
			other.enlarge(size);
		boolean[][] maskCopy = new boolean[getSize()][getSize()];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				maskCopy[x][y] = get(x, y) || other.get(x, y);
			}
		}
		mask = maskCopy;
		return this;
	}

	public BinaryMask intersect(BinaryMask other) {
		int size = Math.max(getSize(), other.getSize());
		if (getSize() != size)
			enlarge(size);
		if (other.getSize() != size)
			other.enlarge(size);
		boolean[][] maskCopy = new boolean[getSize()][getSize()];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				maskCopy[x][y] = get(x, y) && other.get(x, y);
			}
		}
		mask = maskCopy;
		return this;
	}

	public BinaryMask minus(BinaryMask other) {
		int size = Math.max(getSize(), other.getSize());
		if (getSize() != size)
			enlarge(size);
		if (other.getSize() != size)
			other.enlarge(size);
		boolean[][] maskCopy = new boolean[getSize()][getSize()];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				maskCopy[x][y] = get(x, y) && !other.get(x, y);
			}
		}
		mask = maskCopy;
		return this;
	}

	public BinaryMask fillCircle(float x, float y, float radius, boolean value) {
		int ex = (int) StrictMath.min(getSize(), x + radius);
		int ey = (int) StrictMath.min(getSize(), y + radius);
		float dx;
		float dy;
		float radius2 = radius * radius;
		for (int cy = (int) StrictMath.max(0, y - radius); cy < ey; cy++) {
			for (int cx = (int) StrictMath.max(0, x - radius); cx < ex; cx++) {
				dx = x - cx;
				dy = y - cy;
				if (dx * dx + dy * dy <= radius2) {
					mask[cx][cy] = value;
				}
			}
		}
		return this;
	}
	
	public BinaryMask trimEdge(int rimWidth) {
		for (int a = 0; a < rimWidth; a++) {
			for (int b = 0; b < getSize() - rimWidth; b++) {
				mask[a][b] = false;
				mask[getSize() - 1 - a][getSize() - 1 - b] = false;
				mask[b][getSize() - 1 - a] = false;
				mask[getSize() - 1 - b][a] = false;
			}
		}
		return this;
	}
	
	public Vector2f getRandomPosition(){
		int cellCount = 0;
		for (int y = 0; y < getSize(); y++) {
			for (int x = 0; x < getSize(); x++) {
				if(mask[x][y])
					cellCount++;
			}
		}
		if(cellCount == 0)
			return null;
		int cell = random.nextInt(cellCount) + 1;
		cellCount = 0;
		for (int y = 0; y < getSize(); y++) {
			for (int x = 0; x < getSize(); x++) {
				if(mask[x][y])
					cellCount++;
				if(cellCount == cell)
					return new Vector2f(x,y);
			}
		}
		return null;
	}
}
