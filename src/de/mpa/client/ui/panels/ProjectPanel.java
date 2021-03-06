package de.mpa.client.ui.panels;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledPanel;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.error.ErrorLevel;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import de.mpa.client.Client;
import de.mpa.client.Constants;
import de.mpa.client.model.AbstractExperiment;
import de.mpa.client.model.AbstractProject;
import de.mpa.client.model.DatabaseExperiment;
import de.mpa.client.model.DatabaseProject;
import de.mpa.client.model.FileExperiment;
import de.mpa.client.model.FileProject;
import de.mpa.client.model.dbsearch.DbSearchResult;
import de.mpa.client.ui.ClientFrame;
import de.mpa.client.ui.PanelConfig;
import de.mpa.client.ui.TableConfig;
import de.mpa.client.ui.dialogs.GeneralDialog;
import de.mpa.client.ui.dialogs.GeneralDialog.DialogType;
import de.mpa.client.ui.icons.IconConstants;
import de.mpa.db.ProjectManager;

/**
 * Panel for displaying, manipulating and selecting available projects and the
 * experiments nested within them.
 * 
 * @author A. Behne, R. Heyer, T. Muth
 */
public class ProjectPanel extends JPanel {

	/**
	 * The cache of projects.
	 */
	private List<AbstractProject> projects;
	
	/**
	 * The currently selected project.
	 */
	private AbstractProject selectedProject;
	
	/**
	 * The currently selected experiment.
	 */
	private AbstractExperiment selectedExperiment;
	
	/**
	 * The currently loaded experiment.
	 */
	private AbstractExperiment currentExperiment;
	
	/**
	 * Table view of all projects stored in the remote database.
	 */
	protected JXTable projectTbl;

	/**
	 * Button to bring up a blank project properties dialog for creating new
	 * projects.
	 */
	private JButton addProjectBtn;

	/**
	 * Button to bring up a dialog for displaying and modifying the currently
	 * selected project's properties.
	 */
	private JButton modifyProjectBtn;
	
	/**
	 * Button to remove the currently selected project from the database.
	 */
	private JButton deleteProjectBtn;

	/**
	 * Table view of all experiments of the currently selected project.
	 */
	protected JXTable experimentTbl;
	
	/**
	 * Button to bring up a blank experiment properties dialog for creating new
	 * experiments.
	 */
	private JButton addExperimentBtn;
	
	/**
	 * Button to bring up a dialog for displaying and modifying the currently
	 * selected experiment's properties.
	 */
	private JButton modifyExperimentBtn;
	
	/**
	 * Button to remove the currently selected experiment from the database.
	 */
	private JButton deleteExperimentBtn;

	/**
	 * The 'Next' navigation button.
	 */
	private JButton nextBtn;

	/**
	 * The 'Skip to Results' navigation button
	 */
	private JButton skipBtn;

	/**
	 * Constructs a panel containing components for selecting and configuring
	 * projects and experiments as part of the search pipeline.
	 */
	public ProjectPanel() {
		this.initComponents();
		
		// Updates the project table.
		this.refreshProjectTable();
	}
	
