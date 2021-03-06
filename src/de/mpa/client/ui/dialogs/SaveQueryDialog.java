package de.mpa.client.ui.dialogs;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.error.ErrorLevel;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import de.mpa.client.Constants;
import de.mpa.client.ui.ClientFrame;
import de.mpa.client.ui.ScreenConfig;
import de.mpa.client.ui.icons.IconConstants;
import de.mpa.graphdb.cypher.CypherQuery;
import de.mpa.graphdb.io.QueryHandler;
import de.mpa.graphdb.io.UserQueries;

public class SaveQueryDialog extends JDialog {
	
	private GraphQueryDialog owner;
	private JTextField queryNameTtf;
	private JTextArea queryTextTta;
	
	public SaveQueryDialog(GraphQueryDialog owner, String title, boolean modal) {
		super(owner, title, modal);
		this.owner = owner;
		setTitle(title);
		initComponents();
		showDialog();
	}

	private void initComponents() {
		Container cp = this.getContentPane();		
		cp.setLayout(new FormLayout("5dlu, p:g, 5dlu", "5dlu, p, 5dlu, p, 15dlu, p, 5dlu, p, 15dlu, p, 5dlu"));
		
		JLabel queryNameLbl = new JLabel("Query Name:");
		queryNameTtf = new JTextField(25);
		
		JLabel queryTextLbl = new JLabel("Query Text:");
		queryTextTta = new JTextArea(4, 0);
		queryTextTta.setEditable(false);
		queryTextTta.setFont(new Font("Courier", queryTextTta.getFont().getStyle(), 12));		

		JScrollPane queryTextScp = new JScrollPane(queryTextTta);
		queryTextScp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		queryTextScp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		queryTextTta.setText(owner.getSelectedQuery().toString());
		
		// Configure button panel containing 'OK' and 'Cancel' options
		FormLayout layout = new FormLayout("p, 5dlu, p", "p");
		JPanel buttonPnl = new JPanel(layout);
		
		// Configure 'Save' button
		JButton saveBtn = new JButton("Save", IconConstants.SAVE_ICON);
		saveBtn.setRolloverIcon(IconConstants.SAVE_ROLLOVER_ICON);
		saveBtn.setPressedIcon(IconConstants.SAVE_PRESSED_ICON);
		saveBtn.setHorizontalAlignment(SwingConstants.LEFT);
		saveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SaveTask().execute();
			}
		});		
		saveBtn.setPreferredSize(saveBtn.getPreferredSize());
		
		// Configure 'Cancel' button
		JButton cancelBtn = new JButton("Cancel", IconConstants.CROSS_ICON);
		cancelBtn.setRolloverIcon(IconConstants.CROSS_ROLLOVER_ICON);
		cancelBtn.setPressedIcon(IconConstants.CROSS_PRESSED_ICON);
		cancelBtn.setHorizontalAlignment(SwingConstants.LEFT);
		cancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});		
		cancelBtn.setPreferredSize(cancelBtn.getPreferredSize());		
		buttonPnl.add(saveBtn, CC.xy(1,  1));
		buttonPnl.add(cancelBtn, CC.xy(3,  1));
		
		cp.add(queryNameLbl, CC.xy(2, 2));
		cp.add(queryNameTtf, CC.xy(2, 4));
		cp.add(queryTextLbl, CC.xy(2, 6));
		cp.add(queryTextScp, CC.xy(2, 8));
		cp.add(buttonPnl, CC.xy(2, 10));
	}
	
	
	/**
	 * This method shows the dialog.
	 */
	private void showDialog() {
		// Configure size and position
		this.pack();
		this.setResizable(false);
		ScreenConfig.centerInScreen(this);
		
		// Show dialog
		this.setVisible(true);
	}
	
	/**
	 * Close method for the dialog.
	 */
	private void close() {
		// Save the export headers
		dispose();
	}
	
	/**
	 * Class to save the queries in a background thread.
	 * 
	 * @author Thilo Muth
	 */
	private class SaveTask extends SwingWorker {
		boolean success = false;
		@Override
		protected Object doInBackground() {
			
			try {
				File queryFile = new File(this.getClass().getResource(Constants.CONFIGURATION_PATH + "userqueries.xml").toURI());
				UserQueries userQueries = owner.getUserQueries();
				CypherQuery userQuery = owner.getSelectedQuery();
				userQuery.setTitle(queryNameTtf.getText());
				userQueries.addQuery(userQuery);
				QueryHandler.exportUserQueries(userQueries, queryFile);
			} catch (Exception e) {
				JXErrorPane.showDialog(ClientFrame.getInstance(), new ErrorInfo("Severe Error", e.getMessage(), null, null, e, ErrorLevel.SEVERE, null));
			}
			success = true;
			return 0;
		}		
		
		/**
		 * Continues when the results retrieval has finished.
		 */
		public void done() {			
			owner.updateUserQueries();
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			close();
			if(success) JOptionPane.showMessageDialog(owner, "Cypher Query has been saved.");
		}
	}

}
