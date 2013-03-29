package org.aksw.lassie.util;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.lassie.kb.KnowledgeBase;
import org.aksw.lassie.kb.LocalKnowledgeBase;
import org.aksw.lassie.kb.RemoteKnowledgeBase;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.owl.Description;
import org.dllearner.utilities.LabelShortFormProvider;
import org.dllearner.utilities.owl.OWLAPIDescriptionConvertVisitor;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

public class PrintUtils {

	public static String toHTMLWithLabels(Map<Description, List<? extends EvaluatedDescription>> mapping, KnowledgeBase source, KnowledgeBase target){
		ManchesterOWLSyntaxOWLObjectRendererImpl sourceRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
		LabelShortFormProvider sfp;
		if(source.isRemote()){
			sfp = new LabelShortFormProvider(((RemoteKnowledgeBase)source).getEndpoint());
		} else {
			sfp = new LabelShortFormProvider(((LocalKnowledgeBase)source).getModel());
		}
		sourceRenderer.setShortFormProvider(sfp);
		ManchesterOWLSyntaxOWLObjectRendererImpl targetRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
		if(source.isRemote()){
			sfp = new LabelShortFormProvider(((RemoteKnowledgeBase)target).getEndpoint());
		} else {
			sfp = new LabelShortFormProvider(((LocalKnowledgeBase)target).getModel());
		}
		targetRenderer.setShortFormProvider(sfp);
		
		StringBuilder sb = new StringBuilder();
		DecimalFormat dfPercent = new DecimalFormat("0.00%");
		sb.append("<html>\n");
		sb.append("<table>\n");
		sb.append("<thead><tr><th>Source Class</th><th>Target Class Expressions</th><th>Accuracy</th></tr></thead>\n");
		sb.append("<tbody>\n");
		
		
		for (Entry<Description, List<? extends org.dllearner.core.EvaluatedDescription>> entry : mapping.entrySet()) {
			Description key = entry.getKey();
			String renderedKey = sourceRenderer.render(OWLAPIDescriptionConvertVisitor.getOWLClassExpression(key));
			List<? extends org.dllearner.core.EvaluatedDescription> value = entry.getValue();
			if(value == null){
				sb.append("<tr><th>" + renderedKey + "</th>\n");
				sb.append("<tr><td>ERROR</td><td></td></tr>\n");
			} else {
				sb.append("<tr><th rowspan=\"" + (value.size()+1) + "\">" + renderedKey + "</th>\n");
			    for (EvaluatedDescription evaluatedDescription : value) {
			    	sb.append("<tr>"); 
			    	String renderedDesc = targetRenderer.render(OWLAPIDescriptionConvertVisitor.getOWLClassExpression(evaluatedDescription.getDescription()));
			     	sb.append("<td>" + renderedDesc + "</td>");
			     	sb.append("<td>" + dfPercent.format(evaluatedDescription.getAccuracy()) + "</td>");
			     	sb.append("</tr>\n");
				}
			}
		}
		
		sb.append("</tbody>\n");
		sb.append("</table>\n");
		sb.append("</html>\n");
		
		return sb.toString();
	}
	
	public static String toHTML(Map<Description, List<? extends EvaluatedDescription>> mapping){
		StringBuilder sb = new StringBuilder();
		DecimalFormat dfPercent = new DecimalFormat("0.00%");
		sb.append("<html>\n");
		sb.append("<table>\n");
		sb.append("<thead><tr><th>Source Class</th><th>Target Class Expressions</th></tr></thead>\n");
		sb.append("<tbody>\n");
		
		
		for (Entry<Description, List<? extends org.dllearner.core.EvaluatedDescription>> entry : mapping.entrySet()) {
			Description key = entry.getKey();
			List<? extends org.dllearner.core.EvaluatedDescription> value = entry.getValue();
			if(value == null){
				sb.append("<tr><th>" + OWLAPIDescriptionConvertVisitor.getOWLClassExpression(key) + "</th>\n");
				sb.append("<tr><td>ERROR</td></tr>\n");
			} else {
				sb.append("<tr><th rowspan=\"" + value.size()+1 + "\">" + OWLAPIDescriptionConvertVisitor.getOWLClassExpression(key) + "</th>\n");
			    for (EvaluatedDescription evaluatedDescription : value) {
			    	sb.append("<tr><td>" + 
			    OWLAPIDescriptionConvertVisitor.getOWLClassExpression(evaluatedDescription.getDescription()) + 	"(" + dfPercent.format(evaluatedDescription.getAccuracy()) + ")" 
			    			+ "</td></tr>\n");
				}
			}
		}
		
		sb.append("</tbody>\n");
		sb.append("</table>\n");
		sb.append("</html>\n");
		
		return sb.toString();
	}
	
	public static void printMappingPretty(Map<Description, List<? extends EvaluatedDescription>> mapping){
		DecimalFormat dfPercent = new DecimalFormat("0.00%");
		System.out.println("Source Class -> Target Class Expression");
		for (Entry<Description, List<? extends org.dllearner.core.EvaluatedDescription>> entry : mapping.entrySet()) {
			Description key = entry.getKey();
			int length = OWLAPIDescriptionConvertVisitor.getOWLClassExpression(key).toString().length();
			String indention = "";
			for(int i = 0; i < length; i++){
				indention += " ";
			}
			List<? extends org.dllearner.core.EvaluatedDescription> value = entry.getValue();
			System.out.println(OWLAPIDescriptionConvertVisitor.getOWLClassExpression(key));
			if(value == null){
				System.out.println(indention + "\t->\t ERROR"); 
			} else {
				for (EvaluatedDescription evaluatedDescription : value) {
					System.out.println(indention + "\t->\t" + 
				OWLAPIDescriptionConvertVisitor.getOWLClassExpression(evaluatedDescription.getDescription()) + 
				"(" + dfPercent.format(evaluatedDescription.getAccuracy()) + ")");
				}
			}
			
		}
	}
	
	public static void printMapping(Map<Description, List<? extends EvaluatedDescription>> mapping){
		System.out.println("Source Class -> Target Class Expression");
		for (Entry<Description, List<? extends org.dllearner.core.EvaluatedDescription>> entry : mapping.entrySet()) {
			Description key = entry.getKey();
			int length = key.toString().length();
			String indention = "";
			for(int i = 0; i < length; i++){
				indention += " ";
			}
			List<? extends org.dllearner.core.EvaluatedDescription> value = entry.getValue();
			System.out.println(key.toString());
			for (EvaluatedDescription evaluatedDescription : value) {
				System.out.println(indention + "\t->\t" + evaluatedDescription);
			}
		}
	}
	
	public static String prettyPrint(Description desc){
		return OWLAPIDescriptionConvertVisitor.getOWLClassExpression(desc).toString();
	}

}
