package parser;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Getter;

@Getter @ToString @AllArgsConstructor
public class Answer
{
	protected int gid;
	protected String qid;
	protected String answer;
}
