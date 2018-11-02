import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileRequest implements java.io.Serializable {
	private String fileName;
	private byte[] hash;
	private long size;
	private byte[] key;
	private transient byte[] data;

	public FileRequest() {
	}

	public FileRequest(File file, byte[] key) throws NoSuchAlgorithmException, IOException {
		this.fileName = file.getName();
		MessageDigest digest = MessageDigest.getInstance("sha-512");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[65536];
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
			int read;
			while ((read = in.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
				out.write(buffer, 0, read);
			}
		}
		hash = digest.digest();
		size = file.length();
		data = out.toByteArray();
		this.key = key;
	}

	public String getFileName() {
		return fileName;
	}

	public byte[] getHash() {
		return hash;
	}

	public long getSize() {
		return size;
	}

	public byte[] getData() {
		return data;
	}

	public byte[] getKey() {
		return key;
	}
}
