package utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;

public class ClGenerator
{
//============================================code list generation methods==========================================================

	public static HashMap<String, String> getIntCl(int l)
	{
		HashMap<String, String> ans_map = new HashMap<>();
		for (int i = 1; i <= l; i++){
			String a = Integer.toString(i);
			ans_map.put(a,a);
		}
		return ans_map;
	}

	public static HashMap<String, String> getYNCl()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("Y", "yes");
		ans_map.put("N", "no");
		return ans_map;
	}

	public static HashMap<String, String> getGenderCl()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("M", "male");
		ans_map.put("F", "female");
		return ans_map;
	}

	public static HashMap<String, String> getISDCL()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("I", "Increase");
		ans_map.put("S", "Same");
		ans_map.put("D", "Decrease");
		return ans_map;
	}

	public static HashMap<String, String> getYNUCL()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("Y", "Yes");
		ans_map.put("N", "No");
		ans_map.put("U", "Uncertain");
		return ans_map;
	}

	public static HashMap<String, String> getMCCL()
	{
		HashMap<String, String> ans_map = new HashMap<>();
		ans_map.put("Y", "Yes");
		ans_map.put("", "No");
		return ans_map;
	}

	public static HashMap<String, String>[] getDualScaleCls(String qid, HashMap<String, String> ids, Node a_node)
	{
		@SuppressWarnings("unchecked")
		HashMap<String, String>[] cls = new HashMap[2];
		cls[0] = new HashMap<>();
		cls[1] = new HashMap<>();
		Iterator<Map.Entry<String,String>> iter = ids.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String,String> entry = iter.next();
			Element row = (Element) a_node.selectSingleNode("row[aid=" + entry.getKey() + "]");

			if (row.elementText("scale_id").equals("0")) {
				cls[0].put(row.elementText("code"), entry.getValue());
				iter.remove();
			}

			if (row.elementText("scale_id").equals("1")) {
				cls[1].put(row.elementText("code"), entry.getValue());
				iter.remove();
			}
		}
		return cls;
	}
}
