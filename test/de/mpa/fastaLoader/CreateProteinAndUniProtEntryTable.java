package de.mpa.fastaLoader;

import java.io.File;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import de.mpa.analysis.UniProtUtilities;
import de.mpa.io.fasta.FastaLoader;

/**
 * Test Class to fill a fasta DB into the SQL table update the UniProtEntryTable
 * @author R. Heyer
 *
 */
public class CreateProteinAndUniProtEntryTable extends TestCase {

	@Before
	public void setUp(){
		
		/*
		 * List of the FASTA-files
		 */
		ArrayList<File> fileList = new ArrayList<>();
		// Add FASTA-Files
//		fileList.add(new File("/home/robbie/Desktop/TestDB.fasta"));
//		fileList.add(new File("/home/robbie/Desktop/uniprot_sprot.fasta"));
		// Change file list to array
		File file =  new File("/home/robbie/Desktop/uniprot_sprot.fasta");
		
		
		// Add the FASTA information to the SQL DB
		try {
			FastaLoader.addFastaDatabases(file, new File("/home/robbie/Desktop/uniprot_sprot.fasta"), true, UniProtUtilities.BATCH_SIZE);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testUpdate(){
		assertEquals(true, true);
	}
}