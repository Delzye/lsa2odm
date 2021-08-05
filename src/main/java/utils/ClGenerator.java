/* MIT License

Copyright (c) 2021 Anton Mende (Delzye)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */
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
