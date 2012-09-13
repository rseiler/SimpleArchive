package simplearchive;

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

	public SimpleArchive(String archive) throws IOException {
		this(new File(archive), false);
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
		indexEntries.add(new IndexEntry(name, data.length, randomAccessFile.getFilePointer()));
		randomAccessFile.write(data);
	}

	public void addFile(String filename) throws IOException {
		addFile(new File(filename));
	}

	public void addFile(File file) throws IOException {
		if (file.length() > 0xFFFFFFFFL) {
			throw new IOException("File is too big (maximal size is 4gb).");
		}

		addFile(file.getName(), new FileInputStream(file));
	}

	public void addFile(String name, InputStream is) throws IOException {
		long pos = randomAccessFile.getFilePointer();
		byte[] data = new byte[4096];
		try {
			while (is.available() > 0) {
				if (is.available() < 4096) {
					data = new byte[is.available()];
				}
				is.read(data);
				randomAccessFile.write(data);
			}
		} finally {
			if (is != null) {
				is.close();
			}
		}
		indexEntries.add(new IndexEntry(name, data.length, pos));
	}

	public List<IndexEntry> getIndexEntries() {
		return indexEntries;
	}

	public byte[] getFile(IndexEntry indexEntry) throws IOException {
		if (indexEntry.getSize() > Integer.MAX_VALUE) {
			throw new IOException("File is too big (maximal size is 2gb). Use readFile() instead.");
		}

		byte[] file = new byte[(int) indexEntry.getSize()];
		randomAccessFile.seek(indexEntry.getPos());
		randomAccessFile.read(file);
		return file;
	}

	public InputStream readFile(final IndexEntry indexEntry) throws IOException {
		if (indexEntry.getSize() > 0xFFFFFFFFL) {
			throw new IOException("File is too big (maximal size is 4gb).");
		}

		randomAccessFile.seek(indexEntry.getPos());
		return new InputStream() {
			private long readBytes = 0;

			@Override
			public int read() throws IOException {
				readBytes++;
				return randomAccessFile.read();
			}

			@Override
			public int available() throws IOException {
				if (indexEntry.getSize() - readBytes > Integer.MAX_VALUE) {
					return Integer.MAX_VALUE;
				}
				return (int) (indexEntry.getSize() - readBytes);
			}
		};
	}

	public void finish() throws IOException {
		if (!isEmpty()) {
			long filePointer = randomAccessFile.getFilePointer();
			randomAccessFile.write(indexData);
			for (IndexEntry indexEntry : indexEntries) {
				randomAccessFile.writeUTF(indexEntry.getName());
				randomAccessFile.writeInt((int) indexEntry.getSize());
			}
			int indexDataLen = (int) (randomAccessFile.getFilePointer() - filePointer);
			randomAccessFile.writeInt(indexDataLen);
		}
	}

	private void readIndexBytes() throws IOException {
		if (!isEmpty()) {
			randomAccessFile.seek(archive.length() - INTEGER_BYTES);
			int indexLen = randomAccessFile.readInt();
			indexData = new byte[indexLen];
			randomAccessFile.seek(archive.length() - INTEGER_BYTES - indexLen);
			randomAccessFile.read(indexData);
			randomAccessFile.seek(archive.length() - INTEGER_BYTES - indexLen);
		}
	}

	private void readIndexData() throws IOException {
		if (!isEmpty()) {
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
	}

	private boolean isEmpty() {
		return archive.length() <= 4;
	}
}
