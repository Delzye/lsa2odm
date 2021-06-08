package parser.lss;

import java.util.HashMap;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class AnswersList
{
	protected String answers_oid;
	protected HashMap<String,String> answers;
	protected String language;
	protected String type;
	protected boolean simple;

	public AnswersList(String oid, HashMap<String, String> a, String t, boolean s)
	{
		this.answers_oid = oid;
		this.answers = a;
		this.type = t;
		this.simple = s;
	}
}
