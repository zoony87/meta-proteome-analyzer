package de.mpa.client.blast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.mpa.analysis.UniProtUtilities;
import de.mpa.analysis.taxonomy.TaxonomyUtils.TaxonomyDefinition;
import de.mpa.client.Client;
import de.mpa.client.model.dbsearch.UniProtEntryMPA;
import de.mpa.client.ui.dialogs.BlastDialog.BlastResultOption;
import de.mpa.db.DBManager;
import de.mpa.db.accessor.Mascothit;
import de.mpa.db.accessor.Omssahit;
import de.mpa.db.accessor.ProteinAccessor;
import de.mpa.db.accessor.SearchHit;
import de.mpa.db.accessor.Taxonomy;
import de.mpa.db.accessor.UniprotentryAccessor;
import de.mpa.db.accessor.XTandemhit;
import de.mpa.io.fasta.DigFASTAEntry;

/**
 * Class to BLAST a batch of sequences
 * @author Robert Heyer and Sebastian Dorl
 */
public class RunMultiBlast {
	
	/**
	 * The number of protein entries for each BLAST
	 */
	private static int BLASTBATCHSIZE = 1000;
	
	
//	/**
//	 * Constructor for a BLAST search process with multiple queries
//	 * @param blastFile  The file of the BLAST algorithm executable
//	 * @param database  The preformatted FASTA database for the BLAST algorithm
//	 * @param evalue  The E-value cutoff for the BLAST algorithm
//	 * @param experimentID. The experimentID  The list of dbEntry objects for the BLAST query
//	 */
//	public RunMultiBlast(String blastFile, String database, double evalue, long experimentID){
//		this.blastFile = blastFile;
//		this.database = database;
//		this.evalue = evalue;
//		this.experimentID = experimentID;
//	}
	
	/**
	 * Run NCBI BLASTP and retrieve the result.
	 * @param queryList. The List with all proteins already stored as DigFastaEntry
	 * @param blastFile. The String for the BLAST algorithm
	 * @param database. The string for the database against BLAST is executed
	 * @param evalue. The e-value for the BLAST algorithm
	 * @return. The BLAST result map
	 * @throws IOException
	 */
	public static HashMap<String, BlastResult> performBLAST(ArrayList<DigFASTAEntry> queryList, String blastFile,String database,Double evalue) throws IOException{
		
		// The file of a temporary FASTA file for the BLAST
		File fastaFile = File.createTempFile("blast_input", ".fasta");
		// The temporary output file for the BLAST
		File outputFile = File.createTempFile("blast_output", ".out");
		
		// Create a temporary FASTA-File to serve as input for the BLAST
		BufferedWriter writer = new BufferedWriter(new FileWriter(fastaFile));		
		for (DigFASTAEntry dbEntry : queryList) {
			// write the identifier
			writer.write(">" + dbEntry.getIdentifier());
			writer.newLine();
			// add the sequence
			writer.write(dbEntry.getSequence());			
			writer.newLine();
		}
		writer.flush();
		writer.close();
		// Construct BLAST query
		ArrayList<String> blastQuery = new ArrayList<String>();
		blastQuery.add(blastFile);
		// Add database 
		blastQuery.add("-db");
		blastQuery.add(database);
		// Add input file
		blastQuery.add("-query");
		blastQuery.add(fastaFile.getAbsolutePath());
		// Add output file
		blastQuery.add("-out");
		blastQuery.add(outputFile.getAbsolutePath());
		// Add e-value threshold
		blastQuery.add("-evalue");
		blastQuery.add(Double.toString(evalue));
		// Add custom output format: 
		blastQuery.add("-outfmt");
		// Full list of output options: https://www.ncbi.nlm.nih.gov/books/NBK279675/
		blastQuery.add("10 qacc sacc bitscore evalue pident stitle ");
		// Add number of threads
		blastQuery.add("-num_threads");
		blastQuery.add(Integer.toString(8));
		
		blastQuery.trimToSize();

		// Construct Process
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(blastQuery);
			builder.redirectErrorStream(true);
			process = builder.start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}finally{
			process.destroy();
		}
		
