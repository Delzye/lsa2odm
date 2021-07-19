package parser.lss;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Survey
{
	protected String name;
	protected String description;
	protected String language;
	protected String id;
	
	protected ArrayList<QuestionGroup> groups;
	protected ArrayList<Question> questions;
	protected ArrayList<Integer> qid_list;
	protected ArrayList<Condition> cond_list;

	public Survey()
	{
		groups = new ArrayList<QuestionGroup>();
		questions = new ArrayList<Question>();
		qid_list = new ArrayList<Integer>();
		cond_list = new ArrayList<Condition>();
	}

	public void addQuestion(Question q)
	{
		questions.add(q);
	}

	public void addCondition(Condition c)
	{
		cond_list.add(c);
	}
}

