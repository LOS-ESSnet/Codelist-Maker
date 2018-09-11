package eu.europa.ec.eurostat.los.codes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class CodelistMaker {

	private static Workbook wb = null;

	public final static String EXCEL_FILE_NAME = "src/main/resources/codelists.xls";
	public final static String BASE_URI = "http://id.linked-open-statistics.org/";
	public final static String CODES_BASE_URI = BASE_URI + "codes/";
	public final static String CONCEPTS_BASE_URI = BASE_URI + "concepts/";
	public final static String BASE_CONCEPT_SCHEME_LABEL="Code list for";

	public static void main(String[] args) throws IOException {

		wb = new HSSFWorkbook(new FileInputStream(EXCEL_FILE_NAME));

		Iterator<Sheet> sheetIterator = wb.sheetIterator();
		while (sheetIterator.hasNext()) {

			Sheet clSheet = sheetIterator.next();
			String clTag = normalize(clSheet.getSheetName().trim().toLowerCase());

			Model codelistModel = createConceptScheme(clTag, clSheet);
			RDFDataMgr.write(new FileOutputStream("src/main/resources/cl-" + clTag + ".ttl"), codelistModel, Lang.TURTLE);
		}
	}

	public static Model createConceptScheme(String clTag, Sheet clSheet) {

		Model clModel = ModelFactory.createDefaultModel();
		clModel.setNsPrefix("rdfs", RDFS.getURI());
		clModel.setNsPrefix("owl", OWL.getURI());
		clModel.setNsPrefix("skos", SKOS.getURI());
		clModel.setNsPrefix("los-codes", CODES_BASE_URI);
		clModel.setNsPrefix("los-concepts", CONCEPTS_BASE_URI);

		// Create the concept scheme and the associated concept from sheet name
		String conceptName = normalize(clSheet.getSheetName());
		String clName = BASE_CONCEPT_SCHEME_LABEL +" "+conceptName;
		String clURI = CODES_BASE_URI + clTag;
		Resource scheme = clModel.createResource(clURI, SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, clName, "fr");
		scheme.addProperty(SKOS.notation, clModel.createTypedLiteral(clTag));
		Resource concept = clModel.createResource(CONCEPTS_BASE_URI + StringUtils.capitalize(conceptName), OWL.Class);
		concept.addProperty(RDF.type, RDFS.Class);
		concept.addProperty(RDFS.subClassOf, SKOS.Concept);
		concept.addProperty(SKOS.prefLabel, clModel.createLiteral(clTag, "fr"));
		concept.addProperty(SKOS.notation, clModel.createLiteral(clTag, "fr"));
		scheme.addProperty(RDFS.seeAlso, concept);
		concept.addProperty(RDFS.seeAlso, scheme);

		// Iterate through lines (skipping the first one) to add the codes
		Iterator<Row> rowIterator = clSheet.rowIterator();
		rowIterator.next();
		while (rowIterator.hasNext()) {
			Row currentRow = rowIterator.next();
			String itemCode = getStringValue(currentRow.getCell(0));
			String itemName = getStringValue(currentRow.getCell(1));
			Resource item = clModel.createResource(clURI + "/" + itemCode, concept);
			item.addProperty(RDF.type, SKOS.Concept);
			item.addProperty(SKOS.notation, itemCode);
			item.addProperty(SKOS.prefLabel, clModel.createLiteral(itemName, "fr"));
			item.addProperty(SKOS.inScheme, scheme);
		}

		return clModel;
	}
	
	private static String getStringValue(Cell cellule) {
		cellule.setCellType(CellType.STRING);
		return cellule.toString().trim();
	}

	private static String normalize(String original) {

		String normalForm = Normalizer.normalize(original, Normalizer.Form.NFD);
		normalForm = normalForm.replaceAll(" ", "-");
		normalForm = normalForm.replaceAll("[^\\p{ASCII}]", "");

		return normalForm;
	}
}
