package parser;

public class QuestionGroup
{
	protected String name;
	protected int gid;
	protected String description;

	public void setName(String name)
	{
		this.name = name;
	}

	public void setGID(int gid)
	{
		this.gid = gid;
	}

	public void setDescription(String desc)
	{
		this.description = desc;
	}
	public String getName()
	{
		return this.name;
	}
	public String getGIDString()
	{
		return this.gid + "";
	}
	public int getGID()
	{
		return this.gid;
	}
	public String getDescription()
	{
		return this.description;
	}
}
