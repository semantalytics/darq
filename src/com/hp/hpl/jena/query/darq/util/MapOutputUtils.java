package com.hp.hpl.jena.query.darq.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.query.darq.core.MapMultipleServiceGroup;
import com.hp.hpl.jena.query.darq.core.MapServiceGroup;
import com.hp.hpl.jena.query.darq.core.RemoteService;
import com.hp.hpl.jena.query.util.IndentedWriter;

public class MapOutputUtils extends OutputUtils {

	public MapOutputUtils() {
		super();
	}

	public static void printServiceGroupArrayList(List<MapServiceGroup> l) {
		IndentedWriter out = new IndentedWriter(System.out);
		outServiceGroupList(l, out);
		out.flush();
	}

	public static String serviceGroupListToString(List<MapServiceGroup> l) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		IndentedWriter out = new IndentedWriter(os);
		outServiceGroupList(l, out);
		out.flush();
		return new String(os.toByteArray());
	}

	public static String serviceGroupToString(MapServiceGroup sg) {
		ArrayList<MapServiceGroup> l = new ArrayList<MapServiceGroup>();
		l.add(sg);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		IndentedWriter out = new IndentedWriter(os);
		outServiceGroupList(l, out);
		out.flush();
		return new String(os.toByteArray());
	}

	public static void outServiceGroupList(List<MapServiceGroup> l, IndentedWriter out) {

		for (MapServiceGroup sg : l) {
			outServiceGroup(sg, out);
		}

	}

	public static void outServiceGroup(MapServiceGroup sg, IndentedWriter out) {

		if (sg instanceof MapMultipleServiceGroup) {
			for (RemoteService s : ((MapMultipleServiceGroup) sg).getServices())
				out.println("+" + s.getLabel() + " (" + s.getUrl() + ")");
		} else {

			out.println(sg.getService().getLabel() + " (" + sg.getService().getUrl() + ")");
		}
		out.incIndent();
		out.println(sg.getTriple().toString());
		out.decIndent();

	}

}
