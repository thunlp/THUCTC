package org.thunlp.language.chinese;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BigramWordSegment implements WordSegment, Serializable {
	private List<String> segs = new ArrayList<String>();
	
	public boolean outputPosTag() {
		return false;
	}

	public String[] segment(String text) {
		int start, end;
		
		start = 0;
		end = 0;
		segs.clear();
		boolean precedentByChinese = false;
		while ( end < text.length() ) {
			if ( Character.isSpaceChar(text.charAt(end)) ) {
				segs.add(text.substring(start, end));
				while ( end < text.length() && Character.isSpaceChar( text.charAt(end) ) )
						end++;
				if ( end >= text.length() )
					break;
				start = end;
			}
			if ( LangUtils.isChinese(text.codePointAt(end)) ) {
				if ( end > start ) {
					segs.add(text.substring(start, end));
					precedentByChinese = false;
				} else {
					precedentByChinese = true;
				}
				start = end;
				if ( start < text.length() - 1 && 
				    LangUtils.isChinese(text.codePointAt(start + 1)) ) {		
					segs.add(text.substring(start, start+2));
					end = ++start;
				} else {
					if ( ! precedentByChinese ) {
						segs.add(text.substring(start, start + 1));
					}
					end = ++start;
				}
			} else {
				end ++;
			}
		}
		return segs.toArray(new String[segs.size()]);
	}

	public static void main (String args[]) {
		BigramWordSegment bws = new BigramWordSegment();
		String [] result = bws.segment("客户端没有在限定的时间内将数据2012发送给服务器，Currently, the aerotriangulation object space coordinate system of aerial image is built on the tangent plane of the earth, and the scope of data processing is limited by the geographical scope. 服务器为了保证服务性能，认定那个连接已经失效，所以出现上述异常。 ");
		for (String str : result) {
			System.out.println(str);
		}
	}
	
}
