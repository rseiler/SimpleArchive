package simplearchive;

public final class IndexEntry {
	private final String name;
	private final long size;
	private final long pos;

	IndexEntry(String name, int size, long pos) {
		this.name = name;
		this.size = size;
		this.pos = pos;
	}

	public String getName() {
		return name;
	}

	public long getSize() {
		return size;
	}

	public long getPos() {
		return pos;
	}

	@Override
	public String toString() {
		return "IndexEntry [name=" + name + ", size=" + size + ", pos=" + pos + "]";
	}

}