/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * AnnotationEditor.java
 *
 * Created on 12-Aug-2011, 2:53:25 PM
 */

package ca.uwo.csd.ai.nlp.gui;

import ca.uwo.csd.ai.nlp.ling.ann.Annotator;
import ca.uwo.csd.ai.nlp.io.CONLL01TextReader;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.io.LPTextReader;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.io.TextReader;
import ca.uwo.csd.ai.nlp.ling.ann.CharniakParser;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseAnnotator;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.utils.TreeViewDialog;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class AnnotationEditor extends javax.swing.JFrame implements TableModelListener {

    private SerialAnnotator annotator;
    private TextReader textReader;
    private Text corpusText;
    private String columnNames[];
    private File corpusFile;
    private int sentIndex;
    private FileWriter writer;
    private Object data[][];    
    private FileWriter logWriter;
    private TreeViewDialog treeViewDialog;
    //private ParserAnnotator parserAnnotator;
    private CharniakParser parserAnnotator;
    private Markup markup;
    
    /** Creates new form AnnotationEditor */
    public AnnotationEditor() {
        initComponents();
        myInit();
    }

    private void myInit() {        
        annotator = new SerialAnnotator();
        initTable();
        sentIndex = -1;
        backButton.setEnabled(false);
        nextButton.setEnabled(false);
        try {
            logWriter = new FileWriter(".\\resource\\ml\\data\\discourse\\ann_editor_log.txt", true);
        } catch (IOException ex) {
            System.err.println("Can not open log writer..");
        }
        treeViewDialog = new TreeViewDialog(this);
        //parserAnnotator = new ParserAnnotator();
        parserAnnotator = new CharniakParser();
    }
    
    private void initTable() {
        String[] fieldNames = annotator.getFieldNames();
        this.initTable(fieldNames);
    }
    
    private void initTable(String[] fieldNames) {
        columnNames = new String[fieldNames.length + 1];
        columnNames[0] = "Word";
        System.arraycopy(fieldNames, 0, columnNames, 1, fieldNames.length);
    }
    
    private void loadFile(File file) {
        this.corpusFile = file;
        this.corpusText = textReader.read(file);
        this.sentIndex = 0;
        this.writer = null;
        if (corpusText.size() > 1) nextButton.setEnabled(true);
        /*for (Sentence s : corpusText) {
            s = annotator.annotate(s);
        }*/
        showAnnotation();
    }
    private void loadText(String text) {        
        this.corpusText = textReader.read(text);
        this.sentIndex = 0;
        this.writer = null;
        if (corpusText.size() > 1) nextButton.setEnabled(true);
        /*for (Sentence s : corpusText) {
            s = annotator.annotate(s);
        }*/
        showAnnotation();
    }
    private void openWriter() {
        JFileChooser fileChooser = new JFileChooser(".//resource//ml//data");
        fileChooser.showDialog(this, "Save");
        File file = fileChooser.getSelectedFile();
        System.out.println(file.getAbsolutePath());
        if (file != null) {
            try {
                writer = new FileWriter(file);
            } catch (IOException ex) {
                System.out.println("Can create file: " + file.getAbsolutePath());
                System.err.println(ex.getCause());
                writer = null;
            }
        }
    }
    private void showAnnotation() {
        Sentence s = corpusText.get(sentIndex);
        try {
            s = annotator.annotate(s);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Exception occured", JOptionPane.ERROR_MESSAGE);
        }
        updateTable(s);
        updateTextPane(s);
    }

    private void updateTextPane(Sentence s) {
        lineTextPane.setContentType("text/html");
        lineTextPane.setText(markup.markup(s));
    }
    private void updateTable(Sentence s) {
        lineTextPane.setText(s.toString());
        data = new Object[s.size()][columnNames.length];
        for (int i = 0; i < s.size(); i++) {
            data[i][0] = s.get(i).word();
            for (int j = 1; j < columnNames.length; j++) {
                data[i][j] = s.get(i).getTag(columnNames[j]);
            }
        }
        annotationTable.setModel(new DefaultTableModel(data, columnNames));
        annotationTable.getModel().addTableModelListener(this);

        /*for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].matches("CLS_BN_S|CLS_BN_E")) {
                annotationTable.getColumnModel().getColumn(i).setCellRenderer(new MyColoredTableCellRenderer("X"));
            }
        }*/
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        annotationTable = new javax.swing.JTable();
        backButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        lineTextPane = new javax.swing.JTextPane();
        nextButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        logButton = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        openAnnMenuItem = new javax.swing.JMenuItem();
        openConnArgMenuItem = new javax.swing.JMenuItem();
        inputMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        showTreeMenuItem = new javax.swing.JMenuItem();
        showDepGraphMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ca.uwo.csd.ai.nlp");

        annotationTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        annotationTable.setColumnSelectionAllowed(true);
        jScrollPane2.setViewportView(annotationTable);
        annotationTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        jTabbedPane1.addTab("Annotation", jScrollPane2);

        backButton.setMnemonic('b');
        backButton.setText("Back");
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        jScrollPane1.setViewportView(lineTextPane);

        nextButton.setMnemonic('n');
        nextButton.setText("Next");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        saveButton.setMnemonic('s');
        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        logButton.setMnemonic('l');
        logButton.setText("Log");
        logButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logButtonActionPerformed(evt);
            }
        });

        jMenu1.setText("File");

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setText("Open Raw text");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openMenuItem);

        openAnnMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        openAnnMenuItem.setText("Open Annotated Text");
        openAnnMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openAnnMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openAnnMenuItem);

        openConnArgMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        openConnArgMenuItem.setText("Open Annotated Arg Text");
        openConnArgMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openConnArgMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openConnArgMenuItem);

        inputMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        inputMenuItem.setText("Input");
        inputMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(inputMenuItem);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exitMenuItem);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Show");

        showTreeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        showTreeMenuItem.setText("Phrase Structure");
        showTreeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showTreeMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(showTreeMenuItem);

        showDepGraphMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        showDepGraphMenuItem.setText("Dependency Graph");
        showDepGraphMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showDepGraphMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(showDepGraphMenuItem);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(backButton, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(201, 201, 201)
                        .addComponent(saveButton)
                        .addGap(43, 43, 43)
                        .addComponent(logButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 199, Short.MAX_VALUE)
                        .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nextButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(backButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(saveButton)
                    .addComponent(logButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
        if (this.sentIndex < (corpusText.size() - 1)) {
            this.sentIndex++;
            backButton.setEnabled(true);
            showAnnotation();
        }
        if (this.sentIndex == corpusText.size()) nextButton.setEnabled(false);
    }//GEN-LAST:event_nextButtonActionPerformed

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        if (this.sentIndex > 0) {
            this.sentIndex--;
            nextButton.setEnabled(true);
            showAnnotation();
        }
        if (this.sentIndex == 0) backButton.setEnabled(false);
    }//GEN-LAST:event_backButtonActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        if (writer == null) {
            openWriter();
        } 
        if (writer != null) writeAnnotation(new int[]{0, 2, 3, 6, 7});
        else System.out.println("Writer null");
        
    }//GEN-LAST:event_saveButtonActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        cleanup();
        this.dispose();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void openAnnMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openAnnMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser(".//resource//ml//data");
        fileChooser.showDialog(this, "Open Annotated Text");
        File file = fileChooser.getSelectedFile();
        System.out.println(file.getAbsolutePath());
        if (file != null) {
            markup = new ClauseMarkup();
            annotator = new SerialAnnotator();
            textReader = new CONLL01TextReader(true);
            initTable(new String[]{"POS", "CHUNK", "CLS_BN_S", "CLS_BN_E"});
            loadFile(file);
        }
    }//GEN-LAST:event_openAnnMenuItemActionPerformed

    private void logButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logButtonActionPerformed
        if (logWriter != null) {
            String comment = JOptionPane.showInputDialog(this, "Enter a comment:", "Comment", JOptionPane.PLAIN_MESSAGE);
            try {
                String tmp = lineTextPane.getText().replaceAll("<[^<]+>", "");
                tmp = tmp.replaceAll("\n", "");
                tmp = tmp.replaceAll("\\s+", " ");
                logWriter.write(tmp + "\n");
                if (comment != null) logWriter.write("###\n"+comment+"###\n");
                logWriter.flush();
            } catch (IOException ex) {
                Logger.getLogger(AnnotationEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_logButtonActionPerformed

    private void showTreeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showTreeMenuItemActionPerformed
        Sentence s = corpusText.get(this.sentIndex);
        s = parserAnnotator.annotate(s);
        treeViewDialog.setTree(s.getParseTree());
        treeViewDialog.repaint();
        treeViewDialog.setVisible(true);
    }//GEN-LAST:event_showTreeMenuItemActionPerformed

    private void openConnArgMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openConnArgMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser(".//resource//ml//data");
        fileChooser.showDialog(this, "Open Connective Argument Annotated Text");
        File file = fileChooser.getSelectedFile();
        System.out.println(file.getAbsolutePath());
        if (file != null) {
            markup = new ClauseArgMarkup();
            annotator = new SerialAnnotator();
            annotator.add(new GeniaTagger());            
            annotator.add(new ClauseBoundaryAnnotator(false));
            annotator.add(new ClauseAnnotator());
            annotator.add(new DiscourseMarkerAnnotator(false));
            textReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"word", "CONN", "ARG1S", "ARG1E", "ARG2S", "ARG2E"});
            //initTable(new String[]{"POS", "CHUNK", "DIS_CON", "DIS_CON_SCP","DIS_CON_TYP","CLS_ANN","CLS_S#","CLS_E#","Arg"});
            initTable(new String[]{"POS", "CHUNK", "DIS_CON","CLS_ANN","CLS_S#","CLS_E#"});
            loadFile(file);
        }
    }//GEN-LAST:event_openConnArgMenuItemActionPerformed

    private void showDepGraphMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showDepGraphMenuItemActionPerformed
        Sentence s = corpusText.get(this.sentIndex);
        if (!s.isAnnotatedBy("PARSED")) s = parserAnnotator.annotate(s);
        Tree t = s.getParseTree();
        EnglishGrammaticalStructure egs = new EnglishGrammaticalStructure(t);
        Collection<TypedDependency> tld = egs.typedDependenciesCCprocessed(true);
        try {
            String path = System.getProperty("java.io.tmpdir") + "tmp.png";
            com.chaoticity.dependensee.Main.writeImage(t, tld, "tmp.png");
            Desktop.getDesktop().open(new File("tmp.png"));
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }//GEN-LAST:event_showDepGraphMenuItemActionPerformed

    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser(".//resource//ml//data");
        fileChooser.showDialog(this, "Open Raw Text");
        File file = fileChooser.getSelectedFile();
        System.out.println(file.getAbsolutePath());
        if (file != null) {
            markup = new ClauseAnnotationMarkup();
            annotator = new SerialAnnotator();
            annotator.add(new GeniaTagger());            
            annotator.add(new ClauseBoundaryAnnotator(false));
            annotator.add(new ClauseAnnotator());
            annotator.add(new DiscourseMarkerAnnotator(false));
            textReader = new LPTextReader();
            initTable(new String[]{"POS", "CHUNK", "DIS_CON", "CLS_BN_S", "CLS_BN_E", "CLS_ANN"});
            loadFile(file);
        }
    }//GEN-LAST:event_openMenuItemActionPerformed

    private void inputMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputMenuItemActionPerformed
        String msg = JOptionPane.showInputDialog(this, "Enter one or more lines:", "Input", JOptionPane.PLAIN_MESSAGE);
        if (msg != null) {
            markup = new ClauseAnnotationMarkup();
            annotator = new SerialAnnotator();
            annotator.add(new GeniaTagger());            
            annotator.add(new ClauseBoundaryAnnotator(false));
            annotator.add(new ClauseAnnotator());
            annotator.add(new DiscourseMarkerAnnotator(false));
            textReader = new LPTextReader();
            initTable(new String[]{"POS", "CHUNK", "DIS_CON", "CLS_BN_S", "CLS_BN_E", "CLS_ANN"});
            loadText(msg);
        }
    }//GEN-LAST:event_inputMenuItemActionPerformed

    private void cleanup() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ex) {
                System.err.println(ex.getCause());
            }
        }
        if (logWriter != null) {
            try {
                logWriter.close();
            } catch (IOException ex) {
                System.err.println(ex.getCause());
            }
        }
    }
    private void writeAnnotation(int columns[]) {
        try {
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < columns.length; j++) {
                    if (j != 0) writer.write(" ");
                    writer.write((String) data[i][columns[j]]);
                    //System.out.println((String) data[i][columns[j]]);
                }
                writer.write("\n");
            }
            writer.write("\n");
            writer.flush();
        } catch (IOException ex) {
            System.out.println("Can not write to output file:" + ex.getCause());
        }
    }
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AnnotationEditor().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable annotationTable;
    private javax.swing.JButton backButton;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem inputMenuItem;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextPane lineTextPane;
    private javax.swing.JButton logButton;
    private javax.swing.JButton nextButton;
    private javax.swing.JMenuItem openAnnMenuItem;
    private javax.swing.JMenuItem openConnArgMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JButton saveButton;
    private javax.swing.JMenuItem showDepGraphMenuItem;
    private javax.swing.JMenuItem showTreeMenuItem;
    // End of variables declaration//GEN-END:variables

    public void tableChanged(TableModelEvent e) {
        /*System.out.println("Column: " + e.getColumn());
        System.out.println("First row: " + e.getFirstRow());
        System.out.println("Source: " + e.getSource());
        System.out.println("Type: " + e.getType());
        TableModel tm = (TableModel) e.getSource();
        System.out.println("Value: " + tm.getValueAt(e.getFirstRow(), e.getColumn()));
        System.out.println("obj: " + data[e.getFirstRow()][e.getColumn()]);*/
        if (e.getColumn() != TableModelEvent.ALL_COLUMNS) {
            TableModel tm = (TableModel) e.getSource();
            data[e.getFirstRow()][e.getColumn()] = tm.getValueAt(e.getFirstRow(), e.getColumn());
        }
    }

}
/*class MyColoredTableCellRenderer
    extends DefaultTableCellRenderer
{
    String defaultEntry;
    public MyColoredTableCellRenderer(String defaultEntry) {
        this.defaultEntry = defaultEntry;
    }

    @Override
    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean selected, boolean focused, int row, int column)
    {
        Component cell = super.getTableCellRendererComponent
         (table, value, selected, focused, row, column);

        String content = (String) value;
        if (!content.equals(defaultEntry)) cell.setBackground(Color.LIGHT_GRAY);
        else cell.setBackground(null);


        return cell;
    }
}*/

