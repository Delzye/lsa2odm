package parser.lss;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import lombok.extern.log4j.Log4j;
import utils.ClGenerator;
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
	protected Properties prop;
	@Getter protected ArrayList<String> date_time_qids;

	protected Node q_l10ns_node;
	protected Node sq_node;
	protected Node q_node;
	protected Node a_node;
	/**
	 * The Constructor
	 * @param lss The .lss-file, which will be parsed
	 */
	public LssParser(File lss)
	{
		this.lss = lss;
		survey = new Survey();
		date_time_qids = new ArrayList<>();

		// Load properties from the config file
		try (InputStream input = LssParser.class.getResourceAsStream("/config.properties")) {
			prop = new Properties();
            prop.load(input);
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
	}

	/**
	 * Main Method of this class, calls all needed functions to parse the file and create a {@link parser.Survey} object with the information
	 *
	 */
	public void parseDocument()
	{
		try {
		SAXReader saxReader = new SAXReader();
		doc = saxReader.read(lss);

		// Basic Survey Metadata
		Element survey_elem = (Element) doc.selectSingleNode("//document/surveys_languagesettings/rows/row");
		survey.setName(survey_elem.element("surveyls_title").getText());
		survey.setDescription(survey_elem.element("surveyls_description").getText());
		survey.setId(survey_elem.element("surveyls_survey_id").getText());
		
		// questions and question groups
		parseQuestionGroups();
		parseQuestions();

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * Gets all question groups from the group_l10ns element in the document and adds corresponding objects to the groups list in the survey
	 */
	private void parseQuestionGroups()
	{
		@SuppressWarnings("unchecked")
		List<Element> groups_elem = doc.selectNodes("//document/group_l10ns/rows/row");
		for(Element elem : groups_elem) {
			
			QuestionGroup group = new QuestionGroup();	
			group.setName(elem.element("group_name").getText());
			group.setGid(Integer.parseInt(elem.selectSingleNode("gid").getText()));

			log.info("Added question group: " + group.gid);

			// Add a description, if there is one
			Node desc = elem.selectSingleNode("description");
			if (desc != null) {
				group.setDescription(elem.getText());
			}
			
			survey.groups.add(group);
		}
		
	}

	/**
	 * Iterates through all elements of questions and subquestions and adds the parsed and edited questions to the questions list in the survey
	 *
	 */
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
			log.info("Working on question: " + qid);
			Question q = new Question(qid, 
									  Integer.parseInt(question.element("gid").getText()),
									  question.element("type").getText(),
									  q_l10ns_node.selectSingleNode("row[qid=" + qid + "]/question").getText(),
									  question.element("title").getText(),
									  question.element("mandatory").getText().equals("N") ? "No" : "Yes",
									  q_l10ns_node.selectSingleNode("row[qid=" + qid + "]/language").getText());
			addCondition(q);

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
					if (check_int_only(q)) {
						q.setType("I");
					}
					add_range(q);
					survey.addQuestion(q);
					break;
				// Single 5-Point Choice
				case "5":
					q.setType("A");
					q.setAnswers(new AnswersList("5pt.cl", ClGenerator.getIntCl(5), "string", true));
					survey.addQuestion(q);
					break;
				// Yes/No
				case "Y":
					q.setType("A");
					q.setAnswers(new AnswersList("YN.cl", ClGenerator.getYNCl(), "string", false));
					survey.addQuestion(q);
					break;
				// Gender
				case "G":
					q.setType("A");
					q.setAnswers(new AnswersList("Gender.cl", ClGenerator.getGenderCl(), "string", false));
					survey.addQuestion(q);
					break;
				// List with comment
				case "O":
					addComment(q);
				// Radio List
				case "L":
				// Dropdown List
				case "!":
					if (question.elementTextTrim("other").equals("Y")) {
						addOtherQuestion(q);
					}
					q.setType("A");
					q.setAnswers(new AnswersList(q.getQid() + ".cl", getAnswerCodesByID(q.qid), "string", false));
					survey.addQuestion(q);
					break;
				// Multiple Texts
				// Input On Demand
				case "Q":
					addSubquestions(q, "T");
					break;
				// Multiple Numeric Inputs
				case "K":
					add_range(q);
					if (check_int_only(q)){
						addSubquestions(q, "I");
					} else {
						addSubquestions(q, "N");
					}
					break;
				// Dual scale array
				case "1":
					HashMap<String, String>[] code_lists = ClGenerator.getDualScaleCls(q.getQid(), getAnswerIdsByID(q.getQid()), a_node);
					addSubquestionsWithCL(q, q.getQid() + ".0", code_lists[0], "string", false, "-0");
					addSubquestionsWithCL(q, q.getQid() + ".1", code_lists[1], "string", false, "-1");
					break;
				// Array by column
				case "H":
				// Flexible Array
				case "F":
					addSubquestionsWithCL(q, q.getQid().concat(".cl"), getAnswerCodesByID(q.getQid()), "string", false, "");
					break;
				// 5pt Array
				case "A":
					addSubquestionsWithCL(q, "5pt.cl", ClGenerator.getIntCl(5), "integer", true, "");
					break;
				// 10pt Array
				case "B":
					addSubquestionsWithCL(q, "10pt.cl", ClGenerator.getIntCl(10), "integer", true, "");
					break;
				// Increase/Same/Decrease Array
				case "E":
					addSubquestionsWithCL(q, "ISD.cl", ClGenerator.getISDCL(), "string", false, "");
					break;
				// 10pt Array
				case "C":
					addSubquestionsWithCL(q, "YNU.cl", ClGenerator.getYNUCL(), "string", false, "");
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
					addSubquestionsWithCL(q, "MC.cl", ClGenerator.getMCCL(), "string", false, "");
					if (question.elementTextTrim("other").equals("Y")) {
						q.setQid(q.getQid().concat("other"));
						q.setType("T");
						survey.addQuestion(q);
					}
					break;
				// MC with comments
				case "P":
					addMcWithComments(q);
					break;
				default:
					log.error("Question type not supported: " + q.type);
			}
		}
	}

	/**
	 * <p> for a question with numerical input, check if only integers are allowed as answers </p>
	 * @param q The question to check
	 * @return true if only integers, false if floats
	 */

	private boolean check_int_only(Question q)
	{
		Node val = doc.selectSingleNode("//document/question_attributes/rows/row[qid=" + q.getQid() + " and attribute=num_value_int_only]/value");
		return val.getText().equals("1") ? true : false;
	}

	/**
	 * <p> For a question with numerical input, check if there are range limitations and add them, if there are any </p>
	 * @param q The question to check
	 */
	private void add_range(Question q)
	{
		String min = doc.selectSingleNode("//document/question_attributes/rows/row[qid=" + q.getQid() + " and attribute=min_num_value_n]/value").getText();
		String max = doc.selectSingleNode("//document/question_attributes/rows/row[qid=" + q.getQid() + " and attribute=max_num_value_n]/value").getText();

		if (min != "") {
			q.setFloat_range_min(min);
		}
		if (max != "") {
			q.setFloat_range_max(max);
		}
	}

