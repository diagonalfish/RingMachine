package net.voidfunction.rm.common.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

/**
 * Stores the data contained in RMFiles via their IDs.
 */
public class FileRepository {

	private String directory;
	
	public FileRepository(String directory) {
		this.directory = directory;
	}
	
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
	
	public boolean fileExists(String id) {
		return new File(getFileName(id)).canRead();
	}
	
	public InputStream readFile(String id) throws IOException {
		if (!fileExists(id)) throw new IOException("Can't load file with id '" + id + "'");
		return new FileInputStream(new File(getFileName(id)));
	}
	
	public void putFile(InputStream data, RMFile file) throws IOException {
		File fileObj = new File(getFileName(file.getId()));
		try {
			checkDirectory();
			
			FileOutputStream fileOut = new FileOutputStream(getFileName(file.getId()));
			IOUtils.copy(data, fileOut);
			data.close();
			fileOut.close();
			
			//Check hash against one in the file
			byte[] newHash = FileUtils.sha256Hash(fileObj);
			if (!Arrays.equals(newHash, file.getHash())) {
				throw new IOException("Hash of created file does not match");
			}
		} catch (IOException e) {
			fileObj.delete(); //Attempt to clean up
			throw e;
		}
		
	}
	
	public void deleteFile(String id) throws IOException {
		checkDirectory();
		File fileObj = new File(getFileName(id));
		if (fileObj.delete())
			throw new IOException("Could not delete file with id '" + id + "'");
	}
	
	private String getFileName(String id) {
		return directory + File.pathSeparator + id;
	}
}
