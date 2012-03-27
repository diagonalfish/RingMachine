package net.voidfunction.rm.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;

/**
 * Stores a list of files and their data. Provies a means to access file information
 * and data on demand.
 */
public class FileRepository {

	private String directory;
	private HashMap<String, RMFile> fileObjects;

	public FileRepository(String directory) {
		this.directory = directory;
	}

	/* Loading and saving file hash table */
	
	/**
	 * Loads the files.dat file from this FileRepository's directory. If it exists, it
	 * should contain a HashMap of RMFiles.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void loadFiles() throws IOException {
		String fileDatName = getFileName("files.dat");
		try {
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fileDatName)));
			Object inObj = in.readObject();
			fileObjects = (HashMap<String, RMFile>)inObj;
			in.close();
		} catch (FileNotFoundException fnfe) {
			// It's ok if it does not exist.
			fileObjects = new HashMap<String, RMFile>();
			RMLog.info("File repository list (" + fileDatName + ") not found. Creating a new one.");
		} catch (ClassNotFoundException e) {
			// Very unlikely, so we default to empty list
			RMLog.warn("Encountered error loading file  list (" + fileDatName + "). Starting with empty list.");
			fileObjects = new HashMap<String, RMFile>();
		}
		RMLog.info("File repository containing " + fileObjects.size() + " loaded.");
	}
	
	/**
	 * Saves this repository's HashMap of RMFiles to files.dat in the repository's directory.
	 * @throws IOException
	 */
	public void saveFiles() throws IOException {
		String fileDatName = getFileName("files.dat");
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileDatName)));
		out.writeObject(fileObjects);
		out.close();
	}
	
	/* Functions for adding to/removing from file hash table */
	
	/**
	 * Adds a new file to the list of files we know about.
	 * @param file
	 */
	public void addFile(RMFile file) {
		if (!fileObjects.containsKey(file))
			fileObjects.put(file.getId(), file);
	}

	/**
	 * Removes a file from the list of files we know about.
	 * @param id
	 */
	public void removeFile(String id) {
		RMFile file = getFileById(id);
		if (file == null) return;
		fileObjects.remove(file);
	}

	/**
	 * Returns an RMFile for a given file ID, or null if it doesn't exist.
	 * @param id
	 * @return
	 */
	public RMFile getFileById(String id) {
		return fileObjects.get(id);
	}

	/**
	 * Return a list of all files we know about.
	 * @return
	 */
	public Collection<RMFile> getfileObjects() {
		return fileObjects.values();
	}
	
	/* Functions for manipulating file data */
	
	/**
	 * Checks whether the FileRepository's save directory exists and is writable. Will
	 * attempts to create the directory if it doesn't exist.
	 * @throws IOException
	 */
	public void checkDirectory() throws IOException {
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
	 * @param id
	 * @return
	 */
	public boolean fileDataExists(String id) {
		return new File(getFileName(id)).canRead();
	}

	/**
	 * Returns an InputStream of the data for the given file ID, or null if the file
	 * does not exist.
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public InputStream getFileData(String id) throws IOException {
		if (getFileById(id) == null)
			return null;
		if (!fileDataExists(id))
			throw new IOException("Can't load file with id '" + id + "'");
		return new FileInputStream(new File(getFileName(id)));
	}

	/**
	 * Saves the data for a given file ID to the repository's save folder.
	 * @param data
	 * @param id
	 * @throws IOException
	 */
	public void storeFileData(InputStream data, String id) throws IOException {
		RMFile file = getFileById(id);
		
		if (file == null) throw new IOException("File with id " + id + " does not exist.");
		
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
	 * Deletes any data we have stored for the file with the given ID. Must be called
	 * separately from removeFile().
	 * @param id
	 * @throws IOException
	 */
	public void deleteFileData(String id) throws IOException {
		if (getFileById(id) == null)
			return;
		
		checkDirectory();
		File fileObj = new File(getFileName(id));
		if (!fileObj.delete())
			throw new IOException("Could not delete file with id '" + id + "'");
	}

	// Util function for getting the local storage filename for a given file
	private String getFileName(String id) {
		return directory + File.pathSeparator + id;
	}
}
