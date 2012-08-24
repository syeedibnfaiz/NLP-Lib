/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * AnnotationViewer.java
 *
 * Created on 11-Aug-2011, 10:05:35 AM
 */

package ca.uwo.csd.ai.nlp.gui;

import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.utils.TreeViewDialog;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author tonatuni
 */
public class AnnotationViewer extends javax.swing.JFrame {

    SerialAnnotator annotator;
    LPSentReader sentReader;
    TreeViewDialog treeViewDialog;
    Sentence sentence;
    String columnNames[];

    /** Creates new form AnnotationViewer */
    public AnnotationViewer() {
        initComponents();
        myInit();
    }

    private void myInit() {
        annotator = new SerialAnnotator();
        annotator.add(new GeniaTagger());
        annotator.add(new ParserAnnotator());
        annotator.add(new DiscourseMarkerAnnotator(true));
        annotator.add(new ClauseBoundaryAnnotator(true));

        sentReader = new LPSentReader();

        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        queryTextArea.getInputMap(JComponent.WHEN_FOCUSED).put(enter, "pressedReturn");

        AbstractAction testAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                updateInfo(queryTextArea.getText());
            }
        };
        queryTextArea.getActionMap().put("pressedReturn", testAction);

        treeViewDialog = new TreeViewDialog(this);        

        initTable();
    }
    
    private void initTable() {
        String[] fieldNames = annotator.getFieldNames();
        columnNames = new String[fieldNames.length + 1];
        columnNames[0] = "Word";
        for (int i = 0; i < fieldNames.length; i++) {
            columnNames[i + 1] = fieldNames[i];
        }
    }
    private void updateInfo(String text) {
        sentence = sentReader.read(text);
        sentence = annotator.annotate(sentence);
        updateBasicInfo(sentence);

        updateClauseView(sentence);                

    }
    private void updateBasicInfo(Sentence s) {
        //String columnNames[] = {"Word", "Base", "POS", "Chunk", "D-Sense", "D-Type", "Clause-S", "Clause-E"};
        
        Object data[][] = new Object[s.size()][columnNames.length];
        for (int i = 0; i < s.size(); i++) {
            data[i][0] = s.get(i).word();            
            for (int j = 1; j < columnNames.length; j++) {
                data[i][j] = s.get(i).getTag(columnNames[j]);
            }
        }
        basicInfoTable.setModel(new DefaultTableModel(data, columnNames));
        /*basicInfoTable.getColumnModel().getColumn(4).setCellRenderer(new MyColoredTableCellRenderer("O"));
        basicInfoTable.getColumnModel().getColumn(5).setCellRenderer(new MyColoredTableCellRenderer("O"));
        basicInfoTable.getColumnModel().getColumn(6).setCellRenderer(new MyColoredTableCellRenderer("X"));
        basicInfoTable.getColumnModel().getColumn(7).setCellRenderer(new MyColoredTableCellRenderer("X"));
         */
        //adding some cell renderer, hardcoded
        /*for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].matches("DIS_CON|DIS_CON_TYP|NE")) {
                basicInfoTable.getColumnModel().getColumn(i).setCellRenderer(new MyColoredTableCellRenderer("O"));
            } else if (columnNames[i].matches("CLS_BN_S|CLS_BN_E")) {
                basicInfoTable.getColumnModel().getColumn(i).setCellRenderer(new MyColoredTableCellRenderer("X"));
            }
        }*/
        motherTabbedPane.repaint();
    }

    private void updateClauseView(Sentence s) {
        String tmp = "<p style=\"font-size: 24pt\">";
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
            if (word.getTag("DIS_CON").equals("O")) tmp += word.word();
            else tmp += "<u><b>" + word.word() + "</b></u>";

            if (word.getTag("CLS_BN_E").equals("E")) {
                tmp += "<b><span style=\"color:red\">)</span></b>";
            }
            tmp += " ";
        }
        for (int i = 0; i < (lCount - rCount); i++) {
            tmp += "<b><span style=\"color:green\">)</span></b>";
        }
        tmp += "</p>";
        clauseTextPane.setContentType("text/html");
        clauseTextPane.setText(tmp);
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        motherTabbedPane = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        basicInfoTable = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        clauseTextPane = new javax.swing.JTextPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        queryTextArea = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        basicInfoTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Word", "Base", "POS", "CHUNK"
            }
        ));
        jScrollPane2.setViewportView(basicInfoTable);

        motherTabbedPane.addTab("Basic Info", jScrollPane2);

        clauseTextPane.setFont(new java.awt.Font("Times New Roman", 0, 24)); // NOI18N
        jScrollPane3.setViewportView(clauseTextPane);

        motherTabbedPane.addTab("Clause View", jScrollPane3);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        queryTextArea.setColumns(20);
        queryTextArea.setLineWrap(true);
        queryTextArea.setRows(5);
        jScrollPane1.setViewportView(queryTextArea);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 695, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
                .addContainerGap())
        );

        jMenu1.setText("Show");

        jMenuItem1.setText("Phrase Structure");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Help");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(motherTabbedPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 727, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(motherTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        treeViewDialog.setTree(sentence.getParseTree());
        treeViewDialog.repaint();
        treeViewDialog.setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AnnotationViewer().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable basicInfoTable;
    private javax.swing.JTextPane clauseTextPane;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane motherTabbedPane;
    private javax.swing.JTextArea queryTextArea;
    // End of variables declaration//GEN-END:variables

}
class ColoredTableCellRenderer
    extends DefaultTableCellRenderer
{
    String defaultEntry;
    public ColoredTableCellRenderer(String defaultEntry) {
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
}