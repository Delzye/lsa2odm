package parser;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j;
import lombok.Getter;
import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;


@Log4j
public class LssParser
{

	protected File lss;
	@Getter protected Survey survey;
	Document doc;

	public LssParser(File lss)
	{
		this.lss = lss;
		survey = new Survey();
	}

	public void parseDocument()
	{
		try {
		SAXReader saxReader = new SAXReader();
		doc = saxReader.read(lss);

		Element survey_elem = (Element) doc.selectSingleNode("//document/surveys_languagesettings/rows/row");
		survey.name = survey_elem.selectSingleNode("surveyls_title").getText();
		survey.description = survey_elem.selectSingleNode("surveyls_description").getText();
		survey.id = Integer.parseInt(survey_elem.selectSingleNode("surveyls_survey_id").getText());
		log.info(survey.name + survey.id + survey.description);

		@SuppressWarnings("unchecked")
		List<Element> groups_elem = doc.selectNodes("//document/group_l10ns/rows/row");
		for(Element elem : groups_elem) {
			QuestionGroup group = new QuestionGroup();	
			group.setName(elem.selectSingleNode("group_name").getText());
			group.setGID(Integer.parseInt(elem.selectSingleNode("gid").getText()));
			log.info("Added question group: " + group.gid);
			Node desc = elem.selectSingleNode("description");
			if (desc != null) {
				group.setDescription(elem.getText());
			}
			survey.groups.add(group);
		}
		
		parseQuestions();

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	private void parseQuestions()
	{
		@SuppressWarnings("unchecked")
		List<Element> questions_list = doc.selectNodes("//document/questions/rows/row");
		Node q_l10ns_elem = doc.selectSingleNode("//document/question_l10ns/rows");
		Node sq_elem = doc.selectSingleNode("//document/subquestions/rows");

		for (Element question : questions_list) {

			Question q = new Question();
			q.title = question.selectSingleNode("title").getText();
			q.qid = question.element("qid").getText();
			q.gid = Integer.parseInt(question.selectSingleNode("gid").getText());
			q.type = question.element("type").getText();
			q.mandatory = question.element("mandatory").getText();
			q.question = q_l10ns_elem.selectSingleNode("row[qid=" + q.qid + "]/question").getText();
			q.help = "";

			log.info("Working on question: " + q.qid);

			switch (q.type) {
				// Normal Text Fields
				case "S":
				case "T":
				case "U":
					q.type = "T";
					survey.questions.add(q);
					break;
				// Single 5-Point Choice
				case "5":
					q.type = "A";
					q.answers = new AnswersList();
					q.answers.setAnswers_oid("5pt.cl");
					q.getAnswers().setSimple(true);
					for (int i = 1; i <= 5; i++) {
						q.answers.answers.put(i, "" + i);
					}
					survey.questions.add(q);
					break;
				// Multiple Texts
				case "Q":
					@SuppressWarnings("unchecked")
					List<Element> sq_list = sq_elem.selectNodes("row[parent_qid=" + q.qid + "]/qid");
					List<Integer> sqids = sq_list.stream()
													 .map(e -> e.getText())
													 .map(Integer::parseInt)
													 .collect(Collectors.toList());  // Make a list of qid elements to a list of ints
					for (int sqid : sqids) {
						Question sq = new Question();
						sq.question = q.question + " " + q_l10ns_elem.selectSingleNode("row[qid=" + sqid + "]/question").getText();
						sq.title = Integer.toString(sqid);
						sq.qid = q.qid + q.title;
						sq.gid = q.gid;
						sq.type = "T";
						sq.mandatory = q.mandatory;
						sq.help = q.help;

						survey.questions.add(sq);
					}
					break;
				default:
					log.info("Question type not supported: " + q.type);
			}
		}
	}
}
