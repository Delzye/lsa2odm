package parser;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AnswersList
{
	protected String answers_oid;
	protected HashMap<Integer,String> answers;
	protected String language;
	protected String type;
	protected boolean simple;

	public AnswersList()
	{
		answers = new HashMap<Integer,String>();
	}
}
