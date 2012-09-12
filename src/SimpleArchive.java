import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class SimpleArchive {

	private static final int INTEGER_BYTES = Integer.SIZE / Byte.SIZE;

	private File archive;
	private RandomAccessFile randomAccessFile;

	private byte[] indexData = new byte[0];
	private List<IndexEntry> indexEntries = new ArrayList<>();

	public SimpleArchive(File archive) throws IOException {
		this(archive, false);
	}

	public SimpleArchive(File archive, boolean writeOnly) throws IOException {
		this.archive = archive;
		boolean archiveExists = archive.exists();
		randomAccessFile = new RandomAccessFile(archive, "rw");

		if (archiveExists) {
			if (writeOnly) {
				readIndexBytes();
			} else {
				readIndexData();
			}
		}
	}

	public void addFile(String name, byte[] data) throws IOException {
		indexEntries.add(new IndexEntry(name, data.length, (int) randomAccessFile.getFilePointer()));
		randomAccessFile.write(data);
	}

	public void addFile(String name, File file) throws IOException {
		byte[] data = new byte[(int) file.length()];;
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			inputStream.read(data);
		} finally {
			if(inputStream != null) {
				inputStream.close();
			}
		}
		addFile(name, data);
	}

	public List<IndexEntry> getIndexEntries() {
		return indexEntries;
	}

	public byte[] getFile(IndexEntry indexEntry) throws IOException {
		byte[] file = new byte[indexEntry.size];
		randomAccessFile.seek(indexEntry.getPos());
		randomAccessFile.read(file);
		return file;
	}

	public void finish() throws IOException {
		long filePointer = randomAccessFile.getFilePointer();
		randomAccessFile.write(indexData);
		for (IndexEntry indexEntry : indexEntries) {
			randomAccessFile.writeUTF(indexEntry.name);
			randomAccessFile.writeInt(indexEntry.size);
		}
		int indexDataLen = (int) (randomAccessFile.getFilePointer() - filePointer);
		randomAccessFile.writeInt(indexDataLen);
	}

	private void readIndexBytes() throws IOException {
		randomAccessFile.seek(archive.length() - INTEGER_BYTES);
		int indexLen = randomAccessFile.readInt();
		indexData = new byte[indexLen];
		randomAccessFile.seek(archive.length() - INTEGER_BYTES - indexLen);
		randomAccessFile.read(indexData);
		randomAccessFile.seek(archive.length() - INTEGER_BYTES - indexLen);
	}

	private void readIndexData() throws IOException {
		randomAccessFile.seek(archive.length() - INTEGER_BYTES);

		int indexLen = randomAccessFile.readInt();
		long indexDataStartPos = archive.length() - INTEGER_BYTES - indexLen;
		long indexDataEndPos = archive.length() - INTEGER_BYTES;

		randomAccessFile.seek(indexDataStartPos);
		long pos = 0;
		while (randomAccessFile.getFilePointer() < indexDataEndPos) {
			String name = randomAccessFile.readUTF();
			int len = randomAccessFile.readInt();
			indexEntries.add(new IndexEntry(name, len, pos));
			pos += len;
		}

		randomAccessFile.seek(indexDataStartPos);
	}

	public static final class IndexEntry {
		private final String name;
		private final int size;
		private final long pos;

		private IndexEntry(String name, int size, long pos) {
			this.name = name;
			this.size = size;
			this.pos = pos;
		}

		public String getName() {
			return name;
		}

		public int getSize() {
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
}
