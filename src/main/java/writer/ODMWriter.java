package writer;

import parser.Survey;
import parser.Response;
import parser.Answer;
import parser.Condition;
import parser.QuestionGroup;
import parser.Question;

import java.time.LocalDateTime;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

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
	Element clinical_data;

	Survey survey;
	Properties prop;
	
	String meta_data_oid;
	HashMap<Integer, Element> question_groups;
	ArrayList<String> written_cl_oids;
	
	public ODMWriter(Survey s)
	{
		this.survey = s;
		this.doc = DocumentHelper.createDocument();
		this.question_groups = new HashMap<Integer, Element>();
		this.written_cl_oids = new ArrayList<String>();

		// Load properties from the config file
		try (InputStream input = new FileInputStream("src/main/java/app/config.properties")) {
			prop = new Properties();
            prop.load(input);
        } catch (IOException ex) {
            log.error(ex.getClass());
        }
	}

	public void createODMFile()
	{
		createODMRoot();
		addStudyData();
		addQuestionGroups();
		
		// Code Lists have to be insterted at a certain point inbetween other elements
		// save them to a separate document first and insert all at the end
		Document tmp = DocumentHelper.createDocument();
		code_lists = tmp.addElement("code_lists");

		// Exectue main functions
		addQuestions();
		addConditions();
		addClinicalDataElement();
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
			log.error(e);
		}
	}

//===================================== private functions =======================================
	private void createODMRoot()
	{
		root = doc.addElement("ODM")
					.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
					.addAttribute("xmlns", "http://www.cdisc.org/ns/odm/v1.3")
					.addAttribute("xsi:schemaLocation", "")
					.addAttribute("Description", "ODM-version of LimeSurvey Survey with ID: " + survey.getId())
					.addAttribute("FileType", "Snapshot")
					.addAttribute("FileOID", survey.getId())
					.addAttribute("CreationDateTime", LocalDateTime.now().toString())
					.addAttribute("ODMVersion", "1.3");
	}


	private void addStudyData()
	{
		// Study Element
		Element study = root.addElement("Study")
			.addAttribute("OID", prop.getProperty("dummy.survey_oid"));

		// Add the global variables
		Element glob_var = study.addElement("GlobalVariables");
		glob_var.addElement("StudyName").addText(prop.getProperty("dummy.study_name"));
		glob_var.addElement("StudyDescription");
		glob_var.addElement("ProtocolName").addText(prop.getProperty("dummy.protocol_name"));
		
		// Construct the MetaDataOID
		meta_data_oid = prop.getProperty("odm.meta_data_prefix") + survey.getId();

		// Add the MetaDataVersion
		meta_data = study.addElement("MetaDataVersion")
						 .addAttribute("OID", meta_data_oid)
						 .addAttribute("Name", "");
		meta_data.addElement("Protocol").addElement("StudyEventRef")
										.addAttribute("StudyEventOID", prop.getProperty("dummy.study_event_oid"))
										.addAttribute("Mandatory", "No");

		// Add the dummy StudyEvent
		Element study_event = meta_data.addElement("StudyEventDef")
									   .addAttribute("OID", prop.getProperty("dummy.study_event_oid"))
									   .addAttribute("Name", prop.getProperty("dummy.study_event_name"))
									   .addAttribute("Repeating", "No")
									   .addAttribute("Type", "Common");
		study_event.addElement("FormRef")
				   .addAttribute("FormOID", survey.getId())
				   .addAttribute("Mandatory", "No");

		// Add the Form
		form = meta_data.addElement("FormDef")
						.addAttribute("OID", survey.getId())
						.addAttribute("Name", survey.getName())
						.addAttribute("Repeating", "No");
		form.addElement("Description").addText(survey.getDescription());
	}

	private void addQuestionGroups()
	{
		for (QuestionGroup qg : survey.getGroups()) {

			// Add the reference
			form.addElement("ItemGroupRef")
				.addAttribute("ItemGroupOID", qg.getGIDString())
				.addAttribute("Mandatory", "Yes"); // TODO: Dynamic mandatory?

			// Add the question group
			Element qg_elem =  meta_data.addElement("ItemGroupDef")
										.addAttribute("OID", qg.getGIDString())
										.addAttribute("Name", qg.getName())
										.addAttribute("Repeating", "No");
			
			if (qg.getDescription() != null) {
				qg_elem.addElement("Description")
					   .addText(qg.getDescription());
			}
			
			question_groups.put(qg.getGID(), qg_elem);
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
			if (q.getCond() == "") {
				addQuestionRef(q.getGid(), q.getQid(), q.getMandatory());
			}
			else {
				addQuestionRefWithCond(q.getGid(), q.getQid(), q.getMandatory(), q.getCond());
			}
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

	private void addConditions()
	{
		for (Condition c : survey.getCond_list()) {
			Element fe = meta_data.addElement("ConditionDef")
					 .addAttribute("OID", c.getOid())
					 .addAttribute("Name", c.getOid())
					 .addElement("FormalExpression")
					 .addAttribute("Context", c.getType());
			fe.addText(c.getCond());
		}
	}

	private void addClinicalDataElement()
	{
		clinical_data = root.addElement("ClinicalData")
			.addAttribute(" StudyOID", prop.getProperty("dummy.survey_oid"))
			.addAttribute("MetaDataVersionOID", meta_data_oid);
	}

	public void addAnswers(LinkedList<Response> responses)
	{
		for (Response r : responses) {
			Element form_data = clinical_data.addElement("SubjectData")
				.addAttribute("SubjectKey", Integer.toString(r.getId()))
				.addElement("StudyEventData")
				.addAttribute(" StudyEventOID", prop.getProperty("dummy.study_event_oid"))
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
				.addAttribute("Name", answers_oid)
				.addAttribute("DataType", q.getAnswers().getType());
			if (q.getAnswers().isSimple()) {
				for (Map.Entry<String, String> e : q.getAnswers().getAnswers().entrySet()) {
					cl.addElement("EnumeratedElement")
						.addAttribute("CodedValue", e.getValue());
				}
			} else {
				for (Map.Entry<String, String> e : q.getAnswers().getAnswers().entrySet()) {
					cl.addElement("CodeListItem")
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

	private void addQuestionRefWithCond(int gid, String qid, String mandatory, String cond_oid)
	{
		question_groups.get(gid).addElement("ItemRef")
			.addAttribute("ItemOID", qid)
			.addAttribute("Mandatory", mandatory)
			.addAttribute("CollectionExceptionConditionOID", cond_oid);
	}
}
