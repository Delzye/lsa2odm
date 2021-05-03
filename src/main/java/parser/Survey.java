package parser;

import java.util.ArrayList;

public class Survey
{
	protected String name;
	protected String description;
	protected int id;
	
	protected ArrayList<QuestionGroup> groups;
	protected ArrayList<Question> questions;
	protected ArrayList<Integer> qidList;

	public Survey()
	{
		groups = new ArrayList<QuestionGroup>();
		questions = new ArrayList<Question>();
		qidList = new ArrayList<Integer>();
	}
	
	public String getName()
	{
		return this.name;
	}
	public String getDescription()
	{
		return this.description;
	}
	public String getIDString()
	{
		return "" + id;
	}
	public ArrayList<QuestionGroup> getGroups()
	{
		return this.groups;
	}
	public ArrayList<Question> getQuestions()
	{
		return this.questions;
	}
}

