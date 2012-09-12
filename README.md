SimpleArchive
=============

It's a very simple and fast archive library for Java.
 
It supports: 
- UTF-8 names
- Adding files to an existing archive without the need to recreate the archive
	
It doesn't supports:
- Compression
- Any kind of meta data
	
Limitations:
- 2gb for a stored file
- 2gb for the index
- 2^63 bytes archive size
- 255 bytes utf-8 file name

You could easily increase the limitation for a stored file and for the index if you change it to a unsigned int or to a long.


## Usage
```java
// creates or opens the archive 
SimpleArchive archive = new SimpleArchive(new File("test.dat"));

// some file
File file = new File("some_file");

// adds the file to the archive
archive.addFile(file.getName(), file);

// IMPORTANT! writes the index to the archive.
archive.finish(); 


// reads the index and iterate over all stored index files
List<IndexEntry> indexEntries = archive.getIndexEntries();
for (IndexEntry indexEntry : indexEntries) {
	// reads the data of the stored file
	byte[] fileData = archive.getFile(indexEntry);
}


// if you just want to add a file to an archive 
new SimpleArchive(new File("test.dat"), true);
// then it's write only and it dosen't generate the indexEntries list.
```


## Archive Structure
file 1 | file 2 | ... | index file 1 | index file 2 | ... | length of index data [int]

**file**: just the binary data of the file

**index file**:  the length of the sting in bytes [unsigned short] | utf-8 filename |  file size [int]

**length of index data**: ("archive file size" - "length of index data" - 4) points to the start of the index file block.