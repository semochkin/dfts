import java.io.Serializable;

/**
 * @author 16779246
 * @since 29.10.18
 */
public class CertMsg implements Serializable
{
	private String hostname;
	private String ip;
	private String alias;
	private String address;
	private byte[] data;

	public CertMsg()
	{
	}

	public CertMsg(String hostname, String ip, String alias, String address, byte[] data)
	{
		this.hostname = hostname;
		this.ip = ip;
		this.alias = alias;
		this.data = data;
		this.address = address;
	}

	public String getHostname()
	{
		return hostname;
	}

	public String getIp()
	{
		return ip;
	}

	public String getAlias()
	{
		return alias;
	}

	public byte[] getData()
	{
		return data;
	}

	public String getAddress() {
		return address;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("CertMsg{");
		sb.append("hostname='").append(hostname).append('\'');
		sb.append(", ip='").append(ip).append('\'');
		sb.append(", alias='").append(alias).append('\'');
		sb.append(", @ ").append(address);
		sb.append('}');
		return sb.toString();
	}
}