//=======================================================add conditions=============================================================

	/**
	 * <p>
	 * Adds a condition to survey.cond_list, if there are any, if there are multiple ones, they are concatenated with "AND"
	 * The entire expression is then negated
	 * Also adds the id to "cond" of the question, if there is at least one condition
	 * </p>
	 * @param q The question, to which corresponding conditions will be searched and added
	 * @return 0 if there are any conditions, -1 if not
	 */
	private int addCondition(Question q)
	{
		String condition = "NOT(";
		@SuppressWarnings("unchecked")
		List<Element> cond_elements = doc.selectNodes("//document/conditions/rows/row[qid=" + q.getQid() + "]");
		int i = 0;
		for (Element c : cond_elements) {
			i++;
			Pattern ans_p = Pattern.compile("^\\d+X(\\d+)X(.+?)$");
			Matcher match = ans_p.matcher(c.elementText("cfieldname"));
			match.find();
			String cond_str = "(";
			String path = "$(SE-" + prop.getProperty("dummy.study_event_oid") + "/F-" + survey.getId() + "/IG-" + match.group(1) + "/I-" + match.group(2) + ")";

			if (c.elementText("method").equals("RX")) {
				cond_str += "MATCH(";

				String regex = c.elementText("value");
				int regex_length = regex.length();
				// Remove beginning whitespace
				regex = regex.substring(1, regex_length);

				cond_str += (regex + ", " + path + ")");
			} else {
				cond_str += path;
				String val = c.elementText("value");
				cond_str += (c.elementText("method") + (val.equals("") ? "NULL" : val));
			}

			cond_str += ")";
			condition = condition.concat(cond_str);
			condition = condition.concat(i < cond_elements.size()? " AND " : ")");
		}
		
		if (cond_elements.size() != 0) {
			survey.addCondition(new Condition(prop.getProperty("imi.syntax_name"), q.getQid().concat(prop.getProperty("ext.cond")), condition));
			q.setCond(q.getQid().concat(prop.getProperty("ext.cond")));
			return 0;
		}
		return -1;
	}