	/**
	 * Initializes and configures the panel's components.
	 */
	private void initComponents() {
		
		// Layout for the project panel
		this.setLayout(new FormLayout("5dlu, p:g, 5dlu, p:g, 5dlu", "5dlu, f:p:g, 5dlu, b:p, 5dlu"));
		
		// Setup the project table
		JScrollPane projectTblScp = this.setupProjectTable();
		
		// Setup the project management buttons.
		JPanel projectBtnPnl = this.setupProjectButtonPanel();
		
		JPanel projectPnl = new JPanel(new FormLayout("5dlu, p:g, 5dlu", "5dlu, f:p:g, 5dlu, b:p, 5dlu"));
		projectPnl.add(projectTblScp, CC.xy(2, 2));
		projectPnl.add(projectBtnPnl, CC.xy(2, 4));
		
		JXTitledPanel projTtlPnl = PanelConfig.createTitledPanel("Projects", projectPnl);
		
		// Experiment table
		JScrollPane experimentTblScp = this.setupExperimentTable();

		// Setup the experiment management buttons.
		JPanel experimentBtnPnl = this.setupExperimentButtonPanel();
		
		JPanel experimentPnl = new JPanel(new FormLayout("5dlu, p:g, 5dlu", "5dlu, f:p:g, 5dlu, p, 5dlu"));
		experimentPnl.add(experimentTblScp, CC.xy(2, 2));
		experimentPnl.add(experimentBtnPnl, CC.xy(2, 4));
		
		JXTitledPanel expTtlPnl = PanelConfig.createTitledPanel("Experiments", experimentPnl);
		
		// Next button
		JPanel navPnl = new JPanel(new FormLayout("r:p:g, 5dlu, r:p, 5dlu, r:p", "b:p:g"));
		
		skipBtn = ClientFrame.getInstance().createNavigationButton(true, false);
		skipBtn.setText("Results");
		skipBtn.setIcon(IconConstants.SKIP_ICON);
		skipBtn.setRolloverIcon(IconConstants.SKIP_ROLLOVER_ICON);
		skipBtn.setPressedIcon(IconConstants.SKIP_PRESSED_ICON);
		skipBtn.removeActionListener(skipBtn.getActionListeners()[0]);
		skipBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ClientFrame clientFrame = ClientFrame.getInstance();
				clientFrame.setTabSelectedIndex(ClientFrame.INDEX_RESULTS_PANEL);
			}
		});

		nextBtn = ClientFrame.getInstance().createNavigationButton(true, false);
		
		navPnl.add(ClientFrame.getInstance().createNavigationButton(false, false), CC.xy(1, 1));
		navPnl.add(skipBtn, CC.xy(3, 1));
		navPnl.add(nextBtn, CC.xy(5, 1));

		this.add(projTtlPnl, CC.xy(2, 2));
		this.add(expTtlPnl, CC.xy(4, 2));
		this.add(navPnl, CC.xy(4, 4));
	}

	/**
	 * Creates and returns the table view of projects.
	 * @return the project table wrapped in a scroll pane
	 */
	private JScrollPane setupProjectTable() {
		// Table for projects
		projectTbl = new JXTable(new DefaultTableModel() { 
			{
				setColumnIdentifiers(new Object[] { "#", "Project Title", "Creation Date"});
			}
			@Override
			public boolean isCellEditable(int row, int col) {
				return false;
			}
			@Override
			public Class<?> getColumnClass(int col) {
				switch (col) {
					case 0:
						return Integer.class;
					case 1:
						return String.class;
					case 2:
						return Date.class;
					default:
						return getValueAt(0, col).getClass();
				}
			}
			@Override
			public Object getValueAt(int row, int column) {
				Object value = super.getValueAt(row, 0);
				if (value instanceof AbstractProject) {
					AbstractProject project = (AbstractProject) value;
					switch (column) {
						case 0:
							return project.getID();
						case 1:
							return project.getTitle();
						case 2:
							return project.getCreationDate();
						default:
							return value;
					}
				}
				return null;
			}
		});
		
		// Selection model for the list: Select one entry of the table only
		projectTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// Enables some relevant buttons.
		projectTbl.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent evt) {
				int selRow = projectTbl.getSelectedRow();
				if (selRow != -1) {
					selRow = projectTbl.convertRowIndexToModel(selRow);
					AbstractProject project = (AbstractProject) projectTbl.getModel().getValueAt(selRow, -1);
					if (project != selectedProject) {
						selectedProject = project;
						selectedExperiment = null;
						refreshExperimentTable(selectedProject);
					}
				}
				
				this.updateComponents();
			}
			
			/**
			 * Updates states of various UI components in response to table selection.
			 */
			private void updateComponents() {
				ClientFrame clientFrame = ClientFrame.getInstance();
				
				// update text fields
				String title = (selectedProject != null) ? selectedProject.getTitle() : "None";
				clientFrame.getStatusBar().getProjectTextField().setText(title);
				clientFrame.getStatusBar().getExperimentTextField().setText("None");
				
				// enable buttons
				modifyProjectBtn.setEnabled(true);
				deleteProjectBtn.setEnabled(true);
				addExperimentBtn.setEnabled(true);
				
				// disable tabs
				for (int i = ClientFrame.INDEX_INPUT_PANEL; i < ClientFrame.INDEX_LOGGING_PANEL; i++) {
					clientFrame.setTabEnabledAt(i, false);
				}
				
				// disable navigation buttons
				skipBtn.setEnabled(false);
				skipBtn.setToolTipText(null);
				nextBtn.setEnabled(false);
			}
		});
		
		// Set the column layout
		TableConfig.setColumnWidths(projectTbl, new double[] { 1, 10, 4 });
	
		// Add nice striping effect
		projectTbl.addHighlighter(TableConfig.getSimpleStriping());
	
		// Modify column control widget
		TableConfig.configureColumnControl(projectTbl);
		
		// Add project table to scroll pane
		JScrollPane projectTblScp = new JScrollPane(projectTbl);
		projectTblScp.setPreferredSize(new Dimension(450, 350));
		projectTblScp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		return projectTblScp;
	}

	/**
	 * Creates and configures the project management buttons.
	 */
	private JPanel setupProjectButtonPanel() {
		final ClientFrame clientFrame = ClientFrame.getInstance();
		
		// create button panel
		FormLayout layout = new FormLayout("p:g, 5dlu, p:g, 5dlu, p:g", "0dlu, p, 0dlu");
		layout.setColumnGroups(new int[][] { { 1, 3, 5 } });
		JPanel projectBtnPnl = new JPanel(layout);
		
		Insets buttonInsets = new Insets(5, 4, 5, 4);
		
		// create 'Add Project' button
		addProjectBtn = new JButton("Add Project", IconConstants.PROJECT_ADD_ICON);
		addProjectBtn.setRolloverIcon(IconConstants.PROJECT_ADD_ROLLOVER_ICON);
		addProjectBtn.setPressedIcon(IconConstants.PROJECT_ADD_PRESSED_ICON);
		addProjectBtn.setMargin(buttonInsets);
		
		addProjectBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				AbstractProject project;
				if (Client.isViewer()) {
					project = new FileProject();
				} else {
					project = new DatabaseProject();
				}
				GeneralDialog dialog = new GeneralDialog(DialogType.NEW_PROJECT, project);
				int result = dialog.showDialog();
				if (result == GeneralDialog.RESULT_SAVED) {
					refreshProjectTable();
					setSelectedProject(project);
				}
			}
		});
		

		// create 'View/Edit Details' button
		modifyProjectBtn = new JButton("View/Edit Details", IconConstants.PROJECT_VIEW_ICON);
		modifyProjectBtn.setRolloverIcon(IconConstants.PROJECT_VIEW_ROLLOVER_ICON);
		modifyProjectBtn.setPressedIcon(IconConstants.PROJECT_VIEW_PRESSED_ICON);
		modifyProjectBtn.setMargin(buttonInsets);
		modifyProjectBtn.setEnabled(false);
		
		modifyProjectBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (selectedProject != null) {
					GeneralDialog dialog = new GeneralDialog(DialogType.MODIFY_PROJECT, selectedProject);
					int result = dialog.showDialog();
					if (result == GeneralDialog.RESULT_SAVED) {
						// refresh table and re-select project and experiment
						AbstractExperiment experiment = selectedExperiment;
						refreshProjectTable();
						setSelectedProject(selectedProject);
						setSelectedExperiment(experiment);
					}
				}
			}
		});
		
		// create 'Delete Project' button
		deleteProjectBtn = new JButton("Delete Project", IconConstants.PROJECT_DELETE_ICON);
		deleteProjectBtn.setRolloverIcon(IconConstants.PROJECT_DELETE_ROLLOVER_ICON);
		deleteProjectBtn.setPressedIcon(IconConstants.PROJECT_DELETE_PRESSED_ICON);
		deleteProjectBtn.setMargin(buttonInsets);
		deleteProjectBtn.setEnabled(false);
		
		deleteProjectBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				int choice = JOptionPane.showConfirmDialog(clientFrame, "<html>Are you sure you want to delete the selected project?<br>Changes are irreversible.</html>", "Delete Project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

				if (choice == JOptionPane.OK_OPTION) {
					try {
						selectedProject.delete();
						selectedProject = null;
						selectedExperiment = null;
						refreshProjectTable();

						// Disable buttons
						modifyProjectBtn.setEnabled(false);
						deleteProjectBtn.setEnabled(false);
						addExperimentBtn.setEnabled(false);
						modifyExperimentBtn.setEnabled(false);
						deleteExperimentBtn.setEnabled(false);

						// Reset textfields
						clientFrame.getStatusBar().getProjectTextField().setText("None");
						clientFrame.getStatusBar().getExperimentTextField().setText("None");

					} catch (Exception e) {
						e.printStackTrace();
						
					}
				}
			}
		});
		
		// add buttons to panel
		projectBtnPnl.add(addProjectBtn,CC.xy(1, 2));
		projectBtnPnl.add(modifyProjectBtn,CC.xy(3, 2));
		projectBtnPnl.add(deleteProjectBtn,CC.xy(5, 2));
		
		return projectBtnPnl;
	}
	
	/**
	 * Creates and returns the table view of experiments.
	 * @return the experiments table wrapped in a scroll pane
	 */
	private JScrollPane setupExperimentTable() {
		// Table for projects
		experimentTbl = new JXTable(new DefaultTableModel() { 
			{
				setColumnIdentifiers(new Object[] { "#", "Experiment Title", "Creation Date"});
			}

			public boolean isCellEditable(int row, int col) {
				return false;
			}

			public Class<?> getColumnClass(int col) {
				switch (col) {
					case 0:
						return Integer.class;
					case 1:
						return String.class;
					case 2:
						return Date.class;
					default:
						return getValueAt(0, col).getClass();
				}
			}
			
			@Override
			public Object getValueAt(int row, int column) {
				Object value = super.getValueAt(row, 0);
				if (value instanceof AbstractExperiment) {
					AbstractExperiment experiment = (AbstractExperiment) value;
					switch (column) {
						case 0:
							return experiment.getID();
						case 1:
							return experiment.getTitle();
						case 2:
							return experiment.getCreationDate();
						default:
							return value;
					}
				}
				return null;
			}
		});
		
		// Selection model for the list: Select one entry of the table only
		experimentTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// Add Selection Listener
		experimentTbl.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent evt) {
				// clear cached results
				if (selectedExperiment != null) {
					selectedExperiment.clearSearchResult();
				}
				
				int selRow = experimentTbl.getSelectedRow();
				if (selRow != -1) {
					selRow = experimentTbl.convertRowIndexToModel(selRow);
					selectedExperiment = (AbstractExperiment) experimentTbl.getModel().getValueAt(selRow, -1);
				}
				
				this.updateComponents();
			}

			/**
			 * Updates states of various UI components in response to table selection.
			 */
			private void updateComponents() {
				ClientFrame clientFrame = ClientFrame.getInstance();
				
				// update text fields
				String title = (selectedExperiment != null) ? selectedExperiment.getTitle() : "None";
				clientFrame.getStatusBar().getExperimentTextField().setText(title);
				
				// enable buttons
				modifyExperimentBtn.setEnabled(true);
				deleteExperimentBtn.setEnabled(true);
				
				clientFrame.getResultsPanel().setProcessingEnabled(false);
				
				// check whether the selected experiment has any search results associated with it
				boolean hasResult = (selectedExperiment != null) ? selectedExperiment.hasSearchResult() : false;
				
				// enable input and results tabs
				if (!clientFrame.isViewer()) {
					clientFrame.setTabEnabledAt(ClientFrame.INDEX_INPUT_PANEL, true);
				}
				clientFrame.setTabEnabledAt(ClientFrame.INDEX_RESULTS_PANEL, hasResult);
				
				// enable navigation buttons
				skipBtn.setEnabled(hasResult);
				skipBtn.setToolTipText((hasResult) ? null : "No searches have been performed under this experiment yet.");
				nextBtn.setEnabled(true);
				
			}
		});
	
		// Set the column layout
		TableConfig.setColumnWidths(experimentTbl, new double[] { 1, 10, 4 });
	
		// Add nice striping effect
		experimentTbl.addHighlighter(TableConfig.getSimpleStriping());
	
		// Modify column control widget
		TableConfig.configureColumnControl(experimentTbl);
		
		// Add experiment table to scroll pane
		JScrollPane experimentTblScp = new JScrollPane(experimentTbl);
		experimentTblScp.setPreferredSize(new Dimension(450, 350));
		experimentTblScp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		return experimentTblScp;
	}

	/**
	 * Creates and configures the experiment management buttons.
	 */
	private JPanel setupExperimentButtonPanel() {
		final ClientFrame clientFrame = ClientFrame.getInstance();
		
		// create button panel
		FormLayout layout = new FormLayout("p:g, 5dlu, p:g, 5dlu, p:g", "0dlu, p, 0dlu");
		layout.setColumnGroups(new int[][] { { 1, 3, 5 } });
		JPanel experimentBtnPnl = new JPanel(layout);
		
		Insets buttonInsets = new Insets(5, 4, 5, 4);
		
		// create 'Add Experiment' button
		addExperimentBtn = new JButton("Add Experiment", IconConstants.EXPERIMENT_ADD_ICON);
		addExperimentBtn.setRolloverIcon(IconConstants.EXPERIMENT_ADD_ROLLOVER_ICON);
		addExperimentBtn.setPressedIcon(IconConstants.EXPERIMENT_ADD_PRESSED_ICON);
		addExperimentBtn.setMargin(buttonInsets);
		addExperimentBtn.setEnabled(false);
		
		addExperimentBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				// pre-create experiment using current project as parent
				AbstractExperiment experiment;
				if (Client.isViewer()) {
					experiment = new FileExperiment();
				} else {
					experiment = new DatabaseExperiment();
				}
				experiment.setProject(selectedProject);
				
				// special case for file-based experiments
				if (Client.isViewer()) {
					// show result file chooser
					JFileChooser chooser = new JFileChooser();
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setFileFilter(Constants.MPA_FILE_FILTER);
					chooser.setDialogTitle("Select Result File");
					int result = chooser.showOpenDialog(clientFrame);
					if (result == JFileChooser.APPROVE_OPTION) {
						File resultFile = chooser.getSelectedFile();
						((FileExperiment) experiment).setResultFile(resultFile);
						
						// check whether spectrum file with same name exists
						String filename = resultFile.getName();
						File spectrumFile = new File(resultFile.getParentFile(),
								filename.substring(0, filename.lastIndexOf('.')) + ".mgf");
						if (!spectrumFile.exists()) {
							// prompt for spectrum file selection
							chooser = new JFileChooser(resultFile);
							chooser.setAcceptAllFileFilterUsed(false);
							chooser.setFileFilter(Constants.MGF_FILE_FILTER);
							chooser.setDialogTitle("Select Spectrum File");
							result = chooser.showOpenDialog(clientFrame);
							if (result == JFileChooser.APPROVE_OPTION) {
								spectrumFile = chooser.getSelectedFile();
							} else {
								// we'll allow having no spectrum file, for now
							}
						}
						((FileExperiment) experiment).setSpectrumFile(spectrumFile);
					} else {
						// no result file selected, abort
						return;
					}
				}
				
				// show experiment creation dialog
				GeneralDialog dialog = new GeneralDialog(DialogType.NEW_EXPERIMENT, experiment);
				int res = dialog.showDialog();
				
				// check if new experiment has been created (setting a title is mandatory)
				if (res == GeneralDialog.RESULT_SAVED) {
					// update experiment selection
					((DefaultTableModel) experimentTbl.getModel()).addRow(new Object[] { experiment });
					setSelectedExperiment(experiment);
				}
			}
		});
		
		// create 'View/Edit Details' button
		modifyExperimentBtn = new JButton("View/Edit Details", IconConstants.EXPERIMENT_VIEW_ICON);
		modifyExperimentBtn.setRolloverIcon(IconConstants.EXPERIMENT_VIEW_ROLLOVER_ICON);
		modifyExperimentBtn.setPressedIcon(IconConstants.EXPERIMENT_VIEW_PRESSED_ICON);
		modifyExperimentBtn.setMargin(buttonInsets);
		modifyExperimentBtn.setEnabled(false);
		
		modifyExperimentBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				GeneralDialog dialog = new GeneralDialog(DialogType.MODIFY_EXPERIMENT, selectedExperiment);
				int result = dialog.showDialog();
				if (result == GeneralDialog.RESULT_SAVED) {
					refreshExperimentTable(selectedProject);
					setSelectedExperiment(selectedExperiment);
				}
			}
		});
		
		// create 'Delete Experiment' button
		deleteExperimentBtn = new JButton("Delete Experiment", IconConstants.EXPERIMENT_DELETE_ICON);
		deleteExperimentBtn.setRolloverIcon(IconConstants.EXPERIMENT_DELETE_ROLLOVER_ICON);
		deleteExperimentBtn.setPressedIcon(IconConstants.EXPERIMENT_DELETE_PRESSED_ICON);
		deleteExperimentBtn.setMargin(buttonInsets);
		deleteExperimentBtn.setEnabled(false);
		
		deleteExperimentBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				int choice = JOptionPane.showConfirmDialog(clientFrame, "<html>Are you sure you want to delete the selected experiment?<br>Changes are irreversible.</html>", "Delete Experiment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			
				if (choice == JOptionPane.OK_OPTION) {
					try {
						selectedExperiment.delete();
						selectedProject.getExperiments().remove(selectedExperiment);
						selectedExperiment = null;
						refreshExperimentTable(selectedProject);
						
						// Disable buttons
						modifyExperimentBtn.setEnabled(false);
						deleteExperimentBtn.setEnabled(false);

						// Reset textfields
						clientFrame.getStatusBar().getExperimentTextField().setText("None");
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		// add buttons to panel
		experimentBtnPnl.add(addExperimentBtn,CC.xy(1, 2));
		experimentBtnPnl.add(modifyExperimentBtn,CC.xy(3, 2));
		experimentBtnPnl.add(deleteExperimentBtn,CC.xy(5, 2));
		
		return experimentBtnPnl;
	}
	
	/**
	 * Clears and re-populates the project table.
	 * @throws SQLException if a database error occurs
	 */
	public void refreshProjectTable() {
		try {
			TableConfig.clearTable(projectTbl);
			TableConfig.clearTable(experimentTbl);
			
			projects = ProjectManager.getInstance().getProjects();
			
			for (AbstractProject project : projects) {
				((DefaultTableModel) projectTbl.getModel()).addRow(new Object[] { project });
			}
		} catch (Exception e) {
			JXErrorPane.showDialog(ClientFrame.getInstance(),
							new ErrorInfo("Severe Error", e.getMessage(), null, null, e, ErrorLevel.SEVERE, null));
		}
	}
	
	/**
	 * Returns the list of available projects.
	 * @return the projects
	 */
	public List<AbstractProject> getProjects() {
		return projects;
	}
	
	public AbstractProject getSelectedProject() {
		return selectedProject;
	}
	
	/**
	 * Selects the specified project in the projects table.
	 * @param project the project to select
	 */
	public void setSelectedProject(AbstractProject project) {
		if (project != null) {
			for (int row = 0; row < projectTbl.getRowCount(); row++) {
				AbstractProject p = (AbstractProject) projectTbl.getValueAt(row, -1);
				if (p.equals(project)) {
					projectTbl.getSelectionModel().setSelectionInterval(row, row);
					break;
				}
			}
		} else {
			projectTbl.clearSelection();
		}
	}
	
	/**
	 * Clears and re-populates the experiment table using the experiments of the
	 * specified project.
	 * @param project the project containing the experiments to populate the table with
	 */
	public void refreshExperimentTable(AbstractProject project) {
		TableConfig.clearTable(experimentTbl);
		
		for (AbstractExperiment experiment : project.getExperiments()) {
			((DefaultTableModel) experimentTbl.getModel()).addRow(new Object[] { experiment });
		}
	}
	
	/**
	 * Selects the specified project in the experiments table.
	 * @param experiment the experiment to select
	 */
	public void setSelectedExperiment(AbstractExperiment experiment) {
		if (experiment != null) {
			for (int row = 0; row < experimentTbl.getRowCount(); row++) {
				if (experimentTbl.getValueAt(row, -1).equals(experiment)) {
					experimentTbl.getSelectionModel().setSelectionInterval(row, row);
					break;
				}
			}
		} else {
			experimentTbl.clearSelection();
		}
	}
	
	/**
	 * Returns the selected experiment. Note this is distinct from the currently
	 * <i>loaded</i> experiment.
	 * @return the selected experiment
	 */
	public AbstractExperiment getSelectedExperiment() {
		return selectedExperiment;
	}
	
	/**
	 * Returns the currently loaded experiment. Note this is distinct from the
	 * currently <i>selected</i> experiment.
	 * @return the currently loaded experiment
	 */
	public AbstractExperiment getCurrentExperiment() {
		return currentExperiment;
	}
	
	/**
	 * Returns the current search result object.
	 * @return the current search result
	 */
	public DbSearchResult getSearchResult() {
		if (currentExperiment != null) {
			if (!currentExperiment.equals(selectedExperiment)) {
				// clear cached results
				currentExperiment.clearSearchResult();
				currentExperiment = selectedExperiment;
			}
			return currentExperiment.getSearchResult();
		} else if (selectedExperiment != null) {
			currentExperiment = selectedExperiment;
			return currentExperiment.getSearchResult();
		}
		return null;
	}
	
}
