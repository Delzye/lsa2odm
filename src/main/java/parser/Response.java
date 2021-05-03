package parser;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Response
{
	@Setter protected int id;
	HashMap<Integer, ArrayList<Answer>> answers;

	public Response()
	{
		answers = new HashMap<>();
	}

	public void addToAnswers(Answer a)
	{
		answers.get(a.getGid()).add(a);
	}
}