//=======================================================add questions==============================================================

	/**
	 * <p> For a question, that has subquestions, add all subquestions to the list as individual questions </p>
	 * @param q The parent question
	 * @param type The type of answer expected for each subquestion ("T" for text and "N" for numeric)
	 */
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

	/**
	 * <p> For a question that has subquestions and a set list of answer options, add all subquestions to the list as individual questions </p>
	 * @param q The parent question
	 * @param oid The OID for the code list
	 * @param ans The Map with the answer options
	 * @param t The type of the answer ("integer" or "string")
	 * @param b True, if the code list is simple, meaning the answer in the lsr is equal to the entire answer, false if the answer in the lsr contains the code of the answer instead
	 * @param qid_append A string appended to the QID of all subquestions 
	 */
	private void addSubquestionsWithCL(Question q, String oid, HashMap<String, String> ans, String t, boolean b, String qid_append)
	{
		List<String> sqids = getSqIds(q.getQid());
		for (String sqid : sqids) {
			String question = q_l10ns_node.selectSingleNode("row[qid=" + sqid + "]/question").getText();
			String sq_title = sq_node.selectSingleNode("row[qid=" + sqid + "]/title").getText();
			Question sq = new Question(q.qid + sq_title + qid_append,
					q.gid,
					"A",
					q.question + " " + question,
					sqid,
					q.mandatory,
					q.language);
			sq.setHelp(q.help);
			sq.setAnswers(new AnswersList(oid, ans, t, b));

			survey.addQuestion(sq);
		}
	}

	/**
	 * <p> For a question of the matrix type (type == ; || :) find all subquestions and add a question for each cell in the matrix</p>
	 * @param q The parent question for the matrix
	 */
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

	/**
	 * <p> If a question has the answer option "Other", add another question, which stores the text written by a participant in "Other"
	 * Also adds a condition, that makes the connection between both questions clear </p>
	 * @param q The question, which has an "Other" option
	 */
	private void addOtherQuestion(Question q)
	{
		Question q_other = new Question(q);
		q_other.setQid(q_other.getQid().concat("other"));
		q_other.setType("T");
		survey.addQuestion(q_other);
		/*	Cond: SE-StudyEventOID/F-FormOID[RepeatKey]/IG-ItemGroupOID/I-ItemOID == "-oth-"
		 *
		 */
		String cond_oid = q.getQid().concat(prop.getProperty("ext.cond"));
		log.debug("Added cond_oid");
		String cond_str = "$(SE-" + prop.getProperty("dummy.study_event_oid") + "/F-" + survey.getId() + "/IG-" + q.getGid() + "/I-" + q.getQid() + ")!=\"-oth-\"";
		log.debug("added cond_str");
		survey.addCondition(new Condition(prop.getProperty("imi.syntax_name"), cond_oid, cond_str));
		q_other.setCond(cond_oid);
	}

	/**
	 * <p> If a question has the option to add comments to the answer, add another question, which stores the text written by a participant in "Comment"
	 * Also adds a condition, that makes the connection between both questions clear </p>
	 * @param q The question, which can be commented
	 */
	private void addComment(Question q)
	{
		Question q_comm = new Question(q);
		q_comm.setQid(q_comm.getQid().concat("comment"));
		q_comm.setType("T");
		/*	Cond: SE-StudyEventOID/F-FormOID[RepeatKey]/IG-ItemGroupOID/I-ItemOID == "-oth-"
		 */
		String cond_oid = q.getQid().concat(prop.getProperty("ext.cond"));
		String cond_str = "$(SE-" + prop.getProperty("dummy.study_event_oid") + "/F-" + survey.getId() + "/IG-" + q.getGid() + "/I-" + q.getQid() + ")==NULL";
		survey.addCondition(new Condition(prop.getProperty("imi.syntax_name"), cond_oid, cond_str));
		q_comm.setCond(cond_oid);
		survey.addQuestion(q_comm);
	}

	/**
	 * <p> Equvialent to addSubquestionsWithCL, but also calls addComment for each subquestion</p>
	 * @param q The multiple choice question
	 */
	private void addMcWithComments(Question q)
	{
		List<String> sqids = getSqIds(q.getQid());
		for (String sqid : sqids) {
			String question = q_l10ns_node.selectSingleNode("row[qid=" + sqid + "]/question").getText();
			String sq_title = sq_node.selectSingleNode("row[qid=" + sqid + "]/title").getText();
			Question sq = new Question(q.qid + sq_title,
					q.gid,
					"A",
					q.question + " " + question,
					sqid,
					q.mandatory,
					q.language);
			sq.setHelp(q.help);
			sq.setAnswers(new AnswersList("MC.cl", ClGenerator.getMCCL(), "string", false));

			survey.addQuestion(sq);

			// Add comment question
			addComment(sq);
		}
	}