		// Parse contents of the output file
		BufferedReader reader = new BufferedReader(new FileReader(outputFile));
		String line;
		// Map linking accession to BLASTResult
		HashMap<String, BlastResult> blastResultMap = new HashMap<String, BlastResult>();
		// parse line by line
		while ((line = reader.readLine()) != null) {
			// added a limit of 5?? splits, this should leave all commas in the description alone
		    String[] splits = line.split(",", 6);
		    String query = splits[0];
		    String subject = splits[1];
		    String bitscore = splits[2];
		    String eValue = splits[3];
		    String identity = splits[4];		 
		    // get accession from name
		    String[] sbjctsplit = subject.split("[|]");
		    String accession = sbjctsplit[1];
		    // get description from subject title
		    String[] titlesplit = splits[5].split("[ ]", 2);
		    String title =titlesplit[1];
		    // make or get the result object
		    BlastResult result;
		    if (blastResultMap.keySet().contains(query)) { 
				// this query is known
		    	result = blastResultMap.get(query);
			} else {
				// query is new
				result = new BlastResult();
				result.setName(query);
				blastResultMap.put(query, result);
			}
		    
		    // add the hit information to the result
		    BlastHit hit = new BlastHit(accession, title);
		    hit.setScore(Double.parseDouble(bitscore));
		    hit.seteValue(Double.parseDouble(eValue));
		    hit.setIdentities(Double.parseDouble(identity));
		    result.putBlastHitsMap(hit);
		}
		reader.close();
		return blastResultMap;
	}

//	/**
//	 * Gets the BLAST results map
//	 * @return. The BLSAT resultsmap
//	 */
//	public Map<String, BlastResult> getBlastResultMap() {
//		return blastResultMap;
//	}

	/**
	 * @param blastFile.  The file of the BLAST algorithm executable
	 * @param database.  The preformatted FASTA database for the BLAST algorithm
	 * @param evalue.  The E-value cutoff for the BLAST algorithm
	 * @param experimentID. The experimentID  The list of dbEntry objects for the BLAST query, take "-1" for all experiments.
	 * @param resultOption. The result option, defining which BLAST hits are used such as "All hits", "Best evalue" etc.
	 * @throws SQLException
	 * @throws IOException 
	 */
	public static void performBLAST4Experiments(String blastFile, String database, double evalue, long experimentID, BlastResultOption resultOption) throws SQLException, IOException {
		
		// Get the database connection
		Connection conn = DBManager.getInstance().getConnection();
		
		// Show progress
		if (Client.getInstance() != null) {	
			Client.getInstance().firePropertyChange("new message", null, "RUNNING BLAST");
			Client.getInstance().firePropertyChange("indeterminate", false,	true);
		}

		// find all proteins in the database
		List<ProteinAccessor> proteins = new ArrayList<ProteinAccessor>();
		if (experimentID == -1L) {
			proteins = ProteinAccessor.getAllProteinsWithoutUniProtEntry(conn);
		}else{
			// gather all proteins from the experiment
			List<Mascothit> mascotHits = Mascothit.getHitsFromExperimentID(experimentID, conn);
			List<XTandemhit> xtandemHits = XTandemhit.getHitsFromExperimentID(experimentID, conn);
			List<Omssahit> omssaHits = Omssahit.getHitsFromExperimentID(experimentID, conn);
			List<SearchHit> hits = new ArrayList<SearchHit>();
			hits.addAll(mascotHits);
			hits.addAll(xtandemHits);
			hits.addAll(omssaHits);
			for (SearchHit hit : hits) {
				//TODO: May increase speed by batch queries
				ProteinAccessor aProt = ProteinAccessor.findFromID(hit.getFk_proteinid(), conn);
				// add proteins which have no uniprotentry (condition: fk = -1)
				if (aProt.getFK_UniProtID() == -1) {
					proteins.add(aProt);
				}
			}
		}
		// Go through all protein hits and BLAST the unannotated ones
		// MAP for each BLAST batch
		ArrayList<DigFASTAEntry> blastBatchList = new ArrayList<DigFASTAEntry>();
		// taxonomy map for common ancestor retrieval
		Map<Long, Taxonomy> taxonomyMap = Taxonomy.retrieveTaxonomyMap(conn);
		// Number of un-annotated proteins
		
		int noBLASTProts = 0;
		// maps SQL-proteinids to whole proteinaccessors
		TreeMap<Long, ProteinAccessor> protaccessors = new TreeMap<Long, ProteinAccessor>(); 
		for (ProteinAccessor proteinAcc : proteins) {
			noBLASTProts++;
			// Fill proteinaccessor in protein hit
			DigFASTAEntry fastaProt = new DigFASTAEntry((""+proteinAcc.getProteinid()) ,proteinAcc.getDescription(), proteinAcc.getDescription(), proteinAcc.getSequence(),DigFASTAEntry.Type.METAGENOME1 , null);
			protaccessors.put(proteinAcc.getProteinid(), proteinAcc);
			// Add protein to BLAST list
			blastBatchList.add(fastaProt);
			// Perform batch for BLAST 
			if (noBLASTProts % BLASTBATCHSIZE == 0 || noBLASTProts == proteins.size()) { 
				// Show progress
				if (Client.getInstance() != null) {	
					Client.getInstance().firePropertyChange("new message", null, "BLAST proteins " + noBLASTProts + " of " + proteins.size());
					Client.getInstance().firePropertyChange("resetall", -1L, (long) proteins.size());
					Client.getInstance().firePropertyChange("resetcur", -1L, (long) proteins.size());
//					Client.getInstance().firePropertyChange("indeterminate", true,	false);
				}
				
				// Perform the BLAST, resultmap keys== SQL proteinID as string and the BLAST proposals as BLASTResultObject
				HashMap<String, BlastResult> resultBLASTmap = performBLAST(blastBatchList, blastFile, database, evalue);
				if (Client.getInstance() != null) {
					Client.getInstance().firePropertyChange("indeterminate", true,	false);				
				}

				// store BLAST results
				storeBLASTinDB(resultBLASTmap, evalue, resultOption, taxonomyMap, protaccessors, database);
				// Reset the BLAST Batch
				blastBatchList = new ArrayList<DigFASTAEntry>();
			}
		}
		
		// Show progress
		if (Client.getInstance() != null) {	
			Client.getInstance().firePropertyChange("new message", null, "BLAST Finished ");
		}
	}
		
