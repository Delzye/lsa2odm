package writer;

import parser.lss.Survey;
import parser.lss.Condition;
import parser.lss.QuestionGroup;
import parser.lss.Question;
import parser.lsr.Response;
import parser.lsr.Answer;

import java.time.LocalDateTime;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
	protected Document doc;
	protected Element root;
	protected Element meta_data;
	protected Element form;
	protected Element code_lists;
	protected Element clinical_data;

	protected Survey survey;
	protected Properties prop;
	
	protected HashMap<Integer, Element> question_groups;
	protected ArrayList<String> written_cl_oids;
	
	public ODMWriter(Survey s)
	{
		this.survey = s;
		this.doc = DocumentHelper.createDocument();
		this.question_groups = new HashMap<Integer, Element>();
		this.written_cl_oids = new ArrayList<String>();

		// Load properties from the config file
		try (InputStream input = ODMWriter.class.getResourceAsStream("/config.properties")) {
			prop = new Properties();
            prop.load(input);
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
	}

	/**
	 * <p>Create the ODM structure and add ItemGroups and Items to it</p>
	 * */
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

	/**
	 * <p>Add SubjectData etc. to clinical data from the list</p>
	 * @param responses A list of all {@link Response} elements, which will be added to the ODM file
	 * */
	public void addAnswers(ArrayList<Response> responses)
	{
		log.info("Adding Answers to the ODM document");
		HashMap<String, Element> subject_se_list = new HashMap<>();
		// For each response add a subject_data element
		for (Response r : responses) {
			Element se = subject_se_list.get(r.getId());
			if (se == null) {
				se = clinical_data.addElement("SubjectData")
								  .addAttribute("SubjectKey", r.getId())
								  .addElement("StudyEventData")
								  .addAttribute("StudyEventOID", prop.getProperty("dummy.study_event_oid"));
				subject_se_list.put(r.getId(), se);
			}

			Element form_data = se.addElement("FormData")
								  .addAttribute("FormOID", survey.getId())
								  .addAttribute("FormRepeatKey", r.getRepeat_key());

			// Answers are split into multiple lists, one per question group
			// For each question group add a ItemGroup
			for (Map.Entry<Integer, ArrayList<Answer>> entry : r.getAnswers().entrySet()) {
				Element ig_data = form_data.addElement("ItemGroupData")
					.addAttribute("ItemGroupOID", Integer.toString(entry.getKey()));

				// For each answer in the group add an ItemData element
				for (Answer a : entry.getValue()) {
					ig_data.addElement("ItemData")
						.addAttribute("ItemOID", a.getQid())
						.addAttribute("Value", a.getAnswer());
				}
			}
		}
	}

	/**
	 * <p>Write the document, which was created in createODMFile() to the path parameter</p>
	 * @param path Where to write the file
	 * */
	public void writeFile(String path)
	{
		log.info("Writing ODM-File");
		try{
			FileWriter fileWriter = new FileWriter(path + survey.getId() + ".xml");
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter(fileWriter, format);
			writer.write(doc);
			writer.close();
		} catch (Exception e) {
			log.error(e);
		}
	}
//===================================== private functions =======================================
	/**
	 * <p>Create the root element ODM</p>
	 * */
	private void createODMRoot()
	{
		log.info("Creating the ODM root element");
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


	/**
	 * <p>Add the document structure from Study to FormDef</p>
	 * */
	private void addStudyData()
	{
		log.info("Adding study data to ODM");
		// Study Element
		Element study = root.addElement("Study")
			.addAttribute("OID", prop.getProperty("dummy.survey_oid"));

		// Add the global variables
		Element glob_var = study.addElement("GlobalVariables");
		glob_var.addElement("StudyName").addText(prop.getProperty("dummy.study_name"));
		glob_var.addElement("StudyDescription");
		glob_var.addElement("ProtocolName").addText(prop.getProperty("dummy.protocol_name"));
		
		// Construct the MetaDataOID
		String meta_data_oid = prop.getProperty("odm.meta_data_prefix") + survey.getId();

		// Add the MetaDataVersion
		meta_data = study.addElement("MetaDataVersion")
						 .addAttribute("OID", meta_data_oid)
						 .addAttribute("Name", meta_data_oid);
		meta_data.addElement("Protocol").addElement("StudyEventRef")
										.addAttribute("StudyEventOID", prop.getProperty("dummy.study_event_oid"))
										.addAttribute("Mandatory", "Yes");

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
						.addAttribute("Repeating", "Yes");
		form.addElement("Description").addText(survey.getDescription());
	}

	/**
	 * <p>Add the ItemGroup Elements to the document from the data in the {@link Survey}</p>
	 * */
	private void addQuestionGroups()
	{
		log.info("Adding question groups to the ODM document");
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
			
			question_groups.put(qg.getGid(), qg_elem);
			log.debug("Added the question group: " + qg.getGid());
		}
	}

	/**
	 * <p>Add Item Elements from the data in the {@link Survey}</p>
	 * */
	private void addQuestions()
	{
		log.info("Adding questions to the ODM document");
		// Save code lists in another document temporarily so we don't have to insert between existing elements
		Document tmp = DocumentHelper.createDocument();
		tmp.addElement("code_lists");

		for (Question q : survey.getQuestions()) {
			log.debug("Adding question " + q.getQid() + " to group: " + q.getGid());
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
				case "I":
					addQuestion(q, "integer");
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

	/**
	 * <p>Add ConditionDef elements from the data in the {@link Survey}/p>
	 * */
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

	/**
	 * <p>Insert the clinical data element into the document</p>
	 * */
	private void addClinicalDataElement()
	{
		log.info("Adding the clincal data element to ODM");
		String meta_data_oid = prop.getProperty("odm.meta_data_prefix") + survey.getId();

		clinical_data = root.addElement("ClinicalData")
			.addAttribute("StudyOID", prop.getProperty("dummy.survey_oid"))
			.addAttribute("MetaDataVersionOID", meta_data_oid);
	}

//====================================== helper functions ======================================= 
	/**
	 * <p>Add a single ItemDef from the Question q</p>
	 * @param q The {@link Question} containing the data
	 * @param data_type The DataType for the ItemDef
	 * @return A reference to the created Element
	 * */
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

		// integer range checks for numerical questions
		if (q.getFloat_range_min() != null) {
			e.addElement("RangeCheck")
			 .addAttribute("Comparator", "GE")
			 .addElement("CheckValue")
			 .addText(q.getFloat_range_min());
		}
		if (q.getFloat_range_max() != null) {
			e.addElement("RangeCheck")
			 .addAttribute("Comparator", "LE")
			 .addElement("CheckValue")
			 .addText(q.getFloat_range_max());
		}

		return e;
	}

	/**
	 * <p>Add a single ItemDef from the Question q with a CodeListRef</p>
	 * @param q The {@link Question} containing the data
	 * */
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

	/**
	 * <p>Add a ItemRef to a ItemGroupDef</p>
	 * @param gid The ID of the group to which the Ref will be added
	 * @param qid The ID of the question
	 * @param mandatory Whether or not the question has to be answered
	 * */
	private void addQuestionRef(int gid, String qid, String mandatory)
	{
		question_groups.get(gid).addElement("ItemRef")
			.addAttribute("ItemOID", qid)
			.addAttribute("Mandatory", mandatory);
	}

	/**
	 * <p>Add a ItemRef with a CollectionExceptionConditionOID to a ItemGroupDef</p>
	 * @param gid The ID of the group to which the Ref will be added
	 * @param qid The ID of the question
	 * @param mand Whether or not the question has to be answered
	 * @param cond_oid The ID of the condition
	 * */
	private void addQuestionRefWithCond(int gid, String qid, String mand, String cond_oid)
	{
		question_groups.get(gid).addElement("ItemRef")
			.addAttribute("ItemOID", qid)
			.addAttribute("Mandatory", mand)
			.addAttribute("CollectionExceptionConditionOID", cond_oid);
	}
}
