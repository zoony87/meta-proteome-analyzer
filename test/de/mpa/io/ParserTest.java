package de.mpa.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ParserTest extends TestCase{
	
	private ArrayList<String> fileStrings = new ArrayList<String>();

	public ParserTest() {
		ArrayList<ArrayList<String>> fileNames = new ArrayList<ArrayList<String>>();
		ArrayList<String> folderNames = new ArrayList<String>();
		String[] files = new String[] {
//				"ESI;Bande1,Spot1",
//				"MALDI;1A1,1A2,1A3,1A4,1B1,1B2",
				"QSTAR;Test"
		};
		
		for (String str : files) {
			String[] strParts = str.split(";");
			folderNames.add(strParts[0]);
			strParts = strParts[1].split(",");
			ArrayList<String> strList = new ArrayList<String>();
			for (String file : strParts) {
				strList.add(file);
			}
			fileNames.add(strList);
		}

		String pathName = "test/de/mpa/resources";
		
		for (int i = 0; i < fileNames.size(); i++) {
			ArrayList<String> str = fileNames.get(i);
			for (int j = 0; j < str.size(); j++) {
				fileStrings.add(pathName + "/" + folderNames.get(i) + "/" + str.get(j) + ".mgf");
			}
		}
	}
	
	public void testParseFiles(){
		for (String str : fileStrings) {
			try {
	        	MascotGenericFileReader reader = new MascotGenericFileReader(new File(str));
	        	List<MascotGenericFile> spectrumFiles = reader.getSpectrumFiles();
	        	
	        	for (MascotGenericFile file : spectrumFiles) {
					System.out.println("Filename: " + file.getFilename());
					System.out.println("   m/z of Precursor: " + file.getPrecursorMZ());
					System.out.println("Charge of Precursor: " + file.getCharge() + "+");
//					HashMap<Double, Double> peaks = file.getPeaks();
//					TreeSet<Double> treeset = new TreeSet<Double>(peaks.keySet());
					ArrayList<Peak> peaks = file.getPeakList();
					int i = 1;
//					for (Double mz : treeset) {
//						if (i%15 == 1) System.out.println("#\t   m/z\t\t   I");
//						System.out.println(i + "\t" + mz.toString() + "\t" + peaks.get(mz).toString());
//						i++;
//					}
					for (Peak peak : peaks) {
						if (i%15 == 1) System.out.println("#\t   m/z\t\t   I");
						System.out.println(i + "\t" + peak.mz + "\t" + peak.intensity);
						i++;
					}
					System.out.println("Highest intensity: " + file.getHighestIntensity());
					System.out.println("  Total intensity: " + file.getTotalIntensity());
				}
	        	
	        } catch (Exception e) {
	            e.printStackTrace();
	            fail();
	        }
		}
	}
}