//===================================================getters via id=================================================================

	/**
	 * Finds all QIDs of Subquestions that belong to a question
	 * @param qid The ID of the question, for which the subquestion IDs should be returned
	 * @return A List of Strings with the IDs of all subquestions to a parent question with ID qid
	 */
	private List<String> getSqIds(String qid)
	{
		@SuppressWarnings("unchecked")
		List<Element> sq_list = sq_node.selectNodes("row[parent_qid=" + qid + "]/qid");
		return sq_list.stream()
			.map(e -> e.getText())
			.collect(Collectors.toList());  // Make a list of qid elements to a list of strings
	}

	/**
	 * <p> Find all answer options for a question and return them in a map with the code as key and the answer as value </p>
	 * @param id The Question-ID, for which the answers should be returned
	 * @return All answer options for a question with QID id in a map in the format <code, answer>
	 */
	private HashMap<String, String> getAnswerCodesByID(String id)
	{
		HashMap<String, String> ans_map = new HashMap<String, String>();
		Node ans_l10ns = doc.selectSingleNode("//document/answer_l10ns/rows");
		@SuppressWarnings("unchecked")
		List<Element> a_meta = doc.selectNodes("//document/answers/rows/row[qid=" + id + "]");
		for (Element elem : a_meta) {
			ans_map.put(elem.elementText("code"), ans_l10ns.selectSingleNode("row[aid=" + elem.elementText("aid") + "]/answer").getText());
		}
		return ans_map;
	}

	/**
	 * <p> Finds all answer options for a question and returns them in a map with the Answer-ID as key and the answer as value </p>
	 * @param id The Question-ID, for which the answers should be returned
	 * @return All answer options for a question with QID id in a map in the format <AID, answer>
	 */
	private HashMap<String, String> getAnswerIdsByID(String id)
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
}
