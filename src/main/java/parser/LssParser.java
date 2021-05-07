package parser;

import java.io.File;
import java.util.HashMap;
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

//########################################### basic survey metadata #############################################################
		Element survey_elem = (Element) doc.selectSingleNode("//document/surveys_languagesettings/rows/row");
		survey.setName(survey_elem.element("surveyls_title").getText());
		survey.setDescription(survey_elem.element("surveyls_description").getText());
		survey.setId(survey_elem.element("surveyls_survey_id").getText());
		
		parseQuestionGroups();
		parseQuestions();

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	private void parseQuestionGroups()
	{
//########################################### Get the question groups #############################################################
		@SuppressWarnings("unchecked")
		List<Element> groups_elem = doc.selectNodes("//document/group_l10ns/rows/row");
		for(Element elem : groups_elem) {
			QuestionGroup group = new QuestionGroup();	
			group.setName(elem.element("group_name").getText());
			group.setGID(Integer.parseInt(elem.selectSingleNode("gid").getText()));
			log.info("Added question group: " + group.gid);
			Node desc = elem.selectSingleNode("description");
			if (desc != null) {
				group.setDescription(elem.getText());
			}
			survey.groups.add(group);
		}
		
	}

	private void parseQuestions()
	{
		@SuppressWarnings("unchecked")
		List<Element> questions_list = doc.selectNodes("//document/questions/rows/row");
		Node q_l10ns_elem = doc.selectSingleNode("//document/question_l10ns/rows");
		Node sq_elem = doc.selectSingleNode("//document/subquestions/rows");

		for (Element question : questions_list) {
			String qid = question.element("qid").getText();
			Question q = new Question(qid, 
									  Integer.parseInt(question.element("gid").getText()),
									  question.element("type").getText(),
									  q_l10ns_elem.selectSingleNode("row[qid=" + qid + "]/question").getText(),
									  question.element("title").getText(),
									  question.element("mandatory").getText());

			log.info("Working on question: " + q.qid);

			switch (q.type) {
				// Normal Text Fields
				case "S":
				case "T":
				case "U":
					q.setType("T");
					survey.questions.add(q);
					break;
				// Single 5-Point Choice
				case "5":
					q.setType("A");
					q.setAnswers(new AnswersList("5pt.cl", getIntCl(5), "string", true));
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
						Question sq = new Question(q.qid + q.title,
									  q.gid,
									  "T",
									  q.question + " " + q_l10ns_elem.selectSingleNode("row[qid=" + sqid + "]/question").getText(),
									  Integer.toString(sqid),
									  q.mandatory);
						sq.setHelp(q.help);

						survey.questions.add(sq);
					}
					break;
				case "!":
					q.setType("A");
					q.setAnswers(new AnswersList(q.getQid() + ".cl", getAnswersByID(q.qid), "string", false));
					survey.addQuestion(q);
					break;
				default:
					log.info("Question type not supported: " + q.type);
			}
		}
	}

	private HashMap<String, String> getAnswersByID(String id)
	{
		HashMap<String, String> ans_map = new HashMap<String, String>();
		Node ans_l10ns = doc.selectSingleNode("//document/answer_l10ns/rows");
		@SuppressWarnings("unchecked")
		List<Element> a_meta = doc.selectNodes("//document/answers/rows/row[qid=" + id +"]");
		for (Element elem : a_meta) {
			log.info(elem.elementText("code"));
			ans_map.put(elem.elementText("code"), ans_l10ns.selectSingleNode("row[aid=" + elem.elementText("aid") + "]/answer").getText());
		}
		return ans_map;
	}

	private HashMap<String, String> getIntCl(int l)
	{
		HashMap<String, String> ans_map = new HashMap<>();
		for (int i = 1; i <= l; i++){
			String a = Integer.toString(i);
			ans_map.put(a,a);
		}
		return ans_map;
	}
}
