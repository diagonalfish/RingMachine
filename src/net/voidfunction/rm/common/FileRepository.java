package net.voidfunction.rm.common;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;

/**
 * Stores a list of files and their data. Provies a means to access file
 * information and data on demand.
 */
public class FileRepository {

	private Node node;
	private String directory;
	private HashMap<String, RMFile> fileObjects;

	public FileRepository(Node node, String directory) {
		this.node = node;
		this.directory = directory;
	}

	/* Loading and saving file hash table */

	/**
	 * Loads the files.dat file from this FileRepository's directory. If it
	 * exists, it should contain a HashMap of RMFiles.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void loadFiles() throws IOException {
		checkDirectory();
		String fileDatName = getDataFileName();
		try {
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
				fileDatName)));
			Object inObj = in.readObject();
			fileObjects = (HashMap<String, RMFile>)inObj;
			in.close();
		} catch (FileNotFoundException fnfe) {
			// It's ok if it does not exist.
			fileObjects = new HashMap<String, RMFile>();
			node.getLog().info(
				"File repository list (" + fileDatName + ") not found. Starting with empty list.");
		} catch (ClassNotFoundException e) {
			// Very unlikely, so we default to empty list
			node.getLog().warn(
				"Encountered error loading file  list (" + fileDatName + "). Starting with empty list.");
			fileObjects = new HashMap<String, RMFile>();
		}
		node.getLog().info("File repository (" + fileObjects.size() + " files) loaded.");
	}

	/**
	 * Saves this repository's HashMap of RMFiles to files.dat in the
	 * repository's directory.
	 * 
	 * @throws IOException
	 */
	public void saveFiles() {
		try {
			String fileDatName = getFileName("files.dat");
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
				fileDatName)));
			out.writeObject(fileObjects);
			out.close();
		} catch (IOException e) {
			node.getLog().severe(
				"Could not save file database: " + e.getClass().toString() + " - " + e.getMessage());
		}
	}

	/* Functions for adding to/removing from file hash table */

	/**
	 * Adds a new file to the list of files we know about, along with its data.
	 * 
	 * @param file
	 * @param data
	 * @throws IOException 
	 */
	public synchronized void addFile(RMFile file, InputStream data) throws IOException {
		if (!fileObjects.containsKey(file.getId())) {
			fileObjects.put(file.getId(), file);
			saveFiles();
		}
		storeFileData(data, file.getId());
	}

	/**
	 * Removes a file from the list of files we know about.
	 * 
	 * @param id
	 */
	public synchronized void removeFile(String id) throws IOException {
		RMFile file = getFileById(id);
		if (file == null)
			return;

		deleteFileData(id);
		
		fileObjects.remove(id);
		saveFiles();
	}

	/**
	 * Remove all files whose ids are not contained in the given
	 * list of Strings.
	 * @param keepFiles
	 */
	public synchronized int removeAllExcept(List<Object> keepFiles) throws IOException {
		int counter = 0;
		for (String id : new ArrayList<String>(fileObjects.keySet())) {
			if (!keepFiles.contains(id)) {
				removeFile(id);
				counter++;
			}
		}
		return counter;
	}
	
	/**
	 * Returns an RMFile for a given file ID, or null if it doesn't exist.
	 * 
	 * @param id
	 * @return
	 */
	public synchronized RMFile getFileById(String id) {
		return fileObjects.get(id);
	}

	/**
	 * Check whether a file exists with the given id.
	 * 
	 * @param id
	 * @return
	 */
	public synchronized boolean checkFile(String id) {
		return fileObjects.containsKey(id);
	}

	/**
	 * Return a list of all files we know about.
	 * 
	 * @return
	 */
	public synchronized Collection<RMFile> getFileObjects() {
		return fileObjects.values();
	}

	public synchronized int getFileCount() {
		return fileObjects.size();
	}

	/* Functions for manipulating file data */

	/**
	 * Checks whether the FileRepository's save directory exists and is
	 * writable. Will attempts to create the directory if it doesn't exist.
	 * 
	 * @throws IOException
	 */
	public synchronized void checkDirectory() throws IOException {
		File dirCheck = new File(directory);
		if (!dirCheck.exists()) {
			if (!dirCheck.mkdir())
				throw new IOException("Could not create file repository directory");
		}
		if (!dirCheck.isDirectory() || !dirCheck.canWrite()) {
			throw new IOException("Cannot write to file repository directory");
		}
	}

	/**
	 * Returns whether this repository knows about a file with the given ID.
	 * 
	 * @param id
	 * @return
	 */
	public synchronized boolean fileDataExists(String id) {
		return new File(getFileName(id)).canRead();
	}

	/**
	 * Returns an InputStream of the data for the given file ID, or null if the
	 * file does not exist.
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public synchronized InputStream getFileData(String id) throws IOException {
		if (getFileById(id) == null)
			return null;
		if (!fileDataExists(id))
			throw new IOException("Can't load file with id '" + id + "'");
		return new FileInputStream(new File(getFileName(id)));
	}

	/**
	 * Saves the data for a given file ID to the repository's save folder.
	 * 
	 * @param data
	 * @param id
	 * @throws IOException
	 */
	private void storeFileData(InputStream data, String id) throws IOException {
		RMFile file = getFileById(id);

		if (file == null)
			throw new IOException("File with id " + id + " does not exist.");

		File fileObj = new File(getFileName(id));
		try {
			checkDirectory();

			FileOutputStream fileOut = new FileOutputStream(getFileName(file.getId()));
			IOUtils.copy(data, fileOut);
			data.close();
			fileOut.close();

			// Check hash against one in the file
			byte[] newHash = FileUtils.sha256Hash(fileObj);
			if (!Arrays.equals(newHash, file.getHash())) {
				throw new IOException("Hash of created file does not match");
			}
		} catch (IOException e) {
			fileObj.delete(); // Attempt to clean up
			throw e;
		}

	}

	/**
	 * Deletes any data we have stored for the file with the given ID. Must be
	 * called separately from removeFile().
	 * 
	 * @param id
	 * @throws IOException
	 */
	private synchronized void deleteFileData(String id) throws IOException {
		if (getFileById(id) == null)
			return;

		checkDirectory();
		File fileObj = new File(getFileName(id));
		fileObj.delete();
	}

	public String getDataFileName() {
		return getFileName("files.dat");
	}

	// Util function for getting the local storage filename for a given file
	private String getFileName(String id) {
		return directory + "/" + id;
	}

}