interface Markup {
    public String markup(Sentence s);
}

class ClauseMarkup implements Markup {

    public String markup(Sentence s) {
        String tmp = "<p style=\"font-size: 20pt\">";
        int lCount = 0;
        int rCount = 0;
        for (TokWord word : s) {
            if (word.getTag("CLS_BN_S").equals("S")) {
                lCount++;
            }
            if (word.getTag("CLS_BN_E").equals("E")) {
                rCount++;
            }
        }
        for (int i = 0; i < (rCount - lCount); i++) {
            tmp += "<b><span style=\"color:green\">(</span></b>";
        }
        for (TokWord word : s) {
            if (word.getTag("CLS_BN_S").equals("S")) {
                tmp += "<b><span style=\"color:red\">(</span></b>";
            }
            /*if (word.getTag("DIS_CON").equals("O")) tmp += word.word();
            else tmp += "<u><b>" + word.word() + "</b></u>";*/
            tmp += word.word();
            if (word.getTag("CLS_BN_E").equals("E")) {
                tmp += "<b><span style=\"color:red\">)</span></b>";
            }
            tmp += " ";
        }
        for (int i = 0; i < (lCount - rCount); i++) {
            tmp += "<b><span style=\"color:green\">)</span></b>";
        }
        tmp += "</p>";

        return tmp;
    }

}

