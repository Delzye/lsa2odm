package parser;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Question
{
	protected String qid;
	protected int gid;
	protected String type;
	protected String question;
	protected String help;
	protected String title;
	protected String mandatory;
	protected AnswersList answers;
}
