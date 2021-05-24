package parser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	protected Document doc;
	@Getter protected ArrayList<String> date_time_qids;

	Node q_l10ns_node;
	Node sq_node;
	Node q_node;
	Node a_node;

	public LssParser(File lss)
	{
		this.lss = lss;
		survey = new Survey();
		date_time_qids = new ArrayList<>();
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
		q_node = doc.selectSingleNode("//document/questions/rows");
		a_node = doc.selectSingleNode("//document/answers/rows");

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
				// Date/Time
				case "D":
					date_time_qids.add(q.getQid());
				// Numeric Input
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
						addOtherQuestion(q);
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
				// Multiple Numeric Inputs
				case "K":
					addSubquestions(q, "N");
					break;
				// Dual scale array
				case "1":
					HashMap<String, String>[] code_lists = getDualScaleCls(q.qid);
					addSubquestionsWithCL(q, "A", q.getQid() + ".0", code_lists[0], "string", false, "-0");
					addSubquestionsWithCL(q, "A", q.getQid() + ".1", code_lists[1], "string", false, "-1");
					break;
				// Array by column
				case "H":
				// Flexible Array
				case "F":
					addSubquestionsWithCL(q, "A", q.getQid().concat(".cl"), getAnswersByID(q.getQid()), "string", false, "");
					break;
				// 5pt Array
				case "A":
					addSubquestionsWithCL(q, "A", "5pt.cl", getIntCl(5), "integer", true, "");
					break;
				// 10pt Array
				case "B":
					addSubquestionsWithCL(q, "A", "10pt.cl", getIntCl(10), "integer", true, "");
					break;
				// Increase/Same/Decrease Array
				case "E":
					addSubquestionsWithCL(q, "A", "ISD.cl", getISDCL(), "string", false, "");
					break;
				// 10pt Array
				case "C":
					addSubquestionsWithCL(q, "A", "YNU.cl", getYNUCL(), "string", false, "");
					break;
				// Matrix with numerical input
				case ":":
					q.setType("N");
					addQuestionMatrix(q);
					break;
				// Matrix with text input
				case ";":
					q.setType("T");
					addQuestionMatrix(q);
					break;
				// Multiple Choice (Normal, Bootstrap, Image select)
				case "M":
					addSubquestionsWithCL(q, "A", "MC.cl", getMCCL(), "string", false, "");
					if (question.elementTextTrim("other").equals("Y")) {
						q.setQid(q.getQid().concat("other"));
						q.setType("T");
						survey.addQuestion(q);
					}
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
			ans_map.put(elem.elementText("code"), ans_l10ns.selectSingleNode("row[aid=" + elem.elementText("aid") + "]/answer").getText());
		}
		return ans_map;
	}

	private HashMap<String, String> getAnswersByIDWithID(String id)
	{
		HashMap<String, String> ans_map = new HashMap<String, String>();
		Node ans_l10ns = doc.selectSingleNode("//document/answer_l10ns/rows");
		@SuppressWarnings("unchecked")
		List<Element> a_meta = doc.selectNodes("//document/answers/rows/row[qid=" + id +"]");
		for (Element elem : a_meta) {
			ans_map.put(elem.elementText("aid"), ans_l10ns.selectSingleNode("row[aid=" + elem.elementText("aid") + "]/answer").getText());
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

	private HashMap<String, String> getISDCL()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("I", "Increase");
		ans_map.put("S", "Same");
		ans_map.put("D", "Decrease");
		return ans_map;
	}

	private HashMap<String, String> getYNUCL()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("Y", "Yes");
		ans_map.put("N", "No");
		ans_map.put("U", "Uncertain");
		return ans_map;
	}

	private HashMap<String, String> getMCCL()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("Y", "Yes");
		ans_map.put("", "No");
		return ans_map;
	}


	private void addSubquestionsWithCL(Question q, String type, String oid, HashMap<String, String> ans, String t, boolean b, String qid_append)
	{
		List<String> sqids = getSqIds(q.getQid());
		for (String sqid : sqids) {
			String question = q_l10ns_node.selectSingleNode("row[qid=" + sqid + "]/question").getText();
			String sq_title = sq_node.selectSingleNode("row[qid=" + sqid + "]/title").getText();
			Question sq = new Question(q.qid + sq_title + qid_append,
						  q.gid,
						  type,
						  q.question + " " + question,
						  sqid,
						  q.mandatory,
						  q.language);
			sq.setHelp(q.help);
			sq.setAnswers(new AnswersList(oid, ans, t, b));

			survey.addQuestion(sq);
		}
	}

	private void addSubquestions(Question q, String type)
	{
		List<String> sqids = getSqIds(q.getQid());
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

	private void addQuestionMatrix(Question q)
	{
		HashMap<String, String> sq_x = new HashMap<>();
		HashMap<String, String> sq_y = new HashMap<>();
		List<String> sqids = getSqIds(q.getQid());

		// Sort Questions into X and y Axis (if <relevance/> contains any text the question is on the Y Axis)
		for (String sqid : sqids) {
			String question = q_l10ns_node.selectSingleNode("row[qid=" + sqid + "]/question").getText();
			if (sq_node.selectSingleNode("row[qid=" + sqid + "]/relevance").getText().equals("")) {
				sq_x.put(sqid, question);
			} else {
				sq_y.put(sqid, question);
			}
		}
		
		// Add each cell of the matrix as an individual question
		Question sq;
		for (Map.Entry<String, String> x_entry : sq_x.entrySet()) {
			for (Map.Entry<String, String> y_entry : sq_y.entrySet()) {
				String y_title = sq_node.selectSingleNode("row[qid=" + y_entry.getKey() + "]/title").getText();
				String x_title = sq_node.selectSingleNode("row[qid=" + x_entry.getKey() + "]/title").getText();
				sq = new Question(q);
				sq.setQid(q.getQid() + y_title + "_" + x_title);
				sq.setQuestion(q.question + " " + y_entry.getValue() + " " + x_entry.getValue());
				sq.setTitle(y_entry.getKey() + "/" + x_entry.getKey());
				survey.addQuestion(sq);
			}
		}
	}

	private HashMap<String, String>[] getDualScaleCls(String qid)
	{
		@SuppressWarnings("unchecked")
		HashMap<String, String>[] cls = new HashMap[2];
		cls[0] = getAnswersByIDWithID(qid);
		cls[1] = new HashMap<>();
		Iterator<Map.Entry<String,String>> iter = cls[0].entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String,String> entry = iter.next();
			if (a_node.selectSingleNode("row[aid=" + entry.getKey() + "]/scale_id").getText().equals("1")) {
				cls[1].put(entry.getKey(), entry.getValue());
				iter.remove();
			}
		}
		return cls;
	}

	private List<String> getSqIds(String qid)
	{
		@SuppressWarnings("unchecked")
		List<Element> sq_list = sq_node.selectNodes("row[parent_qid=" + qid + "]/qid");
		return sq_list.stream()
					  .map(e -> e.getText())
					  .collect(Collectors.toList());  // Make a list of qid elements to a list of strings
	}

	private void addOtherQuestion(Question q)
	{
		Question q_other = new Question(q);
		q_other.setQid(q_other.getQid().concat("other"));
		q_other.setType("T");
		survey.addQuestion(q_other);
		/*	Cond: SE-StudyEventOID/F-FormOID[RepeatKey]/IG-ItemGroupOID/I-ItemOID == "-oth-"
		 *
		 */
		String cond_oid = q.getQid().concat(".cond");
		survey.addCondition(new Condition(cond_oid, Integer.toString(q.getGid()), q.getQid()));
		q_other.setCond(cond_oid);
	}
}
