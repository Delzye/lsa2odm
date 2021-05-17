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
	protected String language;
	protected AnswersList answers;
	protected String cond;

	public Question(String qid, int gid, String type, String q, String title, String m, String l)
	{
		this.qid = qid;
		this.gid = gid;
		this.type = type;
		this.question = q;
		this.title = title;
		this.mandatory = m;
		this.language = l;
	}

	public Question(Question q)
	{
		this.qid = q.qid;
		this.gid = q.gid;
		this.type = q.type;
		this.question = q.question;
		this.title = q.title;
		this.mandatory = q.mandatory;
	}

	public void setAnswerOID(String oid)
	{
		answers.setAnswers_oid(oid);
	}

	public void addAnswer(String k, String v)
	{
		answers.answers.put(k, v);
	}

	public void setAnswerType(String type)
	{
		answers.type = type;
	}

	public void setAnswerLanguage(String lang)
	{
		answers.language = lang;
	}

	public void isAnswerSimple(boolean b)
	{
		answers.simple = b;
	}
}
