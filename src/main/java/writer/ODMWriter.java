package writer;

import parser.Survey;
import parser.Response;
import parser.Answer;
import parser.QuestionGroup;
import parser.Question;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.log4j.Log4j;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

@Log4j
public class ODMWriter
{
	Document doc;
	Element root;
	Element meta_data;
	Element form;
	Element code_lists;

	Survey survey;
	ArrayList<Response> responses;
	
	String meta_data_oid;
	String survey_oid;
	String study_event_oid;
	HashMap<Integer, Element> question_groups;
	ArrayList<String> written_cl_oids;
	
	public ODMWriter(Survey s, ArrayList<Response> r)
	{
		this.survey = s;
		this.responses = r;
		this.doc = DocumentHelper.createDocument();
		this.question_groups = new HashMap<Integer, Element>();
		this.written_cl_oids = new ArrayList<String>();
	}

	public void createODMFile()
	{
		createODMRoot();
		addStudyData();
		addQuestionGroups();
		
		Document tmp = DocumentHelper.createDocument();
		code_lists = tmp.addElement("code_lists");
		addQuestions();

		writeAnswersToDocument();
	}

	public void writeFile()
	{
		try{
			FileWriter fileWriter = new FileWriter("src/main/xml/odm.xml");
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter(fileWriter, format);
			writer.write(doc);
			writer.close();
		} catch (Exception e) {
			log.info(e);
		}
	}

//===================================== private functions =======================================
	private void createODMRoot()
	{
		root = doc.addElement("ODM")
					.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
					.addAttribute("xmlns", "http://www.cdisc.org/ns/odm/v1.3")
					.addAttribute("xsi:schemaLocation", "")
					.addAttribute("Description", "")
					.addAttribute("FileType", "Snapshot")
					.addAttribute("FileOID", "")
					.addAttribute("CreationDateTime", "")
					.addAttribute("ODMVersion", "1.3")
					.addAttribute("Originator", "")
					.addAttribute("SourceSystem", "")
					.addAttribute("SourceSystemVersion", "");
	}


	private void addStudyData()
	{
		survey_oid = "PlaceholderOID";
		Element study = root.addElement("Study")
			.addAttribute("OID", survey_oid);

		Element glob_var = study.addElement("GlobalVariables");
		glob_var.addElement("StudyName");
		glob_var.addElement("StudyDescription");
		glob_var.addElement("ProtocolName");
		
		meta_data_oid = "MetaData" + survey.getId();
		study_event_oid = "Event.1";
		meta_data = study.addElement("MetaDataVersion")
						 .addAttribute("OID", meta_data_oid)
						 .addAttribute("Name", "");
		meta_data.addElement("Protocol").addElement("StudyEventRef")
										.addAttribute("StudyEventOID", study_event_oid)
										.addAttribute("Mandatory", "No");

		Element study_event = meta_data.addElement("StudyEventDef")
									   .addAttribute("OID", study_event_oid)
									   .addAttribute("Repeating", "No")
									   .addAttribute("Type", "Common");
		study_event.addElement("FormRef")
				   .addAttribute("FormOID", survey.getId())
				   .addAttribute("Mandatory", "No");

		form = meta_data.addElement("FormDef")
						.addAttribute("OID", survey.getId())
						.addAttribute("Name", survey.getName())
						.addAttribute("Repeating", "No");
	}

	private void addQuestionGroups()
	{
		for (QuestionGroup qg : survey.getGroups()) {
			form.addElement("ItemGroupRef")
				.addAttribute("ItemGroupOID", qg.getGIDString())
				.addAttribute("Mandatory", "Yes"); // TODO: Dynamic mandatory?
			question_groups.put(qg.getGID(), meta_data.addElement("ItemGroupDef")
										  .addAttribute("OID", qg.getGIDString())
										  .addAttribute("Name", qg.getName())
										  .addAttribute("Repeating", "No"));
			log.info("Added the question group: " + qg.getGID());
		}
	}

	private void addQuestions()
	{
		// Save code lists in another document temporarily so we don't have to insert between existing elements
		Document tmp = DocumentHelper.createDocument();
		tmp.addElement("code_lists");

		for (Question q : survey.getQuestions()) {
			log.info("Adding question to group: " + q.getGid());
			addQuestionRef(q.getGid(), q.getQid(), q.getMandatory());
			switch (q.getType()) {
				case "T":
					addQuestion(q, "string");
					break;
				case "A":
					addQuestionWithCL(q);
					break;
				case "N":
					addQuestion(q, "float");
					break;
				case "D":
					addQuestion(q, "datetime");
					break;
				 default:
					log.info("Not yet supported: " + q.getType());
					break;
			}
		}
		meta_data.appendContent(code_lists);
	}

	private void writeAnswersToDocument()
	{
		Element clinical_data = root.addElement("ClinicalData")
			.addAttribute(" StudyOID", survey_oid)
			.addAttribute("MetaDataVersionOID", meta_data_oid);

		for (Response r : responses) {
			Element form_data = clinical_data.addElement("SubjectData")
				.addAttribute("SubjectKey", Integer.toString(r.getId()))
				.addElement("StudyEventData")
				.addAttribute(" StudyEventOID", study_event_oid)
				.addElement("FormData")
				.addAttribute("FormOID", survey.getId());
			for (Map.Entry<Integer, ArrayList<Answer>> entry : r.getAnswers().entrySet()) {
				Element ig_data = form_data.addElement("ItemGroupData")
					.addAttribute("ItemGroupOID", Integer.toString(entry.getKey()));
				for (Answer a : entry.getValue()) {
					ig_data.addElement("ItemData")
						.addAttribute("ItemOID", a.getQid())
						.addAttribute("Value", a.getAnswer());
				}
			}
		}
	}
//====================================== helper functions ======================================= 
	private Element addQuestion(Question q, String data_type)
	{
		Element e =  meta_data.addElement("ItemDef")
							  .addAttribute("OID", q.getQid())
							  .addAttribute("Name", q.getTitle())
							  .addAttribute("DataType", data_type);
		e.addElement("Question")
		 .addElement("TranslatedText")
		 .addAttribute("xml:lang", q.getLanguage())
		 .addText(q.getQuestion());
		return e;
	}

	private void addQuestionWithCL(Question q)
	{
		String answers_oid = q.getAnswers().getAnswers_oid();
		addQuestion(q, q.getAnswers().getType()).addElement("CodeListRef")
			.addAttribute("CodeListOID", answers_oid);
		if (!written_cl_oids.contains(answers_oid)) {
			Element cl = code_lists.addElement("CodeList")
				.addAttribute("OID", answers_oid)
				.addAttribute("Name", "")
				.addAttribute("DataType", q.getAnswers().getType());
			if (q.getAnswers().isSimple()) {
				for (Map.Entry<String, String> e : q.getAnswers().getAnswers().entrySet()) {
					cl.addElement("EnumeratedElement")
						.addAttribute("CodedValue", e.getValue());
				}
			} else {
				for (Map.Entry<String, String> e : q.getAnswers().getAnswers().entrySet()) {
					cl.addElement("CodeListElement")
						.addAttribute("CodedValue", e.getKey())
						.addElement("Decode")
						.addElement("TranslatedText")
						.addAttribute("xml:lang", "")
						.addText(e.getValue());
				}
			}
			written_cl_oids.add(answers_oid);
		}
	}

	private void addQuestionRef(int gid, String qid, String mandatory)
	{
		question_groups.get(gid).addElement("ItemRef")
			.addAttribute("ItemOID", qid)
			.addAttribute("Mandatory", mandatory);
	}
}
