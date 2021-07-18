package parser.lsr;

import parser.lss.QuestionGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.log4j.Log4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

@Log4j
public class LsrParser
{
	protected File lsr_file;
	protected Document doc;
	protected Element rows;
	protected ArrayList<QuestionGroup> qg_list;
	protected List<String> date_time_qids;

	public LsrParser(File lsr_file, ArrayList<QuestionGroup> qg_list, ArrayList<String> dt_qids)
	{
		this.lsr_file = lsr_file;
		this.qg_list = qg_list;
		this.date_time_qids = dt_qids;
	}

	/**
	 * <p>Create a new document in doc and put the contents of the lsr file into it</p>
	 *
	 * */
	public void createDocument()
	{
		try {
		SAXReader saxReader = new SAXReader();
		doc = saxReader.read(lsr_file); 
		} catch (Exception e) {
			log.error("Could not read lsr_file: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * <p>Iterate over all rows in responses and create Response Instances from the data</p>
	 * @return A list of all created Response-Objects
	 * */
	public ArrayList<Response> parseAnswers()
	{
		log.info("Parsing Responses");
		@SuppressWarnings("unchecked")
		List<Element> row_list = doc.selectNodes("//document/responses/rows/row");

		ArrayList<Response> responses = new ArrayList<>();

		HashMap<String, Integer> rep_keys = new HashMap<>();
		
		// Iterate over all row-Elements (one per survey-participant)
		for (Element row : row_list) {

			Response r = new Response();
			Element token = row.element("token");
			if (token == null) {
				r.setId(row.element("id").getText()); 
			} else {
				r.setId(token.getText());
			}

			int rep_key = rep_keys.get(r.getId()) == null ? 1 : rep_keys.get(r.getId());
			r.setRepeat_key(rep_key + "");
			rep_keys.put(r.getId(), ++rep_key);

			// Sort Answers by the question group the question belongs to
			for (QuestionGroup qg : qg_list) {
				r.answers.put(qg.getGid(), new ArrayList<Answer>());
			}
			@SuppressWarnings("unchecked")
			List<Element> children = row.elements();

			// Iterate over all elements in row (most of which are answers, but also id, language etc.)
			for (Element e : children) {

				// Pattern for Answer-Names: _{Survey_id}X{gid}X{qid}{subquestion_title}?{ext}?
				// Where ext can be one of "other|comment"
				Pattern ans_p = Pattern.compile("^_\\d+X(\\d+)X(.+?)$");
				log.debug(e.getName());
				Matcher e_match = ans_p.matcher(e.getName());

				// If this is an answer and not another element
				if (e_match.find()) {
					log.debug("Found matching Element, parsing first group: " + e_match.group(1) + " Second group: " + e_match.group(2));
					String gid = e_match.group(1);
					String qid = e_match.group(2);

					log.debug("Working on question: " + qid);

					String ans = e.getText();

					// only add an answer if it isn't empty
					if (ans != "") { 
						if (date_time_qids.contains(qid)) {
							ans = ans.replace(" ", "T");
						}
						Answer a = new Answer(Integer.parseInt(gid), qid, ans);
						r.addToAnswers(a);
					}
				}
			}
			responses.add(r);
		}
		return responses;
	}
}
