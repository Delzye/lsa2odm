package parser.lss;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QuestionGroup
{
	protected String name;
	protected int gid;
	protected String description;

	public String getGIDString()
	{
		return Integer.toString(this.gid);
	}
}
