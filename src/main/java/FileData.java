import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileData implements Serializable {
	private byte[] data;
	private String id;

	public FileData() {
	}

	public FileData(byte[] bytes, String uid) {
		this.data = bytes;
		id = uid;
	}

	public byte[] getData() {
		return data;
	}

	public String getId() {
		return id;
	}
}
