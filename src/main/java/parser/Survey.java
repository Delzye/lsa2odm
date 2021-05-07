package parser;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Survey
{
	protected String name;
	protected String description;
	protected String id;
	
	protected ArrayList<QuestionGroup> groups;
	protected ArrayList<Question> questions;
	protected ArrayList<Integer> qidList;

	public Survey()
	{
		groups = new ArrayList<QuestionGroup>();
		questions = new ArrayList<Question>();
		qidList = new ArrayList<Integer>();
	}

	public void addQuestion(Question q)
	{
		questions.add(q);
	}
}