//		
//		
//			// status can be finished or failed
//			String status;
//			// make batches
//			Map<String, BlastResult> blastBatch;
//			int broken_count = 0;
//				}
//				Client.getInstance().firePropertyChange("new message", null, "FOUND " + broken_count + " BROKEN PROTEIN ENTRIES");
//				// Actually Start the BLAST
//				RunMultiBlast blaster = new RunMultiBlast(blastFile, blastDatabase, eValue, blastEntries);
//				status = "FINISHED";				
//				try {
//					blaster.startBlast();
//				} catch (IOException e) {
//					e.printStackTrace();
//					status = "FAILED";
//				}
//				// get results and submit to db
//				blastBatch = blaster.getBlastResultMap();
//				Client.getInstance().firePropertyChange("new message", null, "STORING BLAST HITS BATCH " + batchnumber + " OF " + batchamount);
//				Client.getInstance().firePropertyChange("indeterminate", false,	true);
//				Client.getInstance().firePropertyChange("resetall", -1L, (long) blastBatch.size());
//				Client.getInstance().firePropertyChange("resetcur", -1L, (long) blastBatch.size());
//				// update database with new proteins				
//				for (ProteinAccessor prot : blastProteins) {
//					// store the old accession
//					String old_accession = prot.getAccession();	
//					// get proteinid for the old protein entry
//					Long old_proteinid = prot.getProteinid();
//					// get protein_sequence from the old protein entry
//					String old_sequence = prot.getSequence();				
//					// Get best BLAST hit
//					if (blastBatch.get(old_accession) != null) {
//						// best blast hit, used for ??
//						//BlastHit bestBlastHit = blastBatch.get(old_accession).getBestBitScoreBlastHit();
//						// list of all blasthits that satisfy the evalue threshold  (evalue check is redundant)
//						List<BlastHit> allblasthits = blastBatch.get(old_accession).getBestBlastHits(eValue);		
//						// get all the protein information that needs to be copied (only in case of multiple blast hits for this accession)
//						// inits
//						List<Pep2prot> peptoprot_list = null;
//						List<MascothitTableAccessor> mascothit_list = null;
//						List<OmssahitTableAccessor> omssahit_list = null;
//						List<XtandemhitTableAccessor> xtandemhit_list = null;
//						// but only if batch contains more than one entry (saves time)
//						if (allblasthits.size() > 1) {
//							// make sql-queries 
//							// get pep2prots for this protein							
//							peptoprot_list = Pep2prot.get_pep2prots_for_proteinid(old_proteinid, conn);
//							// get mascothits for this protein
//							mascothit_list = Mascothit.getHitsFromProteinID(old_proteinid, conn);
//							// get omssahits for this protein
//							omssahit_list = Omssahit.getHitsFromProteinid(old_proteinid, conn);
//							// get xtandemhits for this protein
//							xtandemhit_list = XTandemhit.getHitsFromProteinID(old_proteinid, conn);
//						}
//						
//						// TODO: here we can add a check to determine if a blast-protein contains any or all of the peptides found in the search
//						
//						// get protein sequence (from where?)
//						// get all peptide sequences that we actually found (from sql-db)
//						
//						// needs protein-sequence and the protein-sequences for all pep2prot-entries, then the "allblasthits"-list can be reduced 
//				
//						// mark first hit, because one protein entry has to be updated
//						// TODO: this code is very slow, because of all the sql-updates for copied entries						
//						boolean first_hit = true;
//						int commitcount = 0;
//						for (BlastHit hit : allblasthits) {
//							commitcount++;
//							found_protein_count++;
//							// new accession is constructed from the old accession and the accession from BLAST result
//							String new_accession = old_accession + "_BLAST_" + hit.getAccession();														
//							// new description TODO: new accessions might cause problems somewhere else (check this) 
//							String newDescription = "MG: " + hit.getName() + " [" + hit.getAccession() + "] Score: " + hit.getScore();							
//							// if we have 
//							if (first_hit) {
//								// update the old entry
//								ProteinAccessor.upDateProteinEntry(prot.getProteinid(),
//										new_accession, newDescription,
//										prot.getSequence(), prot.getCreationdate(),
//										conn);
//								// unmark first hit
//								first_hit = false;
//								// mark this accession for uniprot update (the hit-accession is used for uniprot lookup)
//								if (uniprot_map.containsKey(hit.getAccession())) {									
//									uniprot_map.get(hit.getAccession()).add(prot.getProteinid());	
//								} else {
//									List<Long> idlist = new ArrayList<Long>();
//									uniprot_map.put(hit.getAccession(), idlist);
//								}
//							} else {
//								// make new entries 								
//								// create new protein entry
//    							// this adds a new protein
//    							ProteinAccessor protAccessor = ProteinAccessor.addProteinToDatabase(new_accession, newDescription, old_sequence, conn);								
//								// create pep2prot references
//								for (Pep2prot old_pep2prot_entry : peptoprot_list) {
//									Pep2prot.linkPeptideToProtein(old_pep2prot_entry.getFk_peptideid(), protAccessor.getProteinid(), conn);
//								}
//								// create duplicate mascothits
//								for (MascothitTableAccessor old_mascothit : mascothit_list) {								
//									Mascothit.copymascothit(protAccessor.getProteinid(), old_mascothit, conn);
//								}
//								// create duplicate xtandemhits
//								for (XtandemhitTableAccessor old_xtandemhit : xtandemhit_list) {								
//									XTandemhit.copyxtandemhit(protAccessor.getProteinid(), old_xtandemhit, conn);
//								}								
//								// create duplicate omssahits
//								for (OmssahitTableAccessor old_omssahit : omssahit_list) {								
//									Omssahit.copyomssahit(protAccessor.getProteinid(), old_omssahit, conn);
//								}
//								// mark this accession for uniprot update (the hit-accession is used for uniprot lookup)
//								if (uniprot_map.containsKey(hit.getAccession())) {									
//									uniprot_map.get(hit.getAccession()).add(protAccessor.getProteinid());	
//								} else {
//									List<Long> idlist = new ArrayList<Long>();
//									uniprot_map.put(hit.getAccession(), idlist);
//								}
//								// anything missing?--> commit!?							
//							}
//							if ((commitcount % 500) == 0) {
//								conn.commit();
//							}
//						}
//						conn.commit();
//					}
//				}
//				Client.getInstance().firePropertyChange("indeterminate", true, false);
//				Client.getInstance().firePropertyChange("new message", null, "BLAST FOUND " + found_protein_count + " PROTEINS");
//				Client.getInstance().firePropertyChange("new message", null, "RUNNING BLAST " + status);
//				Client.getInstance().firePropertyChange("resetall", -1L, 100L);
//				Client.getInstance().firePropertyChange("resetcur", -1L, 100L);
//			}
//
//	
//		}
//		return uniprot_map;
//	}
	
	/**
	 * Method to store the results of the BLAST
	 * @param <V>
	 * @param resultBLAST. The BLASt result map (key proteinID)
	 * @param evalue. The evalue of the BLAST
	 * @param resultOption. The result option, defining which BLAST hits are used.
	 * @param proteinAccessorMap. contains proteinaccessors mapped to their sql ids
	 * @param database. The database against which the BLAST is executed
	 * @throws SQLException 
	 */
	private static void storeBLASTinDB(HashMap<String, BlastResult> resultMapBLAST, double evalue, BlastResultOption resultOption,
		Map<Long, Taxonomy> taxonomyMap, TreeMap<Long, ProteinAccessor> proteinAccessorMap,String database) throws SQLException {
		// get connection
		Connection conn = DBManager.getInstance().getConnection();
		// Go through all result entries
		// keys are actually SQL-proteinids, not accessions (for speed)
		for (String key : resultMapBLAST.keySet()) {
			BlastResult blastResult = resultMapBLAST.get(key);
			
			//TODO OptionS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			List<BlastHit> bestBlastHits = blastResult.getSelectedBLASTHits(resultOption);
			// Get a list of all result accessions
			List<UniProtEntryMPA> upEntries = new ArrayList<UniProtEntryMPA>();
			
			// Get list of all UniProtentries from the BLAST proposals
			for (BlastHit blastHit : bestBlastHits) {
				// get Uniprotentry from this uniprotid
				Long fk_uniprotid = ProteinAccessor.findUniprotidFromAccession(blastHit.getAccession(), conn);
				//TODO: faster sql query 
				upEntries.add(new UniProtEntryMPA(UniprotentryAccessor.findFromID(fk_uniprotid, conn)));
			}
			
			// Create a common ancestor UNIPROT ENTRY
			UniProtEntryMPA common_ancestor = UniProtUtilities.getCommonUniprotEntry(upEntries, taxonomyMap, TaxonomyDefinition.COMMON_ANCESTOR);
			// Treemap containing sql-Protein ID and the UniprotentryMPA
			TreeMap<Long, UniProtEntryMPA> uniProtMap = new TreeMap<Long, UniProtEntryMPA>();
			uniProtMap.put(Long.valueOf(key), common_ancestor);
			// Map linking protID to the uniprotentry ID
			TreeMap<Long, Long> addMultipleUniProtEntriesToDatabase = UniprotentryAccessor.addMultipleUniProtEntriesToDatabase(uniProtMap, conn); 
			
			// Update link in the protein table.
			for (Long protIDkey : addMultipleUniProtEntriesToDatabase.keySet()) {
				// Get protein accessor from protein ID
				ProteinAccessor protAcc = proteinAccessorMap.get(protIDkey);
				protAcc.setFK_uniProtID(addMultipleUniProtEntriesToDatabase.get(protIDkey));
				protAcc.setDescription("BLAST_" + blastResult.getBestEValueBlastHit().getName() + blastResult.getBestEValueBlastHit().getAccession());
				protAcc.setSource("BLAST_" + protAcc.getSource() + "_" + DigFASTAEntry.Type.UNIPROTSPROT.toString());
				// Update the protein entry
				protAcc.update(conn);
			}
			conn.commit();
			if (Client.getInstance() != null) {
				Client.getInstance().firePropertyChange("progressmade", false, true);
			}

		}
		
		conn.close();
	}
}