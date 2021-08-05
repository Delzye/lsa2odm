/* MIT License

Copyright (c) 2021 Anton Mende (Delzye)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */
package parser.lss;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Question
{
	protected String qid;
	protected int gid;
	protected String type;
	protected String question;
	protected String description;
	protected String help;
	protected String title;
	protected String mandatory;
	protected String language;
	protected String cond; // Do not show the question if the referenced condition evaluates to true
	protected AnswersList answers; // Only for Questions with a set list of answer options
	// Only for Questions with numerical input
	protected String float_range_min;
	protected String float_range_max;

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
		this.language = q.language;
		this.cond = q.cond;
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