class ClauseAnnotationMarkup implements Markup {

    public String markup(Sentence s) {
        String tmp = "<p style=\"font-size: 20pt\">";
        
        for (TokWord word : s) {
            String tag = word.getTag("CLS_ANN");
            if (tag != null && !tag.equals("*")) {
                int lCount = 0;
                int rCount = 0;
                for (char ch : tag.toCharArray()) {
                    if (ch == '(') {
                        lCount++;
                    } else if (ch == ')') {
                        rCount++;
                    }
                }
                for (int i = 0; i < lCount; i++) {
                    tmp += "<b><span style=\"color:red\">(</span></b>";
                }
                tmp += word.word();
                for (int i = 0; i < rCount; i++) {
                    tmp += "<b><span style=\"color:red\">)</span></b>";
                }
            } else {
                tmp += word.word();
            }
            tmp += " ";
        }        
        tmp += "</p>";

        return tmp;
    }

}
class ClauseArgMarkup implements Markup {

    public String markup(Sentence s) {
        /*
        String tmp = "<p style=\"font-size: 20pt\">";
        int lCount = 0;
        int rCount = 0;
        for (TokWord word : s) {
            if (word.getTag("CLS_BN_S").equals("S")) {
                lCount++;
            }
            if (word.getTag("CLS_BN_E").equals("E")) {
                rCount++;
            }
        }
        for (int i = 0; i < (rCount - lCount); i++) {
            tmp += "<b><span style=\"color:green\">(</span></b>";
        }
        for (TokWord word : s) {
            if (word.getTag("CLS_BN_S").equals("S")) {
                tmp += "<b><span style=\"color:red\">(</span></b>";
            }

            if (word.getTag("Arg") != null) {
                if (word.getTag("Arg").equals("-1")) tmp += "<span style=\"background-color:#CCCCFF\">";
                else if (word.getTag("Arg").equals("-2")) tmp += "<span style=\"background-color:#FFDB94\">";
            }
            
            tmp += word.word();

            if (word.getTag("Arg") != null && (word.getTag("Arg").equals("1") || word.getTag("Arg").equals("2"))) {
                tmp += "</span>";
            }
            
            if (word.getTag("CLS_BN_E").equals("E")) {
                tmp += "<b><span style=\"color:red\">)</span></b>";
            }
            tmp += " ";
        }
        
        for (int i = 0; i < (lCount - rCount); i++) {
            tmp += "<b><span style=\"color:green\">)</span></b>";
        }
        tmp += "</p>";

        return tmp;*/
        
        String colors[] = new String[]{"#FF0000","#DD1100", "#CC2211", "#AA3322", "#884433","#665544","446655", "227766"};
        String tmp = "<p style=\"font-size: 20pt\">";
        
        for (TokWord word : s) {

            //set background for argument span
            if (!word.getTag("ARG1S").equals("0")) {
                tmp += "<span style=\"background-color:#CCCCFF\">";                
            }
            if (!word.getTag("ARG2S").equals("0")) {
                tmp += "<span style=\"background-color:#FFDB94\">";
            }
            String tag = word.getTag("CLS_ANN");
            if (tag != null && !tag.equals("*")) {
                String sNum[] = word.getTag("CLS_S#").split(":");
                String eNum[] = word.getTag("CLS_E#").split(":");

                for (int i = 0; i < sNum.length; i++) {
                    int x = Integer.parseInt(sNum[i]);
                    if (x == 0) continue;
                    String color = colors[x%colors.length];
                    tmp += "<b><span style=\"color:"+color+"\">(</span></b>";
                }
                
                
                tmp += word.word();                
                
                for (int i = 0; i < eNum.length; i++) {
                    int x = Integer.parseInt(eNum[i]);
                    if (x == 0) continue;
                    String color = colors[x%colors.length];
                    tmp += "<b><span style=\"color:"+color+"\">)</span></b>";
                }
            } else {
                tmp += word.word();
            }
            tmp += " ";            
            if (!word.getTag("ARG1E").equals("0") || !word.getTag("ARG2E").equals("0")) {
                tmp += "</span>";
            }
        }
        tmp += "</p>";

        return tmp;
    }

}