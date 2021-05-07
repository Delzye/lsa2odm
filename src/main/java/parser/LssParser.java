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

	Node q_l10ns_node;
	Node sq_node;

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
		q_l10ns_node = doc.selectSingleNode("//document/question_l10ns/rows");
		sq_node = doc.selectSingleNode("//document/subquestions/rows");

		for (Element question : questions_list) {
			String qid = question.element("qid").getText();
			Question q = new Question(qid, 
									  Integer.parseInt(question.element("gid").getText()),
									  question.element("type").getText(),
									  q_l10ns_node.selectSingleNode("row[qid=" + qid + "]/question").getText(),
									  question.element("title").getText(),
									  question.element("mandatory").getText(),
									  q_l10ns_node.selectSingleNode("row[qid=" + qid + "]/language").getText());

			log.info("Working on question: " + q.qid);

			switch (q.type) {
				// Normal Text Fields
				case "S":
				case "T":
				case "U":
					q.setType("T");
					survey.addQuestion(q);
					break;
				case "N":
					survey.addQuestion(q);
					break;
				// Single 5-Point Choice
				case "5":
					q.setType("A");
					q.setAnswers(new AnswersList("5pt.cl", getIntCl(5), "string", true));
					survey.addQuestion(q);
					break;
				// Yes/No
				case "Y":
					q.setType("A");
					q.setAnswers(new AnswersList("YN.cl", getYNCl(), "string", false));
					survey.addQuestion(q);
					break;
				// Gender
				case "G":
					q.setType("A");
					q.setAnswers(new AnswersList("Gender.cl", getGenderCl(), "string", false));
					survey.addQuestion(q);
					break;
				// List with comment
				case "O":
						Question q_comment = new Question(q);
						q_comment.setQid(q_comment.getQid().concat("comment"));
						q_comment.setType("T");
						survey.addQuestion(q_comment);
				// Radio List
				case "L":
					if (question.elementTextTrim("other").equals("Y")) {
						Question q_other = new Question(q);
						q_other.setQid(q_other.getQid().concat("other"));
						q_other.setType("T");
						survey.addQuestion(q_other);
					}
				// Dropdown List
				case "!":
					q.setType("A");
					q.setAnswers(new AnswersList(q.getQid() + ".cl", getAnswersByID(q.qid), "string", false));
					survey.addQuestion(q);
					break;
				// Multiple Texts
				case "Q":
					addSubquestions(q, "T");
					break;
				// Multiple Texts
				case "K":
					addSubquestions(q, "N");
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

	private HashMap<String, String> getYNCl()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("Y", "yes");
		ans_map.put("N", "no");
		return ans_map;
	}

	private HashMap<String, String> getGenderCl()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("M", "male");
		ans_map.put("F", "female");
		return ans_map;
	}

	private void addSubquestions(Question q, String type)
	{
		@SuppressWarnings("unchecked")
		List<Element> sq_list = sq_node.selectNodes("row[parent_qid=" + q.qid + "]/qid");
		List<String> sqids = sq_list.stream()
										 .map(e -> e.getText())
										 .collect(Collectors.toList());  // Make a list of qid elements to a list of strings
		for (String sqid : sqids) {
			String question = q_l10ns_node.selectSingleNode("row[qid=" + sqid + "]/question").getText();
			String sq_title = sq_node.selectSingleNode("row[qid=" + sqid + "]/title").getText();
			Question sq = new Question(q.qid + sq_title,
						  q.gid,
						  type,
						  q.question + " " + question,
						  sqid,
						  q.mandatory,
						  q.language);
			sq.setHelp(q.help);

			survey.addQuestion(sq);
		}
	}
}
